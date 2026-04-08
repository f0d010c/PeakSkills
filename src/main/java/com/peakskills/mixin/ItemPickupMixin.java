package com.peakskills.mixin;

import com.peakskills.collection.CollectionRegistry;
import com.peakskills.collection.CollectionRewardHandler;
import com.peakskills.collection.CollectionTier;
import com.peakskills.collection.CollectionType;
import com.peakskills.combat.CombatDropTracker;
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
import java.util.UUID;

/**
 * Tracks combat collections by item pickup.
 * Only counts items dropped by mobs killed by this specific player,
 * using CombatDropTracker to map mob UUID → killer UUID.
 */
@Mixin(PlayerEntity.class)
public class ItemPickupMixin {

    @Inject(method = "onPickupItem", at = @At("TAIL"))
    private void onItemPickup(ItemEntity itemEntity, int count, CallbackInfo ci) {
        if (!(((Object) this) instanceof ServerPlayerEntity player)) return;

        Optional<CollectionType> col = CollectionRegistry.fromCombatDrop(
            itemEntity.getStack().getItem());
        if (col.isEmpty()) return;

        // Only count if this item was dropped by a mob this player killed
        // In 1.21.11, ItemEntity has no getThrower() — use getOwner() which returns the source Entity
        net.minecraft.entity.Entity ownerEntity = itemEntity.getOwner();
        UUID thrower = ownerEntity != null ? ownerEntity.getUuid() : null;
        UUID killer = CombatDropTracker.getKiller(thrower);
        if (killer == null || !killer.equals(player.getUuid())) return;

        List<CollectionTier> newTiers = PlayerDataManager.get(player.getUuid())
            .getCollections().increment(col.get(), count);
        CollectionRewardHandler.apply(player, col.get(), newTiers, PlayerDataManager.getServer());
    }
}
