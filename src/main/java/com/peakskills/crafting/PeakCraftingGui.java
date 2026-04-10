package com.peakskills.crafting;

import com.peakskills.gui.SkillsScreenHandler;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /craft GUI — two views:
 *
 *  List view (main):
 *    Recipes shown in centre column (slots 1,10,19,28,37).
 *    Clicking one opens the Detail view.
 *
 *  Detail view (per-recipe):
 *    Ingredients shown with "Have / Need" status.
 *    Craft button (slot 49) — green if craftable, red if not.
 *    Back button (slot 45) returns to list.
 *
 * Layout (54 slots, 6 rows × 9 cols):
 *
 *  List view:
 *    bg bg [R0] bg bg bg bg bg bg
 *    bg bg [R1] bg bg bg bg bg bg
 *    bg bg [R2] bg bg bg bg bg bg
 *    bg bg [R3] bg bg bg bg bg bg
 *    bg bg [R4] bg bg bg bg bg bg
 *    bg bg  bg  bg bg bg bg bg bg
 *
 *  Detail view:
 *    bg bg bg bg [Title] bg bg bg bg
 *    bg [I0] bg [I1] bg [I2] bg bg bg
 *    bg [I3] bg [I4] bg bg  bg bg bg
 *    bg bg  bg  bg  bg  bg  bg bg bg
 *    bg bg  bg  bg  bg [Result] bg bg bg
 *    [Back] bg bg bg bg [Craft] bg bg bg
 */
public class PeakCraftingGui {

    // ── Slot constants ────────────────────────────────────────────────────────

    // List view: recipe icons in column 2 (index 2, 11, 20, 29, 38)
    private static final int[] LIST_SLOTS = { 2, 11, 20, 29, 38 };

    // Detail view
    private static final int[] INGREDIENT_SLOTS = { 10, 12, 14, 19, 21 };
    private static final int RESULT_SLOT   = 40;
    private static final int CRAFT_SLOT    = 49;
    private static final int BACK_SLOT     = 45;

    // ── Open ──────────────────────────────────────────────────────────────────

    public static void open(ServerPlayerEntity player) {
        openList(player);
    }

    // ── List view ─────────────────────────────────────────────────────────────

