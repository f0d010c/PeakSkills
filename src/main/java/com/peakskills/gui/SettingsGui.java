package com.peakskills.gui;

import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

public class SettingsGui {

    private static final int LEVEL_UP_DING_SLOT = 22;

    public static void open(ServerPlayer player) {
        PlayerData data = PlayerDataManager.get(player.getUUID());
        SimpleContainer inv = new SimpleContainer(54);
        populate(inv, data);

        Map<Integer, Runnable> handlers = Map.of(
            LEVEL_UP_DING_SLOT, () -> {
                data.setLimitBurstLevelUpSounds(!data.shouldLimitBurstLevelUpSounds());
                PlayerDataManager.saveAll();
                populate(inv, data);
            }
        );

        player.openMenu(new SimpleMenuProvider(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers),
            Component.literal("PeakSkills Settings").withStyle(ChatFormatting.AQUA)
        ));
    }

    private static void populate(SimpleContainer inv, PlayerData data) {
        ItemStack bg = pane(" ");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg.copy());

        boolean enabled = data.shouldLimitBurstLevelUpSounds();
        ItemStack ding = new ItemStack(enabled ? Items.NOTE_BLOCK : Items.BELL);
        ding.set(DataComponents.CUSTOM_NAME,
            Component.literal("Level-Up Ding Guard: ")
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)
                .append(Component.literal(enabled ? "ON" : "OFF")
                    .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD)));
        ding.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  When ON, level-up dings are muted").withStyle(ChatFormatting.GRAY),
            Component.literal("  after more than 5 levels in 5 minutes.").withStyle(ChatFormatting.GRAY),
            Component.literal("  Level-up messages and rewards still appear.").withStyle(ChatFormatting.DARK_GRAY),
            Component.empty(),
            Component.literal("  Click to toggle").withStyle(ChatFormatting.YELLOW)
        )));
        inv.setItem(LEVEL_UP_DING_SLOT, ding);
    }

    private static ItemStack pane(String name) {
        ItemStack stack = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }
}
