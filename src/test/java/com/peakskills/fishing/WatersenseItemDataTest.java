package com.peakskills.fishing;

import com.peakskills.MinecraftTestBootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WatersenseItemDataTest {

    @BeforeAll
    static void initializeMinecraft() {
        MinecraftTestBootstrap.initializeRegistries();
    }

    @Test
    void customLootUsesSecureMetadataRatherThanItsVanillaCarrier() {
        ItemStack stack = FishingLootTable.preview("ancient_scale");
        assertEquals("ancient_scale", WatersenseItemData.getLootId(stack));
        assertNull(WatersenseItemData.getLootId(new ItemStack(Items.TURTLE_SCUTE)));
    }

    @Test
    void malformedOrInjectedLootIdsAreRejected() {
        ItemStack stack = new ItemStack(Items.STICK);
        CompoundTag watersense = new CompoundTag();
        watersense.putString("loot_id", "../../operator");
        CompoundTag peak = new CompoundTag();
        peak.put("watersense", watersense);
        CompoundTag root = new CompoundTag();
        root.put("peakskills", peak);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));

        assertNull(WatersenseItemData.getLootId(stack));
    }

    @Test
    void everyCustomPoolEntryRetainsItsStableId() {
        for (FishingLootTable.EntryView entry : FishingLootTable.entries()) {
            String id = WatersenseItemData.getLootId(FishingLootTable.preview(entry.id()));
            if (id != null) assertEquals(entry.id(), id);
        }
    }
}
