package com.peakskills.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XPTableTest {

    @Test
    void xpStartsAtZeroAndCapsAtMaxLevel() {
        assertEquals(0, XPTable.xpForLevel(1));
        assertEquals(0, XPTable.xpForLevel(0));
        assertEquals(XPTable.xpForLevel(Skill.MAX_LEVEL), XPTable.xpForLevel(Skill.MAX_LEVEL + 50));
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
    void levelForXpMatchesLevelBoundaries() {
        for (int level = 1; level <= Skill.MAX_LEVEL; level++) {
            long floor = XPTable.xpForLevel(level);
            assertEquals(level, XPTable.levelForXp(floor));

            if (level < Skill.MAX_LEVEL) {
                assertEquals(level, XPTable.levelForXp(XPTable.xpForLevel(level + 1) - 1));
            }
        }
    }

    @Test
    void xpToNextLevelIsZeroAtMaxLevel() {
        assertEquals(0, XPTable.xpToNextLevel(Skill.MAX_LEVEL));
        assertTrue(XPTable.xpToNextLevel(1) > 0);
    }
}
