package com.peakskills.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Map;

public class SkillsScreenHandler extends GenericContainerScreenHandler {

    private final Map<Integer, Runnable> clickHandlers;

    /** No-action constructor (existing GUIs that don't need clicks). */
    public SkillsScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        this(syncId, playerInventory, inventory, Map.of());
    }

    /** Clickable constructor — supply a slot→action map. */
    public SkillsScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory,
                                Map<Integer, Runnable> clickHandlers) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, inventory, 6);
        this.clickHandlers = clickHandlers;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < 54) {
            // Left-click only — fire action if registered
            if (button == 0 && actionType == SlotActionType.PICKUP) {
                Runnable action = clickHandlers.get(slotIndex);
                if (action != null) action.run();
            }
            return; // Always block item movement in GUI slots
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }
}
