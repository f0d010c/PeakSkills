package com.peakskills.mixin;

import com.peakskills.collection.CollectionRewardHandler;
import com.peakskills.collection.CollectionTier;
import com.peakskills.collection.CollectionType;
import com.peakskills.fishing.FishingLootTable;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import com.peakskills.stat.Stat;
import com.peakskills.xp.XpManager;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@Mixin(FishingBobberEntity.class)
public class FishingMixin {

    // Track which bobber UUIDs have already dispensed custom loot.
    // WeakHashMap wrapping a Set — entries are GC'd once the bobber entity is gone.
    // UUID is a value type, so we use a Set<UUID> backed by a map keyed on UUID.
    // Using a simple concurrent-safe set via synchronizedSet.
    private static final Set<UUID> usedBobbers =
        Collections.synchronizedSet(Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>()));

    @Inject(method = "use", at = @At("RETURN"))
    private void onReel(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        int count = cir.getReturnValue();
        if (count <= 0) return;

        FishingBobberEntity self = (FishingBobberEntity)(Object) this;

        // Each bobber should only give custom loot once — prevents the duplication
        // glitch where reeling a nearby dropped ItemEntity re-triggers this mixin.
        UUID bobberUuid = self.getUuid();
        if (!usedBobbers.add(bobberUuid)) return;

        if (!(self.getPlayerOwner() instanceof ServerPlayerEntity player)) return;
        net.minecraft.server.MinecraftServer mcServer = PlayerDataManager.getServer();
        if (mcServer == null) return;
        // Fishing only works in overworld/normal worlds; overworld is always safe fallback
        ServerWorld sw = mcServer.getOverworld();

        // ── Custom loot roll ──────────────────────────────────────────────────
        PlayerData data      = PlayerDataManager.get(player.getUuid());
        int fishingLevel     = data.getLevel(Skill.FISHING);

        double luckRaw = 0;
        var luckAttr = player.getAttributeInstance(Stat.LUCK.getAttribute());
        if (luckAttr != null) luckRaw = luckAttr.getValue();

        FishingLootTable.RollResult result = FishingLootTable.roll(fishingLevel, luckRaw, sw.getRandom());
        if (result == null || result.stack().isEmpty()) return;

        ItemStack loot = result.stack();

        // ── Fishing XP — scaled by rarity of the catch ────────────────────────
        XpManager.addXp(player, Skill.FISHING, result.xp());

        // ── Fishing collections ───────────────────────────────────────────────
        CollectionType fishCol = fishCollection(loot);
        if (fishCol != null) {
            List<CollectionTier> newTiers = data.getCollections().increment(fishCol, 1);
            CollectionRewardHandler.apply(player, fishCol, newTiers, mcServer);
        }

        // ── Spawn item near bobber, angled toward player ───────────────────────
        double x = self.getX(), y = self.getY(), z = self.getZ();
        ItemEntity ie = new ItemEntity(sw, x, y, z, loot);
        double dx = player.getX() - x;
        double dy = player.getY() - y + 0.5;
        double dz = player.getZ() - z;
        double speed = 0.1;
        ie.setVelocity(
            dx * speed,
            dy * speed + Math.sqrt(Math.sqrt(dx*dx + dy*dy + dz*dz)) * 0.08,
            dz * speed
        );
        sw.spawnEntity(ie);

        // ── Action bar catch message ──────────────────────────────────────────
        player.sendMessage(
            Text.literal("✦ ").formatted(Formatting.GOLD)
                .append(Text.literal("Caught: ").formatted(Formatting.GRAY))
                .append(loot.getName()),
            true
        );
    }

    private static CollectionType fishCollection(ItemStack stack) {
        if (stack.isOf(Items.COD))           return CollectionType.COD;
        if (stack.isOf(Items.SALMON))        return CollectionType.SALMON;
        if (stack.isOf(Items.PUFFERFISH))    return CollectionType.PUFFERFISH;
        if (stack.isOf(Items.TROPICAL_FISH)) return CollectionType.TROPICAL_FISH;
        return null;
    }
}
