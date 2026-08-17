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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@Mixin(FishingHook.class)
public class FishingMixin {

    @Shadow
    private boolean biting;

    @Inject(method = "retrieve", at = @At("HEAD"), cancellable = true)
    private void onReel(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        FishingHook self = (FishingHook)(Object) this;

        if (!PeakConfig.get().fishingOverhaulEnabled) return;

        if (self.getHookedIn() != null) {
            cancelAndRemove(self, cir, 0);
            return;
        }

        if (!biting) return;

        if (!self.isInWater()) {
            BlockPos pos = self.blockPosition();
            boolean waterBelow = self.level().getFluidState(pos).is(FluidTags.WATER)
                || self.level().getFluidState(pos.below()).is(FluidTags.WATER);
            if (!waterBelow) {
                cancelAndRemove(self, cir, 0);
                return;
            }
        }

        if (!(self.getPlayerOwner() instanceof ServerPlayer player)) {
            cancelAndRemove(self, cir, 0);
            return;
        }
        net.minecraft.server.MinecraftServer mcServer = PlayerDataManager.getServer();
        if (mcServer == null) {
            cancelAndRemove(self, cir, 0);
            return;
        }
        if (!(self.level() instanceof ServerLevel sw)) {
            cancelAndRemove(self, cir, 0);
            return;
        }

        PlayerData data = PlayerDataManager.get(player.getUUID());
        int fishingLevel = data.getLevel(Skill.FISHING);
        int effectiveLevelBonus = 0;
        int eventContribution = 1;

        double luckRaw = 0;
        var luckAttr = player.getAttribute(Stat.LUCK.getAttribute());
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
        ie.setDeltaMovement(
            dx * speed,
            dy * speed + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08,
            dz * speed
        );
        sw.addFreshEntity(ie);

        player.sendSystemMessage(
            Component.literal("* ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Caught: ").withStyle(ChatFormatting.GRAY))
                .append(loot.getHoverName()),
            true
        );

        cancelAndRemove(self, cir, 1);
    }

    private static void cancelAndRemove(FishingHook bobber, CallbackInfoReturnable<Integer> cir, int returnValue) {
        cir.setReturnValue(returnValue);
        bobber.discard();
    }

    private static CollectionType fishCollection(ItemStack stack) {
        if (stack.is(Items.COD)) return CollectionType.COD;
        if (stack.is(Items.SALMON)) return CollectionType.SALMON;
        if (stack.is(Items.PUFFERFISH)) return CollectionType.PUFFERFISH;
        if (stack.is(Items.TROPICAL_FISH)) return CollectionType.TROPICAL_FISH;
        if (stack.is(Items.LILY_PAD)) return CollectionType.LILY_PAD;
        if (stack.is(Items.INK_SAC)) return CollectionType.INK_SAC;
        if (stack.is(Items.NAUTILUS_SHELL)) return CollectionType.NAUTILUS_SHELL;
        if (stack.is(Items.PRISMARINE_SHARD)) return CollectionType.PRISMARINE;
        return null;
    }
}
