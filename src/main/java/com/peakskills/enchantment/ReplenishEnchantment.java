package com.peakskills.enchantment;

import com.peakskills.PeakSkills;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.*;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

public class ReplenishEnchantment {

    public static final RegistryKey<Enchantment> REPLENISH = RegistryKey.of(
        RegistryKeys.ENCHANTMENT,
        Identifier.of(PeakSkills.MOD_ID, "replenish")
    );

    private static final int MIN_FARMING_LEVEL = 30;

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register(ReplenishEnchantment::onBlockBreak);

        // Unlock the recipe for players who already have Farming 30+ on join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (PlayerDataManager.get(player.getUuid()).getLevel(Skill.FARMING) >= MIN_FARMING_LEVEL) {
                unlockRecipe(player);
            }
        });
    }

    /** Called from XpManager when the Farming skill levels up. */
    public static void onFarmingLevelUp(ServerPlayerEntity player, int from, int to) {
        if (from < MIN_FARMING_LEVEL && to >= MIN_FARMING_LEVEL) {
            unlockRecipe(player);
            player.sendMessage(
                Text.literal("  ✦ Recipe Unlocked: ").formatted(Formatting.GOLD)
                    .append(Text.literal("Replenish I Book").formatted(Formatting.AQUA, Formatting.BOLD))
                    .append(Text.literal(" — surround a Book with Wheat Seeds & Bone Meal.")
                        .formatted(Formatting.GRAY)),
                false);
        }
    }

    private static void unlockRecipe(ServerPlayerEntity player) {
        var recipeId = RegistryKey.of(RegistryKeys.RECIPE,
            Identifier.of(PeakSkills.MOD_ID, "replenish_book"));
        ((ServerWorld) player.getEntityWorld()).getServer().getRecipeManager().get(recipeId)
            .ifPresent(r -> player.unlockRecipes(List.of(r)));
    }

    private static void onBlockBreak(World world, PlayerEntity player, BlockPos pos,
                                     BlockState state, net.minecraft.block.entity.BlockEntity be) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        // Must be holding a hoe with Replenish
        ItemStack tool = serverPlayer.getMainHandStack();
        if (!hasReplenish(tool, serverWorld)) return;

        // Farming 30 required
        if (PlayerDataManager.get(serverPlayer.getUuid()).getLevel(Skill.FARMING) < MIN_FARMING_LEVEL) return;

        // Must be a replantable crop (any age)
        Block block = state.getBlock();
        Item seed = seedFor(block);
        if (seed == null) return;

        // Consume one seed from the drops if available (mature crops drop seeds; immature may not)
        consumeSeedDrop(serverWorld, pos, seed);

        // Replant at age 0 — preserve non-age properties (e.g. CocoaBlock FACING)
        serverWorld.setBlockState(pos, resetAge(block, state));
    }

    private static boolean hasReplenish(ItemStack stack, ServerWorld world) {
        if (stack.isEmpty()) return false;
        RegistryEntry.Reference<Enchantment> entry =
            world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(REPLENISH);
        return EnchantmentHelper.getLevel(entry, stack) > 0;
    }

    private static boolean isMatureCrop(Block block, BlockState state) {
        if (block instanceof CropBlock crop) return crop.isMature(state);
        if (block == Blocks.NETHER_WART) return state.get(NetherWartBlock.AGE) == 3;
        if (block == Blocks.COCOA)       return state.get(CocoaBlock.AGE) == 2;
        return false;
    }

    private static Item seedFor(Block block) {
        if (block == Blocks.WHEAT)      return Items.WHEAT_SEEDS;
        if (block == Blocks.CARROTS)    return Items.CARROT;
        if (block == Blocks.POTATOES)   return Items.POTATO;
        if (block == Blocks.BEETROOTS)  return Items.BEETROOT_SEEDS;
        if (block == Blocks.NETHER_WART) return Items.NETHER_WART;
        if (block == Blocks.COCOA)      return Items.COCOA_BEANS;
        return null;
    }

    private static BlockState resetAge(Block block, BlockState state) {
        if (block instanceof CropBlock)        return state.with(CropBlock.AGE, 0);
        if (block == Blocks.NETHER_WART)       return state.with(NetherWartBlock.AGE, 0);
        if (block == Blocks.COCOA)             return state.with(CocoaBlock.AGE, 0);
        return block.getDefaultState();
    }

    private static void consumeSeedDrop(ServerWorld world, BlockPos pos, Item seed) {
        Box box = new Box(pos).expand(1.0);
        List<ItemEntity> nearby = world.getEntitiesByType(
            net.minecraft.entity.EntityType.ITEM, box,
            e -> e.getStack().isOf(seed));
        if (nearby.isEmpty()) return;
        ItemEntity entity = nearby.get(0);
        ItemStack stack = entity.getStack();
        if (stack.getCount() <= 1) {
            entity.discard();
        } else {
            stack.decrement(1);
        }
    }
}
