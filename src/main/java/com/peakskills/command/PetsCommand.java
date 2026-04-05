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
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class PetsCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                CommandManager.literal("pets")

                    // /pets — open pet menu
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                        PetMenuGui.open(player);
                        return 1;
                    })

                    // /pets addxp <amount> — add XP to your active pet
                    .then(CommandManager.literal("addxp")
                        .then(CommandManager.argument("amount", LongArgumentType.longArg(1))
                            .executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                long amount = LongArgumentType.getLong(ctx, "amount");
                                var roster = PlayerDataManager.get(player.getUuid()).getPetRoster();
                                var pet = roster.getActivePet();
                                if (pet.isEmpty()) {
                                    ctx.getSource().sendError(Text.literal("You have no active pet. Activate one first with /pets."));
                                    return 0;
                                }
                                roster.feedXp(pet.get().getType().affinity, amount);
                                ctx.getSource().sendFeedback(() ->
                                    Text.literal("Added " + String.format("%,d", amount) + " XP to your ")
                                        .formatted(Formatting.GREEN)
                                        .append(Text.literal(pet.get().getRarity().displayName + " " + pet.get().getType().displayName)
                                            .formatted(pet.get().getRarity().color))
                                        .append(Text.literal(".").formatted(Formatting.GREEN)),
                                    false);
                                return 1;
                            })
                        )
                    )

                    // /pets give <type> [rarity] — add a pet to your roster
                    .then(CommandManager.literal("give")
                        .then(CommandManager.argument("type", StringArgumentType.word())
                            .executes(ctx -> givePet(ctx.getSource().getPlayerOrThrow(),
                                StringArgumentType.getString(ctx, "type"), "COMMON"))
                            .then(CommandManager.argument("rarity", StringArgumentType.word())
                                .executes(ctx -> givePet(ctx.getSource().getPlayerOrThrow(),
                                    StringArgumentType.getString(ctx, "type"),
                                    StringArgumentType.getString(ctx, "rarity")))
                            )
                        )
                    )

                    // /pets activate <petId> — set active pet
                    .then(CommandManager.literal("activate")
                        .then(CommandManager.argument("petId", StringArgumentType.word())
                            .executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                String idStr = StringArgumentType.getString(ctx, "petId");
                                try {
                                    UUID id = UUID.fromString(idStr);
                                    PetRoster roster = PlayerDataManager.get(player.getUuid()).getPetRoster();
                                    if (roster.findById(id).isEmpty()) {
                                        ctx.getSource().sendError(Text.literal("Pet not found."));
                                        return 0;
                                    }
                                    roster.setActivePet(id);
                                    String name = roster.findById(id).get().getType().displayName;
                                    player.sendMessage(Text.literal("Active pet set to: " + name)
                                        .formatted(Formatting.GREEN), false);
                                } catch (IllegalArgumentException e) {
                                    ctx.getSource().sendError(Text.literal("Invalid pet ID."));
                                    return 0;
                                }
                                return 1;
                            })
                        )
                    )

                    // /pets deactivate — remove active pet
                    .then(CommandManager.literal("deactivate")
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            PlayerDataManager.get(player.getUuid()).getPetRoster().deactivate();
                            player.sendMessage(Text.literal("Pet deactivated.").formatted(Formatting.YELLOW), false);
                            return 1;
                        })
                    )

                    // /pets upgrade <petId> — upgrade pet rarity
                    .then(CommandManager.literal("upgrade")
                        .then(CommandManager.argument("petId", StringArgumentType.word())
                            .executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                String idStr = StringArgumentType.getString(ctx, "petId");
                                try {
                                    UUID id = UUID.fromString(idStr);
                                    PetUpgradeHandler.tryUpgrade(player, id);
                                } catch (IllegalArgumentException e) {
                                    ctx.getSource().sendError(Text.literal("Invalid pet ID."));
                                    return 0;
                                }
                                return 1;
                            })
                        )
                    )
            )
        );
    }

    private static int givePet(ServerPlayerEntity player, String typeName, String rarityName) {
        PetType type;
        PetRarity rarity;
        try {
            type = PetType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.literal("Unknown pet type: " + typeName
                + ". Valid: " + java.util.Arrays.stream(PetType.values())
                    .map(t -> t.name().toLowerCase()).collect(java.util.stream.Collectors.joining(", ")))
                .formatted(Formatting.RED), false);
            return 0;
        }
        try {
            rarity = PetRarity.valueOf(rarityName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.literal("Unknown rarity: " + rarityName
                + ". Valid: COMMON, UNCOMMON, RARE, EPIC, LEGENDARY")
                .formatted(Formatting.RED), false);
            return 0;
        }

        PetRoster roster = PlayerDataManager.get(player.getUuid()).getPetRoster();
        if (roster.getPets().size() >= PetRoster.MAX_SLOTS) {
            player.sendMessage(Text.literal("Your pet roster is full (20 slots).")
                .formatted(Formatting.RED), false);
            return 0;
        }

        PetInstance pet = new PetInstance(UUID.randomUUID(), type, rarity, 0L);
        roster.addPet(pet);
        player.sendMessage(
            Text.literal("Added ").formatted(Formatting.GREEN)
                .append(Text.literal(rarity.displayName + " " + type.displayName)
                    .formatted(rarity.color))
                .append(Text.literal(" to your roster!").formatted(Formatting.GREEN)),
            false);
        return 1;
    }
}
