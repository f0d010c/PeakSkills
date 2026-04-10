package com.peakskills.crafting;

import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * A custom recipe with per-ingredient quantity requirements.
 * Add new recipes via {@link PeakRecipeRegistry#register()}.
 *
 * The result is a {@link Supplier} so it can reference the enchantment
 * registry lazily at craft time (registry not available during mod init).
 */
public record PeakRecipe(
    String id,
    String displayName,
    List<PeakIngredient> ingredients,
    Supplier<ItemStack> resultFactory
) {
    /** Call at craft time (server is running) to get the result stack. */
    public ItemStack buildResult() {
        return resultFactory.get();
    }
}
