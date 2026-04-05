package com.peakskills.mixin;

import com.peakskills.skill.Skill;
import com.peakskills.xp.XpManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class TradingMixin {

    /**
     * Fires on every slot click. We filter to slot 2 of MerchantScreenHandler
     * (the trade output slot) to award Trading XP on completed trades.
     * MerchantScreenHandler inherits onSlotClick from ScreenHandler, so we
     * target ScreenHandler directly and check instanceof at runtime.
     */
    @Inject(method = "onSlotClick", at = @At("TAIL"))
    private void onTrade(int slotIndex, int button, SlotActionType actionType,
                         PlayerEntity player, CallbackInfo ci) {
        if (!(((Object) this) instanceof MerchantScreenHandler)) return;
        if (slotIndex != 2) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        XpManager.addXp(serverPlayer, Skill.TRADING, 229L);
    }
}
