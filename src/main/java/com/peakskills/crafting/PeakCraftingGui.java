package com.peakskills.crafting;

import com.peakskills.gui.SkillsScreenHandler;
import com.peakskills.skill.Skill;
import com.peakskills.xp.XpManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
 * Skyblock-style crafting GUI.
 *
 * ── List view ──────────────────────────────────────────────────────────────
 *  Row 0: bg  bg  bg  bg [Title] bg  bg  bg  bg
 *  Row 1: bg [R0][R1][R2][R3][R4][R5][R6] bg
 *  Row 2: bg [R7] ...
 *  Row 3-4: bg
 *  Row 5: bg  bg  bg  bg  bg  bg  bg  bg  bg
 *
 * ── Detail view ────────────────────────────────────────────────────────────
 *  Row 0: bg  bg  bg  bg  bg  bg  bg  bg  bg
 *  Row 1: bg [G0][G1][G2] bg [→] bg [Res] bg
 *  Row 2: bg [G3][G4][G5] bg  bg  bg  bg  bg
 *  Row 3: bg [G6][G7][G8] bg  bg  bg  bg  bg
 *  Row 4: bg  bg  bg  bg  bg  bg  bg  bg  bg
 *  Row 5: [←Back] bg bg bg bg bg [Craft] bg bg
 *
 *  G0-G8 map to 3×3 grid slots (index 0=top-left … 8=bottom-right).
 *  Item stack count = required quantity (shows as the corner number like Skyblock).
 */
public class PeakCraftingGui {

    // ── Craft cooldown (per-player, 1 second) ─────────────────────────────────
    private static final Map<UUID, Long> lastCraftTime = new ConcurrentHashMap<>();
    private static final long CRAFT_COOLDOWN_MS = 1_000;

    // ── Detail view slot constants ─────────────────────────────────────────────

    // 3×3 grid: rows 1-3, cols 1-3  (inv slots 10,11,12 / 19,20,21 / 28,29,30)
    private static final int[] GRID_INV_SLOTS = { 10, 11, 12, 19, 20, 21, 28, 29, 30 };

    private static final int ARROW_SLOT  = 14;
    private static final int RESULT_SLOT = 16;
    private static final int BACK_SLOT   = 45;
    private static final int CRAFT_SLOT  = 51;

    // ── List view slot constants ───────────────────────────────────────────────

    // Recipe icons fill rows 1-4, cols 1-7 (up to 28 recipes)
    private static final int[] LIST_RECIPE_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    // ── Open ──────────────────────────────────────────────────────────────────

    public static void open(ServerPlayer player) {
        openList(player);
    }

    // ── List view ─────────────────────────────────────────────────────────────

    private static void openList(ServerPlayer player) {
        List<PeakRecipe> recipes = new ArrayList<>(PeakRecipeRegistry.getAll());

        SimpleContainer inv = new SimpleContainer(54);
        fill(inv, pane(" "));

        // Title
        ItemStack header = new ItemStack(Items.CRAFTING_TABLE);
        header.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        header.set(DataComponents.CUSTOM_NAME,
            Component.literal("(" + recipes.size() + ") PeakSkills Recipes")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        inv.setItem(4, header);

        Map<Integer, Runnable> handlers = new HashMap<>();

        for (int i = 0; i < recipes.size() && i < LIST_RECIPE_SLOTS.length; i++) {
            PeakRecipe recipe = recipes.get(i);
            int slot = LIST_RECIPE_SLOTS[i];
            inv.setItem(slot, listIcon(recipe, player));
            handlers.put(slot, () -> openDetail(player, recipe));
        }

        player.openMenu(new SimpleMenuProvider(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers),
            Component.literal("PeakSkills Recipes").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        ));
    }

    // ── Detail view ───────────────────────────────────────────────────────────

    private static void openDetail(ServerPlayer player, PeakRecipe recipe) {
        SimpleContainer inv = new SimpleContainer(54);
        populateDetail(inv, player, recipe);

        Map<Integer, Runnable> handlers = new HashMap<>();
        handlers.put(BACK_SLOT,  () -> openList(player));
        handlers.put(CRAFT_SLOT, () -> tryCraft(player, recipe, inv));

        player.openMenu(new SimpleMenuProvider(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers),
            Component.literal(recipe.displayName()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
        ));
    }

