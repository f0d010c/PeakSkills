package com.peakskills.fishing;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

/** Stable server-side identity for custom Watersense materials. */
public final class WatersenseItemData {
    private static final String ROOT = "peakskills";
    private static final String WATERSENSE = "watersense";
    private static final String LOOT_ID = "loot_id";

    public static void mark(ItemStack stack, String id, int modelId) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag peak = root.getCompound(ROOT).orElse(new CompoundTag());
        CompoundTag watersense = peak.getCompound(WATERSENSE).orElse(new CompoundTag());
        watersense.putString(LOOT_ID, id);
        watersense.putInt("data_version", 1);
        peak.put(WATERSENSE, watersense);
        root.put(ROOT, peak);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
            new CustomModelData(List.of((float) modelId), List.of(), List.of(id), List.of()));
    }

    public static String getLootId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag root = data.copyTag();
        CompoundTag peak = root.getCompound(ROOT).orElse(null);
        if (peak == null) return null;
        CompoundTag watersense = peak.getCompound(WATERSENSE).orElse(null);
        if (watersense == null) return null;
        String id = watersense.getString(LOOT_ID).orElse("");
        return id.matches("[a-z0-9_]{1,64}") ? id : null;
    }

    private WatersenseItemData() {}
}
