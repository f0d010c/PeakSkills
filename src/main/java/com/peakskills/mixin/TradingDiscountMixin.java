package com.peakskills.mixin;

import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

@Mixin(MerchantMenu.class)
public class TradingDiscountMixin {

    /**
     * Per-handler instance map: offer-list index → discount we applied this session.
     * Non-static so each player gets their own handler with independent tracking.
     * Keyed by index (not TradeOffer object) so it survives across offers being
     * recreated from NBT on server restart within the same session.
     */
    @Unique
    private final Map<Integer, Integer> peakDiscounts = new HashMap<>();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void applyTradingDiscount(int syncId, Inventory playerInventory,
                                      CallbackInfo ci) {
        if (!(playerInventory.player instanceof ServerPlayer sp)) return;

        int level = PlayerDataManager.get(sp.getUUID()).getLevel(Skill.TRADING);
        if (level < 5) return;

        // 0.5% discount per level → ~10% at L20, ~25% at L50, ~50% at L99
        float factor = level * 0.005f;

        MerchantMenu handler = (MerchantMenu)(Object) this;
        MerchantOffers offers = handler.getOffers();
        if (offers == null || offers.isEmpty()) return;

        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);

            // Restore any discount we applied earlier this session before recalculating
            int prev = peakDiscounts.getOrDefault(i, 0);
            if (prev > 0) offer.addToSpecialPriceDiff(prev);

            int base = offer.getBaseCostA().getCount();

            // Calculate discount; guarantee at least 1 emerald off at level 10+ for any trade
            int discount = Math.round(base * factor);
            if (discount <= 0 && level >= 10) discount = 1;
            if (discount <= 0) continue;

            offer.addToSpecialPriceDiff(-discount);
            peakDiscounts.put(i, discount);
        }
    }
}
