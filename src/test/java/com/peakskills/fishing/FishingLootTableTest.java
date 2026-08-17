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
            assertTrue(result.stack().getCount() <= 4);
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
}
