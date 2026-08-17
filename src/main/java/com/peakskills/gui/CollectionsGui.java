package com.peakskills.gui;

import com.peakskills.collection.CollectionData;
import com.peakskills.collection.CollectionRegistry;
import com.peakskills.collection.CollectionType;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * Collections category overview — one icon per category, SkyBlock style.
 *
 * Layout (54 slots):
 *  Row 0 │ bg  bg  bg  bg  [✦ Collections]  bg  bg  bg  bg
 *  Row 1 │ bg  bg  bg  bg        bg          bg  bg  bg  bg
 *  Row 2 │ bg  bg [Mining] [WC] [Excavating] [Farming] [Combat]  bg  bg
 *  Row 3 │ bg  bg  bg  bg        bg          bg  bg  bg  bg
 *  Row 4 │ bg  bg  bg  bg        bg          bg  bg  bg  bg
 *  Row 5 │ bg  bg  bg  bg   [↻ Refresh]     bg  bg  bg  bg
 */
public class CollectionsGui {

    // ── Category definitions ──────────────────────────────────────────────────

    public static final String[] CATEGORIES = {
        "Mining", "Woodcutting", "Excavating", "Farming", "Fishing", "Combat"
    };

    private static final Item[] CATEGORY_ICONS = {
        Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL,
        Items.IRON_HOE, Items.FISHING_ROD, Items.IRON_SWORD
    };

    private static final ChatFormatting[] CATEGORY_COLORS = {
        ChatFormatting.GRAY, ChatFormatting.GREEN, ChatFormatting.YELLOW,
        ChatFormatting.DARK_GREEN, ChatFormatting.AQUA, ChatFormatting.RED
    };

    private static final Item[] CATEGORY_PANES = {
        Items.GRAY_STAINED_GLASS_PANE,
        Items.GREEN_STAINED_GLASS_PANE,
        Items.YELLOW_STAINED_GLASS_PANE,
        Items.LIME_STAINED_GLASS_PANE,
        Items.CYAN_STAINED_GLASS_PANE,
        Items.RED_STAINED_GLASS_PANE
    };

    // 6 category icon slots (two rows of 3, centred)
    private static final int[] CATEGORY_SLOTS = { 20, 21, 22, 23, 24, 25 };

    // ── Open ──────────────────────────────────────────────────────────────────

    public static void open(ServerPlayer viewer) {
        open(viewer, PlayerDataManager.get(viewer.getUUID()));
    }

    public static void open(ServerPlayer viewer, PlayerData data) {
        SimpleContainer inv = new SimpleContainer(54);
        Map<Integer, Runnable> handlers = new HashMap<>();

        for (int i = 0; i < 54; i++)
            inv.setItem(i, bg());

        // ── Title (slot 4) ────────────────────────────────────────────────────
        ItemStack header = new ItemStack(Items.CHEST);
        header.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        header.set(DataComponents.CUSTOM_NAME,
            Component.literal("✦ Collections").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        int totalUnlocked = 0, totalMax = CollectionType.values().length * 9;
        for (CollectionType t : CollectionType.values())
            totalUnlocked += data.getCollections().getUnlockedTier(t);

        header.set(DataComponents.LORE, new ItemLore(List.of(
            sep(),
            Component.literal(" Track resources you've gathered").withStyle(ChatFormatting.GRAY),
            Component.literal(" Unlock items, stats & recipes at each tier").withStyle(ChatFormatting.GRAY),
            Component.empty(),
            Component.literal(" Overall Progress: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(String.format("%.1f%%  (%d/%d tiers)",
                    totalUnlocked * 100.0 / totalMax, totalUnlocked, totalMax))
                    .withStyle(ChatFormatting.WHITE)),
            sep()
        )));
        inv.setItem(4, header);