    private static void openList(ServerPlayerEntity player) {
        Collection<PeakRecipe> allRecipes = PeakRecipeRegistry.getAll();
        List<PeakRecipe> recipes = new ArrayList<>(allRecipes);

        SimpleInventory inv = new SimpleInventory(54);
        ItemStack bg = pane(Items.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setStack(i, bg.copy());

        // Header
        ItemStack header = new ItemStack(Items.CRAFTING_TABLE);
        header.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("Custom Recipes").formatted(Formatting.GOLD, Formatting.BOLD));
        header.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("  Click a recipe to view and craft it").formatted(Formatting.GRAY)
        )));
        inv.setStack(4, header);

        Map<Integer, Runnable> handlers = new HashMap<>();

        for (int i = 0; i < recipes.size() && i < LIST_SLOTS.length; i++) {
            PeakRecipe recipe = recipes.get(i);
            inv.setStack(LIST_SLOTS[i], recipeListIcon(recipe, player));
            handlers.put(LIST_SLOTS[i], () -> openDetail(player, recipe));
        }

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers),
            Text.literal("Custom Recipes").formatted(Formatting.GOLD, Formatting.BOLD)
        ));
    }

    // ── Detail view ───────────────────────────────────────────────────────────

    private static void openDetail(ServerPlayerEntity player, PeakRecipe recipe) {
        SimpleInventory inv = new SimpleInventory(54);
        ItemStack bg = pane(Items.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setStack(i, bg.copy());

        // Title
        ItemStack title = new ItemStack(Items.NETHER_STAR);
        title.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        title.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(recipe.displayName()).formatted(Formatting.AQUA, Formatting.BOLD));
        inv.setStack(4, title);

        // Ingredients
        List<PeakIngredient> ingredients = recipe.ingredients();
        for (int i = 0; i < ingredients.size() && i < INGREDIENT_SLOTS.length; i++) {
            inv.setStack(INGREDIENT_SLOTS[i], ingredientIcon(ingredients.get(i), player));
        }

        // Result preview (greyed name if not craftable)
        boolean canCraft = canCraft(player, recipe);
        ItemStack resultPreview = recipe.buildResult();
        if (!canCraft) {
            resultPreview.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("✗ " + recipe.displayName()).formatted(Formatting.RED, Formatting.BOLD));
        }
        inv.setStack(RESULT_SLOT, resultPreview);

        // Craft button
        inv.setStack(CRAFT_SLOT, craftButton(canCraft));

        // Back button
        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("← Back").formatted(Formatting.YELLOW, Formatting.BOLD));
        inv.setStack(BACK_SLOT, back);

        Map<Integer, Runnable> handlers = new HashMap<>();
        handlers.put(BACK_SLOT, () -> openList(player));
        handlers.put(CRAFT_SLOT, () -> tryCraft(player, recipe));

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers),
            Text.literal(recipe.displayName()).formatted(Formatting.AQUA, Formatting.BOLD)
        ));
    }

    // ── Craft logic ───────────────────────────────────────────────────────────

    private static void tryCraft(ServerPlayerEntity player, PeakRecipe recipe) {
        // Verify
        for (PeakIngredient ingredient : recipe.ingredients()) {
            int have = countInInventory(player, ingredient.item());
            if (have < ingredient.count()) {
                player.sendMessage(
                    Text.literal("✗ Missing: ").formatted(Formatting.RED)
                        .append(Text.literal(ingredient.count() - have + "× ")
                            .formatted(Formatting.WHITE))
                        .append(Text.translatable(ingredient.item().getTranslationKey())
                            .formatted(Formatting.YELLOW)),
                    false);
                return;
            }
        }
        // Consume
        for (PeakIngredient ingredient : recipe.ingredients()) {
            removeFromInventory(player, ingredient.item(), ingredient.count());
        }
        // Give result
        ItemStack result = recipe.buildResult();
        player.getInventory().insertStack(result);
        if (!result.isEmpty()) {
            // Inventory was full — drop at feet
            player.dropItem(result, false);
        }
        player.sendMessage(
            Text.literal("✦ Crafted: ").formatted(Formatting.GOLD)
                .append(Text.literal(recipe.displayName()).formatted(Formatting.AQUA, Formatting.BOLD)),
            false);
        // Reopen detail to refresh availability
        openDetail(player, recipe);
    }

    private static boolean canCraft(ServerPlayerEntity player, PeakRecipe recipe) {
        for (PeakIngredient ingredient : recipe.ingredients()) {
            if (countInInventory(player, ingredient.item()) < ingredient.count()) return false;
        }
        return true;
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

    private static ItemStack recipeListIcon(PeakRecipe recipe, ServerPlayerEntity player) {
        boolean craftable = canCraft(player, recipe);
        ItemStack stack = recipe.buildResult().copy();
        List<Text> lore = new ArrayList<>();
        lore.add(Text.literal("  Ingredients:").formatted(Formatting.GRAY));
        for (PeakIngredient ing : recipe.ingredients()) {
            int have = countInInventory(player, ing.item());
            boolean ok = have >= ing.count();
            lore.add(
                Text.literal("  " + (ok ? "✔ " : "✗ ")).formatted(ok ? Formatting.GREEN : Formatting.RED)
                    .append(Text.translatable(ing.item().getTranslationKey()).formatted(Formatting.WHITE))
                    .append(Text.literal("  " + have + "/" + ing.count())
                        .formatted(ok ? Formatting.GREEN : Formatting.RED))
            );
        }
        lore.add(Text.empty());
        lore.add(Text.literal(craftable ? "  ► Click to craft" : "  ✗ Missing materials")
            .formatted(craftable ? Formatting.GREEN : Formatting.RED));
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    private static ItemStack ingredientIcon(PeakIngredient ingredient, ServerPlayerEntity player) {
        int have = countInInventory(player, ingredient.item());
        boolean ok = have >= ingredient.count();
        ItemStack stack = new ItemStack(ingredient.item());
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.translatable(ingredient.item().getTranslationKey())
                .formatted(ok ? Formatting.GREEN : Formatting.RED, Formatting.BOLD));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("  Need: ").formatted(Formatting.GRAY)
                .append(Text.literal(String.valueOf(ingredient.count())).formatted(Formatting.WHITE)),
            Text.literal("  Have: ").formatted(Formatting.GRAY)
                .append(Text.literal(String.valueOf(have))
                    .formatted(ok ? Formatting.GREEN : Formatting.RED))
        )));
        return stack;
    }

    private static ItemStack craftButton(boolean canCraft) {
        ItemStack stack = new ItemStack(canCraft ? Items.LIME_DYE : Items.GRAY_DYE);
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(canCraft ? "► Craft" : "✗ Missing Materials")
                .formatted(canCraft ? Formatting.GREEN : Formatting.RED, Formatting.BOLD));
        if (!canCraft) {
            stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("  You don't have enough materials").formatted(Formatting.DARK_GRAY)
            )));
        }
        return stack;
    }

    private static ItemStack pane(Item item, String name) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return s;
    }
}
