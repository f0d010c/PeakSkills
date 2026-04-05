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
    private final Map<Integer, Runnable> rightClickHandlers;

    /** No-action constructor. */
    public SkillsScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        this(syncId, playerInventory, inventory, Map.of(), Map.of());
    }

    /** Left-click only. */
    public SkillsScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory,
                                Map<Integer, Runnable> clickHandlers) {
        this(syncId, playerInventory, inventory, clickHandlers, Map.of());
    }

    /** Left-click + right-click handlers. */
    public SkillsScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory,
                                Map<Integer, Runnable> clickHandlers,
                                Map<Integer, Runnable> rightClickHandlers) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, inventory, 6);
        this.clickHandlers      = clickHandlers;
        this.rightClickHandlers = rightClickHandlers;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < 54) {
            if (actionType == SlotActionType.PICKUP) {
                if (button == 0) {
                    Runnable action = clickHandlers.get(slotIndex);
                    if (action != null) action.run();
                } else if (button == 1) {
                    Runnable action = rightClickHandlers.get(slotIndex);
                    if (action != null) action.run();
                }
            }
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }
}
