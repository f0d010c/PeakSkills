package com.peakskills.fishing.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public class FishingItemRegistry {

    private static final LinkedHashMap<String, FishingItemDef> ITEMS = new LinkedHashMap<>();

    static {
        reg(new FishingItemDef("reed_rod", "Reed Rod", Items.FISHING_ROD,
            Formatting.GREEN, 5, 0.05, 1, 0));
        reg(new FishingItemDef("deepwater_rod", "Deepwater Rod", Items.FISHING_ROD,
            Formatting.AQUA, 25, 0.12, 4, 1));
        reg(new FishingItemDef("relic_rod", "Relic Rod", Items.FISHING_ROD,
            Formatting.LIGHT_PURPLE, 50, 0.20, 8, 2));
    }

    public static Collection<FishingItemDef> all() {
        return ITEMS.values();
    }

    public static Optional<FishingItemDef> get(String id) {
        return Optional.ofNullable(ITEMS.get(id));
    }

    public static ItemStack create(String id) {
        FishingItemDef def = get(id).orElseThrow(() -> new IllegalArgumentException("Unknown fishing item: " + id));
        ItemStack stack = new ItemStack(def.baseItem());

        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("peakskillsFishingItem", true);
        nbt.putString("fishingItemId", def.id());
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(def.displayName()).formatted(def.color(), Formatting.BOLD));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("  PeakSkills Fishing Item").formatted(Formatting.DARK_GRAY),
            Text.empty(),
            Text.literal("  Requires Fishing ").formatted(Formatting.GRAY)
                .append(Text.literal(String.valueOf(def.requiredFishingLevel())).formatted(Formatting.AQUA)),
            Text.literal("  +" + percent(def.fishingXpBonus()) + " Fishing XP").formatted(Formatting.GREEN),
            Text.literal("  +" + def.effectiveLevelBonus() + " effective Fishing level").formatted(Formatting.GREEN),
            def.eventContributionBonus() > 0
                ? Text.literal("  +" + def.eventContributionBonus() + " community event contribution").formatted(Formatting.GREEN)
                : Text.literal("  No community event bonus").formatted(Formatting.DARK_GRAY)
        )));
        return stack;
    }

    private static void reg(FishingItemDef def) {
        if (def.id() == null || def.id().isBlank()) throw new IllegalArgumentException("Fishing item id is blank");
        if (def.requiredFishingLevel() < 1 || def.requiredFishingLevel() > 99) {
            throw new IllegalArgumentException("Invalid Fishing level requirement: " + def.id());
        }
        ITEMS.put(def.id(), def);
    }

    private static String percent(double value) {
        return String.format("%.0f%%", value * 100.0);
    }
}
