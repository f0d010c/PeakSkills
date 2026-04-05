package com.peakskills.mixin;

import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.WeakHashMap;

@Mixin(MerchantScreenHandler.class)
public class TradingDiscountMixin {

    /**
     * Tracks what discount we previously applied to each TradeOffer so we can
     * restore and re-apply correctly when the screen is reopened at a higher level.
     * WeakHashMap ensures dead offers are GC'd automatically.
     */
    private static final WeakHashMap<TradeOffer, Integer> appliedDiscounts = new WeakHashMap<>();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void applyTradingDiscount(int syncId, PlayerInventory playerInventory,
                                      CallbackInfo ci) {
        if (!(playerInventory.player instanceof ServerPlayerEntity sp)) return;
        int level = PlayerDataManager.get(sp.getUuid()).getLevel(Skill.TRADING);
        if (level < 5) return; // no discount below level 5

        // 0.5% discount per level → ~10% at L20, ~25% at L50, ~50% at L99
        float factor = level * 0.005f;

        MerchantScreenHandler handler = (MerchantScreenHandler)(Object) this;
        TradeOfferList offers = handler.getRecipes();
        if (offers == null) return;

        for (TradeOffer offer : offers) {
            // Restore any previous PeakSkills discount before recalculating
            int prev = appliedDiscounts.getOrDefault(offer, 0);
            if (prev > 0) offer.increaseSpecialPrice(prev);

            int base = offer.getOriginalFirstBuyItem().getCount();
            int discount = Math.round(base * factor);
            if (discount <= 0) continue;

            offer.increaseSpecialPrice(-discount);
            appliedDiscounts.put(offer, discount);
        }
    }
}
