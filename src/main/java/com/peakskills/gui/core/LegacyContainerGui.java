package com.peakskills.gui.core;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.gui.GuiLike;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.pb4.sgui.api.gui.SlotBasedGui;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * Secure SGui bridge for PeakSkills' existing container renderers.
 *
 * <p>The renderer-owned {@link SimpleContainer} remains the source of truth, so
 * existing live refreshes continue to work while all client clicks are handled
 * by SGui virtual slots. Player inventory manipulation is always locked.</p>
 */
public final class LegacyContainerGui extends SimpleGui {
    private static final long ACTION_COOLDOWN_NANOS = 100_000_000L;

    private final Map<Integer, Runnable> leftActions;
    private final Map<Integer, Runnable> rightActions;
    private final Map<Integer, Runnable> alternateActions;
    private long lastActionAt;

    private LegacyContainerGui(ServerPlayer player, Component title, SimpleContainer inventory,
                               Map<Integer, Runnable> leftActions,
                               Map<Integer, Runnable> rightActions,
                               Map<Integer, Runnable> alternateActions) {
        super(MenuType.GENERIC_9x6, player, false);
        if (inventory.getContainerSize() != 54) {
            throw new IllegalArgumentException("PeakSkills GUIs must contain exactly 54 slots");
        }
        // Keep the renderer-owned maps: paged menus replace handlers in-place.
        this.leftActions = leftActions;
        this.rightActions = rightActions;
        this.alternateActions = alternateActions;
        setTitle(title);
        setLockPlayerInventory(true);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            setSlot(slot, new BackedElement(inventory, slot));
        }
    }

    public static void open(ServerPlayer player, Component title, SimpleContainer inventory,
                            Map<Integer, Runnable> leftActions) {
        open(player, title, inventory, leftActions, Map.of(), Map.of());
    }

    public static void open(ServerPlayer player, Component title, SimpleContainer inventory,
                            Map<Integer, Runnable> leftActions,
                            Map<Integer, Runnable> rightActions,
                            Map<Integer, Runnable> alternateActions) {
        new LegacyContainerGui(player, title, inventory, leftActions, rightActions, alternateActions).open();
    }

    private void handleClick(int slot, ClickType type) {
        Runnable action = switch (type) {
            case MOUSE_LEFT -> leftActions.get(slot);
            case MOUSE_RIGHT -> rightActions.get(slot);
            case MOUSE_MIDDLE, MOUSE_LEFT_SHIFT, MOUSE_RIGHT_SHIFT -> alternateActions.get(slot);
            default -> null;
        };
        if (action == null || !isOpen() || getPlayer().hasDisconnected()) return;

        long now = System.nanoTime();
        if (now - lastActionAt < ACTION_COOLDOWN_NANOS) return;
        lastActionAt = now;
        action.run();
    }

    @Override
    public boolean onAnyClick(int index, ClickType type, ContainerInput action) {
        return false;
    }

    private final class BackedElement implements GuiElement {
        private final SimpleContainer inventory;
        private final int slot;

        private BackedElement(SimpleContainer inventory, int slot) {
            this.inventory = inventory;
            this.slot = slot;
        }

        @Override
        public ItemStack getItemStack() {
            return inventory.getItem(slot).copy();
        }

        @Override
        public ItemStack getItemStackForDisplay(GuiLike gui) {
            return inventory.getItem(slot).copy();
        }

        @Override
        public ClickCallback getGuiCallback() {
            return (index, type, action, gui) -> handleClick(slot, type);
        }
    }
}
