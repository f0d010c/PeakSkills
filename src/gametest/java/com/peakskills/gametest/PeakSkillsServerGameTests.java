package com.peakskills.gametest;

import com.mojang.brigadier.tree.CommandNode;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import com.peakskills.skill.XPTable;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;

import java.util.List;
import java.util.UUID;

public class PeakSkillsServerGameTests {

    private static final List<String> PLAYER_COMMANDS = List.of(
        "skills", "profile", "collections", "pets", "craft", "settings", "skilltop", "skillrank"
    );

    @GameTest
    public void registersPublicCommandSurface(TestContext context) {
        var root = context.getWorld().getServer().getCommandManager().getDispatcher().getRoot();
        for (String command : PLAYER_COMMANDS) {
            context.assertTrue(root.getChild(command) != null, "Missing /" + command + " command");
        }
        context.complete();
    }

    @GameTest
    public void hidesAdminSkillCommandsFromNonOperators(TestContext context) {
        var dispatcher = context.getWorld().getServer().getCommandManager().getDispatcher();
        CommandNode<ServerCommandSource> skills = dispatcher.getRoot().getChild("skills");
        context.assertTrue(skills != null, "Missing /skills command");

        ServerPlayerEntity player = fakePlayer(context, "permission_test");
        ServerCommandSource source = player.getCommandSource();
        for (String command : List.of("addxp", "setlevel", "reset", "removexp", "backup", "restore", "fishingevent")) {
            CommandNode<ServerCommandSource> node = skills.getChild(command);
            context.assertTrue(node != null, "Missing /skills " + command + " command");
            context.assertFalse(node.canUse(source), "Non-operator can use /skills " + command);
        }
        context.complete();
    }

    @GameTest
    public void playerProgressionWorksInsideARealServer(TestContext context) {
        ServerPlayerEntity player = fakePlayer(context, "progression_test");
        PlayerData data = PlayerDataManager.get(player.getUuid());
        long threshold = XPTable.xpForLevel(10);

        context.assertTrue(data.addXp(Skill.MINING, threshold), "Expected the player to level up");
        context.assertEquals(10, data.getLevel(Skill.MINING), "Mining level did not reach 10");
        context.assertEquals(threshold, data.getXp(Skill.MINING), "Mining XP was not stored exactly");
        context.complete();
    }

    private static ServerPlayerEntity fakePlayer(TestContext context, String name) {
        return FakePlayer.get(context.getWorld(), new GameProfile(UUID.randomUUID(), name));
    }
}
