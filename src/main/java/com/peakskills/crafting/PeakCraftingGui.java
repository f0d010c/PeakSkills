package com.peakskills.crafting;

import com.peakskills.gui.SkillsScreenHandler;
import com.peakskills.skill.Skill;
import com.peakskills.xp.XpManager;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    public static void open(ServerPlayerEntity player) {
        openList(player);
    }

    // ── List view ─────────────────────────────────────────────────────────────

    private static void openList(ServerPlayerEntity player) {
        List<PeakRecipe> recipes = new ArrayList<>(PeakRecipeRegistry.getAll());

        SimpleInventory inv = new SimpleInventory(54);
        fill(inv, pane(" "));

        // Title
        ItemStack header = new ItemStack(Items.CRAFTING_TABLE);
        header.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        header.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("(" + recipes.size() + ") PeakSkills Recipes")
                .formatted(Formatting.GOLD, Formatting.BOLD));
        inv.setStack(4, header);

        Map<Integer, Runnable> handlers = new HashMap<>();

        for (int i = 0; i < recipes.size() && i < LIST_RECIPE_SLOTS.length; i++) {
            PeakRecipe recipe = recipes.get(i);
            int slot = LIST_RECIPE_SLOTS[i];
            inv.setStack(slot, listIcon(recipe, player));
            handlers.put(slot, () -> openDetail(player, recipe));
        }

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers),
            Text.literal("PeakSkills Recipes").formatted(Formatting.GOLD, Formatting.BOLD)
        ));
    }

    // ── Detail view ───────────────────────────────────────────────────────────

    private static void openDetail(ServerPlayerEntity player, PeakRecipe recipe) {
        SimpleInventory inv = new SimpleInventory(54);
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
                inv.setStack(invSlot, gridIngredientIcon(grid[g], player));
            } else {
                inv.setStack(invSlot, pane("·")); // empty grid cell
            }
        }

        // Arrow
        ItemStack arrow = new ItemStack(Items.ARROW);
        arrow.set(DataComponentTypes.CUSTOM_NAME, Text.literal("→").formatted(Formatting.WHITE));
        inv.setStack(ARROW_SLOT, arrow);

        // Result
        boolean craftable = canCraft(player, recipe);
        ItemStack result = recipe.buildResult();
        if (!craftable) {
            result.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("✗ " + recipe.displayName()).formatted(Formatting.RED, Formatting.BOLD));
        }
        inv.setStack(RESULT_SLOT, result);

        // Back
        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("← Back").formatted(Formatting.YELLOW, Formatting.BOLD));
        inv.setStack(BACK_SLOT, back);

        // Craft button
        inv.setStack(CRAFT_SLOT, craftButton(craftable));

        Map<Integer, Runnable> handlers = new HashMap<>();
        handlers.put(BACK_SLOT,  () -> openList(player));
        handlers.put(CRAFT_SLOT, () -> tryCraft(player, recipe));

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers),
            Text.literal(recipe.displayName()).formatted(Formatting.AQUA, Formatting.BOLD)
        ));
    }

    // ── Craft logic ───────────────────────────────────────────────────────────

    private static void tryCraft(ServerPlayerEntity player, PeakRecipe recipe) {
        // Cooldown — prevent spam-clicking the craft button
        long now = System.currentTimeMillis();
        long last = lastCraftTime.getOrDefault(player.getUuid(), 0L);
        if (now - last < CRAFT_COOLDOWN_MS) return;

        // Aggregate required counts per item (same item can appear in multiple grid slots)
        Map<Item, Integer> required = aggregateRequired(recipe);

        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            int have = countInInventory(player, entry.getKey());
            if (have < entry.getValue()) {
                player.sendMessage(
                    Text.literal("✗ Missing: ").formatted(Formatting.RED)
                        .append(Text.literal((entry.getValue() - have) + "× ")
                            .formatted(Formatting.WHITE))
                        .append(Text.translatable(entry.getKey().getTranslationKey())
                            .formatted(Formatting.YELLOW)),
                    false);
                return;
            }
        }

        // Build result before consuming ingredients — if it fails, player loses nothing
        ItemStack result;
        try {
            result = recipe.buildResult();
        } catch (Exception e) {
            player.sendMessage(
                Text.literal("✗ Crafting failed — please report this to an admin.")
                    .formatted(Formatting.RED), false);
            return;
        }

        lastCraftTime.put(player.getUuid(), now);

        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            removeFromInventory(player, entry.getKey(), entry.getValue());
        }

        player.getInventory().insertStack(result);
        if (!result.isEmpty()) {
            player.dropItem(result, false);
        }

        XpManager.addXp(player, Skill.CRAFTING, 500);

        player.sendMessage(
            Text.literal("✦ Crafted: ").formatted(Formatting.GOLD)
                .append(Text.literal(recipe.displayName()).formatted(Formatting.AQUA, Formatting.BOLD)),
            false);

        openDetail(player, recipe);
    }

    private static boolean canCraft(ServerPlayerEntity player, PeakRecipe recipe) {
        for (Map.Entry<Item, Integer> entry : aggregateRequired(recipe).entrySet()) {
            if (countInInventory(player, entry.getKey()) < entry.getValue()) return false;
        }
        return true;
    }

    /** Sums counts for each unique item across all grid slots. */
    private static Map<Item, Integer> aggregateRequired(PeakRecipe recipe) {
        Map<Item, Integer> totals = new HashMap<>();
        for (PeakIngredient ing : recipe.ingredients()) {
            totals.merge(ing.item(), ing.count(), Integer::sum);
        }
        return totals;
    }

    private static int countInInventory(ServerPlayerEntity player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(item)) count += stack.getCount();
        }
        return count;
    }

    private static void removeFromInventory(ServerPlayerEntity player, Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(item)) {
                int take = Math.min(stack.getCount(), remaining);
                stack.decrement(take);
                remaining -= take;
            }
        }
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    /** Recipe icon for the list view — result item with ingredient summary in lore. */
    private static ItemStack listIcon(PeakRecipe recipe, ServerPlayerEntity player) {
        boolean craftable = canCraft(player, recipe);
        ItemStack stack = recipe.buildResult().copy();

        List<Text> lore = new ArrayList<>();
        lore.add(Text.literal("  " + recipe.category() + " Recipe").formatted(Formatting.DARK_GRAY));
        lore.add(Text.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").formatted(Formatting.DARK_GRAY));
        for (Map.Entry<Item, Integer> entry : aggregateRequired(recipe).entrySet()) {
            int have = countInInventory(player, entry.getKey());
            boolean ok = have >= entry.getValue();
            lore.add(
                Text.literal("  " + (ok ? "✔ " : "✗ ")).formatted(ok ? Formatting.GREEN : Formatting.RED)
                    .append(Text.translatable(entry.getKey().getTranslationKey()).formatted(Formatting.WHITE))
                    .append(Text.literal("  " + have + "/" + entry.getValue())
                        .formatted(ok ? Formatting.GREEN : Formatting.RED))
            );
        }
        lore.add(Text.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").formatted(Formatting.DARK_GRAY));
        lore.add(Text.literal(craftable ? "  ► Click to view & craft" : "  ✗ Missing materials")
            .formatted(craftable ? Formatting.GREEN : Formatting.RED));

        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    /**
     * Ingredient icon for the 3×3 grid.
     * Stack count = required amount (shows as the corner number like Skyblock).
     * Name color = green if player has enough, red if not.
     */
    private static ItemStack gridIngredientIcon(PeakIngredient ingredient, ServerPlayerEntity player) {
        // Aggregate total required for this item across all slots
        int have = countInInventory(player, ingredient.item());
        boolean ok = have >= ingredient.count();

        // Set count to required amount — this is what shows as the corner number
        ItemStack stack = new ItemStack(ingredient.item(), ingredient.count());
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.translatable(ingredient.item().getTranslationKey())
                .formatted(ok ? Formatting.GREEN : Formatting.RED));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("  Have: ").formatted(Formatting.GRAY)
                .append(Text.literal(String.valueOf(have))
                    .formatted(ok ? Formatting.GREEN : Formatting.RED)),
            Text.literal("  Need: ").formatted(Formatting.GRAY)
                .append(Text.literal(String.valueOf(ingredient.count())).formatted(Formatting.WHITE))
        )));
        return stack;
    }

    private static ItemStack craftButton(boolean canCraft) {
        ItemStack stack = new ItemStack(canCraft ? Items.LIME_DYE : Items.BARRIER);
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(canCraft ? "► Craft" : "✗ Missing Materials")
                .formatted(canCraft ? Formatting.GREEN : Formatting.RED, Formatting.BOLD));
        return stack;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void fill(SimpleInventory inv, ItemStack stack) {
        for (int i = 0; i < 54; i++) inv.setStack(i, stack.copy());
    }

    private static ItemStack pane(String name) {
        ItemStack s = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return s;
    }
}
