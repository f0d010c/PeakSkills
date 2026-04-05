package com.peakskills.collection;

import com.peakskills.stat.Stat;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * A reward granted when a collection tier is first reached.
 * Three variants:
 *   ItemReward  — player receives an ItemStack
 *   StatBonus   — permanent stat increase (applied via StatManager)
 *   RecipeUnlock — adds a recipe to the player's recipe book
 */
public sealed interface CollectionReward
        permits CollectionReward.ItemReward,
                CollectionReward.StatBonus,
                CollectionReward.RecipeUnlock {

    record ItemReward(ItemStack stack) implements CollectionReward {}

    record StatBonus(Stat stat, int levels) implements CollectionReward {
        /** Raw attribute value this bonus adds. */
        public double rawValue() { return stat.getValuePerLevel() * levels; }

        /** Human-readable display value (e.g. "+20 Defense"). */
        public int displayValue() { return (int)(rawValue() * stat.getDisplayScale()); }
    }

    record RecipeUnlock(Identifier recipeId) implements CollectionReward {}
}
