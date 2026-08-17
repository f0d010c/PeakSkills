package com.peakskills.mixin;

import com.peakskills.gear.GearRequirements;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public class GearRestrictionMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void checkGearRequirement(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = (Slot)(Object) this;

        // Only restrict armor slots (slot indices 36-39 in player inventory)
        if (!(slot.container instanceof net.minecraft.world.entity.player.Inventory playerInv)) return;
        int index = slot.getContainerSlot();
        if (index < 36 || index > 39) return;

        Player player = playerInv.player;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        GearRequirements.Requirement req = GearRequirements.getRequirement(stack.getItem());
        if (req == null) return;

        PlayerData data = PlayerDataManager.get(serverPlayer.getUUID());
        if (data.getLevel(req.skill()) < req.level()) {
            serverPlayer.sendSystemMessage(
                Component.literal("Requires ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(req.skill().getDisplayName() + " level " + req.level())
                        .withStyle(ChatFormatting.YELLOW)),
                true
            );
            cir.setReturnValue(false);
        }
    }
}
