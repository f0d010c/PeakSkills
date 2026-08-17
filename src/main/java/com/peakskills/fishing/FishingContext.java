package com.peakskills.fishing;

/** Immutable, server-computed facts used for one fishing outcome. */
public record FishingContext(
    int fishingLevel,
    double rawLuck,
    int fishingLuckEnchantment,
    FishingDepth depth,
    FishingMood mood,
    String biome,
    boolean raining,
    boolean night,
    int waterDepth,
    int sampledWaterBlocks
) {
    public FishingContext {
        fishingLevel = Math.max(1, Math.min(99, fishingLevel));
        rawLuck = Double.isFinite(rawLuck) ? Math.max(0.0, rawLuck) : 0.0;
        fishingLuckEnchantment = Math.max(0, Math.min(10, fishingLuckEnchantment));
        biome = biome == null || biome.isBlank() ? "minecraft:unknown" : biome;
        waterDepth = Math.max(0, Math.min(32, waterDepth));
        sampledWaterBlocks = Math.max(0, Math.min(75, sampledWaterBlocks));
    }

    /** Bounded weight bonus; Luck improves odds but never bypasses level/depth gates. */
    public double rareWeightBonus() {
        return Math.min(1.5, rawLuck * 0.25 + fishingLuckEnchantment * 0.15);
    }
}
