package com.peakskills.player;

import com.google.gson.*;
import com.peakskills.PeakSkills;
import com.peakskills.collection.CollectionType;
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
            dataDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                    .resolve("peakskills").resolve("players");
            try {
                Files.createDirectories(dataDir);
            } catch (IOException e) {
                PeakSkills.LOGGER.error("Failed to create player data directory", e);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID uuid = handler.player.getUuid();
            cache.put(uuid, load(uuid));
            StatManager.applyStats(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
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
            PeakSkills.LOGGER.error("Failed to load data for {}", uuid, e);
            return new PlayerData(uuid);
        }
    }

    private static void save(PlayerData data) {
        if (dataDir == null) return;
        Path file = dataDir.resolve(data.getUuid() + ".json");

        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(toJson(data), writer);
        } catch (IOException e) {
            PeakSkills.LOGGER.error("Failed to save data for {}", data.getUuid(), e);
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

        JsonObject xpObj = new JsonObject();
        for (Skill skill : Skill.values()) {
            xpObj.addProperty(skill.name(), data.getXp(skill));
        }
        root.add("xp", xpObj);
        root.addProperty("mana", data.getMana());
        root.addProperty("maxMana", data.getMaxMana());

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

        return root;
    }

    static PlayerData fromJson(UUID uuid, JsonObject json) {
        PlayerData data = new PlayerData(uuid);

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
                    PeakSkills.LOGGER.warn("Skipping corrupt pet entry: {}", e.getMessage());
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
                    data.getCollections().getUnlockedTiers()
                        .put(type, tierObj.get(type.name()).getAsInt());
                }
            }
        }

        return data;
    }
}
