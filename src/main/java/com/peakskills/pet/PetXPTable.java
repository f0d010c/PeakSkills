package com.peakskills.pet;

/**
 * XP table for pets. Higher rarity requires more XP per level.
 * Uses a simple quadratic curve scaled by a rarity multiplier.
 */
public class PetXPTable {

    /** XP required to reach a given level at a given rarity. */
    public static long xpForLevel(int level, PetRarity rarity) {
        if (level <= 1) return 0;
        double multiplier = rarityMultiplier(rarity);
        // Quadratic: each level costs progressively more
        long total = 0;
        for (int i = 2; i <= level; i++) {
            total += (long) (100 * Math.pow(i - 1, 1.5) * multiplier);
        }
        return total;
    }

    /** Current level from total XP at given rarity. */
    public static int levelForXp(long xp, PetRarity rarity) {
        int level = 1;
        while (level < rarity.levelCap && xpForLevel(level + 1, rarity) <= xp) {
            level++;
        }
        return level;
    }

    /** XP needed to go from current level to next within the rarity cap. */
    public static long xpToNextLevel(int currentLevel, PetRarity rarity) {
        if (currentLevel >= rarity.levelCap) return 0;
        return xpForLevel(currentLevel + 1, rarity) - xpForLevel(currentLevel, rarity);
    }

    private static double rarityMultiplier(PetRarity rarity) {
        return switch (rarity) {
            case COMMON    -> 1.0;
            case UNCOMMON  -> 1.5;
            case RARE      -> 2.2;
            case EPIC      -> 3.0;
            case LEGENDARY -> 4.0;
        };
    }
}
