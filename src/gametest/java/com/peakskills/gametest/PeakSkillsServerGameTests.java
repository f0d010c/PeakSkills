package com.peakskills.gametest;

import com.mojang.brigadier.tree.CommandNode;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.player.PlayerDataFailsafe;
import com.peakskills.fishing.event.FishingCommunityEventManager;
import com.peakskills.skill.Skill;
import com.peakskills.skill.XPTable;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import java.util.UUID;
import com.peakskills.collection.CollectionRegistry;
import com.peakskills.collection.CollectionType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PeakSkillsServerGameTests {

    private static final List<String> PLAYER_COMMANDS = List.of(
        "skills", "profile", "collections", "pets", "craft", "settings", "fishing",
        "fishingjournal", "skilltop", "skillrank"
    );

    @GameTest
    public void registersPublicCommandSurface(GameTestHelper context) {
        var root = context.getLevel().getServer().getCommands().getDispatcher().getRoot();
        for (String command : PLAYER_COMMANDS) {
            context.assertTrue(root.getChild(command) != null, "Missing /" + command + " command");
        }
        context.succeed();
    }

    @GameTest
    public void hidesAdminSkillCommandsFromNonOperators(GameTestHelper context) {
        var dispatcher = context.getLevel().getServer().getCommands().getDispatcher();
        CommandNode<CommandSourceStack> skills = dispatcher.getRoot().getChild("skills");
        context.assertTrue(skills != null, "Missing /skills command");

        ServerPlayer player = fakePlayer(context, "permission_test");
        CommandSourceStack source = player.createCommandSourceStack();
        for (String command : List.of("addxp", "setlevel", "reset", "removexp", "backup", "restore", "fishingevent")) {
            CommandNode<CommandSourceStack> node = skills.getChild(command);
            context.assertTrue(node != null, "Missing /skills " + command + " command");
            context.assertFalse(node.canUse(source), "Non-operator can use /skills " + command);
        }
        context.succeed();
    }

    @GameTest
    public void dialogCallbackCommandIsPlayerOnly(GameTestHelper context) {
        var dispatcher = context.getLevel().getServer().getCommands().getDispatcher();
        CommandNode<CommandSourceStack> callback = dispatcher.getRoot().getChild("_peakskills_ui");
        context.assertTrue(callback != null, "Missing native-dialog callback command");
        context.assertTrue(callback.canUse(fakePlayer(context, "dialog_callback").createCommandSourceStack()),
            "Player cannot submit a native-dialog callback");
        try {
            int result = dispatcher.execute("_peakskills_ui invalid-token",
                context.getLevel().getServer().createCommandSourceStack());
            context.assertValueEqual(0, result, "Console callback did not fail closed");
        } catch (Exception exception) {
            context.fail("Console callback check failed: " + exception.getMessage());
        }
        context.succeed();
    }

    @GameTest
    public void playerProgressionWorksInsideARealServer(GameTestHelper context) {
        ServerPlayer player = fakePlayer(context, "progression_test");
        PlayerData data = PlayerDataManager.get(player.getUUID());
        long threshold = XPTable.xpForLevel(10);

        context.assertTrue(data.addXp(Skill.MINING, threshold), "Expected the player to level up");
        context.assertValueEqual(10, data.getLevel(Skill.MINING), "Mining level did not reach 10");
        context.assertValueEqual(threshold, data.getXp(Skill.MINING), "Mining XP was not stored exactly");
        context.succeed();
    }

    @GameTest
    public void playerBackupCanBeRestoredInsideARealServer(GameTestHelper context) {
        try {
            UUID playerId = UUID.randomUUID();
            PlayerData data = PlayerDataManager.get(playerId);
            long savedXp = XPTable.xpForLevel(12);
            data.addXp(Skill.FISHING, savedXp);

            PlayerDataFailsafe.backup(playerId, context.getLevel().getServer());
            data.addXp(Skill.FISHING, 50_000);
            context.assertTrue(PlayerDataFailsafe.restore(playerId, context.getLevel().getServer()).isPresent(),
                "Expected a backup to be restored");
            context.assertValueEqual(savedXp, PlayerDataManager.get(playerId).getXp(Skill.FISHING),
                "Restore did not recover the exact saved Fishing XP");
            context.succeed();
        } catch (Exception exception) {
            context.fail("Backup/restore round trip failed: " + exception.getMessage());
        }
    }

    @GameTest
    public void fishingEventInputsAreClamped(GameTestHelper context) {
        var server = context.getLevel().getServer();
        FishingCommunityEventManager.stop(server);
        context.assertTrue(FishingCommunityEventManager.start(server, Integer.MAX_VALUE, Integer.MAX_VALUE),
            "Expected the event to start");
        String status = FishingCommunityEventManager.statusText().getString();
        context.assertTrue(status.contains("0 / 1000000"), "Goal was not clamped: " + status);
        context.assertTrue(FishingCommunityEventManager.stop(server), "Expected the event to stop cleanly");
        context.succeed();
    }

    @GameTest
    public void collectionDropMatchingUsesTheGeneratedItem(GameTestHelper context) {
        context.assertTrue(CollectionRegistry.matchesItem(CollectionType.OAK_WOOD,
            new ItemStack(Items.STRIPPED_OAK_LOG, 2)), "Stripped log did not map to oak collection");
        context.assertFalse(CollectionRegistry.matchesItem(CollectionType.GRAVEL,
            new ItemStack(Items.FLINT)), "Flint incorrectly counted as gravel quantity");
        context.succeed();
    }

    private static ServerPlayer fakePlayer(GameTestHelper context, String name) {
        return FakePlayer.get(context.getLevel(), new GameProfile(UUID.randomUUID(), name));
    }
}
