package com.peakskills.collection;

import com.peakskills.stat.StatManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/**
 * Applies collection tier rewards to a player and announces the unlock.
 */
public class CollectionRewardHandler {

    public static void apply(ServerPlayer player,
                             CollectionType type,
                             List<CollectionTier> newTiers,
                             MinecraftServer server) {
        if (newTiers.isEmpty()) return;

        boolean statChanged = false;

        for (CollectionTier tier : newTiers) {
            // ── Announcement ──────────────────────────────────────────────────
            player.sendSystemMessage(
                Component.literal("  ★ ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(type.displayName + " Collection ").withStyle(type.color, ChatFormatting.BOLD))
                    .append(Component.literal("Tier " + tier.tierLabel() + " Unlocked!").withStyle(ChatFormatting.YELLOW)),
                false
            );

            // ── Rewards ───────────────────────────────────────────────────────
            for (CollectionReward reward : tier.rewards()) {
                switch (reward) {

                    case CollectionReward.ItemReward ir -> {
                        ItemStack stack = ir.stack().copy();
                        boolean inserted = player.getInventory().add(stack);
                        if (!inserted) player.drop(ir.stack().copy(), false);

                        player.sendSystemMessage(
                            Component.literal("    → ").withStyle(ChatFormatting.DARK_GRAY)
                                .append(Component.literal("Reward: ").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal("x" + ir.stack().getCount() + " "
                                    + ir.stack().getHoverName().getString()).withStyle(ChatFormatting.GREEN)),
                            false
                        );
                    }

                    case CollectionReward.StatBonus sb -> {
                        statChanged = true;
                        player.sendSystemMessage(
                            Component.literal("    → ").withStyle(ChatFormatting.DARK_GRAY)
                                .append(Component.literal("Reward: ").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal("+" + sb.displayValue()
                                    + " " + sb.stat().getIcon()
                                    + " " + sb.stat().getDisplayName()).withStyle(ChatFormatting.GREEN)),
                            false
                        );
                    }

                    case CollectionReward.RecipeUnlock ru -> {
                        ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key =
                            ResourceKey.create(Registries.RECIPE, ru.recipeId());
                        server.getRecipeManager()
                            .byKey(key)
                            .ifPresent(entry -> player.awardRecipes(List.of(entry)));

                        String name = formatPath(ru.recipeId().getPath());
                        player.sendSystemMessage(
                            Component.literal("    → ").withStyle(ChatFormatting.DARK_GRAY)
                                .append(Component.literal("Recipe Unlocked: ").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(name).withStyle(ChatFormatting.AQUA)),
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
