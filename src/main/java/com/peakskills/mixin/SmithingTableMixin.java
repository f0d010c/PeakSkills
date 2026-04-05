package com.peakskills.mixin;

import com.peakskills.skill.Skill;
import com.peakskills.xp.XpManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class SmithingTableMixin {

    /**
     * Awards Smithing XP when the player takes the result from the smithing table.
     * Slot 3 = smithing result (0=template, 1=base, 2=addition, 3=result).
     * SmithingScreenHandler does not override onSlotClick, so we target ScreenHandler.
     */
    @Inject(method = "onSlotClick", at = @At("TAIL"))
    private void onSmithingTake(int slotIndex, int button, SlotActionType actionType,
                                PlayerEntity player, CallbackInfo ci) {
        if (!(((Object) this) instanceof SmithingScreenHandler)) return;
        if (slotIndex != 3) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        XpManager.addXp(serverPlayer, Skill.SMITHING, 35);
    }
}
