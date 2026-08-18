package com.peakskills.mixin;

import com.peakskills.collection.CollectionRewardHandler;
import com.peakskills.collection.CollectionTier;
import com.peakskills.collection.CollectionType;
import com.peakskills.config.PeakConfig;
import com.peakskills.fishing.FishingLootTable;
import com.peakskills.fishing.FishingContext;
import com.peakskills.fishing.FishingEnvironment;
import com.peakskills.fishing.FishingGearBridge;
import com.peakskills.fishing.FishingModifiers;
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
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.stats.Stats;

@Mixin(FishingHook.class)
public class FishingMixin {

    @Shadow
    private boolean biting;

    @Shadow
    private int timeUntilLured;

    private boolean peakskills$swiftReelApplied;

    @Inject(method = "catchingFish", at = @At("TAIL"))
    private void peakskills$applySwiftReel(BlockPos pos, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        FishingHook self = (FishingHook)(Object) this;
        if (!(self.getPlayerOwner() instanceof ServerPlayer player)) return;
        if (timeUntilLured <= 0) {
            peakskills$swiftReelApplied = false;
            return;
        }
        if (peakskills$swiftReelApplied) return;
        boolean raining = self.level().isRaining();
        double reduction = Math.max(FishingGearBridge.swiftReelReduction(player.getMainHandItem(), raining),
            FishingGearBridge.swiftReelReduction(player.getOffhandItem(), raining));
        if (reduction > 0) timeUntilLured = Math.max(20, (int) Math.ceil(timeUntilLured * (1.0 - reduction)));
        peakskills$swiftReelApplied = true;
    }

    @Inject(method = "retrieve", at = @At("HEAD"), cancellable = true)
    private void onReel(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        FishingHook self = (FishingHook)(Object) this;

        if (!PeakConfig.get().fishingOverhaulEnabled) return;

        // The overhaul replaces successful loot catches only. Entity pulling,
        // empty reels, and other vanilla rod behavior remain vanilla-owned.
        if (self.getHookedIn() != null) return;

        if (!biting) return;

        if (!self.isInWater()) {
            BlockPos pos = self.blockPosition();
            boolean waterBelow = self.level().getFluidState(pos).is(FluidTags.WATER)
                || self.level().getFluidState(pos.below()).is(FluidTags.WATER);
            if (!waterBelow) return;
        }

        if (!(self.getPlayerOwner() instanceof ServerPlayer player)) return;
        if (!(self.level() instanceof ServerLevel sw)) return;
        net.minecraft.server.MinecraftServer mcServer = sw.getServer();

        PlayerData data = PlayerDataManager.get(player.getUUID());
        int fishingLevel = data.getLevel(Skill.FISHING);
        int eventContribution = 1;

        double luckRaw = 0;
        var luckAttr = player.getAttribute(Stat.LUCK.getAttribute());
        if (luckAttr != null) luckRaw = luckAttr.getValue();

        int enchantmentLuck = EnchantmentHelper.getFishingLuckBonus(sw, usedItem, player);
        FishingEnvironment.Environment environment = FishingEnvironment.inspect(sw, self.blockPosition());
        FishingContext context = new FishingContext(
            fishingLevel, luckRaw, enchantmentLuck,
            environment.depth(), environment.mood(), environment.biome(),
            environment.raining(), environment.night(),
            environment.waterDepth(), environment.sampledWaterBlocks()
        );
        FishingModifiers modifiers = FishingGearBridge.read(player, usedItem, data);
        FishingLootTable.RollResult result = FishingLootTable.roll(context, modifiers, sw.getRandom());
        if (result == null || result.stack().isEmpty()) return;

        ItemStack loot = result.stack();

        double abilityMult = SkillAbilityRegistry.getFlatXpMultiplier(Skill.FISHING, fishingLevel);
        double configMult = PeakConfig.get().fishingXpMultiplier;
        long fishingXp = configMult <= 0 ? 0L
            : Math.max(1L, Math.round(result.xp() * abilityMult * configMult * modifiers.xpMultiplier()));
        if (fishingXp > 0) XpManager.addXp(player, Skill.FISHING, fishingXp);
        FishingCommunityEventManager.recordCatch(player, eventContribution);

        boolean newDiscovery = !data.getFishingJournal().hasDiscovered(result.entryId());
        data.getFishingJournal().record(context, result);
        FishingGearBridge.consumeBait(usedItem);

        CollectionType fishCol = fishCollection(loot);
        if (fishCol != null) {
            List<CollectionTier> newTiers = data.getCollections().increment(fishCol, result.totalQuantity());
            CollectionRewardHandler.apply(player, fishCol, newTiers, mcServer);
        }

        double x = self.getX();
        double y = self.getY();
        double z = self.getZ();
        double dx = player.getX() - x;
        double dy = player.getY() - y + 0.5;
        double dz = player.getZ() - z;
        double speed = 0.1;
        List<ItemStack> awardedStacks = new java.util.ArrayList<>(result.copies());
        for (int copy = 0; copy < result.copies(); copy++) {
            ItemStack awarded = loot.copy();
            awardedStacks.add(awarded.copy());
            ItemEntity ie = new ItemEntity(sw, x, y, z, awarded);
            ie.setDeltaMovement(
                dx * speed,
                dy * speed + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08,
                dz * speed
            );
            sw.addFreshEntity(ie);
        }

        CriteriaTriggers.FISHING_ROD_HOOKED.trigger(player, usedItem, self, awardedStacks);
        player.awardStat(Stats.CUSTOM.get(Stats.FISH_CAUGHT), 1);
        ExperienceOrb.award(sw, player.position(), sw.getRandom().nextInt(1, 7));

        player.sendSystemMessage(
            Component.literal("* ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Caught: ").withStyle(ChatFormatting.GRAY))
                .append(loot.getHoverName())
                .append(Component.literal(" x" + result.totalQuantity()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" · " + environment.depth().displayName
                    + " · " + environment.mood().displayName).withStyle(ChatFormatting.DARK_AQUA))
                .append(newDiscovery
                    ? Component.literal(" · NEW").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                    : Component.empty()),
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
