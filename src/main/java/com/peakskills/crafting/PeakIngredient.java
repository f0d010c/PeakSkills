package com.peakskills.crafting;

import net.minecraft.item.Item;

/**
 * A single ingredient slot in a custom recipe.
 *
 * @param item      The required item.
 * @param count     How many of this item are needed from this slot.
 * @param gridSlot  Position in the 3×3 display grid (0–8, left-to-right top-to-bottom).
 *                  The same item can appear in multiple slots — crafting logic sums all counts.
 */
public record PeakIngredient(Item item, int count, int gridSlot) {}
