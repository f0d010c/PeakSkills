package com.peakskills.mixin;

import com.peakskills.gear.GearRequirements;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public class GearRestrictionMixin {

    @Inject(method = "canInsert", at = @At("HEAD"), cancellable = true)
    private void checkGearRequirement(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = (Slot)(Object) this;

        // Only restrict armor slots (slot indices 36-39 in player inventory)
        if (!(slot.inventory instanceof net.minecraft.entity.player.PlayerInventory playerInv)) return;
        int index = slot.getIndex();
        if (index < 36 || index > 39) return;

        PlayerEntity player = playerInv.player;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        GearRequirements.Requirement req = GearRequirements.getRequirement(stack.getItem());
        if (req == null) return;

        PlayerData data = PlayerDataManager.get(serverPlayer.getUuid());
        if (data.getLevel(req.skill()) < req.level()) {
            serverPlayer.sendMessage(
                Text.literal("Requires ")
                    .formatted(Formatting.RED)
                    .append(Text.literal(req.skill().getDisplayName() + " level " + req.level())
                        .formatted(Formatting.YELLOW)),
                true
            );
            cir.setReturnValue(false);
        }
    }
}
