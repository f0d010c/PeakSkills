package com.peakskills.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Caps the visible heart display to 2 rows (40 HP) regardless of actual max health.
 * Actual HP is preserved server-side; hearts drain proportionally.
 */
@Mixin(InGameHud.class)
public abstract class HeartCapMixin {

    private static final float MAX_DISPLAY_HP = 40.0f;
    private static final ThreadLocal<Boolean> CAPPING = ThreadLocal.withInitial(() -> false);

    @Shadow
    protected abstract void renderHealthBar(DrawContext context, PlayerEntity player,
        int x, int y, int lines, int regeneratingHeartIndex, float maxHealth,
        int lastHealth, int health, int absorptionAmount, boolean blinking);

    /** Also cap the heart container count returned for HUD layout. */
    @Inject(method = "getHeartCount", at = @At("RETURN"), cancellable = true)
    private void capHeartCount(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() > 20) {
            cir.setReturnValue(20);
        }
    }

    /**
     * Intercept renderHealthBar — if maxHealth exceeds 40, cancel and re-call
     * with proportionally scaled values so hearts fit in 2 rows.
     * ThreadLocal flag prevents infinite recursion on the re-call.
     */
    @Inject(method = "renderHealthBar", at = @At("HEAD"), cancellable = true)
    private void capHealthBar(DrawContext context, PlayerEntity player,
        int x, int y, int lines, int regeneratingHeartIndex, float maxHealth,
        int lastHealth, int health, int absorptionAmount, boolean blinking,
        CallbackInfo ci) {

        if (CAPPING.get()) return; // re-entrant call with already-scaled values, pass through
        if (maxHealth <= MAX_DISPLAY_HP) return; // already fits, no change needed

        ci.cancel();
        float scale = MAX_DISPLAY_HP / maxHealth;
        CAPPING.set(true);
        try {
            renderHealthBar(context, player, x, y, 2, regeneratingHeartIndex,
                MAX_DISPLAY_HP,
                Math.round(lastHealth * scale),
                Math.round(health * scale),
                0, // cap absorption display too
                blinking);
        } finally {
            CAPPING.set(false);
        }
    }
}
