package com.peakskills.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XPTableTest {

    @Test
    void xpStartsAtZeroAndCapsAtMaxLevel() {
        assertEquals(0, XPTable.xpForLevel(Integer.MIN_VALUE));
        assertEquals(0, XPTable.xpForLevel(1));
        assertEquals(XPTable.xpForLevel(Skill.MAX_LEVEL), XPTable.xpForLevel(Integer.MAX_VALUE));
    }

    @Test
    void xpTableIsStrictlyIncreasingUntilMaxLevel() {
        long previous = XPTable.xpForLevel(1);
        for (int level = 2; level <= Skill.MAX_LEVEL; level++) {
            long current = XPTable.xpForLevel(level);
            assertTrue(current > previous, "level " + level + " should require more XP");
            previous = current;
        }
    }

    @Test
    void levelsMatchEveryXpBoundary() {
        assertEquals(1, XPTable.levelForXp(Long.MIN_VALUE));
        for (int level = 1; level <= Skill.MAX_LEVEL; level++) {
            long threshold = XPTable.xpForLevel(level);
            assertEquals(level, XPTable.levelForXp(threshold));

            if (level < Skill.MAX_LEVEL) {
                assertEquals(level, XPTable.levelForXp(XPTable.xpForLevel(level + 1) - 1));
            }
        }
        assertEquals(Skill.MAX_LEVEL, XPTable.levelForXp(Long.MAX_VALUE));
    }

    @Test
    void nextLevelDeltaMatchesAdjacentThresholds() {
        for (int level = 1; level < Skill.MAX_LEVEL; level++) {
            assertEquals(
                XPTable.xpForLevel(level + 1) - XPTable.xpForLevel(level),
                XPTable.xpToNextLevel(level)
            );
        }
        assertEquals(0, XPTable.xpToNextLevel(Skill.MAX_LEVEL));
        assertEquals(0, XPTable.xpToNextLevel(Integer.MAX_VALUE));
    }
}
