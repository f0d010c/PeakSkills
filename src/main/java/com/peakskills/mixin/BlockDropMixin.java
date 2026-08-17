package com.peakskills.mixin;

import com.peakskills.collection.CollectionRegistry;
import com.peakskills.collection.CollectionRewardHandler;
import com.peakskills.collection.CollectionTier;
import com.peakskills.collection.CollectionType;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.world.PlacedBlocksState;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Observes Minecraft's already-rolled block drops without changing them. */
@Mixin(Block.class)
public abstract class BlockDropMixin {
    @Redirect(
        method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;")
    )
    private static List<ItemStack> peakskills$countActualDrops(BlockState state,
            ServerLevel serverLevel, BlockPos pos, BlockEntity blockEntity,
            Entity breaker, ItemInstance tool) {
        List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, blockEntity, breaker, tool);
        if (!(breaker instanceof ServerPlayer player)) return drops;
        if (player.distanceToSqr(pos.getCenter()) > 64.0) return drops;

        CollectionType type = CollectionRegistry.fromBlock(state).orElse(null);
        if (type == null) return drops;
        if (!"Farming".equals(type.category)
                && PlacedBlocksState.get(serverLevel.getServer()).isPlaced(pos.asLong())) return drops;

        Block block = state.getBlock();
        if ((block == Blocks.SUGAR_CANE || block == Blocks.BAMBOO || block == Blocks.CACTUS)
                && serverLevel.getBlockState(pos.below()).getBlock() != block) return drops;

        int amount = drops.stream()
            .filter(drop -> CollectionRegistry.matchesItem(type, drop))
            .mapToInt(ItemStack::getCount)
            .sum();
        if (amount <= 0) return drops;

        List<CollectionTier> tiers = PlayerDataManager.get(player.getUUID())
            .getCollections().increment(type, amount);
        CollectionRewardHandler.apply(player, type, tiers, serverLevel.getServer());
        return drops;
    }
}
