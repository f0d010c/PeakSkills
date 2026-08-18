package com.peakskills.fishing;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingModifiersTest {

    @Test
    void doubleHookUsesTheApprovedRankCurveAndClampsAtTen() {
        int[] expected = {0, 4, 7, 10, 13, 16, 19, 22, 25, 28, 32};
        for (int level = 0; level <= 10; level++) {
            assertEquals(expected[level] / 100.0, modifiers(level).doubleHookChance(), 0.000_001);
        }
        assertEquals(0.32, modifiers(Integer.MAX_VALUE).doubleHookChance(), 0.000_001);
    }

    @Test
    void swiftReelAndScholarValuesReachTheirDocumentedRankTenStrength() {
        FishingModifiers rankTen = new FishingModifiers("", 0, 0, 0, 10, 0, 0, 0, 0, 0,
            false, false, false, "", Set.of());
        assertEquals(1.40, rankTen.xpMultiplier(), 0.000_001);
    }

    @Test
    void weightMultipliersStayBoundedEvenWithEveryBonus() {
        FishingModifiers maximum = new FishingModifiers("rod_of_the_first_tide", 10, 10, 10, 10,
            10, 10, 10, 10, 10, true, true, true, "relic_bait", Set.of());
        FishingContext context = new FishingContext(99, 100, 10, FishingDepth.ANCIENT,
            FishingMood.ABYSS_STIR, "minecraft:warm_ocean", true, true, 32, 75);

        double value = maximum.weightMultiplier("abyssal_star", FishingOutcomeCategory.RELIC,
            FishingLootTable.Rarity.LEGENDARY, context, 1.8);
        assertTrue(value > 1.0);
        assertTrue(value <= 8.0);
    }

    private static FishingModifiers modifiers(int doubleHook) {
        return new FishingModifiers("", doubleHook, 0, 0, 0, 0, 0, 0, 0, 0,
            false, false, false, "", Set.of());
    }
}
