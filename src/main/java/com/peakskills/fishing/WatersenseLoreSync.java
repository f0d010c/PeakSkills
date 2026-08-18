package com.peakskills.fishing;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Migrates carried identified Watersense catches to the current names and descriptions. */
public final class WatersenseLoreSync {
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    ItemStack stack = player.getInventory().getItem(slot);
                    if (WatersenseItemData.getLootId(stack) != null) {
                        FishingLootTable.refreshPresentation(stack);
                    }
                }
            }
        });
    }

    private WatersenseLoreSync() {}
}
