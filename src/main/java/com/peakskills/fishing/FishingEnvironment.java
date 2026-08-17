package com.peakskills.fishing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;

/** Bounded world inspection for a bobber; performs no scans wider than 75 blocks. */
public final class FishingEnvironment {
    private static final int MAX_DEPTH_SCAN = 32;
    private static final long MOOD_WINDOW_TICKS = 20L * 60L * 10L;

    public static Environment inspect(ServerLevel level, BlockPos bobberPos) {
        BlockPos waterPos = level.getFluidState(bobberPos).is(FluidTags.WATER)
            ? bobberPos : bobberPos.below();
        int depth = 0;
        for (int offset = 0; offset < MAX_DEPTH_SCAN; offset++) {
            if (!level.getFluidState(waterPos.below(offset)).is(FluidTags.WATER)) break;
            depth++;
        }

        int volume = 0;
        for (int y = 0; y < 3; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (level.getFluidState(waterPos.offset(x, -y, z)).is(FluidTags.WATER)) volume++;
                }
            }
        }

        FishingDepth tier = classifyDepth(depth, volume);
        FishingMood mood = mood(level, waterPos, tier);
        String biome = level.getBiome(waterPos).unwrapKey()
            .map(key -> key.identifier().toString())
            .orElse("minecraft:unknown");
        long clock = Math.floorMod(level.getOverworldClockTime(), 24_000L);
        boolean night = clock >= 13_000L && clock <= 23_000L;
        return new Environment(tier, mood, biome, level.isRainingAt(waterPos), night, depth, volume);
    }

    public static FishingDepth classifyDepth(int depth, int sampledWaterBlocks) {
        int safeDepth = Math.max(0, depth);
        int safeVolume = Math.max(0, sampledWaterBlocks);
        if (safeDepth >= 25 && safeVolume >= 60) return FishingDepth.ANCIENT;
        if (safeDepth >= 13 && safeVolume >= 50) return FishingDepth.ABYSSAL;
        if (safeDepth >= 6 && safeVolume >= 35) return FishingDepth.DEEP_WATER;
        if (safeDepth >= 3 && safeVolume >= 20) return FishingDepth.RIVERBED;
        return FishingDepth.SHALLOW;
    }

    private static FishingMood mood(ServerLevel level, BlockPos pos, FishingDepth depth) {
        long window = level.getOverworldClockTime() / MOOD_WINDOW_TICKS;
        long regionX = pos.getX() >> 6;
        long regionZ = pos.getZ() >> 6;
        long hash = level.getSeed() ^ (regionX * 341873128712L) ^ (regionZ * 132897987541L)
            ^ (window * 42317861L);
        int roll = Math.floorMod(Long.hashCode(hash), 100);
        if (depth.ordinal() >= FishingDepth.ABYSSAL.ordinal() && roll < 8) return FishingMood.ABYSS_STIR;
        if (roll < 23) return FishingMood.FEEDING_FRENZY;
        if (roll < 35) return FishingMood.TREASURE_RIPPLE;
        if (roll < 47) return FishingMood.MURKY_WAKE;
        return FishingMood.CALM_WATERS;
    }

    public record Environment(FishingDepth depth, FishingMood mood, String biome,
                              boolean raining, boolean night, int waterDepth,
                              int sampledWaterBlocks) {}

    private FishingEnvironment() {}
}
