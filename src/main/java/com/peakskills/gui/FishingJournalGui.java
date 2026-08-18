package com.peakskills.gui;

import com.peakskills.fishing.FishingDepth;
import com.peakskills.fishing.FishingJournal;
import com.peakskills.fishing.FishingLootTable;
import com.peakskills.fishing.FishingMood;
import com.peakskills.fishing.FishingOutcomeCategory;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/** Server-rendered Watersense discovery journal; no client mod is required. */
public final class FishingJournalGui {
    private static final int[] LOOT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38
    };

    public static void open(ServerPlayer viewer) {
        PlayerData data = PlayerDataManager.get(viewer.getUUID());
        FishingJournal journal = data.getFishingJournal();
        SimpleContainer inv = new SimpleContainer(54);
        ItemStack background = named(Items.BLACK_STAINED_GLASS_PANE, " ", ChatFormatting.BLACK);
        for (int slot = 0; slot < 54; slot++) inv.setItem(slot, background.copy());

        ItemStack header = named(Items.FISHING_ROD, "Watersense Journal", ChatFormatting.AQUA);
        header.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        header.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("Fishing " + data.getLevel(com.peakskills.skill.Skill.FISHING))
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
            Component.literal("Catches: " + journal.getTotalCatches()).withStyle(ChatFormatting.GRAY),
            Component.literal("Items landed: " + journal.getTotalItems()).withStyle(ChatFormatting.GRAY),
            Component.literal("Biomes visited: " + journal.getBiomes().size()).withStyle(ChatFormatting.DARK_AQUA)
        )));
        inv.setItem(4, header);

        List<FishingLootTable.EntryView> entries = FishingLootTable.entries();
        for (int index = 0; index < entries.size() && index < LOOT_SLOTS.length; index++) {
            FishingLootTable.EntryView entry = entries.get(index);
            boolean found = journal.hasDiscovered(entry.id());
            ItemStack icon = found ? FishingLootTable.preview(entry.id())
                : named(Items.GRAY_DYE, "Undiscovered", ChatFormatting.DARK_GRAY);
            if (!found) icon.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("Explore more waters to identify this catch.").withStyle(ChatFormatting.GRAY),
                Component.literal("Requires " + entry.minDepth().displayName + " · Fishing " + entry.minLevel())
                    .withStyle(ChatFormatting.DARK_GRAY)
            )));
            inv.setItem(LOOT_SLOTS[index], icon);
        }

        inv.setItem(45, summary(Items.COD, "Catch Types", categoryLore(journal)));
        inv.setItem(46, summary(Items.HEART_OF_THE_SEA, "Depths", discoveryLore(
            FishingDepth.values(), journal.getDepths()::contains)));
        inv.setItem(47, summary(Items.PRISMARINE_CRYSTALS, "Water Moods", discoveryLore(
            FishingMood.values(), journal.getMoods()::contains)));
        inv.setItem(48, summary(Items.MAP, "Biomes", List.of(
            Component.literal(journal.getBiomes().size() + " discovered").withStyle(ChatFormatting.AQUA))));

        ItemStack back = named(Items.ARROW, "Back to Peak Guide", ChatFormatting.YELLOW);
        inv.setItem(49, back);
        ItemStack refresh = named(Items.SUNFLOWER, "Refresh", ChatFormatting.GREEN);
        inv.setItem(50, refresh);
        Map<Integer, Runnable> handlers = new HashMap<>();
        handlers.put(49, () -> PeakGuideGui.open(viewer));
        handlers.put(50, () -> open(viewer));
        com.peakskills.gui.core.LegacyContainerGui.open(viewer,
            Component.literal("Watersense Journal").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
            inv, handlers);
    }

    private static List<Component> categoryLore(FishingJournal journal) {
        List<Component> lore = new ArrayList<>();
        for (FishingOutcomeCategory category : FishingOutcomeCategory.values()) {
            lore.add(Component.literal(pretty(category.name()) + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(journal.getCategoryCatches(category)))
                    .withStyle(ChatFormatting.WHITE)));
        }
        return lore;
    }

    private static <T extends Enum<T>> List<Component> discoveryLore(T[] values,
            java.util.function.Predicate<T> discovered) {
        List<Component> lore = new ArrayList<>();
        for (T value : values) {
            String name = value instanceof FishingDepth depth ? depth.displayName
                : ((FishingMood) value).displayName;
            lore.add(Component.literal((discovered.test(value) ? "✓ " : "? ") + name)
                .withStyle(discovered.test(value) ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        }
        return lore;
    }

    private static ItemStack summary(net.minecraft.world.item.Item item, String name, List<Component> lore) {
        ItemStack stack = named(item, name, ChatFormatting.GOLD);
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private static ItemStack named(net.minecraft.world.item.Item item, String name, ChatFormatting color) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(color, ChatFormatting.BOLD));
        return stack;
    }

    private static String pretty(String value) {
        String lower = value.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private FishingJournalGui() {}
}
