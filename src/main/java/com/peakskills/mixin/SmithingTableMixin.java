package com.peakskills.mixin;

import com.peakskills.skill.Skill;
import com.peakskills.xp.XpManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class SmithingTableMixin {

    /**
     * Awards Smithing XP when the player takes the result from the smithing table.
     * Slot 3 = smithing result (0=template, 1=base, 2=addition, 3=result).
     * SmithingScreenHandler does not override onSlotClick, so we target ScreenHandler.
     */
    @Inject(method = "clicked", at = @At("TAIL"))
    private void onSmithingTake(int slotIndex, int button, ContainerInput actionType,
                                Player player, CallbackInfo ci) {
        if (!(((Object) this) instanceof SmithingMenu)) return;
        if (slotIndex != 3) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        XpManager.addXp(serverPlayer, Skill.SMITHING, 35);
    }
}
