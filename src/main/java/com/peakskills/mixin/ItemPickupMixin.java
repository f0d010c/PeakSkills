package com.peakskills.mixin;

import com.peakskills.collection.CollectionRegistry;
import com.peakskills.collection.CollectionRewardHandler;
import com.peakskills.collection.CollectionTier;
import com.peakskills.collection.CollectionType;
import com.peakskills.player.PlayerDataManager;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

/**
 * Tracks combat collections by item pickup rather than mob kills.
 * Increments the collection count by the number of items picked up.
 */
@Mixin(PlayerEntity.class)
public class ItemPickupMixin {

    @Inject(method = "onPickupItem", at = @At("TAIL"))
    private void onItemPickup(ItemEntity itemEntity, int count, CallbackInfo ci) {
        if (!(((Object) this) instanceof ServerPlayerEntity player)) return;

        Optional<CollectionType> col = CollectionRegistry.fromCombatDrop(
            itemEntity.getStack().getItem());
        if (col.isEmpty()) return;

        List<CollectionTier> newTiers = PlayerDataManager.get(player.getUuid())
            .getCollections().increment(col.get(), count);
        CollectionRewardHandler.apply(player, col.get(), newTiers, PlayerDataManager.getServer());
    }
}
