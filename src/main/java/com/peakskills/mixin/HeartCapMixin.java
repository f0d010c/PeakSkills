package com.peakskills.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Caps the visible heart display to 2 rows (20 hearts = 40 HP) regardless of actual max health.
 * Actual HP is preserved server-side; extra health above 40 acts as an invisible buffer.
 * Hearts drain proportionally: 80/100 HP shows 16/20 hearts, 20/100 shows 4/20, etc.
 */
@Mixin(InGameHud.class)
public class HeartCapMixin {

    private static final int MAX_DISPLAY_HEARTS    = 20;   // 2 rows × 10 hearts
    private static final float MAX_DISPLAY_HP      = 40.0f; // 20 hearts × 2 half-hearts
    private static final int MAX_DISPLAY_ROWS      = 2;

    /** Cap the heart container count to 2 rows. */
    @Inject(method = "getHeartCount", at = @At("RETURN"), cancellable = true)
    private void capHeartCount(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() > MAX_DISPLAY_HEARTS) {
            cir.setReturnValue(MAX_DISPLAY_HEARTS);
        }
    }

    /**
     * Scale health, prevHealth, maxHealth, heartCount, and rows in renderHealthBar
     * so hearts always fit within 2 rows and deplete proportionally.
     */
    @ModifyArgs(
        method = "renderStatusBars",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHealthBar(" +
                "Lnet/minecraft/client/gui/DrawContext;" +
                "Lnet/minecraft/entity/player/PlayerEntity;" +
                "IIIIFIIIZ)V")
    )
    private void scaleHeartDisplay(Args args) {
        float maxHealth = args.get(6); // actual max health (HP)
        if (maxHealth <= MAX_DISPLAY_HP) return; // already fits in 2 rows, no change needed

        float scale = MAX_DISPLAY_HP / maxHealth;

        int health    = args.get(7); // health in half-hearts
        int prevHealth = args.get(8);

        args.set(4, MAX_DISPLAY_HEARTS);           // heartCount
        args.set(5, MAX_DISPLAY_ROWS);             // rows
        args.set(6, MAX_DISPLAY_HP);               // maxHealth
        args.set(7, Math.round(health * scale));   // health (scaled)
        args.set(8, Math.round(prevHealth * scale)); // prevHealth (scaled)
    }
}
