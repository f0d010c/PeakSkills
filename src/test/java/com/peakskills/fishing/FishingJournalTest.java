package com.peakskills.fishing;

import com.peakskills.MinecraftTestBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingJournalTest {
    @BeforeAll
    static void initializeMinecraft() {
        MinecraftTestBootstrap.initializeRegistries();
    }

    @Test
    void recordsOneCatchButEveryItemInItsStack() {
        FishingJournal journal = new FishingJournal();
        FishingContext context = new FishingContext(30, 0, 0, FishingDepth.DEEP_WATER,
            FishingMood.FEEDING_FRENZY, "minecraft:ocean", true, false, 8, 50);
        FishingLootTable.RollResult result = new FishingLootTable.RollResult("test_fish",
            new ItemStack(Items.COD, 4), 30, FishingLootTable.Rarity.COMMON,
            FishingOutcomeCategory.FISH);

        journal.record(context, result);

        assertEquals(1, journal.getTotalCatches());
        assertEquals(4, journal.getTotalItems());
        assertEquals(1, journal.getCategoryCatches(FishingOutcomeCategory.FISH));
        assertTrue(journal.getDepths().contains(FishingDepth.DEEP_WATER));
        assertTrue(journal.hasDiscovered("test_fish"));
    }

    @Test
    void duplicatedUnstackableCatchRecordsAndAwardsTwoItems() {
        FishingLootTable.RollResult result = new FishingLootTable.RollResult("totem_of_the_deep",
            new ItemStack(Items.TOTEM_OF_UNDYING), 1283, FishingLootTable.Rarity.LEGENDARY,
            FishingOutcomeCategory.TREASURE, 2);
        FishingJournal journal = new FishingJournal();
        FishingContext context = new FishingContext(99, 0, 0, FishingDepth.ANCIENT,
            FishingMood.CALM_WATERS, "minecraft:deep_ocean", false, false, 32, 75);

        journal.record(context, result);

        assertEquals(2, result.totalQuantity());
        assertEquals(2, journal.getTotalItems());
        assertEquals(1, journal.getTotalCatches());
    }
}
