package com.peakskills.crafting;

import net.minecraft.item.Items;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Central registry for all custom quantity-based recipes.
 *
 * To add a new recipe:
 *   1. Add a static result builder in {@link PeakRecipeResult} if the result needs custom components.
 *   2. Add one {@code reg(...)} call in {@link #register()} below.
 *   Done — the GUI picks it up automatically.
 *
 * Grid slots (3×3):
 *   0 1 2
 *   3 4 5
 *   6 7 8
 */
public class PeakRecipeRegistry {

    private static final LinkedHashMap<String, PeakRecipe> RECIPES = new LinkedHashMap<>();

    public static void register() {
        // Replenish I Book
        // Grid layout:
        //   [Wheat×16] [BoneMeal×4] [Carrot×16]
        //   [BoneMeal×4] [Book×1 ] [BoneMeal×4]
        //   [Potato×16] [BoneMeal×4] [NetherWart×16]
        // Bone Meal: 4 per slot × 4 slots = 16 total
        reg("replenish_book",
            "Replenish I Book",
            "Farming",
            List.of(
                new PeakIngredient(Items.WHEAT,       16, 0),
                new PeakIngredient(Items.BONE_MEAL,    4, 1),
                new PeakIngredient(Items.CARROT,      16, 2),
                new PeakIngredient(Items.BONE_MEAL,    4, 3),
                new PeakIngredient(Items.BOOK,         1, 4),
                new PeakIngredient(Items.BONE_MEAL,    4, 5),
                new PeakIngredient(Items.POTATO,      16, 6),
                new PeakIngredient(Items.BONE_MEAL,    4, 7),
                new PeakIngredient(Items.NETHER_WART, 16, 8)
            ),
            PeakRecipeResult::replenishBook
        );
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static Collection<PeakRecipe> getAll() {
        return RECIPES.values();
    }

    public static Optional<PeakRecipe> getById(String id) {
        return Optional.ofNullable(RECIPES.get(id));
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static void reg(String id, String displayName, String category,
                             List<PeakIngredient> ingredients,
                             java.util.function.Supplier<net.minecraft.item.ItemStack> resultFactory) {
        RECIPES.put(id, new PeakRecipe(id, displayName, category, ingredients, resultFactory));
    }
}
