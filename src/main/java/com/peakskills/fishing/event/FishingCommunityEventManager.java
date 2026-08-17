package com.peakskills.fishing.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.peakskills.PeakLog;
import com.peakskills.config.PeakConfig;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import com.peakskills.xp.XpManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FishingCommunityEventManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, Integer> contributions = new HashMap<>();
    private static final Map<UUID, PendingReward> pendingRewards = new HashMap<>();
    private static Path file;
    private static boolean active;
    private static int goal;
    private static int progress;
    private static long endsAtMs;
    private static int lastAnnouncedQuarter;
    private static boolean dirty;
    private static int saveTicks;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            file = server.getWorldPath(LevelResource.ROOT).resolve("peakskills").resolve("fishing_event.json");
            load();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> save());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            deliverPending(handler.getPlayer()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (active && System.currentTimeMillis() >= endsAtMs) {
                finish(server, false);
            }
            if (++saveTicks >= 100) {
                saveTicks = 0;
                if (dirty) save();
            }
        });
    }

    public static boolean start(MinecraftServer server, int requestedGoal, int minutes) {
        if (!PeakConfig.get().communityFishingEventsEnabled || active) return false;
        active = true;
        goal = clamp(requestedGoal, 1, 1_000_000);
        progress = 0;
        endsAtMs = System.currentTimeMillis() + clamp(minutes, 1, 1440) * 60_000L;
        lastAnnouncedQuarter = 0;
        contributions.clear();
        broadcast(server, Component.literal("Fishing Event started! Land ")
            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
            .append(Component.literal(String.valueOf(goal)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" catches before time runs out.").withStyle(ChatFormatting.AQUA)));
        save();
        return true;
    }

    public static boolean stop(MinecraftServer server) {
        if (!active) return false;
        finish(server, false);
        return true;
    }

    public static void recordCatch(ServerPlayer player, int contribution) {
        if (!active || contribution <= 0) return;
        MinecraftServer server = PlayerDataManager.getServer();
        if (server == null) return;

        int safeContribution = Math.min(contribution, 100);
        progress = Math.min(goal, progress + safeContribution);
        contributions.merge(player.getUUID(), safeContribution,
            (left, right) -> left > Integer.MAX_VALUE - right ? Integer.MAX_VALUE : left + right);
        dirty = true;

        int quarter = goal <= 0 ? 4 : Math.min(4, (progress * 4) / goal);
        if (quarter > lastAnnouncedQuarter && quarter < 4) {
            lastAnnouncedQuarter = quarter;
            broadcast(server, Component.literal("Fishing Event: ")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(progress + " / " + goal).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)));
        }

        if (progress >= goal) {
            finish(server, true);
        }
    }

    public static Component statusText() {
        if (!active) return Component.literal("No Fishing Event is active.").withStyle(ChatFormatting.GRAY);
        long remaining = Math.max(0, endsAtMs - System.currentTimeMillis()) / 1000L;
        return Component.literal("Fishing Event: ").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(progress + " / " + goal).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" (" + remaining + "s left)").withStyle(ChatFormatting.GRAY));
    }

    private static void finish(MinecraftServer server, boolean completed) {
        if (completed) {
            broadcast(server, Component.literal("Fishing Event complete! Rewards granted to contributors.")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            rewardContributors(server);
        } else {
            broadcast(server, Component.literal("Fishing Event ended at " + progress + " / " + goal + ".")
                .withStyle(ChatFormatting.YELLOW));
        }
        active = false;
        progress = 0;
        goal = 0;
        endsAtMs = 0;
        lastAnnouncedQuarter = 0;
        contributions.clear();
        save();
    }

    private static void rewardContributors(MinecraftServer server) {
        for (Map.Entry<UUID, Integer> entry : contributions.entrySet()) {
            int count = entry.getValue();
            long xp = Math.min(10_000_000L, PeakConfig.get().communityFishingRewardXp + (long) count * 25L);
            int crystals = Math.min(64, Math.max(1, count / 10));
            pendingRewards.merge(entry.getKey(), new PendingReward(xp, crystals), PendingReward::combine);
        }
        dirty = true;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            deliverPending(player);
        }
    }

    private static void deliverPending(ServerPlayer player) {
        PendingReward pending = pendingRewards.remove(player.getUUID());
        if (pending == null) return;
        XpManager.addXp(player, Skill.FISHING, pending.xp());
        ItemStack reward = new ItemStack(Items.PRISMARINE_CRYSTALS, pending.crystals());
        if (!player.getInventory().add(reward)) player.drop(reward, false);
        player.sendSystemMessage(Component.literal("Fishing Event reward: ")
            .withStyle(ChatFormatting.AQUA)
            .append(Component.literal(String.format("%,d Fishing XP", pending.xp())).withStyle(ChatFormatting.GREEN))
            .append(Component.literal(" and " + pending.crystals() + " Prismarine Crystals")
                .withStyle(ChatFormatting.GRAY)), false);
        dirty = true;
        save();
    }

    private static void broadcast(MinecraftServer server, Component text) {
        server.getPlayerList().broadcastSystemMessage(text, false);
    }

    private static void load() {
        if (file == null || !Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) return;
            active = json.has("active") && json.get("active").getAsBoolean();
            goal = clamp(json.has("goal") ? json.get("goal").getAsInt() : 0, 0, 1_000_000);
            progress = clamp(json.has("progress") ? json.get("progress").getAsInt() : 0, 0, goal);
            endsAtMs = Math.max(0L, json.has("endsAtMs") ? json.get("endsAtMs").getAsLong() : 0L);
            contributions.clear();
            if (json.has("contributions") && json.get("contributions").isJsonObject()) {
                JsonObject obj = json.getAsJsonObject("contributions");
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    try {
                        contributions.put(UUID.fromString(entry.getKey()),
                            clamp(entry.getValue().getAsInt(), 0, 1_000_000));
                    } catch (Exception ignored) {}
                }
            }
            pendingRewards.clear();
            if (json.has("pendingRewards") && json.get("pendingRewards").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry
                        : json.getAsJsonObject("pendingRewards").entrySet()) {
                    try {
                        JsonObject reward = entry.getValue().getAsJsonObject();
                        long xp = Math.max(0L, Math.min(10_000_000L, reward.get("xp").getAsLong()));
                        int crystals = clamp(reward.get("crystals").getAsInt(), 0, 64);
                        if (xp > 0 || crystals > 0) {
                            pendingRewards.put(UUID.fromString(entry.getKey()), new PendingReward(xp, crystals));
                        }
                    } catch (Exception ignored) {}
                }
            }
            if (goal == 0 || System.currentTimeMillis() >= endsAtMs) {
                active = false;
                contributions.clear();
            }
        } catch (Exception e) {
            PeakLog.warn("Failed to load fishing event state: {}", e.getMessage());
        }
    }

    private static void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                JsonObject json = new JsonObject();
                json.addProperty("active", active);
                json.addProperty("goal", goal);
                json.addProperty("progress", progress);
                json.addProperty("endsAtMs", endsAtMs);
                JsonObject contrib = new JsonObject();
                contributions.forEach((uuid, count) -> contrib.addProperty(uuid.toString(), count));
                json.add("contributions", contrib);
                JsonObject pending = new JsonObject();
                pendingRewards.forEach((uuid, reward) -> {
                    JsonObject value = new JsonObject();
                    value.addProperty("xp", reward.xp());
                    value.addProperty("crystals", reward.crystals());
                    pending.add(uuid.toString(), value);
                });
                json.add("pendingRewards", pending);
                GSON.toJson(json, writer);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException e) {
            PeakLog.warn("Failed to save fishing event state: {}", e.getMessage());
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record PendingReward(long xp, int crystals) {
        private static PendingReward combine(PendingReward left, PendingReward right) {
            long xp = left.xp > Long.MAX_VALUE - right.xp ? Long.MAX_VALUE : left.xp + right.xp;
            return new PendingReward(Math.min(10_000_000L, xp),
                Math.min(64, left.crystals + right.crystals));
        }
    }
}
