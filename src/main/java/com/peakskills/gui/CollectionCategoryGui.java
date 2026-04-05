package com.peakskills.gui;

import com.peakskills.collection.CollectionData;
import com.peakskills.collection.CollectionRegistry;
import com.peakskills.collection.CollectionTier;
import com.peakskills.collection.CollectionType;
import com.peakskills.player.PlayerData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.inventory.SimpleInventory;
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
 * Dense category view — all collection items for one category shown as a grid.
 * Matches Hypixel SkyBlock's per-category collection screen.
 *
 * Layout (54 slots):
 *  Row 0 │ col col col col [TITLE] col col col col   ← category stripe
 *  Rows 1-4 │ items packed left→right, bg filling gaps (7 items wide, skipping cols 0 and 8)
 *  Row 5 │ bg  bg  bg  bg  [← Back]  bg  bg  bg  bg
 */
public class CollectionCategoryGui {

    // Item slots: rows 1-4, columns 1-7  (avoids border columns 0 and 8)
    private static final int[] ITEM_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,   // row 1
        19, 20, 21, 22, 23, 24, 25,   // row 2
        28, 29, 30, 31, 32, 33, 34,   // row 3
        37, 38, 39, 40, 41, 42, 43    // row 4
    };

    public static void open(ServerPlayerEntity viewer, PlayerData data,
                            String category, Runnable backAction) {
        SimpleInventory inv = new SimpleInventory(54);
        Map<Integer, Runnable> handlers = new HashMap<>();

        CollectionType[] types = CollectionsGui.typesForCategory(category);
        CollectionData   cd    = data.getCollections();

        // ── Background ────────────────────────────────────────────────────────
        for (int i = 0; i < 54; i++)
            inv.setStack(i, CollectionsGui.bg());

        // ── Coloured header stripe (cols 0-3, 5-8) ───────────────────────────
        for (int col : new int[]{0, 1, 2, 3, 5, 6, 7, 8}) {
            ItemStack p = new ItemStack(CollectionsGui.paneForCategory(category));
            p.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            inv.setStack(col, p);
        }

        // ── Title (slot 4) ────────────────────────────────────────────────────
        int maxed = 0;
        for (CollectionType t : types)
            if (cd.getUnlockedTier(t) >= CollectionRegistry.getTiers(t).size()) maxed++;

        ItemStack title = new ItemStack(iconForCategory(category));
        title.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        title.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(category + " Collections")
                .formatted(CollectionsGui.colorForCategory(category), Formatting.BOLD));
        title.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            CollectionsGui.sep(),
            Text.literal(" Collections: ").formatted(Formatting.GRAY)
                .append(Text.literal(types.length + " types").formatted(Formatting.WHITE)),
            Text.literal(" Maxed: ").formatted(Formatting.GRAY)
                .append(Text.literal(maxed + " / " + types.length).formatted(Formatting.YELLOW)),
            CollectionsGui.sep()
        )));
        inv.setStack(4, title);

        // ── Collection items ──────────────────────────────────────────────────
        for (int i = 0; i < types.length && i < ITEM_SLOTS.length; i++) {
            CollectionType type = types[i];
            int slot = ITEM_SLOTS[i];
            inv.setStack(slot, collectionItem(type, cd));
            handlers.put(slot, () ->
                CollectionDetailGui.open(viewer, data, type, () ->
                    CollectionCategoryGui.open(viewer, data, category, backAction))
            );
        }

        // ── Back button (slot 49) ─────────────────────────────────────────────
        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("← Back to Collections").formatted(Formatting.YELLOW, Formatting.BOLD));
        back.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal(" Click to return").formatted(Formatting.DARK_GRAY)
        )));
        inv.setStack(49, back);
        handlers.put(49, backAction);

        viewer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers),
            Text.literal(category + " Collections")
                .formatted(CollectionsGui.colorForCategory(category))
        ));
    }

    // ── Collection item ───────────────────────────────────────────────────────

    private static ItemStack collectionItem(CollectionType type, CollectionData cd) {
        long count    = cd.getCount(type);
        int  unlocked = cd.getUnlockedTier(type);
        List<CollectionTier> tiers = CollectionRegistry.getTiers(type);
        int  maxTier  = tiers.size();

        ItemStack stack = new ItemStack(type.icon);
        if (unlocked > 0) stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(type.displayName + " Collection").formatted(type.color, Formatting.BOLD));

        List<Text> lore = new ArrayList<>();
        lore.add(CollectionsGui.sep());

        // Tier
        Formatting tierColor = unlocked >= maxTier ? Formatting.GOLD : Formatting.WHITE;
        lore.add(Text.literal(" Tier: ").formatted(Formatting.GRAY)
            .append(Text.literal(unlocked + " / " + maxTier).formatted(tierColor, Formatting.BOLD)));

        // Progress toward next tier
        if (unlocked < maxTier) {
            long next = tiers.get(unlocked).threshold();
            float pct = next > 0 ? (float) count / next * 100f : 100f;
            lore.add(Text.literal(" " + CollectionsGui.bar(count, next, 20) + " ")
                    .formatted(Formatting.GREEN)
                .append(Text.literal(String.format("%.1f%%", pct)).formatted(Formatting.WHITE)));
            lore.add(Text.literal(String.format(" %,d / %,d", count, next)).formatted(Formatting.GRAY));
        } else {
            lore.add(Text.literal(" ✦ MAXED OUT ✦").formatted(Formatting.GOLD, Formatting.BOLD));
            lore.add(Text.literal(String.format(" %,d collected", count)).formatted(Formatting.GRAY));
        }

        lore.add(Text.empty());
        lore.add(Text.literal(" Click to view tiers!").formatted(Formatting.YELLOW));
        lore.add(CollectionsGui.sep());

        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    // ── Category icon helper ──────────────────────────────────────────────────

    private static net.minecraft.item.Item iconForCategory(String cat) {
        return switch (cat) {
            case "Mining"      -> Items.IRON_PICKAXE;
            case "Woodcutting" -> Items.IRON_AXE;
            case "Excavating"  -> Items.IRON_SHOVEL;
            case "Farming"     -> Items.IRON_HOE;
            case "Combat"      -> Items.IRON_SWORD;
            default            -> Items.CHEST;
        };
    }
}
