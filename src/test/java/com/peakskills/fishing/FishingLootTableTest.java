package com.peakskills.fishing;

import com.peakskills.MinecraftTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import net.minecraft.util.RandomSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingLootTableTest {

    @BeforeAll
    static void initializeMinecraft() {
        MinecraftTestBootstrap.initializeRegistries();
    }

    @Test
    void levelOneRollsOnlyCommonLootWithBoundedStacks() {
        RandomSource random = RandomSource.create(12345L);
        for (int i = 0; i < 2_000; i++) {
            FishingLootTable.RollResult result = FishingLootTable.roll(1, 0, random);
            assertNotNull(result);
            assertFalse(result.stack().isEmpty());
            assertTrue(result.stack().getCount() >= 1);
            assertTrue(result.stack().getCount() <= 5);
            assertTrue(result.xp() == FishingLootTable.Rarity.COMMON.xp);
        }
    }

    @Test
    void highLevelPoolCanReachEveryRarity() {
        RandomSource random = RandomSource.create(98765L);
        Set<Long> seenXp = new java.util.HashSet<>();
        for (int i = 0; i < 20_000; i++) {
            FishingLootTable.RollResult result = FishingLootTable.roll(99, 0, random);
            assertNotNull(result);
            seenXp.add(result.xp());
        }

        for (FishingLootTable.Rarity rarity : FishingLootTable.Rarity.values()) {
            assertTrue(seenXp.contains(rarity.xp), "Never rolled " + rarity);
        }
    }

    @Test
    void luckCannotBypassLevelOrDepthRequirements() {
        RandomSource random = RandomSource.create(4242L);
        FishingContext context = new FishingContext(1, 10_000, 10, FishingDepth.ANCIENT,
            FishingMood.ABYSS_STIR, "minecraft:deep_ocean", true, true, 32, 75);
        for (int i = 0; i < 2_000; i++) {
            assertTrue(FishingLootTable.roll(context, random).rarity() == FishingLootTable.Rarity.COMMON);
        }
    }

    @Test
    void shallowWaterCannotProduceRareOrLegendaryLoot() {
        RandomSource random = RandomSource.create(987L);
        FishingContext context = new FishingContext(99, 100, 10, FishingDepth.SHALLOW,
            FishingMood.TREASURE_RIPPLE, "minecraft:plains", true, true, 1, 5);
        for (int i = 0; i < 5_000; i++) {
            assertTrue(FishingLootTable.roll(context, random).rarity().ordinal()
                <= FishingLootTable.Rarity.UNCOMMON.ordinal());
        }
    }

    @Test
    void biomeChangesEligibleCatchWeights() {
        FishingContext warm = new FishingContext(20, 0, 0, FishingDepth.RIVERBED,
            FishingMood.CALM_WATERS, "minecraft:warm_ocean", false, false, 4, 30);
        FishingContext frozen = new FishingContext(20, 0, 0, FishingDepth.RIVERBED,
            FishingMood.CALM_WATERS, "minecraft:frozen_ocean", false, false, 4, 30);
        int warmTropical = countEntry(warm, "tropical_fish", 20_000, 77L);
        int frozenTropical = countEntry(frozen, "tropical_fish", 20_000, 77L);
        assertTrue(warmTropical > frozenTropical * 2,
            "Warm ocean did not materially favor tropical fish");
    }

    private static int countEntry(FishingContext context, String id, int rolls, long seed) {
        RandomSource random = RandomSource.create(seed);
        int count = 0;
        for (int i = 0; i < rolls; i++) {
            if (FishingLootTable.roll(context, random).entryId().equals(id)) count++;
        }
        return count;
    }
}
