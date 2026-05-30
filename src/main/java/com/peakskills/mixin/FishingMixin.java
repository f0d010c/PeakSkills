package com.peakskills.mixin;

import com.peakskills.collection.CollectionRewardHandler;
import com.peakskills.collection.CollectionTier;
import com.peakskills.collection.CollectionType;
import com.peakskills.config.PeakConfig;
import com.peakskills.fishing.FishingLootTable;
import com.peakskills.fishing.event.FishingCommunityEventManager;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import com.peakskills.skill.SkillAbilityRegistry;
import com.peakskills.stat.Stat;
import com.peakskills.xp.XpManager;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(FishingBobberEntity.class)
public class FishingMixin {

    @Shadow
    private boolean caughtFish;

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onReel(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        FishingBobberEntity self = (FishingBobberEntity)(Object) this;

        if (!PeakConfig.get().fishingOverhaulEnabled) return;

        if (self.getHookedEntity() != null) {
            cancelAndRemove(self, cir, 0);
            return;
        }

        if (!caughtFish) return;

        if (!self.isTouchingWater()) {
            BlockPos pos = self.getBlockPos();
            boolean waterBelow = self.getEntityWorld().getFluidState(pos).isIn(FluidTags.WATER)
                || self.getEntityWorld().getFluidState(pos.down()).isIn(FluidTags.WATER);
            if (!waterBelow) {
                cancelAndRemove(self, cir, 0);
                return;
            }
        }

        if (!(self.getPlayerOwner() instanceof ServerPlayerEntity player)) {
            cancelAndRemove(self, cir, 0);
            return;
        }
        net.minecraft.server.MinecraftServer mcServer = PlayerDataManager.getServer();
        if (mcServer == null) {
            cancelAndRemove(self, cir, 0);
            return;
        }
        if (!(self.getEntityWorld() instanceof ServerWorld sw)) {
            cancelAndRemove(self, cir, 0);
            return;
        }

        PlayerData data = PlayerDataManager.get(player.getUuid());
        int fishingLevel = data.getLevel(Skill.FISHING);
        int effectiveLevelBonus = 0;
        int eventContribution = 1;

        double luckRaw = 0;
        var luckAttr = player.getAttributeInstance(Stat.LUCK.getAttribute());
        if (luckAttr != null) luckRaw = luckAttr.getValue();

        FishingLootTable.RollResult result = FishingLootTable.roll(fishingLevel + effectiveLevelBonus, luckRaw, sw.getRandom());
        if (result == null || result.stack().isEmpty()) {
            cancelAndRemove(self, cir, 1);
            return;
        }

        ItemStack loot = result.stack();

        double abilityMult = SkillAbilityRegistry.getFlatXpMultiplier(Skill.FISHING, fishingLevel);
        double configMult = PeakConfig.get().fishingXpMultiplier;
        long fishingXp = Math.max(1L, Math.round(result.xp() * abilityMult * configMult));
        XpManager.addXp(player, Skill.FISHING, fishingXp);
        FishingCommunityEventManager.recordCatch(player, eventContribution);

        CollectionType fishCol = fishCollection(loot);
        if (fishCol != null) {
            List<CollectionTier> newTiers = data.getCollections().increment(fishCol, 1);
            CollectionRewardHandler.apply(player, fishCol, newTiers, mcServer);
        }

        double x = self.getX();
        double y = self.getY();
        double z = self.getZ();
        ItemEntity ie = new ItemEntity(sw, x, y, z, loot);
        double dx = player.getX() - x;
        double dy = player.getY() - y + 0.5;
        double dz = player.getZ() - z;
        double speed = 0.1;
        ie.setVelocity(
            dx * speed,
            dy * speed + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08,
            dz * speed
        );
        sw.spawnEntity(ie);

        player.sendMessage(
            Text.literal("* ").formatted(Formatting.GOLD)
                .append(Text.literal("Caught: ").formatted(Formatting.GRAY))
                .append(loot.getName()),
            true
        );

        cancelAndRemove(self, cir, 1);
    }

    private static void cancelAndRemove(FishingBobberEntity bobber, CallbackInfoReturnable<Integer> cir, int returnValue) {
        cir.setReturnValue(returnValue);
        bobber.discard();
    }

    private static CollectionType fishCollection(ItemStack stack) {
        if (stack.isOf(Items.COD)) return CollectionType.COD;
        if (stack.isOf(Items.SALMON)) return CollectionType.SALMON;
        if (stack.isOf(Items.PUFFERFISH)) return CollectionType.PUFFERFISH;
        if (stack.isOf(Items.TROPICAL_FISH)) return CollectionType.TROPICAL_FISH;
        if (stack.isOf(Items.LILY_PAD)) return CollectionType.LILY_PAD;
        if (stack.isOf(Items.INK_SAC)) return CollectionType.INK_SAC;
        if (stack.isOf(Items.NAUTILUS_SHELL)) return CollectionType.NAUTILUS_SHELL;
        if (stack.isOf(Items.PRISMARINE_SHARD)) return CollectionType.PRISMARINE;
        return null;
    }
}
