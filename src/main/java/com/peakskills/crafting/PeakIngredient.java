package com.peakskills.crafting;

import net.minecraft.item.Item;

/**
 * A single ingredient requirement: an item type and how many are needed.
 */
public record PeakIngredient(Item item, int count) {}
