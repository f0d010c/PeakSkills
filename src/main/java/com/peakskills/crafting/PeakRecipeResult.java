package com.peakskills.crafting;

import com.peakskills.enchantment.ReplenishEnchantment;
import com.peakskills.player.PlayerDataManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Builds result ItemStacks for custom recipes.
 * All methods call PlayerDataManager.getServer() so they must only
 * be invoked at craft time (never during mod init).
 *
 * Add a new static method here when a recipe needs a custom-NBT result.
 */
public class PeakRecipeResult {

    public static ItemStack replenishBook() {
        // Build a proper enchanted book with stored Replenish I enchantment
        var registryManager = PlayerDataManager.getServer().getRegistryManager();
        RegistryEntry<Enchantment> replenishEntry = registryManager
            .getOrThrow(RegistryKeys.ENCHANTMENT)
            .getOrThrow(ReplenishEnchantment.REPLENISH);

        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantmentsComponent.Builder builder =
            new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        builder.add(replenishEntry, 1);
        stack.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());

        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("Replenish I").formatted(Formatting.AQUA, Formatting.BOLD));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("  Apply to a Hoe or Axe at an Anvil.").formatted(Formatting.GRAY),
            Text.empty(),
            Text.literal("  • Auto-replants harvested crops").formatted(Formatting.GREEN),
            Text.literal("  • Magnet-collects crop drops nearby").formatted(Formatting.GREEN),
            Text.empty(),
            Text.literal("  Requires Farming 30 to use").formatted(Formatting.DARK_GRAY)
        )));

        return stack;
    }
}
