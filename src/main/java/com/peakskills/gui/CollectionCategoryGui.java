package com.peakskills.gui;

import com.peakskills.collection.CollectionData;
import com.peakskills.collection.CollectionRegistry;
import com.peakskills.collection.CollectionTier;
import com.peakskills.collection.CollectionType;
import com.peakskills.player.PlayerData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

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

    public static void open(ServerPlayer viewer, PlayerData data,
                            String category, Runnable backAction) {
        SimpleContainer inv = new SimpleContainer(54);
        Map<Integer, Runnable> handlers = new HashMap<>();

        CollectionType[] types = CollectionsGui.typesForCategory(category);
        CollectionData   cd    = data.getCollections();

        // ── Background ────────────────────────────────────────────────────────
        for (int i = 0; i < 54; i++)
            inv.setItem(i, CollectionsGui.bg());

        // ── Coloured header stripe (cols 0-3, 5-8) ───────────────────────────
        for (int col : new int[]{0, 1, 2, 3, 5, 6, 7, 8}) {
            ItemStack p = new ItemStack(CollectionsGui.paneForCategory(category));
            p.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            inv.setItem(col, p);
        }

        // ── Title (slot 4) ────────────────────────────────────────────────────
        int maxed = 0;
        for (CollectionType t : types)
            if (cd.getUnlockedTier(t) >= CollectionRegistry.getTiers(t).size()) maxed++;

        ItemStack title = new ItemStack(iconForCategory(category));
        title.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        title.set(DataComponents.CUSTOM_NAME,
            Component.literal(category + " Collections")
                .withStyle(CollectionsGui.colorForCategory(category), ChatFormatting.BOLD));
        title.set(DataComponents.LORE, new ItemLore(List.of(
            CollectionsGui.sep(),
            Component.literal(" Collections: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(types.length + " types").withStyle(ChatFormatting.WHITE)),
            Component.literal(" Maxed: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(maxed + " / " + types.length).withStyle(ChatFormatting.YELLOW)),
            CollectionsGui.sep()
        )));
        inv.setItem(4, title);

        // ── Collection items ──────────────────────────────────────────────────
        for (int i = 0; i < types.length && i < ITEM_SLOTS.length; i++) {
            CollectionType type = types[i];
            int slot = ITEM_SLOTS[i];
            inv.setItem(slot, collectionItem(type, cd));
            handlers.put(slot, () ->
                CollectionDetailGui.open(viewer, data, type, () ->
                    CollectionCategoryGui.open(viewer, data, category, backAction))
            );
        }

        // ── Back button (slot 49) ─────────────────────────────────────────────
        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponents.CUSTOM_NAME,
            Component.literal("← Back to Collections").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        back.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal(" Click to return").withStyle(ChatFormatting.DARK_GRAY)
        )));
        inv.setItem(49, back);
        handlers.put(49, backAction);

        viewer.openMenu(new SimpleMenuProvider(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers),
            Component.literal(category + " Collections")
                .withStyle(CollectionsGui.colorForCategory(category))
        ));
    }

    // ── Collection item ───────────────────────────────────────────────────────

    private static ItemStack collectionItem(CollectionType type, CollectionData cd) {
        long count    = cd.getCount(type);
        int  unlocked = cd.getUnlockedTier(type);
        List<CollectionTier> tiers = CollectionRegistry.getTiers(type);
        int  maxTier  = tiers.size();

        ItemStack stack = new ItemStack(type.icon);
        if (unlocked > 0) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        stack.set(DataComponents.CUSTOM_NAME,
            Component.literal(type.displayName).withStyle(type.color, ChatFormatting.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(CollectionsGui.sep());

        // Tier
        ChatFormatting tierColor = unlocked >= maxTier ? ChatFormatting.GOLD : ChatFormatting.WHITE;
        lore.add(Component.literal(" Tier: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(unlocked + " / " + maxTier).withStyle(tierColor, ChatFormatting.BOLD)));

        // Progress toward next tier
        if (unlocked < maxTier) {
            long next = tiers.get(unlocked).threshold();
            float pct = next > 0 ? (float) count / next * 100f : 100f;
            lore.add(Component.literal(" " + CollectionsGui.bar(count, next, 20) + " ")
                    .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.format("%.1f%%", pct)).withStyle(ChatFormatting.WHITE)));
            lore.add(Component.literal(String.format(" %,d / %,d", count, next)).withStyle(ChatFormatting.GRAY));
        } else {
            lore.add(Component.literal(" ✦ MAXED OUT ✦").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            lore.add(Component.literal(String.format(" %,d collected", count)).withStyle(ChatFormatting.GRAY));
        }

        lore.add(Component.empty());
        lore.add(Component.literal(" Click to view tiers!").withStyle(ChatFormatting.YELLOW));
        lore.add(CollectionsGui.sep());

        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    // ── Category icon helper ──────────────────────────────────────────────────

    private static net.minecraft.world.item.Item iconForCategory(String cat) {
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
