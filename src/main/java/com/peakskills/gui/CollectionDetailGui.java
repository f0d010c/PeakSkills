package com.peakskills.gui;

import com.peakskills.collection.*;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * Hypixel SkyBlock-style collection detail view.
 *
 * Layout (54 slots):
 *  Row 0 │ col col col col [TITLE] col col col col   ← coloured stripe
 *  Row 1 │  bg [T1] [T2] [T3] [T4] [T5] [T6] [T7]  bg
 *  Row 2 │  bg [T8] [T9]  bg  [PROGRESS]  bg   bg   bg   bg
 *  Row 3 │  bg  bg  bg   bg   [BONUSES]   bg   bg   bg   bg
 *  Row 4 │  bg  bg  bg   bg    bg         bg   bg   bg   bg
 *  Row 5 │  bg  bg  bg   bg  [← BACK]    bg   bg   bg   bg
 *
 * Each tier slot shows:
 *  • Unlocked  → actual collection item, glowing, GREEN name
 *  • Current   → YELLOW pane, gold name + progress bar in lore
 *  • Locked    → GRAY pane, dark-gray name
 */
public class CollectionDetailGui {

    // Tier slots: T1-T7 in row 1 (slots 10-16), T8-T9 in row 2 (19-20)
    private static final int[] TIER_SLOTS = { 10, 11, 12, 13, 14, 15, 16, 19, 20 };

