package com.peakskills.mixin;

import com.peakskills.skill.Skill;
import com.peakskills.xp.XpManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.PotionItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class BrewingMixin {

    /**
     * Awards Alchemy XP when the player takes a finished potion from the brewing stand.
     * Slots 0-2 are the potion output slots in BrewingStandScreenHandler.
     * We check for a PotionItem so partial/empty slots and ingredient moves are ignored.
     */
    @Inject(method = "clicked", at = @At("HEAD"))
    private void onTakePotion(int slotIndex, int button, ContainerInput actionType,
                              Player player, CallbackInfo ci) {
        if (!(((Object) this) instanceof BrewingStandMenu handler)) return;
        if (slotIndex < 0 || slotIndex > 2) return;
        if (!(player instanceof ServerPlayer sp)) return;
        if (actionType != ContainerInput.PICKUP && actionType != ContainerInput.QUICK_MOVE) return;

        net.minecraft.world.inventory.Slot slot = handler.getSlot(slotIndex);
        if (slot.getItem().isEmpty()) return;
        if (!(slot.getItem().getItem() instanceof PotionItem)) return;

        XpManager.addXp(sp, Skill.ALCHEMY, 229);
    }
}