        // ── Category icons ────────────────────────────────────────────────────
        CollectionData cd = data.getCollections();
        for (int i = 0; i < CATEGORIES.length; i++) {
            String cat   = CATEGORIES[i];
            int    slot  = CATEGORY_SLOTS[i];
            inv.setItem(slot, categoryIcon(cat, CATEGORY_ICONS[i], CATEGORY_COLORS[i], cd));
            handlers.put(slot, () ->
                CollectionCategoryGui.open(viewer, data, cat, () -> CollectionsGui.open(viewer, data))
            );
        }

        // ── Refresh (slot 49) ─────────────────────────────────────────────────
        ItemStack refresh = new ItemStack(Items.ARROW);
        refresh.set(DataComponents.CUSTOM_NAME,
            Component.literal("↻ Refresh").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        refresh.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal(" Click to reload your data").withStyle(ChatFormatting.DARK_GRAY)
        )));
        inv.setItem(49, refresh);
        handlers.put(49, () -> CollectionsGui.open(viewer, PlayerDataManager.get(viewer.getUUID())));

        com.peakskills.gui.core.LegacyContainerGui.open(viewer,
            Component.literal("✦ Collections").withStyle(ChatFormatting.GOLD), inv, handlers);
    }

    // ── Category icon ─────────────────────────────────────────────────────────

    private static ItemStack categoryIcon(String cat, Item icon,
                                          ChatFormatting color, CollectionData cd) {
        CollectionType[] items = typesForCategory(cat);
        int unlocked = 0; // any tier > 0
        int maxed    = 0; // all tiers done
        for (CollectionType t : items) {
            int tier = cd.getUnlockedTier(t);
            if (tier > 0) unlocked++;
            if (tier >= CollectionRegistry.getTiers(t).size()) maxed++;
        }

        ItemStack stack = new ItemStack(icon);
        if (unlocked > 0) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.set(DataComponents.CUSTOM_NAME,
            Component.literal(cat + " Collections").withStyle(color, ChatFormatting.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(sep());
        lore.add(Component.literal(" View your " + cat + " Collections!").withStyle(ChatFormatting.GRAY));
        lore.add(Component.empty());
        if (maxed >= items.length && items.length > 0) {
            lore.add(Component.literal(" ✦ Collections Maxed ✦").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else {
            lore.add(Component.literal(" Collections Unlocked: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(unlocked + "/" + items.length).withStyle(ChatFormatting.YELLOW)));
            float pct = items.length > 0 ? unlocked * 100f / items.length : 0f;
            lore.add(Component.literal(" " + bar(unlocked, items.length, 20) + " ")
                    .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.format("%.1f%%", pct)).withStyle(ChatFormatting.WHITE)));
        }
        lore.add(Component.empty());
        lore.add(Component.literal(" Click to view!").withStyle(ChatFormatting.YELLOW));
        lore.add(sep());

        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    // ── Utilities (package-visible) ───────────────────────────────────────────

    public static CollectionType[] typesForCategory(String cat) {
        List<CollectionType> out = new ArrayList<>();
        for (CollectionType t : CollectionType.values())
            if (t.category.equals(cat)) out.add(t);
        return out.toArray(new CollectionType[0]);
    }

    public static Item paneForCategory(String cat) {
        for (int i = 0; i < CATEGORIES.length; i++)
            if (CATEGORIES[i].equals(cat)) return CATEGORY_PANES[i];
        return Items.BLACK_STAINED_GLASS_PANE;
    }

    public static ChatFormatting colorForCategory(String cat) {
        for (int i = 0; i < CATEGORIES.length; i++)
            if (CATEGORIES[i].equals(cat)) return CATEGORY_COLORS[i];
        return ChatFormatting.WHITE;
    }

    static ItemStack bg() {
        ItemStack s = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        s.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        return s;
    }

    static Component sep() {
        return Component.literal(" \u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac")
            .withStyle(ChatFormatting.DARK_GRAY);
    }

    static String bar(long value, long max, int len) {
        int filled = max > 0 ? (int) Math.min(len, value * len / max) : len;
        return "█".repeat(filled) + "░".repeat(len - filled);
    }
}
