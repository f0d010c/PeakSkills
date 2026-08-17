package com.peakskills.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.peakskills.gui.PetMenuGui;
import com.peakskills.pet.PetInstance;
import com.peakskills.pet.PetRarity;
import com.peakskills.pet.PetRoster;
import com.peakskills.pet.PetType;
import com.peakskills.pet.PetUpgradeHandler;
import com.peakskills.player.PlayerDataManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import java.util.UUID;

public class PetsCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                Commands.literal("pets")

                    // /pets — open pet menu
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        PetMenuGui.open(player);
                        return 1;
                    })

                    // /pets addxp <amount> — add XP to your active pet (admin only)
                    .then(Commands.literal("addxp")
                        .requires(PetsCommand::isOp)
                        .then(Commands.argument("amount", LongArgumentType.longArg(1, 10_000_000L))
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                long amount = LongArgumentType.getLong(ctx, "amount");
                                var roster = PlayerDataManager.get(player.getUUID()).getPetRoster();
                                var pet = roster.getActivePet();
                                if (pet.isEmpty()) {
                                    ctx.getSource().sendFailure(Component.literal("You have no active pet. Activate one first with /pets."));
                                    return 0;
                                }
                                roster.feedXp(pet.get().getType().affinity, amount);
                                ctx.getSource().sendSuccess(() ->
                                    Component.literal("Added " + String.format("%,d", amount) + " XP to your ")
                                        .withStyle(ChatFormatting.GREEN)
                                        .append(Component.literal(pet.get().getRarity().displayName + " " + pet.get().getType().displayName)
                                            .withStyle(pet.get().getRarity().color))
                                        .append(Component.literal(".").withStyle(ChatFormatting.GREEN)),
                                    false);
                                return 1;
                            })
                        )
                    )

                    // /pets give <type> [rarity] — add a pet to your roster (admin only)
                    .then(Commands.literal("give")
                        .requires(PetsCommand::isOp)
                        .then(Commands.argument("type", StringArgumentType.word())
                            .executes(ctx -> givePet(ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "type"), "COMMON"))
                            .then(Commands.argument("rarity", StringArgumentType.word())
                                .executes(ctx -> givePet(ctx.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(ctx, "type"),
                                    StringArgumentType.getString(ctx, "rarity")))
                            )
                        )
                    )

                    // /pets activate <petId> — set active pet
                    .then(Commands.literal("activate")
                        .then(Commands.argument("petId", StringArgumentType.word())
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                String idStr = StringArgumentType.getString(ctx, "petId");
                                try {
                                    UUID id = UUID.fromString(idStr);
                                    PetRoster roster = PlayerDataManager.get(player.getUUID()).getPetRoster();
                                    if (roster.findById(id).isEmpty()) {
                                        ctx.getSource().sendFailure(Component.literal("Pet not found."));
                                        return 0;
                                    }
                                    roster.setActivePet(id);
                                    String name = roster.findById(id).get().getType().displayName;
                                    player.sendSystemMessage(Component.literal("Active pet set to: " + name)
                                        .withStyle(ChatFormatting.GREEN), false);
                                } catch (IllegalArgumentException e) {
                                    ctx.getSource().sendFailure(Component.literal("Invalid pet ID."));
                                    return 0;
                                }
                                return 1;
                            })
                        )
                    )

                    // /pets deactivate — remove active pet
                    .then(Commands.literal("deactivate")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PlayerDataManager.get(player.getUUID()).getPetRoster().deactivate();
                            player.sendSystemMessage(Component.literal("Pet deactivated.").withStyle(ChatFormatting.YELLOW), false);
                            return 1;
                        })
                    )

                    // /pets upgrade <petId> — upgrade pet rarity
                    .then(Commands.literal("upgrade")
                        .then(Commands.argument("petId", StringArgumentType.word())
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                String idStr = StringArgumentType.getString(ctx, "petId");
                                try {
                                    UUID id = UUID.fromString(idStr);
                                    PetUpgradeHandler.tryUpgrade(player, id);
                                } catch (IllegalArgumentException e) {
                                    ctx.getSource().sendFailure(Component.literal("Invalid pet ID."));
                                    return 0;
                                }
                                return 1;
                            })
                        )
                    )
            )
        );
    }

    private static boolean isOp(CommandSourceStack src) {
        ServerPlayer player = src.getPlayer();
        if (player == null) return !src.isPlayer();
        NameAndId entry = new NameAndId(player.getGameProfile());
        return src.getServer().getPlayerList().getOps().get(entry) != null;
    }

    private static int givePet(ServerPlayer player, String typeName, String rarityName) {
        PetType type;
        PetRarity rarity;
        try {
            type = PetType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.literal("Unknown pet type: " + typeName
                + ". Valid: " + java.util.Arrays.stream(PetType.values())
                    .map(t -> t.name().toLowerCase()).collect(java.util.stream.Collectors.joining(", ")))
                .withStyle(ChatFormatting.RED), false);
            return 0;
        }
        try {
            rarity = PetRarity.valueOf(rarityName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.literal("Unknown rarity: " + rarityName
                + ". Valid: COMMON, UNCOMMON, RARE, EPIC, LEGENDARY")
                .withStyle(ChatFormatting.RED), false);
            return 0;
        }

        PetRoster roster = PlayerDataManager.get(player.getUUID()).getPetRoster();
        if (roster.getPets().size() >= PetRoster.MAX_SLOTS) {
            player.sendSystemMessage(Component.literal("Your pet roster is full (20 slots).")
                .withStyle(ChatFormatting.RED), false);
            return 0;
        }

        PetInstance pet = new PetInstance(UUID.randomUUID(), type, rarity, 0L);
        roster.addPet(pet);
        player.sendSystemMessage(
            Component.literal("Added ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(rarity.displayName + " " + type.displayName)
                    .withStyle(rarity.color))
                .append(Component.literal(" to your roster!").withStyle(ChatFormatting.GREEN)),
            false);
        return 1;
    }
}
