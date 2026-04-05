package com.peakskills.gui;

import com.peakskills.collection.*;
import com.peakskills.player.PlayerData;
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

    public static void open(ServerPlayerEntity viewer, PlayerData data,
                            CollectionType type, Runnable backAction) {
        SimpleInventory inv = new SimpleInventory(54);
        Map<Integer, Runnable> handlers = new HashMap<>();

        CollectionData cd       = data.getCollections();
        long           count    = cd.getCount(type);
        int            unlocked = cd.getUnlockedTier(type);
        List<CollectionTier> tiers = CollectionRegistry.getTiers(type);
        int            maxTier = tiers.size();

        // ── Background ────────────────────────────────────────────────────────
        for (int i = 0; i < 54; i++)
            inv.setStack(i, pane(Items.BLACK_STAINED_GLASS_PANE, " "));

        // ── Coloured header stripe (slots 0-3, 5-8) ───────────────────────────
        Item stripe = stripePane(type);
        for (int col : new int[]{0, 1, 2, 3, 5, 6, 7, 8})
            inv.setStack(col, pane(stripe, " "));

        // ── Title (slot 4) ────────────────────────────────────────────────────
        ItemStack title = new ItemStack(type.icon);
        title.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, unlocked > 0);
        title.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(type.displayName + " Collection").formatted(type.color, Formatting.BOLD));
        title.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            sep(),
            Text.literal(" Category: ").formatted(Formatting.GRAY)
                .append(Text.literal(type.category).formatted(Formatting.WHITE)),
            Text.literal(String.format(" Collected: %,d", count)).formatted(Formatting.GRAY),
            Text.literal(" Tier: ").formatted(Formatting.GRAY)
                .append(Text.literal(unlocked + " / " + maxTier)
                    .formatted(unlocked >= maxTier ? Formatting.GOLD : Formatting.WHITE, Formatting.BOLD)),
            sep()
        )));
        inv.setStack(4, title);

        // ── Tier slots ────────────────────────────────────────────────────────
        for (int i = 0; i < maxTier; i++) {
            CollectionTier tier  = tiers.get(i);
            int            slot  = TIER_SLOTS[i];
            boolean reached      = (i + 1) <= unlocked;
            boolean isCurrent    = (i + 1) == unlocked + 1 && unlocked < maxTier;

            inv.setStack(slot, tierItem(type, tier, count, reached, isCurrent, i + 1));
        }

        // ── Progress summary (slot 22) ────────────────────────────────────────
        inv.setStack(22, progressItem(type, cd, count, unlocked, tiers, maxTier));

        // ── Active stat bonuses (slot 31) ─────────────────────────────────────
        inv.setStack(31, bonusItem(type, cd, unlocked, tiers));

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
            Text.literal(type.displayName + " Collection").formatted(type.color)
        ));
    }

    // ── Tier item ─────────────────────────────────────────────────────────────

    private static ItemStack tierItem(CollectionType type, CollectionTier tier,
                                      long count, boolean reached, boolean isCurrent,
                                      int tierNum) {
        // Glass pane with count = tier number (shows in bottom-right corner of slot)
        Item icon;
        Formatting nameColor;
        String namePrefix;

        if (reached) {
            icon      = Items.LIME_STAINED_GLASS_PANE;
            nameColor = Formatting.GREEN;
            namePrefix = "✔ Tier " + tier.tierLabel();
        } else if (isCurrent) {
            icon      = Items.YELLOW_STAINED_GLASS_PANE;
            nameColor = Formatting.GOLD;
            namePrefix = "◆ Tier " + tier.tierLabel();
        } else {
            icon      = Items.GRAY_STAINED_GLASS_PANE;
            nameColor = Formatting.DARK_GRAY;
            namePrefix = "✗ Tier " + tier.tierLabel();
        }

        ItemStack stack = new ItemStack(icon);
        stack.setCount(tierNum);  // ← tier number shows in bottom-right corner
        if (reached) stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(namePrefix).formatted(nameColor, Formatting.BOLD));

        List<Text> lore = new ArrayList<>();
        lore.add(sep());

        // Threshold
        lore.add(Text.literal(" Required: ").formatted(Formatting.GRAY)
            .append(Text.literal(String.format("%,d × %s", tier.threshold(), type.displayName))
                .formatted(Formatting.WHITE)));

        // Progress bar for current tier
        if (isCurrent) {
            long needed   = tier.threshold();
            float pct     = needed > 0 ? (float) count / needed * 100f : 100f;
            lore.add(Text.empty());
            lore.add(Text.literal(" " + CollectionsGui.bar(count, needed, 20) + " ")
                    .formatted(Formatting.YELLOW)
                .append(Text.literal(String.format("%.1f%%", pct)).formatted(Formatting.WHITE)));
            lore.add(Text.literal(String.format(" %,d / %,d", count, needed)).formatted(Formatting.GRAY));
        }

        // Rewards
        if (!tier.rewards().isEmpty()) {
            lore.add(Text.empty());
            lore.add(Text.literal(" Reward:").formatted(Formatting.GOLD, Formatting.BOLD));
            for (CollectionReward r : tier.rewards())
                lore.add(rewardLine(r));
        }

        // Status footer
        lore.add(Text.empty());
        if (reached) {
            lore.add(Text.literal(" ✔ UNLOCKED").formatted(Formatting.GREEN, Formatting.BOLD));
        } else if (isCurrent) {
            long remaining = Math.max(0, tier.threshold() - count);
            lore.add(Text.literal(String.format(" Need %,d more %s", remaining, type.displayName))
                .formatted(Formatting.RED));
        } else {
            lore.add(Text.literal(" ✗ LOCKED").formatted(Formatting.DARK_GRAY));
        }
        lore.add(sep());

        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    // ── Progress summary item ─────────────────────────────────────────────────

    private static ItemStack progressItem(CollectionType type, CollectionData cd,
                                          long count, int unlocked,
                                          List<CollectionTier> tiers, int maxTier) {
        ItemStack stack = new ItemStack(Items.BOOK);
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("Collection Progress").formatted(Formatting.AQUA, Formatting.BOLD));

        List<Text> lore = new ArrayList<>();
        lore.add(sep());
        lore.add(Text.literal(String.format(" Total Collected: %,d", count)).formatted(Formatting.WHITE));
        lore.add(Text.literal(" Tiers Unlocked: ").formatted(Formatting.GRAY)
            .append(Text.literal(unlocked + " / " + maxTier)
                .formatted(unlocked >= maxTier ? Formatting.GOLD : Formatting.AQUA)));

        if (unlocked < maxTier) {
            long next = tiers.get(unlocked).threshold();
            float pct = next > 0 ? (float) count / next * 100f : 100f;
            lore.add(Text.empty());
            lore.add(Text.literal(" Next Tier " + tiers.get(unlocked).tierLabel() + ":").formatted(Formatting.YELLOW));
            lore.add(Text.literal(" " + CollectionsGui.bar(count, next, 20) + " ")
                    .formatted(Formatting.GREEN)
                .append(Text.literal(String.format("%.1f%%", pct)).formatted(Formatting.WHITE)));
            lore.add(Text.literal(String.format(" %,d / %,d", count, next)).formatted(Formatting.GRAY));
        } else {
            lore.add(Text.empty());
            lore.add(Text.literal(" ✦ MAX TIER REACHED ✦").formatted(Formatting.GOLD, Formatting.BOLD));
        }

        lore.add(sep());
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    // ── Active bonuses item ───────────────────────────────────────────────────

    private static ItemStack bonusItem(CollectionType type, CollectionData cd,
                                       int unlocked, List<CollectionTier> tiers) {
        ItemStack stack = new ItemStack(Items.NETHER_STAR);
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("Active Bonuses").formatted(Formatting.GOLD, Formatting.BOLD));

        List<Text> lore = new ArrayList<>();
        lore.add(sep());

        boolean hasStat   = false;
        boolean hasRecipe = false;

        for (int i = 0; i < unlocked; i++) {
            for (CollectionReward r : tiers.get(i).rewards()) {
                switch (r) {
                    case CollectionReward.StatBonus sb -> {
                        hasStat = true;
                        lore.add(Text.literal(" +" + sb.displayValue() + " "
                            + sb.stat().getIcon() + " " + sb.stat().getDisplayName())
                            .formatted(Formatting.GREEN));
                    }
                    case CollectionReward.RecipeUnlock ru -> {
                        hasRecipe = true;
                        lore.add(Text.literal(" ✦ Recipe: " + formatPath(ru.recipeId().getPath()))
                            .formatted(Formatting.AQUA));
                    }
                    default -> {}
                }
            }
        }

        if (!hasStat && !hasRecipe) {
            lore.add(Text.literal(" No stat bonuses yet").formatted(Formatting.DARK_GRAY));
            lore.add(Text.literal(" Unlock tiers to gain bonuses").formatted(Formatting.DARK_GRAY));
        }

        lore.add(sep());
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
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

    private static Text rewardLine(CollectionReward reward) {
        return switch (reward) {
            case CollectionReward.ItemReward ir ->
                Text.literal("  ◆ x" + ir.stack().getCount() + " "
                    + ir.stack().getName().getString()).formatted(Formatting.GREEN);
            case CollectionReward.StatBonus sb ->
                Text.literal("  ◆ +" + sb.displayValue() + " "
                    + sb.stat().getIcon() + " " + sb.stat().getDisplayName()).formatted(Formatting.GREEN);
            case CollectionReward.RecipeUnlock ru ->
                Text.literal("  ◆ Recipe: " + formatPath(ru.recipeId().getPath())).formatted(Formatting.AQUA);
        };
    }

    private static Text sep() {
        return Text.literal(" \u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac")
            .formatted(Formatting.DARK_GRAY);
    }

    private static ItemStack pane(Item item, String name) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
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
