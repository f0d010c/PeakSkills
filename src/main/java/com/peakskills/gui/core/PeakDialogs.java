package com.peakskills.gui.core;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.ConfirmationDialog;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.level.ServerPlayer;

/** Reusable native-dialog actions and safe, one-use confirmation flow. */
public final class PeakDialogs {
    public static final int DEFAULT_BUTTON_WIDTH = 200;

    public static ActionButton actionButton(Component label, Component tooltip, String token) {
        return new ActionButton(
            new CommonButtonData(label, Optional.of(tooltip), DEFAULT_BUTTON_WIDTH),
            Optional.of(new StaticAction(new ClickEvent.RunCommand("_peakskills_ui " + token))));
    }

    public static void confirm(ServerPlayer player, Component title, Runnable accept, Runnable decline) {
        PeakDialogActions.begin(player);
        String acceptToken = PeakDialogActions.issue(player, accept);
        String declineToken = PeakDialogActions.issue(player, decline);
        CommonDialogData common = new CommonDialogData(title, Optional.of(title), true, false,
            DialogAction.WAIT_FOR_RESPONSE, List.of(), List.of());
        ConfirmationDialog dialog = new ConfirmationDialog(common,
            actionButton(Component.literal("Confirm").withStyle(ChatFormatting.GREEN),
                Component.literal("Perform this action."), acceptToken),
            actionButton(Component.literal("Cancel").withStyle(ChatFormatting.RED),
                Component.literal("Return without making changes."), declineToken));
        player.connection.send(new ClientboundShowDialogPacket(Holder.direct(dialog)));
    }

    private PeakDialogs() {}
}
