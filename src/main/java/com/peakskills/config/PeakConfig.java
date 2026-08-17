package com.peakskills.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.peakskills.PeakLog;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class PeakConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PeakConfig INSTANCE = defaults();

    public boolean fishingOverhaulEnabled = true;
    public double fishingXpMultiplier = 1.0;
    public boolean communityFishingEventsEnabled = true;
    public int defaultCommunityFishingGoal = 250;
    public int defaultCommunityFishingMinutes = 20;
    public long communityFishingRewardXp = 2_500L;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(PeakConfig::load);
    }

    public static PeakConfig get() {
        return INSTANCE;
    }

    private static PeakConfig defaults() {
        return new PeakConfig();
    }

    private static void load(MinecraftServer server) {
        Path dir = server.getWorldPath(LevelResource.ROOT).resolve("peakskills");
        Path file = dir.resolve("config.json");

        try {
            Files.createDirectories(dir);
            if (!Files.exists(file)) {
                INSTANCE = defaults();
                save(file, INSTANCE);
                return;
            }

            try (Reader reader = Files.newBufferedReader(file)) {
                PeakConfig loaded = GSON.fromJson(reader, PeakConfig.class);
                INSTANCE = sanitize(loaded == null ? defaults() : loaded);
            }
            save(file, INSTANCE);
        } catch (IOException e) {
            PeakLog.error("Failed to load PeakSkills config; using defaults", e);
            INSTANCE = defaults();
        }
    }

    private static PeakConfig sanitize(PeakConfig config) {
        config.fishingXpMultiplier = clamp(config.fishingXpMultiplier, 0.0, 100.0);
        config.defaultCommunityFishingGoal = clamp(config.defaultCommunityFishingGoal, 1, 1_000_000);
        config.defaultCommunityFishingMinutes = clamp(config.defaultCommunityFishingMinutes, 1, 1440);
        config.communityFishingRewardXp = clamp(config.communityFishingRewardXp, 0L, 10_000_000L);
        return config;
    }

    private static void save(Path file, PeakConfig config) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(config, writer);
        }
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}
