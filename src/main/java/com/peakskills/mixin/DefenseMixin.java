package com.peakskills.mixin;

import com.peakskills.skill.Skill;
import com.peakskills.xp.XpManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class DefenseMixin {

    @Inject(method = "damage", at = @At("TAIL"))
    private void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (amount <= 0) return;

        ServerPlayerEntity player = (ServerPlayerEntity)(Object) this;

        // Must be wearing at least one piece of armor
        boolean hasArmor =
            !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()  ||
            !player.getEquippedStack(EquipmentSlot.CHEST).isEmpty() ||
            !player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()  ||
            !player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
        if (!hasArmor) return;

        // ~20 XP per damage point — calibrated so tanking ~10 damage/min for 40h reaches 99
        long xp = Math.max(1, Math.round(amount * 20));
        XpManager.addXp(player, Skill.DEFENSE, xp);
    }
}
