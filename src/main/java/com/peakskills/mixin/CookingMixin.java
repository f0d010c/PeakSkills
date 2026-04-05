package com.peakskills.mixin;

import com.peakskills.skill.Skill;
import com.peakskills.xp.XpManager;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public class CookingMixin {

    @Inject(method = "dropExperienceForRecipesUsed", at = @At("HEAD"))
    private void onFurnaceOutput(ServerPlayerEntity player, CallbackInfo ci) {
        XpManager.addXp(player, Skill.COOKING, 113);
    }
}
