package com.peakskills.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.peakskills.gui.SkillsGui;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataFailsafe;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import com.peakskills.skill.XPTable;
import com.peakskills.stat.StatManager;
import com.peakskills.xp.XpManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SkillsCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                CommandManager.literal("skills")

                    // /skills — open your own GUI
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                        SkillsGui.open(player);
                        return 1;
                    })

                    // /skills <player> — view another player's skills
                    .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayerEntity viewer = ctx.getSource().getPlayerOrThrow();
                            String name = StringArgumentType.getString(ctx, "player");
                            ServerPlayerEntity target = ctx.getSource().getServer()
                                .getPlayerManager().getPlayer(name);
                            if (target == null) {
                                ctx.getSource().sendError(Text.literal("Player not found: " + name));
                                return 0;
                            }
                            SkillsGui.open(viewer, PlayerDataManager.get(target.getUuid()),
                                target.getName().getString());
                            return 1;
                        })
                    )

                    // --- ADMIN SUBCOMMANDS (requires op level 2) ---

                    // /skills addxp <player> <skill> <amount>
                    .then(CommandManager.literal("addxp")
                        .requires(src -> {
                            return true; // TODO: restrict to OPs once correct 1.21.11 permission API is found
                        })
                        .then(CommandManager.argument("player", StringArgumentType.word())
                            .then(CommandManager.argument("skill", StringArgumentType.word())
                                .then(CommandManager.argument("amount", LongArgumentType.longArg(1))
                                    .executes(ctx -> {
                                        ServerPlayerEntity target = resolvePlayer(ctx.getSource().getServer(),
                                            StringArgumentType.getString(ctx, "player"));
                                        if (target == null) { ctx.getSource().sendError(Text.literal("Player not found")); return 0; }
                                        Skill skill = resolveSkill(StringArgumentType.getString(ctx, "skill"));
                                        if (skill == null) { ctx.getSource().sendError(Text.literal("Unknown skill")); return 0; }
                                        long amount = LongArgumentType.getLong(ctx, "amount");
                                        XpManager.addXp(target, skill, amount);
                                        ctx.getSource().sendFeedback(() -> Text.literal("Added " + amount + " " + skill.getDisplayName() + " XP to " + target.getName().getString()).formatted(Formatting.GREEN), true);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )

                    // /skills setlevel <player> <skill> <level>
                    .then(CommandManager.literal("setlevel")
                        .requires(src -> {
                            return true; // TODO: restrict to OPs once correct 1.21.11 permission API is found
                        })
                        .then(CommandManager.argument("player", StringArgumentType.word())
                            .then(CommandManager.argument("skill", StringArgumentType.word())
                                .then(CommandManager.argument("level", IntegerArgumentType.integer(1, Skill.MAX_LEVEL))
                                    .executes(ctx -> {
                                        ServerPlayerEntity target = resolvePlayer(ctx.getSource().getServer(),
                                            StringArgumentType.getString(ctx, "player"));
                                        if (target == null) { ctx.getSource().sendError(Text.literal("Player not found")); return 0; }
                                        Skill skill = resolveSkill(StringArgumentType.getString(ctx, "skill"));
                                        if (skill == null) { ctx.getSource().sendError(Text.literal("Unknown skill")); return 0; }
                                        int level = IntegerArgumentType.getInteger(ctx, "level");
                                        PlayerData data = PlayerDataManager.get(target.getUuid());
                                        // Set XP to exactly what's needed for this level (works up and down)
                                        long needed = XPTable.xpForLevel(level);
                                        long current = data.getXp(skill);
                                        long delta = needed - current;
                                        if (delta != 0) data.addXp(skill, delta);
                                        StatManager.applyStats(target);
                                        ctx.getSource().sendFeedback(() -> Text.literal("Set " + target.getName().getString() + "'s " + skill.getDisplayName() + " to level " + level).formatted(Formatting.GREEN), true);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )

                    // /skills top [count]
                    .then(CommandManager.literal("top")
                        .executes(ctx -> sendLeaderboard(ctx.getSource(), 10))
                        .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 50))
                            .executes(ctx -> sendLeaderboard(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "count")))
                        )
                    )

                    // /skills reset <player> [skill]
                    .then(CommandManager.literal("reset")
                        .requires(src -> true)
                        .then(CommandManager.argument("player", StringArgumentType.word())
                            // /skills reset <player> — reset ALL skills
                            .executes(ctx -> {
                                ServerPlayerEntity target = resolvePlayer(ctx.getSource().getServer(),
                                    StringArgumentType.getString(ctx, "player"));
                                if (target == null) { ctx.getSource().sendError(Text.literal("Player not found")); return 0; }
                                PlayerData data = PlayerDataManager.get(target.getUuid());
                                for (Skill skill : Skill.values()) {
                                    long xp = data.getXp(skill);
                                    if (xp > 0) data.addXp(skill, -xp);
                                }
                                StatManager.applyStats(target);
                                ctx.getSource().sendFeedback(() -> Text.literal("Reset all skills for " + target.getName().getString()).formatted(Formatting.YELLOW), true);
                                return 1;
                            })
                            // /skills reset <player> <skill> — reset ONE skill
                            .then(CommandManager.argument("skill", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayerEntity target = resolvePlayer(ctx.getSource().getServer(),
                                        StringArgumentType.getString(ctx, "player"));
                                    if (target == null) { ctx.getSource().sendError(Text.literal("Player not found")); return 0; }
                                    Skill skill = resolveSkill(StringArgumentType.getString(ctx, "skill"));
                                    if (skill == null) { ctx.getSource().sendError(Text.literal("Unknown skill")); return 0; }
                                    PlayerData data = PlayerDataManager.get(target.getUuid());
                                    long xp = data.getXp(skill);
                                    if (xp > 0) data.addXp(skill, -xp);
                                    StatManager.applyStats(target);
                                    ctx.getSource().sendFeedback(() -> Text.literal("Reset " + skill.getDisplayName() + " for " + target.getName().getString()).formatted(Formatting.YELLOW), true);
                                    return 1;
                                })
                            )
                        )
                    )

                    // /skills removexp <player> <skill> <amount>
                    .then(CommandManager.literal("removexp")
                        .requires(src -> true)
                        .then(CommandManager.argument("player", StringArgumentType.word())
                            .then(CommandManager.argument("skill", StringArgumentType.word())
                                .then(CommandManager.argument("amount", LongArgumentType.longArg(1))
                                    .executes(ctx -> {
                                        ServerPlayerEntity target = resolvePlayer(ctx.getSource().getServer(),
                                            StringArgumentType.getString(ctx, "player"));
                                        if (target == null) { ctx.getSource().sendError(Text.literal("Player not found")); return 0; }
                                        Skill skill = resolveSkill(StringArgumentType.getString(ctx, "skill"));
                                        if (skill == null) { ctx.getSource().sendError(Text.literal("Unknown skill")); return 0; }
                                        long amount = LongArgumentType.getLong(ctx, "amount");
                                        PlayerData data = PlayerDataManager.get(target.getUuid());
                                        long current = data.getXp(skill);
                                        long remove = Math.min(amount, current); // can't go below 0
                                        if (remove > 0) data.addXp(skill, -remove);
                                        StatManager.applyStats(target);
                                        ctx.getSource().sendFeedback(() -> Text.literal("Removed " + remove + " " + skill.getDisplayName() + " XP from " + target.getName().getString()).formatted(Formatting.YELLOW), true);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )
                    // /skills backup [player]
                    .then(CommandManager.literal("backup")
                        .requires(src -> true)
                        // /skills backup — back up ALL online players
                        .executes(ctx -> {
                            var source = ctx.getSource();
                            int count = 0;
                            for (ServerPlayerEntity p : source.getServer().getPlayerManager().getPlayerList()) {
                                if (doBackup(source, p) == 1) count++;
                            }
                            int finalCount = count;
                            source.sendFeedback(() -> Text.literal("Backed up " + finalCount + " player(s).").formatted(Formatting.GREEN), true);
                            return finalCount;
                        })
                        // /skills backup <player> — back up a specific player
                        .then(CommandManager.argument("player", StringArgumentType.word())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "player");
                                ServerPlayerEntity target = resolvePlayer(ctx.getSource().getServer(), name);
                                if (target == null) { ctx.getSource().sendError(Text.literal("Player not found: " + name)); return 0; }
                                return doBackup(ctx.getSource(), target);
                            })
                        )
                    )

                    // /skills restore <player>
                    .then(CommandManager.literal("restore")
                        .requires(src -> true)
                        .then(CommandManager.argument("player", StringArgumentType.word())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "player");
                                ServerPlayerEntity target = resolvePlayer(ctx.getSource().getServer(), name);
                                if (target == null) { ctx.getSource().sendError(Text.literal("Player not found: " + name)); return 0; }
                                return doRestore(ctx.getSource(), target);
                            })
                        )
                    )
            )
        );
    }

    private static int sendLeaderboard(net.minecraft.server.command.ServerCommandSource source, int count) {
        var entries = PlayerDataManager.getLeaderboard(count);
        source.sendFeedback(() -> Text.literal("━━━  Top " + count + " Players  ━━━").formatted(Formatting.GOLD, Formatting.BOLD), false);
        if (entries.isEmpty()) {
            source.sendFeedback(() -> Text.literal("  No data yet.").formatted(Formatting.DARK_GRAY), false);
            return 1;
        }
        net.minecraft.server.MinecraftServer server = source.getServer();
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            int rank = i + 1;
            long xp = entry.getValue();
            String name = server.getPlayerManager().getPlayer(entry.getKey()) != null
                ? server.getPlayerManager().getPlayer(entry.getKey()).getName().getString()
                : entry.getKey().toString().substring(0, 8);
            Formatting rankColor = rank == 1 ? Formatting.GOLD : rank == 2 ? Formatting.GRAY : rank == 3 ? Formatting.DARK_RED : Formatting.WHITE;
            source.sendFeedback(() -> Text.literal("  #" + rank + "  ").formatted(rankColor, Formatting.BOLD)
                .append(Text.literal(name).formatted(Formatting.WHITE))
                .append(Text.literal("  —  " + String.format("%,d", xp) + " total XP").formatted(Formatting.DARK_GRAY)), false);
        }
        return 1;
    }

    private static int doBackup(net.minecraft.server.command.ServerCommandSource src, ServerPlayerEntity target) {
        try {
            java.nio.file.Path file = PlayerDataFailsafe.backup(target.getUuid(), src.getServer());
            src.sendFeedback(() -> Text.literal("Backed up " + target.getName().getString()
                + " → " + file.getFileName()).formatted(Formatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            src.sendError(Text.literal("Backup failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int doRestore(net.minecraft.server.command.ServerCommandSource src, ServerPlayerEntity target) {
        try {
            var result = PlayerDataFailsafe.restore(target.getUuid(), src.getServer());
            if (result.isEmpty()) {
                src.sendError(Text.literal("No backup found for " + target.getName().getString()));
                return 0;
            }
            StatManager.applyStats(target);
            java.nio.file.Path file = result.get();
            src.sendFeedback(() -> Text.literal("Restored " + target.getName().getString()
                + " from " + file.getFileName()).formatted(Formatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            src.sendError(Text.literal("Restore failed: " + e.getMessage()));
            return 0;
        }
    }

    private static ServerPlayerEntity resolvePlayer(net.minecraft.server.MinecraftServer server, String name) {
        return server.getPlayerManager().getPlayer(name);
    }

    private static Skill resolveSkill(String name) {
        try {
            return Skill.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
