package com.peakskills.mixin;

import com.peakskills.skill.Skill;
import com.peakskills.xp.XpManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingResultSlot.class)
public class CraftingMixin {

    @Inject(method = "onTakeItem", at = @At("TAIL"))
    private void onCraft(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        // XP based on stack size crafted
        // 1207 XP base per craft action, divided across typical output count
        // so bulk recipes (yield 4-8) don't give disproportionate XP
        int count = Math.max(1, stack.getCount());
        long xp = (long) Math.ceil(151.0 / Math.sqrt(count));
        XpManager.addXp(serverPlayer, Skill.CRAFTING, xp);
    }
}
