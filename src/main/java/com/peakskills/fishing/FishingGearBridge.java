package com.peakskills.fishing;

import com.peakskills.player.PlayerData;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Reads the documented PeakGear fishing metadata without a runtime class dependency. */
public final class FishingGearBridge {
    public static FishingModifiers read(ServerPlayer player, ItemStack rod, PlayerData data) {
        CompoundTag peak = peakGear(rod);
        CompoundTag traits = peak == null ? new CompoundTag()
            : peak.getCompound("fishing_traits").orElse(new CompoundTag());
        Set<String> accessories = new HashSet<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            CompoundTag item = peakGear(player.getInventory().getItem(slot));
            if (item != null) accessories.add(item.getString("id").orElse(""));
        }
        String bait = peak == null ? "" : peak.getString("cast_bait").orElse("");
        return new FishingModifiers(
            peak == null ? "" : peak.getString("id").orElse(""),
            trait(traits, "double_hook"), trait(traits, "treasure_hunter"),
            trait(traits, "relic_seeker"), trait(traits, "scholar"),
            trait(traits, "deepwater"), trait(traits, "stormcaller"),
            trait(traits, "biome_specialist"), trait(traits, "mood_reader"),
            trait(traits, "journalist"), accessories.contains("relic_talisman"),
            accessories.contains("deepwater_charm"), accessories.contains("fishermans_satchel"),
            bait, data.getFishingJournal().getLootDiscoveries());
    }

    public static double swiftReelReduction(ItemStack rod, boolean raining) {
        CompoundTag peak = peakGear(rod);
        if (peak == null) return 0.0;
        int level = trait(peak.getCompound("fishing_traits").orElse(new CompoundTag()), "swift_reel");
        double base = level * 0.04;
        if (raining && "stormwake_rod".equals(peak.getString("id").orElse(""))) base += 0.10;
        return Math.min(0.55, base);
    }

    private static int trait(CompoundTag traits, String id) {
        return clamp(traits.getInt(id).orElse(0), 0, 10);
    }

    private static CompoundTag peakGear(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        return custom == null ? null : custom.copyTag().getCompound("peakgear").orElse(null);
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private FishingGearBridge() {}
}
