package com.peakskills.player;

import com.google.gson.*;
import com.peakskills.PeakLog;
import com.peakskills.PeakSkills;
import com.peakskills.collection.CollectionRegistry;
import com.peakskills.collection.CollectionType;
import com.peakskills.fishing.FishingDepth;
import com.peakskills.fishing.FishingMood;
import com.peakskills.fishing.FishingOutcomeCategory;
import com.peakskills.pet.PetInstance;
import com.peakskills.pet.PetRarity;
import com.peakskills.pet.PetType;
import com.peakskills.skill.Skill;
import com.peakskills.stat.StatManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;


import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private static Path dataDir;
    private static MinecraftServer server;

    public static MinecraftServer getServer() { return server; }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PlayerDataManager.server = server;
            dataDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("peakskills").resolve("players");
            try {
                Files.createDirectories(dataDir);
            } catch (IOException e) {
                PeakLog.error("Failed to create player data directory", e);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID uuid = handler.player.getUUID();
            cache.put(uuid, load(uuid));
            StatManager.applyStats(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUUID();
            StatManager.removeStats(handler.player);
            PlayerData data = cache.remove(uuid);
            if (data != null) save(data);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> saveAll());
    }

    // --- Public API ---

    public static PlayerData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, PlayerDataManager::load);
    }

    public static void saveAll() {
        cache.values().forEach(PlayerDataManager::save);
    }

    /**
     * Returns players sorted by total XP descending. Includes both online (cached)
     * and offline (JSON on disk) players, limited to {@code limit} entries.
     */
    public static List<Map.Entry<UUID, Long>> getLeaderboard(int limit) {
        Map<UUID, Long> scores = new java.util.HashMap<>();

        // Online players from cache
        cache.forEach((uuid, data) -> scores.put(uuid, totalXp(data)));

        // Offline players from disk
        if (dataDir != null) {
            try {
                java.nio.file.Files.list(dataDir)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(file -> {
                        try {
                            String name = file.getFileName().toString();
                            UUID uuid = UUID.fromString(name.substring(0, name.length() - 5));
                            if (scores.containsKey(uuid)) return; // already counted online
                            try (java.io.Reader r = java.nio.file.Files.newBufferedReader(file)) {
                                JsonObject json = GSON.fromJson(r, JsonObject.class);
                                scores.put(uuid, totalXp(fromJson(uuid, json)));
                            }
                        } catch (Exception ignored) {}
                    });
            } catch (IOException ignored) {}
        }

        return scores.entrySet().stream()
            .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
            .limit(limit)
            .toList();
    }

    /** Returns players sorted by combined skill level descending. */
    public static List<Map.Entry<UUID, Integer>> getLeaderboardByLevel(int limit) {
        Map<UUID, Integer> scores = new java.util.HashMap<>();
        cache.forEach((uuid, data) -> scores.put(uuid, data.getTotalLevel()));
        if (dataDir != null) {
            try {
                java.nio.file.Files.list(dataDir)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(file -> {
                        try {
                            String name = file.getFileName().toString();
                            UUID uuid = UUID.fromString(name.substring(0, name.length() - 5));
                            if (scores.containsKey(uuid)) return;
                            try (java.io.Reader r = java.nio.file.Files.newBufferedReader(file)) {
                                JsonObject json = GSON.fromJson(r, JsonObject.class);
                                scores.put(uuid, fromJson(uuid, json).getTotalLevel());
                            }
                        } catch (Exception ignored) {}
                    });
            } catch (IOException ignored) {}
        }
        return scores.entrySet().stream()
            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
            .limit(limit)
            .toList();
    }

    /** Returns all players sorted by a single skill's level descending. */
    public static List<Map.Entry<UUID, Integer>> getSkillLeaderboard(com.peakskills.skill.Skill skill) {
        Map<UUID, Integer> scores = new java.util.HashMap<>();
        cache.forEach((uuid, data) -> scores.put(uuid, data.getLevel(skill)));
        if (dataDir != null) {
            try {
                java.nio.file.Files.list(dataDir)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(file -> {
                        try {
                            String name = file.getFileName().toString();
                            UUID uuid = UUID.fromString(name.substring(0, name.length() - 5));
                            if (scores.containsKey(uuid)) return;
                            try (java.io.Reader r = java.nio.file.Files.newBufferedReader(file)) {
                                JsonObject json = GSON.fromJson(r, JsonObject.class);
                                scores.put(uuid, fromJson(uuid, json).getLevel(skill));
                            }
                        } catch (Exception ignored) {}
                    });
            } catch (IOException ignored) {}
        }
        return scores.entrySet().stream()
            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
            .toList();
    }

    private static long totalXp(PlayerData data) {
        long total = 0;
        for (Skill skill : Skill.values()) total += data.getXp(skill);
        return total;
    }

    // --- IO ---

    private static PlayerData load(UUID uuid) {
        if (dataDir == null) return new PlayerData(uuid);
        Path file = dataDir.resolve(uuid + ".json");
        if (!Files.exists(file)) return new PlayerData(uuid);

        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            return fromJson(uuid, json);
        } catch (IOException e) {
            PeakLog.error("Failed to load data for {}", uuid, e);
            return new PlayerData(uuid);
        }
    }

    private static void save(PlayerData data) {
        if (dataDir == null) return;
        Path file = dataDir.resolve(data.getUuid() + ".json");

        try {
            writeJsonAtomic(file, toJson(data));
        } catch (IOException e) {
            PeakLog.error("Failed to save data for {}", data.getUuid(), e);
        }
    }

    static void writeJsonAtomic(Path file, JsonObject json) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");

        boolean moved = false;
        try (Writer writer = Files.newBufferedWriter(temp)) {
            GSON.toJson(json, writer);
        }

        try {
            Files.move(temp, file,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
            moved = true;
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            moved = true;
        } finally {
            if (!moved) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException cleanupError) {
                    PeakLog.warn("Failed to clean up temp player data file {}: {}",
                        temp.getFileName(), cleanupError.getMessage());
                }
            }
        }
    }

    /** Replace a cached entry — used by PlayerDataFailsafe to restore from backup. */
    static void put(UUID uuid, PlayerData data) {
        cache.put(uuid, data);
    }

    /** Returns all UUIDs currently in the live cache (online players). */
    public static Set<UUID> getCachedUuids() {
        return cache.keySet();
    }

    // --- JSON conversion ---

    static JsonObject toJson(PlayerData data) {
        JsonObject root = new JsonObject();
        root.addProperty("uuid", data.getUuid().toString());
        root.addProperty("dataVersion", PlayerData.DATA_VERSION);

        JsonObject xpObj = new JsonObject();
        for (Skill skill : Skill.values()) {
            xpObj.addProperty(skill.name(), data.getXp(skill));
        }
        root.add("xp", xpObj);
        root.addProperty("mana", data.getMana());
        root.addProperty("maxMana", data.getMaxMana());
        root.addProperty("petsVisible", data.isPetsVisible());
        root.addProperty("limitBurstLevelUpSounds", data.shouldLimitBurstLevelUpSounds());

        // Pets
        JsonArray petsArr = new JsonArray();
        for (PetInstance pet : data.getPetRoster().getPets()) {
            JsonObject p = new JsonObject();
            p.addProperty("id",     pet.getId().toString());
            p.addProperty("type",   pet.getType().name());
            p.addProperty("rarity", pet.getRarity().name());
            p.addProperty("xp",     pet.getXp());
            p.addProperty("active", pet.isActive());
            petsArr.add(p);
        }
        root.add("pets", petsArr);

        // Collections
        JsonObject colCounts = new JsonObject();
        JsonObject colTiers  = new JsonObject();
        for (CollectionType type : CollectionType.values()) {
            long count = data.getCollections().getCount(type);
            if (count > 0) colCounts.addProperty(type.name(), count);
            int tier = data.getCollections().getUnlockedTier(type);
            if (tier > 0) colTiers.addProperty(type.name(), tier);
        }
        root.add("collections", colCounts);
        root.add("collectionTiers", colTiers);

        JsonObject journal = new JsonObject();
        journal.addProperty("totalCatches", data.getFishingJournal().getTotalCatches());
        journal.addProperty("totalItems", data.getFishingJournal().getTotalItems());
        JsonObject categories = new JsonObject();
        for (FishingOutcomeCategory category : FishingOutcomeCategory.values()) {
            long count = data.getFishingJournal().getCategoryCatches(category);
            if (count > 0) categories.addProperty(category.name(), count);
        }
        journal.add("categories", categories);
        journal.add("depths", GSON.toJsonTree(data.getFishingJournal().getDepths().stream()
            .map(Enum::name).toList()));
        journal.add("moods", GSON.toJsonTree(data.getFishingJournal().getMoods().stream()
            .map(Enum::name).toList()));
        journal.add("loot", GSON.toJsonTree(data.getFishingJournal().getLootDiscoveries()));
        journal.add("biomes", GSON.toJsonTree(data.getFishingJournal().getBiomes()));
        root.add("fishingJournal", journal);

        return root;
    }

    static PlayerData fromJson(UUID uuid, JsonObject json) {
        PlayerData data = new PlayerData(uuid);
        data.setDataVersion(json.has("dataVersion") ? json.get("dataVersion").getAsInt() : 1);

        if (json.has("xp")) {
            JsonObject xpObj = json.getAsJsonObject("xp");
            for (Skill skill : Skill.values()) {
                if (xpObj.has(skill.name())) {
                    long amount = xpObj.get(skill.name()).getAsLong();
                    data.addXp(skill, amount);
                }
            }
        }

        if (json.has("maxMana")) data.setMaxMana(json.get("maxMana").getAsDouble());
        if (json.has("petsVisible")) data.setPetsVisible(json.get("petsVisible").getAsBoolean());
        if (json.has("limitBurstLevelUpSounds")) {
            data.setLimitBurstLevelUpSounds(json.get("limitBurstLevelUpSounds").getAsBoolean());
        }

        // Pets
        if (json.has("pets")) {
            UUID activePetId = null;
            for (JsonElement el : json.getAsJsonArray("pets")) {
                JsonObject p = el.getAsJsonObject();
                try {
                    UUID id       = UUID.fromString(p.get("id").getAsString());
                    PetType type  = PetType.valueOf(p.get("type").getAsString());
                    PetRarity rar = PetRarity.valueOf(p.get("rarity").getAsString());
                    long petXp    = p.get("xp").getAsLong();
                    boolean active = p.has("active") && p.get("active").getAsBoolean();
                    PetInstance pet = new PetInstance(id, type, rar, petXp);
                    data.getPetRoster().addPet(pet);
                    if (active) activePetId = id;
                } catch (Exception e) {
                    PeakLog.warn("Skipping corrupt pet entry: {}", e.getMessage());
                }
            }
            if (activePetId != null) data.getPetRoster().setActivePet(activePetId);
        }

        // Collections
        if (json.has("collections")) {
            JsonObject colObj = json.getAsJsonObject("collections");
            for (CollectionType type : CollectionType.values()) {
                if (colObj.has(type.name())) {
                    data.getCollections().getCounts()
                        .put(type, colObj.get(type.name()).getAsLong());
                }
            }
        }
        if (json.has("collectionTiers")) {
            JsonObject tierObj = json.getAsJsonObject("collectionTiers");
            for (CollectionType type : CollectionType.values()) {
                if (tierObj.has(type.name())) {
                    int tier = tierObj.get(type.name()).getAsInt();
                    int maxTier = CollectionRegistry.getTiers(type).size();
                    if (tier > 0 && tier <= maxTier) {
                        data.getCollections().getUnlockedTiers().put(type, tier);
                    }
                }
            }
        }

        if (json.has("fishingJournal") && json.get("fishingJournal").isJsonObject()) {
            JsonObject journal = json.getAsJsonObject("fishingJournal");
            try {
                long catches = journal.has("totalCatches") ? journal.get("totalCatches").getAsLong() : 0;
                long items = journal.has("totalItems") ? journal.get("totalItems").getAsLong() : 0;
                data.getFishingJournal().loadTotals(catches, items);
            } catch (Exception ignored) {}
            if (journal.has("categories") && journal.get("categories").isJsonObject()) {
                JsonObject categories = journal.getAsJsonObject("categories");
                for (FishingOutcomeCategory category : FishingOutcomeCategory.values()) {
                    try {
                        if (categories.has(category.name())) {
                            data.getFishingJournal().loadCategory(category,
                                Math.max(0, categories.get(category.name()).getAsLong()));
                        }
                    } catch (Exception ignored) {}
                }
            }
            loadEnumArray(journal, "depths", FishingDepth.class, data.getFishingJournal()::loadDepth);
            loadEnumArray(journal, "moods", FishingMood.class, data.getFishingJournal()::loadMood);
            loadStringArray(journal, "loot", data.getFishingJournal()::loadLoot);
            loadStringArray(journal, "biomes", data.getFishingJournal()::loadBiome);
        }

        return data;
    }

    private static <E extends Enum<E>> void loadEnumArray(JsonObject parent, String key, Class<E> type,
                                                           java.util.function.Consumer<E> consumer) {
        if (!parent.has(key) || !parent.get(key).isJsonArray()) return;
        int read = 0;
        for (JsonElement element : parent.getAsJsonArray(key)) {
            if (read++ >= com.peakskills.fishing.FishingJournal.MAX_DISCOVERIES) break;
            try { consumer.accept(Enum.valueOf(type, element.getAsString())); } catch (Exception ignored) {}
        }
    }

    private static void loadStringArray(JsonObject parent, String key,
                                        java.util.function.Consumer<String> consumer) {
        if (!parent.has(key) || !parent.get(key).isJsonArray()) return;
        int read = 0;
        for (JsonElement element : parent.getAsJsonArray(key)) {
            if (read++ >= com.peakskills.fishing.FishingJournal.MAX_DISCOVERIES) break;
            try { consumer.accept(element.getAsString()); } catch (Exception ignored) {}
        }
    }
}