    private static void populateDetail(SimpleContainer inv, ServerPlayer player, PeakRecipe recipe) {
        fill(inv, pane(" "));

        // Populate 3×3 grid
        PeakIngredient[] grid = new PeakIngredient[9];
        for (PeakIngredient ing : recipe.ingredients()) {
            if (ing.gridSlot() >= 0 && ing.gridSlot() < 9) {
                grid[ing.gridSlot()] = ing;
            }
        }
        for (int g = 0; g < 9; g++) {
            int invSlot = GRID_INV_SLOTS[g];
            if (grid[g] != null) {
                inv.setItem(invSlot, gridIngredientIcon(grid[g], player));
            } else {
                inv.setItem(invSlot, pane("·")); // empty grid cell
            }
        }

        // Arrow
        ItemStack arrow = new ItemStack(Items.ARROW);
        arrow.set(DataComponents.CUSTOM_NAME, Component.literal("→").withStyle(ChatFormatting.WHITE));
        inv.setItem(ARROW_SLOT, arrow);

        // Result
        boolean craftable = canCraft(player, recipe);
        ItemStack result = recipe.buildResult();
        if (!craftable) {
            result.set(DataComponents.CUSTOM_NAME,
                Component.literal("✗ " + recipe.displayName()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }
        inv.setItem(RESULT_SLOT, result);

        // Back
        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponents.CUSTOM_NAME,
            Component.literal("← Back").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        inv.setItem(BACK_SLOT, back);

        // Craft button
        inv.setItem(CRAFT_SLOT, craftButton(craftable));

    }

    // ── Craft logic ───────────────────────────────────────────────────────────

    private static void tryCraft(ServerPlayer player, PeakRecipe recipe, SimpleContainer inv) {
        // Cooldown — prevent spam-clicking the craft button
        long now = System.currentTimeMillis();
        long last = lastCraftTime.getOrDefault(player.getUUID(), 0L);
        if (now - last < CRAFT_COOLDOWN_MS) return;

        // Aggregate required counts per item (same item can appear in multiple grid slots)
        Map<Item, Integer> required = aggregateRequired(recipe);

        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            int have = countInInventory(player, entry.getKey());
            if (have < entry.getValue()) {
                player.sendSystemMessage(
                    Component.literal("✗ Missing: ").withStyle(ChatFormatting.RED)
                        .append(Component.literal((entry.getValue() - have) + "× ")
                            .withStyle(ChatFormatting.WHITE))
                        .append(Component.translatable(entry.getKey().getDescriptionId())
                            .withStyle(ChatFormatting.YELLOW)),
                    false);
                return;
            }
        }

        // Build result before consuming ingredients — if it fails, player loses nothing
        ItemStack result;
        try {
            result = recipe.buildResult();
        } catch (Exception e) {
            player.sendSystemMessage(
                Component.literal("✗ Crafting failed — please report this to an admin.")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }

        lastCraftTime.put(player.getUUID(), now);

        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            removeFromInventory(player, entry.getKey(), entry.getValue());
        }

        player.getInventory().add(result);
        if (!result.isEmpty()) {
            player.drop(result, false);
        }

        XpManager.addXp(player, Skill.CRAFTING, 500);

        player.sendSystemMessage(
            Component.literal("✦ Crafted: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(recipe.displayName()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)),
            false);

        populateDetail(inv, player, recipe);
    }

    private static boolean canCraft(ServerPlayer player, PeakRecipe recipe) {
        for (Map.Entry<Item, Integer> entry : aggregateRequired(recipe).entrySet()) {
            if (countInInventory(player, entry.getKey()) < entry.getValue()) return false;
        }
        return true;
    }

    /** Sums counts for each unique item across all grid slots. */
    static Map<Item, Integer> aggregateRequired(PeakRecipe recipe) {
        Map<Item, Integer> totals = new HashMap<>();
        for (PeakIngredient ing : recipe.ingredients()) {
            totals.merge(ing.item(), ing.count(), Integer::sum);
        }
        return totals;
    }

    private static int countInInventory(ServerPlayer player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static void removeFromInventory(ServerPlayer player, Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                int take = Math.min(stack.getCount(), remaining);
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    /** Recipe icon for the list view — result item with ingredient summary in lore. */
    private static ItemStack listIcon(PeakRecipe recipe, ServerPlayer player) {
        boolean craftable = canCraft(player, recipe);
        ItemStack stack = recipe.buildResult().copy();

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("  " + recipe.category() + " Recipe").withStyle(ChatFormatting.DARK_GRAY));
        lore.add(Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").withStyle(ChatFormatting.DARK_GRAY));
        for (Map.Entry<Item, Integer> entry : aggregateRequired(recipe).entrySet()) {
            int have = countInInventory(player, entry.getKey());
            boolean ok = have >= entry.getValue();
            lore.add(
                Component.literal("  " + (ok ? "✔ " : "✗ ")).withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED)
                    .append(Component.translatable(entry.getKey().getDescriptionId()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("  " + have + "/" + entry.getValue())
                        .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED))
            );
        }
        lore.add(Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").withStyle(ChatFormatting.DARK_GRAY));
        lore.add(Component.literal(craftable ? "  ► Click to view & craft" : "  ✗ Missing materials")
            .withStyle(craftable ? ChatFormatting.GREEN : ChatFormatting.RED));

        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    /**
     * Ingredient icon for the 3×3 grid.
     * Stack count = required amount (shows as the corner number like Skyblock).
     * Name color = green if player has enough, red if not.
     */
    private static ItemStack gridIngredientIcon(PeakIngredient ingredient, ServerPlayer player) {
        // Aggregate total required for this item across all slots
        int have = countInInventory(player, ingredient.item());
        boolean ok = have >= ingredient.count();

        // Set count to required amount — this is what shows as the corner number
        ItemStack stack = new ItemStack(ingredient.item(), ingredient.count());
        stack.set(DataComponents.CUSTOM_NAME,
            Component.translatable(ingredient.item().getDescriptionId())
                .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  Have: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(have))
                    .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED)),
            Component.literal("  Need: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(ingredient.count())).withStyle(ChatFormatting.WHITE))
        )));
        return stack;
    }

    private static ItemStack craftButton(boolean canCraft) {
        ItemStack stack = new ItemStack(canCraft ? Items.LIME_DYE : Items.BARRIER);
        stack.set(DataComponents.CUSTOM_NAME,
            Component.literal(canCraft ? "► Craft" : "✗ Missing Materials")
                .withStyle(canCraft ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD));
        return stack;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void fill(SimpleContainer inv, ItemStack stack) {
        for (int i = 0; i < 54; i++) inv.setItem(i, stack.copy());
    }

    private static ItemStack pane(String name) {
        ItemStack s = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        s.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return s;
    }
}
