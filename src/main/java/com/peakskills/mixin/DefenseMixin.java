package com.peakskills.mixin;

import com.peakskills.skill.Skill;
import com.peakskills.xp.XpManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class DefenseMixin {

    @Inject(method = "hurtServer", at = @At("TAIL"))
    private void onDamage(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (amount <= 0) return;

        ServerPlayer player = (ServerPlayer)(Object) this;

        // Must be wearing at least one piece of armor
        boolean hasArmor =
            !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()  ||
            !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() ||
            !player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()  ||
            !player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
        if (!hasArmor) return;

        // ~20 XP per damage point — calibrated so tanking ~10 damage/min for 40h reaches 99
        long xp = Math.max(1, Math.round(amount * 20));
        XpManager.addXp(player, Skill.DEFENSE, xp);
    }
}
