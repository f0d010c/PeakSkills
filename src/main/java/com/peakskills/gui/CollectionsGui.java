package com.peakskills.gui;

import com.peakskills.collection.CollectionData;
import com.peakskills.collection.CollectionRegistry;
import com.peakskills.collection.CollectionType;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static final Formatting[] CATEGORY_COLORS = {
        Formatting.GRAY, Formatting.GREEN, Formatting.YELLOW,
        Formatting.DARK_GREEN, Formatting.AQUA, Formatting.RED
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

    public static void open(ServerPlayerEntity viewer) {
        open(viewer, PlayerDataManager.get(viewer.getUuid()));
    }

    public static void open(ServerPlayerEntity viewer, PlayerData data) {
        SimpleInventory inv = new SimpleInventory(54);
        Map<Integer, Runnable> handlers = new HashMap<>();

        for (int i = 0; i < 54; i++)
            inv.setStack(i, bg());

        // ── Title (slot 4) ────────────────────────────────────────────────────
        ItemStack header = new ItemStack(Items.CHEST);
        header.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        header.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("✦ Collections").formatted(Formatting.GOLD, Formatting.BOLD));

        int totalUnlocked = 0, totalMax = CollectionType.values().length * 9;
        for (CollectionType t : CollectionType.values())
            totalUnlocked += data.getCollections().getUnlockedTier(t);

        header.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            sep(),
            Text.literal(" Track resources you've gathered").formatted(Formatting.GRAY),
            Text.literal(" Unlock items, stats & recipes at each tier").formatted(Formatting.GRAY),
            Text.empty(),
            Text.literal(" Overall Progress: ").formatted(Formatting.YELLOW)
                .append(Text.literal(String.format("%.1f%%  (%d/%d tiers)",
                    totalUnlocked * 100.0 / totalMax, totalUnlocked, totalMax))
                    .formatted(Formatting.WHITE)),
            sep()
        )));
        inv.setStack(4, header);

        // ── Category icons ────────────────────────────────────────────────────
        CollectionData cd = data.getCollections();
        for (int i = 0; i < CATEGORIES.length; i++) {
            String cat   = CATEGORIES[i];
            int    slot  = CATEGORY_SLOTS[i];
            inv.setStack(slot, categoryIcon(cat, CATEGORY_ICONS[i], CATEGORY_COLORS[i], cd));
            handlers.put(slot, () ->
                CollectionCategoryGui.open(viewer, data, cat, () -> CollectionsGui.open(viewer, data))
            );
        }

        // ── Refresh (slot 49) ─────────────────────────────────────────────────
        ItemStack refresh = new ItemStack(Items.ARROW);
        refresh.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("↻ Refresh").formatted(Formatting.YELLOW, Formatting.BOLD));
        refresh.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal(" Click to reload your data").formatted(Formatting.DARK_GRAY)
        )));
        inv.setStack(49, refresh);
        handlers.put(49, () -> CollectionsGui.open(viewer, PlayerDataManager.get(viewer.getUuid())));

        viewer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers),
            Text.literal("✦ Collections").formatted(Formatting.GOLD)
        ));
    }

    // ── Category icon ─────────────────────────────────────────────────────────

    private static ItemStack categoryIcon(String cat, Item icon,
                                          Formatting color, CollectionData cd) {
        CollectionType[] items = typesForCategory(cat);
        int unlocked = 0; // any tier > 0
        int maxed    = 0; // all tiers done
        for (CollectionType t : items) {
            int tier = cd.getUnlockedTier(t);
            if (tier > 0) unlocked++;
            if (tier >= CollectionRegistry.getTiers(t).size()) maxed++;
        }

        ItemStack stack = new ItemStack(icon);
        if (unlocked > 0) stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(cat + " Collections").formatted(color, Formatting.BOLD));

        List<Text> lore = new ArrayList<>();
        lore.add(sep());
        lore.add(Text.literal(" View your " + cat + " Collections!").formatted(Formatting.GRAY));
        lore.add(Text.empty());
        if (maxed >= items.length && items.length > 0) {
            lore.add(Text.literal(" ✦ Collections Maxed ✦").formatted(Formatting.GOLD, Formatting.BOLD));
        } else {
            lore.add(Text.literal(" Collections Unlocked: ").formatted(Formatting.GRAY)
                .append(Text.literal(unlocked + "/" + items.length).formatted(Formatting.YELLOW)));
            float pct = items.length > 0 ? unlocked * 100f / items.length : 0f;
            lore.add(Text.literal(" " + bar(unlocked, items.length, 20) + " ")
                    .formatted(Formatting.GREEN)
                .append(Text.literal(String.format("%.1f%%", pct)).formatted(Formatting.WHITE)));
        }
        lore.add(Text.empty());
        lore.add(Text.literal(" Click to view!").formatted(Formatting.YELLOW));
        lore.add(sep());

        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
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

    public static Formatting colorForCategory(String cat) {
        for (int i = 0; i < CATEGORIES.length; i++)
            if (CATEGORIES[i].equals(cat)) return CATEGORY_COLORS[i];
        return Formatting.WHITE;
    }

    static ItemStack bg() {
        ItemStack s = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
        return s;
    }

    static Text sep() {
        return Text.literal(" \u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac")
            .formatted(Formatting.DARK_GRAY);
    }

    static String bar(long value, long max, int len) {
        int filled = max > 0 ? (int) Math.min(len, value * len / max) : len;
        return "█".repeat(filled) + "░".repeat(len - filled);
    }
}
