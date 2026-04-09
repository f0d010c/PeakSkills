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
 *
 * Targets ItemEntity.onPlayerCollision (the method called when a player
 * touches and picks up an item) — onPickupItem does not exist in 1.21.11 Yarn.
 */
@Mixin(ItemEntity.class)
public class ItemPickupMixin {

    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    private void onItemPickup(PlayerEntity playerEntity, CallbackInfo ci) {
        if (!(playerEntity instanceof ServerPlayerEntity player)) return;

        ItemEntity self = (ItemEntity)(Object) this;

        Optional<CollectionType> col = CollectionRegistry.fromCombatDrop(
            self.getStack().getItem());
        if (col.isEmpty()) return;

        // Only count if this item was tagged as a drop from a mob killed by this player.
        // ItemEntity.getOwner() is null for mob loot drops, so we tag item entities
        // by UUID in CombatDropTracker right after the mob dies (in AFTER_DEATH).
        UUID killer = CombatDropTracker.getKillerForItem(self.getUuid());
        if (killer == null || !killer.equals(player.getUuid())) return;

        int count = self.getStack().getCount();
        List<CollectionTier> newTiers = PlayerDataManager.get(player.getUuid())
            .getCollections().increment(col.get(), count);
        CollectionRewardHandler.apply(player, col.get(), newTiers, PlayerDataManager.getServer());
    }
}
