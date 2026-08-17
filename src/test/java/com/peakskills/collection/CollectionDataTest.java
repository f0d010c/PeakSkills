package com.peakskills.collection;

import com.peakskills.MinecraftTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class CollectionDataTest {

    @BeforeAll
    static void initializeMinecraft() {
        MinecraftTestBootstrap.initializeRegistries();
    }

    @Test
    void incrementsUnlockEveryCrossedTierExactlyOnce() {
        CollectionType type = CollectionType.COBBLESTONE;
        List<CollectionTier> tiers = CollectionRegistry.getTiers(type);
        CollectionData data = new CollectionData();

        assertTrue(data.increment(type, tiers.get(0).threshold() - 1).isEmpty());
        assertEquals(1, data.increment(type, 1).size());
        assertEquals(1, data.getUnlockedTier(type));
        assertTrue(data.increment(type, 0).isEmpty());

        long finalThreshold = tiers.get(tiers.size() - 1).threshold();
        assertEquals(tiers.size() - 1, data.increment(type, finalThreshold).size());
        assertEquals(tiers.size(), data.getUnlockedTier(type));
        assertTrue(data.increment(type, 1).isEmpty());
    }

    @Test
    void negativeInputCannotReduceCountAndOverflowSaturates() {
        CollectionData data = new CollectionData();
        CollectionType type = CollectionType.COBBLESTONE;

        data.increment(type, -50);
        assertEquals(0, data.getCount(type));

        data.increment(type, Long.MAX_VALUE - 2);
        data.increment(type, 10);
        assertEquals(Long.MAX_VALUE, data.getCount(type));
    }

    @Test
    void generatedDropsMustMatchTheirCollection() {
        assertTrue(CollectionRegistry.matchesItem(CollectionType.COBBLESTONE,
            new ItemStack(Items.COBBLESTONE, 4)));
        assertFalse(CollectionRegistry.matchesItem(CollectionType.GRAVEL,
            new ItemStack(Items.COD, 1)));
    }
}
