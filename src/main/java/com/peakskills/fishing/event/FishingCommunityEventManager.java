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
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FishingCommunityEventManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, Integer> contributions = new HashMap<>();
    private static Path file;
    private static boolean active;
    private static int goal;
    private static int progress;
    private static long endsAtMs;
    private static int lastAnnouncedQuarter;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            file = server.getSavePath(WorldSavePath.ROOT).resolve("peakskills").resolve("fishing_event.json");
            load();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> save());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (active && System.currentTimeMillis() >= endsAtMs) {
                finish(server, false);
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
        broadcast(server, Text.literal("Fishing Event started! Catch ")
            .formatted(Formatting.AQUA, Formatting.BOLD)
            .append(Text.literal(String.valueOf(goal)).formatted(Formatting.WHITE, Formatting.BOLD))
            .append(Text.literal(" fish before time runs out.").formatted(Formatting.AQUA)));
        save();
        return true;
    }

    public static boolean stop(MinecraftServer server) {
        if (!active) return false;
        finish(server, false);
        return true;
    }

    public static void recordCatch(ServerPlayerEntity player, int contribution) {
        if (!active || contribution <= 0) return;
        MinecraftServer server = PlayerDataManager.getServer();
        if (server == null) return;

        int safeContribution = Math.min(contribution, 100);
        progress = Math.min(goal, progress + safeContribution);
        contributions.merge(player.getUuid(), safeContribution, Integer::sum);

        int quarter = goal <= 0 ? 4 : Math.min(4, (progress * 4) / goal);
        if (quarter > lastAnnouncedQuarter && quarter < 4) {
            lastAnnouncedQuarter = quarter;
            broadcast(server, Text.literal("Fishing Event: ")
                .formatted(Formatting.AQUA)
                .append(Text.literal(progress + " / " + goal).formatted(Formatting.WHITE, Formatting.BOLD)));
        }

        if (progress >= goal) {
            finish(server, true);
        } else {
            save();
        }
    }

    public static Text statusText() {
        if (!active) return Text.literal("No Fishing Event is active.").formatted(Formatting.GRAY);
        long remaining = Math.max(0, endsAtMs - System.currentTimeMillis()) / 1000L;
        return Text.literal("Fishing Event: ").formatted(Formatting.AQUA)
            .append(Text.literal(progress + " / " + goal).formatted(Formatting.WHITE, Formatting.BOLD))
            .append(Text.literal(" (" + remaining + "s left)").formatted(Formatting.GRAY));
    }

    private static void finish(MinecraftServer server, boolean completed) {
        if (completed) {
            broadcast(server, Text.literal("Fishing Event complete! Rewards granted to contributors.")
                .formatted(Formatting.GREEN, Formatting.BOLD));
            rewardContributors(server);
        } else {
            broadcast(server, Text.literal("Fishing Event ended at " + progress + " / " + goal + ".")
                .formatted(Formatting.YELLOW));
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
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            int count = contributions.getOrDefault(player.getUuid(), 0);
            if (count <= 0) continue;

            long xp = Math.min(10_000_000L, PeakConfig.get().communityFishingRewardXp + (long) count * 25L);
            XpManager.addXp(player, Skill.FISHING, xp);
            ItemStack reward = new ItemStack(Items.PRISMARINE_CRYSTALS, Math.min(64, Math.max(1, count / 10)));
            if (!player.getInventory().insertStack(reward)) player.dropItem(reward, false);
            player.sendMessage(Text.literal("Fishing Event reward: ")
                .formatted(Formatting.AQUA)
                .append(Text.literal(String.format("%,d Fishing XP", xp)).formatted(Formatting.GREEN))
                .append(Text.literal(" and Prismarine Crystals").formatted(Formatting.GRAY)), false);
        }
    }

    private static void broadcast(MinecraftServer server, Text text) {
        server.getPlayerManager().broadcast(text, false);
    }

    private static void load() {
        if (file == null || !Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) return;
            active = json.has("active") && json.get("active").getAsBoolean();
            goal = json.has("goal") ? json.get("goal").getAsInt() : 0;
            progress = json.has("progress") ? json.get("progress").getAsInt() : 0;
            endsAtMs = json.has("endsAtMs") ? json.get("endsAtMs").getAsLong() : 0L;
            contributions.clear();
            if (json.has("contributions") && json.get("contributions").isJsonObject()) {
                JsonObject obj = json.getAsJsonObject("contributions");
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    try {
                        contributions.put(UUID.fromString(entry.getKey()),
                            Math.max(0, entry.getValue().getAsInt()));
                    } catch (Exception ignored) {}
                }
            }
            if (System.currentTimeMillis() >= endsAtMs) active = false;
        } catch (Exception e) {
            PeakLog.warn("Failed to load fishing event state: {}", e.getMessage());
        }
    }

    private static void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                JsonObject json = new JsonObject();
                json.addProperty("active", active);
                json.addProperty("goal", goal);
                json.addProperty("progress", progress);
                json.addProperty("endsAtMs", endsAtMs);
                JsonObject contrib = new JsonObject();
                contributions.forEach((uuid, count) -> contrib.addProperty(uuid.toString(), count));
                json.add("contributions", contrib);
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            PeakLog.warn("Failed to save fishing event state: {}", e.getMessage());
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
