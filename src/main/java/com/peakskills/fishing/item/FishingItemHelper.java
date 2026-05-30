package com.peakskills.fishing.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.Optional;

public class FishingItemHelper {

    public static Optional<FishingItemDef> get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return Optional.empty();

        NbtCompound nbt = comp.copyNbt();
        if (!nbt.getBoolean("peakskillsFishingItem").orElse(false)) return Optional.empty();
        return FishingItemRegistry.get(nbt.getString("fishingItemId").orElse(""));
    }
}
