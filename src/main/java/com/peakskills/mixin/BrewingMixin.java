package com.peakskills.mixin;

import com.peakskills.skill.Skill;
import com.peakskills.xp.XpManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.PotionItem;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class BrewingMixin {

    /**
     * Awards Alchemy XP when the player takes a finished potion from the brewing stand.
     * Slots 0-2 are the potion output slots in BrewingStandScreenHandler.
     * We check for a PotionItem so partial/empty slots and ingredient moves are ignored.
     */
    @Inject(method = "onSlotClick", at = @At("HEAD"))
    private void onTakePotion(int slotIndex, int button, SlotActionType actionType,
                              PlayerEntity player, CallbackInfo ci) {
        if (!(((Object) this) instanceof BrewingStandScreenHandler handler)) return;
        if (slotIndex < 0 || slotIndex > 2) return;
        if (!(player instanceof ServerPlayerEntity sp)) return;
        if (actionType != SlotActionType.PICKUP && actionType != SlotActionType.QUICK_MOVE) return;

        net.minecraft.screen.slot.Slot slot = handler.getSlot(slotIndex);
        if (slot.getStack().isEmpty()) return;
        if (!(slot.getStack().getItem() instanceof PotionItem)) return;

        XpManager.addXp(sp, Skill.ALCHEMY, 229);
    }
}
