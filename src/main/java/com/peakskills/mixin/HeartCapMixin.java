package com.peakskills.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Caps the visible heart display to 2 rows (40 HP) regardless of actual max health.
 * Actual HP is preserved server-side; hearts drain proportionally.
 */
@Mixin(InGameHud.class)
public class HeartCapMixin {

    private static final float MAX_DISPLAY_HP = 40.0f;

    /** Cap the heart container count to 2 rows (20 half-hearts). */
    @Inject(method = "getHeartCount", at = @At("RETURN"), cancellable = true)
    private void capHeartCount(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() > 20) {
            cir.setReturnValue(20);
        }
    }

    /** Cap max health used by the HUD renderer to 40 (2 rows). */
    @Redirect(
        method = "renderStatusBars",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;getMaxHealth()F")
    )
    private float capMaxHealth(PlayerEntity player) {
        return Math.min(player.getMaxHealth(), MAX_DISPLAY_HP);
    }

    /** Scale current health proportionally so hearts deplete correctly. */
    @Redirect(
        method = "renderStatusBars",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;getHealth()F")
    )
    private float scaleCurrentHealth(PlayerEntity player) {
        float max = player.getMaxHealth();
        if (max <= MAX_DISPLAY_HP) return player.getHealth();
        return player.getHealth() * (MAX_DISPLAY_HP / max);
    }
}
