package com.peakskills.gui;

import java.util.Map;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class SkillsScreenHandler extends ChestMenu {

    private final Map<Integer, Runnable> clickHandlers;
    private final Map<Integer, Runnable> rightClickHandlers;
    private final Map<Integer, Runnable> middleClickHandlers;

    /** No-action constructor. */
    public SkillsScreenHandler(int syncId, Inventory playerInventory, Container inventory) {
        this(syncId, playerInventory, inventory, Map.of(), Map.of(), Map.of());
    }

    /** Left-click only. */
    public SkillsScreenHandler(int syncId, Inventory playerInventory, Container inventory,
                                Map<Integer, Runnable> clickHandlers) {
        this(syncId, playerInventory, inventory, clickHandlers, Map.of(), Map.of());
    }

    /** Left-click + right-click handlers. */
    public SkillsScreenHandler(int syncId, Inventory playerInventory, Container inventory,
                                Map<Integer, Runnable> clickHandlers,
                                Map<Integer, Runnable> rightClickHandlers) {
        this(syncId, playerInventory, inventory, clickHandlers, rightClickHandlers, Map.of());
    }

    /** Left-click + right-click + middle-click handlers. */
    public SkillsScreenHandler(int syncId, Inventory playerInventory, Container inventory,
                                Map<Integer, Runnable> clickHandlers,
                                Map<Integer, Runnable> rightClickHandlers,
                                Map<Integer, Runnable> middleClickHandlers) {
        super(MenuType.GENERIC_9x6, syncId, playerInventory, inventory, 6);
        this.clickHandlers        = clickHandlers;
        this.rightClickHandlers   = rightClickHandlers;
        this.middleClickHandlers  = middleClickHandlers;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput actionType, Player player) {
        if (slotIndex >= 0 && slotIndex < 54) {
            if (actionType == ContainerInput.PICKUP) {
                if (button == 0) {
                    Runnable action = clickHandlers.get(slotIndex);
                    if (action != null) action.run();
                } else if (button == 1) {
                    Runnable action = rightClickHandlers.get(slotIndex);
                    if (action != null) action.run();
                }
            } else if (actionType == ContainerInput.CLONE || actionType == ContainerInput.QUICK_MOVE) {
                Runnable action = middleClickHandlers.get(slotIndex);
                if (action != null) action.run();
            }
            return;
        }
        super.clicked(slotIndex, button, actionType, player);
    }
}
