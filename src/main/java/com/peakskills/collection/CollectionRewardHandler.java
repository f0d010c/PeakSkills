package com.peakskills.collection;

import com.peakskills.stat.StatManager;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Applies collection tier rewards to a player and announces the unlock.
 */
public class CollectionRewardHandler {

    public static void apply(ServerPlayerEntity player,
                             CollectionType type,
                             List<CollectionTier> newTiers,
                             MinecraftServer server) {
        if (newTiers.isEmpty()) return;

        boolean statChanged = false;

        for (CollectionTier tier : newTiers) {
            // ── Announcement ──────────────────────────────────────────────────
            player.sendMessage(
                Text.literal("  ★ ").formatted(Formatting.GOLD)
                    .append(Text.literal(type.displayName + " Collection ").formatted(type.color, Formatting.BOLD))
                    .append(Text.literal("Tier " + tier.tierLabel() + " Unlocked!").formatted(Formatting.YELLOW)),
                false
            );

            // ── Rewards ───────────────────────────────────────────────────────
            for (CollectionReward reward : tier.rewards()) {
                switch (reward) {

                    case CollectionReward.ItemReward ir -> {
                        ItemStack stack = ir.stack().copy();
                        boolean inserted = player.getInventory().insertStack(stack);
                        if (!inserted) player.dropItem(ir.stack().copy(), false);

                        player.sendMessage(
                            Text.literal("    → ").formatted(Formatting.DARK_GRAY)
                                .append(Text.literal("Reward: ").formatted(Formatting.GRAY))
                                .append(Text.literal("x" + ir.stack().getCount() + " "
                                    + ir.stack().getName().getString()).formatted(Formatting.GREEN)),
                            false
                        );
                    }

                    case CollectionReward.StatBonus sb -> {
                        statChanged = true;
                        player.sendMessage(
                            Text.literal("    → ").formatted(Formatting.DARK_GRAY)
                                .append(Text.literal("Reward: ").formatted(Formatting.GRAY))
                                .append(Text.literal("+" + sb.displayValue()
                                    + " " + sb.stat().getIcon()
                                    + " " + sb.stat().getDisplayName()).formatted(Formatting.GREEN)),
                            false
                        );
                    }

                    case CollectionReward.RecipeUnlock ru -> {
                        RegistryKey<net.minecraft.recipe.Recipe<?>> key =
                            RegistryKey.of(RegistryKeys.RECIPE, ru.recipeId());
                        server.getRecipeManager()
                            .get(key)
                            .ifPresent(entry -> player.unlockRecipes(List.of(entry)));

                        String name = formatPath(ru.recipeId().getPath());
                        player.sendMessage(
                            Text.literal("    → ").formatted(Formatting.DARK_GRAY)
                                .append(Text.literal("Recipe Unlocked: ").formatted(Formatting.GRAY))
                                .append(Text.literal(name).formatted(Formatting.AQUA)),
                            false
                        );
                    }
                }
            }
        }

        // Reapply stats once after processing all tiers so attributes are recalculated
        if (statChanged) {
            StatManager.applyStats(player);
        }
    }

    // "coal_block" → "Coal Block"
    private static String formatPath(String path) {
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : path.toCharArray()) {
            if (c == '_') { sb.append(' '); cap = true; }
            else if (cap) { sb.append(Character.toUpperCase(c)); cap = false; }
            else           { sb.append(c); }
        }
        return sb.toString();
    }
}
