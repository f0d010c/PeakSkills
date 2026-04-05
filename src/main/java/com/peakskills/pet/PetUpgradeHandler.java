package com.peakskills.pet;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles pet rarity upgrades — checks cost, consumes items, upgrades rarity.
 */
public class PetUpgradeHandler {

    public record UpgradeCost(net.minecraft.item.Item item, int count) {}

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
    public static boolean tryUpgrade(ServerPlayerEntity player, UUID petId) {
        PetRoster roster = com.peakskills.player.PlayerDataManager
            .get(player.getUuid()).getPetRoster();

        Optional<PetInstance> opt = roster.findById(petId);
        if (opt.isEmpty()) return false;

        PetInstance pet = opt.get();
        if (!pet.canUpgrade()) {
            player.sendMessage(Text.literal("This pet is not ready to upgrade yet.")
                .formatted(Formatting.RED), true);
            return false;
        }

        PetRarity current = pet.getRarity();

        // Special case: Rare → Epic needs two item types
        if (current == PetRarity.RARE) {
            if (!hasItems(player, Items.EMERALD, 16) || !hasItems(player, Items.DIAMOND, 4)) {
                player.sendMessage(Text.literal("Upgrade requires 16 Emeralds + 4 Diamonds.")
                    .formatted(Formatting.RED), true);
                return false;
            }
            removeItems(player, Items.EMERALD, 16);
            removeItems(player, Items.DIAMOND, 4);
        } else {
            Optional<UpgradeCost> costOpt = getCost(current);
            if (costOpt.isEmpty()) {
                player.sendMessage(Text.literal("This pet cannot be upgraded further.")
                    .formatted(Formatting.RED), true);
                return false;
            }
            UpgradeCost cost = costOpt.get();
            if (!hasItems(player, cost.item(), cost.count())) {
                player.sendMessage(
                    Text.literal("Upgrade requires " + cost.count() + "x ")
                        .formatted(Formatting.RED)
                        .append(Text.translatable(cost.item().getTranslationKey())
                            .formatted(Formatting.YELLOW)),
                    true);
                return false;
            }
            removeItems(player, cost.item(), cost.count());
        }

        PetRarity before = pet.getRarity();
        pet.upgrade();
        PetRarity after = pet.getRarity();

        player.sendMessage(
            Text.literal("✦ Your ")
                .formatted(Formatting.GOLD)
                .append(Text.literal(pet.getType().displayName).formatted(before.color))
                .append(Text.literal(" upgraded to ").formatted(Formatting.WHITE))
                .append(Text.literal(after.displayName + "!").formatted(after.color, Formatting.BOLD)),
            false
        );
        return true;
    }

    private static boolean hasItems(ServerPlayerEntity player, net.minecraft.item.Item item, int count) {
        int found = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) found += stack.getCount();
        }
        return found >= count;
    }

    private static void removeItems(ServerPlayerEntity player, net.minecraft.item.Item item, int toRemove) {
        for (int i = 0; i < player.getInventory().size() && toRemove > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                int take = Math.min(stack.getCount(), toRemove);
                stack.decrement(take);
                toRemove -= take;
            }
        }
    }
}
