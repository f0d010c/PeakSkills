package com.peakskills.command;

import com.peakskills.crafting.PeakCraftingGui;
import com.peakskills.gui.CollectionsGui;
import com.peakskills.gui.FishingJournalGui;
import com.peakskills.gui.PeakGuideGui;
import com.peakskills.gui.PetMenuGui;
import com.peakskills.gui.ProfileGui;
import com.peakskills.gui.SettingsGui;
import com.peakskills.gui.SkillsGui;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Clean public command tree. Legacy commands remain as compatibility aliases. */
public final class PeakGuideCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("peak")
                .executes(context -> open(context.getSource(), PeakGuideGui::open))
                .then(Commands.literal("guide").executes(context -> open(context.getSource(), PeakGuideGui::open)))
                .then(Commands.literal("profile").executes(context -> open(context.getSource(), ProfileGui::open)))
                .then(Commands.literal("skills").executes(context -> open(context.getSource(), SkillsGui::open)))
                .then(Commands.literal("collections").executes(context -> open(context.getSource(), CollectionsGui::open)))
                .then(Commands.literal("fishing").executes(context -> open(context.getSource(), FishingJournalGui::open)))
                .then(Commands.literal("pets").executes(context -> open(context.getSource(), PetMenuGui::open)))
                .then(Commands.literal("recipes").executes(context -> open(context.getSource(), PeakCraftingGui::open)))
                .then(Commands.literal("settings").executes(context -> open(context.getSource(), SettingsGui::open)))
            )
        );
    }

    private static int open(CommandSourceStack source, Consumer<ServerPlayer> action) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("The Peak Guide can only be opened by a player."));
            return 0;
        }
        action.accept(player);
        return 1;
    }

    private PeakGuideCommand() {}
}
