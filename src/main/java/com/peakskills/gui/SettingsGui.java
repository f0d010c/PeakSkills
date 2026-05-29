package com.peakskills.gui;

import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Map;

public class SettingsGui {

    private static final int LEVEL_UP_DING_SLOT = 22;

    public static void open(ServerPlayerEntity player) {
        PlayerData data = PlayerDataManager.get(player.getUuid());
        SimpleInventory inv = new SimpleInventory(54);
        populate(inv, data);

        Map<Integer, Runnable> handlers = Map.of(
            LEVEL_UP_DING_SLOT, () -> {
                data.setLimitBurstLevelUpSounds(!data.shouldLimitBurstLevelUpSounds());
                PlayerDataManager.saveAll();
                populate(inv, data);
            }
        );

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers),
            Text.literal("PeakSkills Settings").formatted(Formatting.AQUA)
        ));
    }

    private static void populate(SimpleInventory inv, PlayerData data) {
        ItemStack bg = pane(" ");
        for (int i = 0; i < 54; i++) inv.setStack(i, bg.copy());

        boolean enabled = data.shouldLimitBurstLevelUpSounds();
        ItemStack ding = new ItemStack(enabled ? Items.NOTE_BLOCK : Items.BELL);
        ding.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("Level-Up Ding Guard: ")
                .formatted(Formatting.WHITE, Formatting.BOLD)
                .append(Text.literal(enabled ? "ON" : "OFF")
                    .formatted(enabled ? Formatting.GREEN : Formatting.RED, Formatting.BOLD)));
        ding.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("  When ON, level-up dings are muted").formatted(Formatting.GRAY),
            Text.literal("  after more than 5 levels in 5 minutes.").formatted(Formatting.GRAY),
            Text.literal("  Level-up messages and rewards still appear.").formatted(Formatting.DARK_GRAY),
            Text.empty(),
            Text.literal("  Click to toggle").formatted(Formatting.YELLOW)
        )));
        inv.setStack(LEVEL_UP_DING_SLOT, ding);
    }

    private static ItemStack pane(String name) {
        ItemStack stack = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return stack;
    }
}
