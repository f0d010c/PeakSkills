package com.peakskills.skill;

/**
 * OSRS-style XP table. Each level requires progressively more XP.
 * Formula: floor(level + 300 * 2^(level/7)) / 4, accumulated.
 */
public class XPTable {

    private static final long[] XP_FOR_LEVEL = computeTable();

    private static long[] computeTable() {
        long[] table = new long[Skill.MAX_LEVEL + 1];
        table[1] = 0;
        double points = 0;
        for (int level = 1; level < Skill.MAX_LEVEL; level++) {
            points += Math.floor(level + 300.0 * Math.pow(2.0, level / 7.0));
            table[level + 1] = (long) Math.floor(points / 8);
        }
        return table;
    }

    /** Total XP required to reach this level from 0. */
    public static long xpForLevel(int level) {
        if (level <= 1) return 0;
        if (level > Skill.MAX_LEVEL) return xpForLevel(Skill.MAX_LEVEL);
        return XP_FOR_LEVEL[level];
    }

    /** Level corresponding to a given total XP amount. */
    public static int levelForXp(long xp) {
        for (int level = Skill.MAX_LEVEL; level >= 1; level--) {
            if (xp >= XP_FOR_LEVEL[level]) return level;
        }
        return 1;
    }

    /** XP needed to go from current level to next. */
    public static long xpToNextLevel(int currentLevel) {
        if (currentLevel >= Skill.MAX_LEVEL) return 0;
        return xpForLevel(currentLevel + 1) - xpForLevel(currentLevel);
    }
}