    public static void open(ServerPlayer viewer, PlayerData data,
                            CollectionType type, Runnable backAction) {
        SimpleContainer inv = new SimpleContainer(54);
        Map<Integer, Runnable> handlers = new HashMap<>();

        CollectionData cd       = data.getCollections();
        long           count    = cd.getCount(type);
        int            unlocked = cd.getUnlockedTier(type);
        List<CollectionTier> tiers = CollectionRegistry.getTiers(type);
        int            maxTier = tiers.size();

        // ── Background ────────────────────────────────────────────────────────
        for (int i = 0; i < 54; i++)
            inv.setItem(i, pane(Items.BLACK_STAINED_GLASS_PANE, " "));

        // ── Coloured header stripe (slots 0-3, 5-8) ───────────────────────────
        Item stripe = stripePane(type);
        for (int col : new int[]{0, 1, 2, 3, 5, 6, 7, 8})
            inv.setItem(col, pane(stripe, " "));

        // ── Title (slot 4) ────────────────────────────────────────────────────
        ItemStack title = new ItemStack(type.icon);
        title.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, unlocked > 0);
        title.set(DataComponents.CUSTOM_NAME,
            Component.literal(type.displayName + " Collection").withStyle(type.color, ChatFormatting.BOLD));
        title.set(DataComponents.LORE, new ItemLore(List.of(
            sep(),
            Component.literal(" Category: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(type.category).withStyle(ChatFormatting.WHITE)),
            Component.literal(String.format(" Collected: %,d", count)).withStyle(ChatFormatting.GRAY),
            Component.literal(" Tier: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(unlocked + " / " + maxTier)
                    .withStyle(unlocked >= maxTier ? ChatFormatting.GOLD : ChatFormatting.WHITE, ChatFormatting.BOLD)),
            sep()
        )));
        inv.setItem(4, title);

        // ── Tier slots ────────────────────────────────────────────────────────
        for (int i = 0; i < maxTier; i++) {
            CollectionTier tier  = tiers.get(i);
            int            slot  = TIER_SLOTS[i];
            boolean reached      = (i + 1) <= unlocked;
            boolean isCurrent    = (i + 1) == unlocked + 1 && unlocked < maxTier;

            inv.setItem(slot, tierItem(type, tier, count, reached, isCurrent, i + 1));
        }

        // ── Progress summary (slot 22) ────────────────────────────────────────
        inv.setItem(22, progressItem(type, cd, count, unlocked, tiers, maxTier));

        // ── Active stat bonuses (slot 31) ─────────────────────────────────────
        inv.setItem(31, bonusItem(type, cd, unlocked, tiers));

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
            Component.literal(type.displayName + " Collection").withStyle(type.color)
        ));
    }

    // ── Tier item ─────────────────────────────────────────────────────────────

    private static ItemStack tierItem(CollectionType type, CollectionTier tier,
                                      long count, boolean reached, boolean isCurrent,
                                      int tierNum) {
        // Glass pane with count = tier number (shows in bottom-right corner of slot)
        Item icon;
        ChatFormatting nameColor;
        String namePrefix;

        if (reached) {
            icon      = Items.LIME_STAINED_GLASS_PANE;
            nameColor = ChatFormatting.GREEN;
            namePrefix = "✔ Tier " + tier.tierLabel();
        } else if (isCurrent) {
            icon      = Items.YELLOW_STAINED_GLASS_PANE;
            nameColor = ChatFormatting.GOLD;
            namePrefix = "◆ Tier " + tier.tierLabel();
        } else {
            icon      = Items.GRAY_STAINED_GLASS_PANE;
            nameColor = ChatFormatting.DARK_GRAY;
            namePrefix = "✗ Tier " + tier.tierLabel();
        }

        ItemStack stack = new ItemStack(icon);
        stack.setCount(tierNum);  // ← tier number shows in bottom-right corner
        if (reached) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        stack.set(DataComponents.CUSTOM_NAME,
            Component.literal(namePrefix).withStyle(nameColor, ChatFormatting.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(sep());

        // Threshold
        lore.add(Component.literal(" Required: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%,d × %s", tier.threshold(), type.displayName))
                .withStyle(ChatFormatting.WHITE)));

        // Progress bar for current tier
        if (isCurrent) {
            long needed   = tier.threshold();
            float pct     = needed > 0 ? (float) count / needed * 100f : 100f;
            lore.add(Component.empty());
            lore.add(Component.literal(" " + CollectionsGui.bar(count, needed, 20) + " ")
                    .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(String.format("%.1f%%", pct)).withStyle(ChatFormatting.WHITE)));
            lore.add(Component.literal(String.format(" %,d / %,d", count, needed)).withStyle(ChatFormatting.GRAY));
        }

        // Rewards
        if (!tier.rewards().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.literal(" Reward:").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            for (CollectionReward r : tier.rewards())
                lore.add(rewardLine(r));
        }

        // Status footer
        lore.add(Component.empty());
        if (reached) {
            lore.add(Component.literal(" ✔ UNLOCKED").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        } else if (isCurrent) {
            long remaining = Math.max(0, tier.threshold() - count);
            lore.add(Component.literal(String.format(" Need %,d more %s", remaining, type.displayName))
                .withStyle(ChatFormatting.RED));
        } else {
            lore.add(Component.literal(" ✗ LOCKED").withStyle(ChatFormatting.DARK_GRAY));
        }
        lore.add(sep());

        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    // ── Progress summary item ─────────────────────────────────────────────────

    private static ItemStack progressItem(CollectionType type, CollectionData cd,
                                          long count, int unlocked,
                                          List<CollectionTier> tiers, int maxTier) {
        ItemStack stack = new ItemStack(Items.BOOK);
        stack.set(DataComponents.CUSTOM_NAME,
            Component.literal("Collection Progress").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(sep());
        lore.add(Component.literal(String.format(" Total Collected: %,d", count)).withStyle(ChatFormatting.WHITE));
        lore.add(Component.literal(" Tiers Unlocked: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(unlocked + " / " + maxTier)
                .withStyle(unlocked >= maxTier ? ChatFormatting.GOLD : ChatFormatting.AQUA)));

        if (unlocked < maxTier) {
            long next = tiers.get(unlocked).threshold();
            float pct = next > 0 ? (float) count / next * 100f : 100f;
            lore.add(Component.empty());
            lore.add(Component.literal(" Next Tier " + tiers.get(unlocked).tierLabel() + ":").withStyle(ChatFormatting.YELLOW));
            lore.add(Component.literal(" " + CollectionsGui.bar(count, next, 20) + " ")
                    .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.format("%.1f%%", pct)).withStyle(ChatFormatting.WHITE)));
            lore.add(Component.literal(String.format(" %,d / %,d", count, next)).withStyle(ChatFormatting.GRAY));
        } else {
            lore.add(Component.empty());
            lore.add(Component.literal(" ✦ MAX TIER REACHED ✦").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }

        lore.add(sep());
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    // ── Active bonuses item ───────────────────────────────────────────────────

    private static ItemStack bonusItem(CollectionType type, CollectionData cd,
                                       int unlocked, List<CollectionTier> tiers) {
        ItemStack stack = new ItemStack(Items.NETHER_STAR);
        stack.set(DataComponents.CUSTOM_NAME,
            Component.literal("Active Bonuses").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(sep());

        boolean hasStat   = false;
        boolean hasRecipe = false;

        for (int i = 0; i < unlocked; i++) {
            for (CollectionReward r : tiers.get(i).rewards()) {
                switch (r) {
                    case CollectionReward.StatBonus sb -> {
                        hasStat = true;
                        lore.add(Component.literal(" +" + sb.displayValue() + " "
                            + sb.stat().getIcon() + " " + sb.stat().getDisplayName())
                            .withStyle(ChatFormatting.GREEN));
                    }
                    case CollectionReward.RecipeUnlock ru -> {
                        hasRecipe = true;
                        lore.add(Component.literal(" ✦ Recipe: " + formatPath(ru.recipeId().getPath()))
                            .withStyle(ChatFormatting.AQUA));
                    }
                    default -> {}
                }
            }
        }

        if (!hasStat && !hasRecipe) {
            lore.add(Component.literal(" No stat bonuses yet").withStyle(ChatFormatting.DARK_GRAY));
            lore.add(Component.literal(" Unlock tiers to gain bonuses").withStyle(ChatFormatting.DARK_GRAY));
        }

        lore.add(sep());
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    // ── Stripe color per collection category ─────────────────────────────────

    private static Item stripePane(CollectionType type) {
        return switch (type.category) {
            case "Mining"      -> Items.GRAY_STAINED_GLASS_PANE;
            case "Woodcutting" -> Items.GREEN_STAINED_GLASS_PANE;
            case "Excavating"  -> Items.YELLOW_STAINED_GLASS_PANE;
            case "Farming"     -> Items.LIME_STAINED_GLASS_PANE;
            case "Fishing"     -> Items.CYAN_STAINED_GLASS_PANE;
            case "Combat"      -> Items.RED_STAINED_GLASS_PANE;
            default            -> Items.GRAY_STAINED_GLASS_PANE;
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Component rewardLine(CollectionReward reward) {
        return switch (reward) {
            case CollectionReward.ItemReward ir ->
                Component.literal("  ◆ x" + ir.stack().getCount() + " "
                    + ir.stack().getHoverName().getString()).withStyle(ChatFormatting.GREEN);
            case CollectionReward.StatBonus sb ->
                Component.literal("  ◆ +" + sb.displayValue() + " "
                    + sb.stat().getIcon() + " " + sb.stat().getDisplayName()).withStyle(ChatFormatting.GREEN);
            case CollectionReward.RecipeUnlock ru ->
                Component.literal("  ◆ Recipe: " + formatPath(ru.recipeId().getPath())).withStyle(ChatFormatting.AQUA);
        };
    }

    private static Component sep() {
        return Component.literal(" \u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac")
            .withStyle(ChatFormatting.DARK_GRAY);
    }

    private static ItemStack pane(Item item, String name) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return s;
    }

    private static String formatPath(String path) {
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : path.toCharArray()) {
            if (c == '_') { sb.append(' '); cap = true; }
            else if (cap) { sb.append(Character.toUpperCase(c)); cap = false; }
            else          { sb.append(c); }
        }
        return sb.toString();
    }
}
