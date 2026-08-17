package com.peakskills.crafting;

import com.peakskills.MinecraftTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PeakCraftingLogicTest {

    @BeforeAll
    static void initializeMinecraft() {
        MinecraftTestBootstrap.initializeRegistries();
    }

    @Test
    void duplicateGridIngredientsAreSummedBeforeValidationAndConsumption() {
        PeakRecipe recipe = new PeakRecipe("aggregate", "Aggregate", "test", List.of(
            new PeakIngredient(Items.WHEAT, 16, 0),
            new PeakIngredient(Items.WHEAT, 24, 8),
            new PeakIngredient(Items.DIAMOND, 1, 4)
        ), () -> new ItemStack(Items.BREAD));

        Map<Item, Integer> required = PeakCraftingGui.aggregateRequired(recipe);

        assertEquals(40, required.get(Items.WHEAT));
        assertEquals(1, required.get(Items.DIAMOND));
        assertEquals(2, required.size());
    }
}
