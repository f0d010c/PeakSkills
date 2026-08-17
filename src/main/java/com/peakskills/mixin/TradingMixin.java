package com.peakskills.mixin;

import com.peakskills.skill.Skill;
import com.peakskills.xp.XpManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class TradingMixin {

    /**
     * Fires on every slot click. We filter to slot 2 of MerchantScreenHandler
     * (the trade output slot) to award Trading XP on completed trades.
     * MerchantScreenHandler inherits onSlotClick from ScreenHandler, so we
     * target ScreenHandler directly and check instanceof at runtime.
     */
    @Inject(method = "clicked", at = @At("TAIL"))
    private void onTrade(int slotIndex, int button, ContainerInput actionType,
                         Player player, CallbackInfo ci) {
        if (!(((Object) this) instanceof MerchantMenu)) return;
        if (slotIndex != 2) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        XpManager.addXp(serverPlayer, Skill.TRADING, 229L);
    }
}
