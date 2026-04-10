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
 */
public class PeakRecipeRegistry {

    private static final LinkedHashMap<String, PeakRecipe> RECIPES = new LinkedHashMap<>();

    public static void register() {
        reg("replenish_book",
            "Replenish I Book",
            List.of(
                new PeakIngredient(Items.WHEAT,       16),
                new PeakIngredient(Items.CARROT,      16),
                new PeakIngredient(Items.POTATO,      16),
                new PeakIngredient(Items.NETHER_WART, 16),
                new PeakIngredient(Items.BONE_MEAL,   16)
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

    private static void reg(String id, String displayName,
                             List<PeakIngredient> ingredients,
                             java.util.function.Supplier<net.minecraft.item.ItemStack> resultFactory) {
        RECIPES.put(id, new PeakRecipe(id, displayName, ingredients, resultFactory));
    }
}
