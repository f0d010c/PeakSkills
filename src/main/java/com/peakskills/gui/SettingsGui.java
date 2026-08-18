package com.peakskills.gui;

import com.peakskills.gui.core.PeakDialogActions;
import com.peakskills.gui.core.PeakDialogs;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.level.ServerPlayer;

/** PeakSkills settings rendered with Minecraft's native server-driven dialog UI. */
public final class SettingsGui {
    public static void open(ServerPlayer player) {
        PlayerData data = PlayerDataManager.get(player.getUUID());
        PeakDialogActions.begin(player);

        String toggleToken = PeakDialogActions.issue(player, () -> {
            data.setLimitBurstLevelUpSounds(!data.shouldLimitBurstLevelUpSounds());
            PlayerDataManager.saveAll();
            open(player);
        });
        String skillsToken = PeakDialogActions.issue(player, () -> PeakGuideGui.open(player));

        boolean enabled = data.shouldLimitBurstLevelUpSounds();
        ActionButton toggle = PeakDialogs.actionButton(
            Component.literal("Level-Up Ding Guard: " + (enabled ? "ON" : "OFF"))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED),
            Component.literal("Limits repeated level-up dings after more than five levels in five minutes."),
            toggleToken);
        ActionButton back = PeakDialogs.actionButton(Component.literal("Back to Peak Guide"),
            Component.literal("Return to the main PeakMod menu."), skillsToken);

        CommonDialogData common = new CommonDialogData(
            Component.literal("PeakSkills Settings").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
            Optional.of(Component.literal("PeakSkills Settings")), true, false,
            DialogAction.WAIT_FOR_RESPONSE, List.of(), List.of());
        MultiActionDialog dialog = new MultiActionDialog(common, List.of(toggle), Optional.of(back), 1);
        player.connection.send(new ClientboundShowDialogPacket(Holder.direct(dialog)));
    }

    private SettingsGui() {}
}
