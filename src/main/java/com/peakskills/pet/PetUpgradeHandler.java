package com.peakskills.pet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Handles pet rarity upgrades — checks cost, consumes items, upgrades rarity.
 */
public class PetUpgradeHandler {

    public record UpgradeCost(net.minecraft.world.item.Item item, int count) {}

    public static Optional<UpgradeCost> getCost(PetRarity rarity) {
        return switch (rarity) {
            case COMMON   -> Optional.of(new UpgradeCost(Items.GOLD_INGOT,     16));
            case UNCOMMON -> Optional.of(new UpgradeCost(Items.DIAMOND,         8));
            case RARE     -> Optional.empty(); // special: two items — handled separately
            case EPIC     -> Optional.of(new UpgradeCost(Items.NETHERITE_INGOT, 4));
            default       -> Optional.empty();
        };
    }

    /**
     * Attempts to upgrade a pet. Returns true on success.
     * Rare → Epic requires Emeralds x16 + Diamond x4 (handled as two checks).
     */
    public static boolean tryUpgrade(ServerPlayer player, UUID petId) {
        PetRoster roster = com.peakskills.player.PlayerDataManager
            .get(player.getUUID()).getPetRoster();

        Optional<PetInstance> opt = roster.findById(petId);
        if (opt.isEmpty()) return false;

        PetInstance pet = opt.get();
        if (!pet.canUpgrade()) {
            player.sendSystemMessage(Component.literal("This pet is not ready to upgrade yet.")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        PetRarity current = pet.getRarity();

        // Special case: Rare → Epic needs two item types
        if (current == PetRarity.RARE) {
            if (!hasItems(player, Items.EMERALD, 16) || !hasItems(player, Items.DIAMOND, 4)) {
                player.sendSystemMessage(Component.literal("Upgrade requires 16 Emeralds + 4 Diamonds.")
                    .withStyle(ChatFormatting.RED), true);
                return false;
            }
            removeItems(player, Items.EMERALD, 16);
            removeItems(player, Items.DIAMOND, 4);
        } else {
            Optional<UpgradeCost> costOpt = getCost(current);
            if (costOpt.isEmpty()) {
                player.sendSystemMessage(Component.literal("This pet cannot be upgraded further.")
                    .withStyle(ChatFormatting.RED), true);
                return false;
            }
            UpgradeCost cost = costOpt.get();
            if (!hasItems(player, cost.item(), cost.count())) {
                player.sendSystemMessage(
                    Component.literal("Upgrade requires " + cost.count() + "x ")
                        .withStyle(ChatFormatting.RED)
                        .append(Component.translatable(cost.item().getDescriptionId())
                            .withStyle(ChatFormatting.YELLOW)),
                    true);
                return false;
            }
            removeItems(player, cost.item(), cost.count());
        }

        PetRarity before = pet.getRarity();
        pet.upgrade();
        PetRarity after = pet.getRarity();

        player.sendSystemMessage(
            Component.literal("✦ Your ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(pet.getType().displayName).withStyle(before.color))
                .append(Component.literal(" upgraded to ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(after.displayName + "!").withStyle(after.color, ChatFormatting.BOLD)),
            false
        );
        return true;
    }

    private static boolean hasItems(ServerPlayer player, net.minecraft.world.item.Item item, int count) {
        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) found += stack.getCount();
        }
        return found >= count;
    }

    private static void removeItems(ServerPlayer player, net.minecraft.world.item.Item item, int toRemove) {
        for (int i = 0; i < player.getInventory().getContainerSize() && toRemove > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                int take = Math.min(stack.getCount(), toRemove);
                stack.shrink(take);
                toRemove -= take;
            }
        }
    }
}
