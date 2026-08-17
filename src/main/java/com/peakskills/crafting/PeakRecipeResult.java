package com.peakskills.crafting;

import com.peakskills.enchantment.ReplenishEnchantment;
import com.peakskills.player.PlayerDataManager;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

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
        var registryManager = PlayerDataManager.getServer().registryAccess();
        Holder<Enchantment> replenishEntry = registryManager
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(ReplenishEnchantment.REPLENISH);

        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable builder =
            new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        builder.upgrade(replenishEntry, 1);
        stack.set(DataComponents.STORED_ENCHANTMENTS, builder.toImmutable());

        stack.set(DataComponents.CUSTOM_NAME,
            Component.literal("Replenish I").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  Apply to a Hoe or Axe at an Anvil.").withStyle(ChatFormatting.GRAY),
            Component.empty(),
            Component.literal("  • Auto-replants harvested crops").withStyle(ChatFormatting.GREEN),
            Component.literal("  • Magnet-collects crop drops nearby").withStyle(ChatFormatting.GREEN),
            Component.empty(),
            Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").withStyle(ChatFormatting.DARK_GRAY),
            Component.literal("  ⚠ Requires Farming 30").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        )));

        return stack;
    }
}
