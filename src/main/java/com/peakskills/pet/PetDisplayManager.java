package com.peakskills.pet;

import com.peakskills.player.PlayerDataManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages a small floating ItemDisplayEntity for each player's active pet.
 * The entity stays at a fixed position on the player's right side and follows them.
 */
public class PetDisplayManager {

    private static final Map<UUID, UUID> displays = new ConcurrentHashMap<>();

    /** Lateral distance from the player's center. */
    private static final double SIDE_DIST = 0.8;
    /** Height above the player's feet. */
    private static final double HEIGHT    = 0.5;

    public static void register() {

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            restoreDisplay(handler.player)
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            killDisplay(handler.player.getUuid(), server)
        );

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (UUID playerUuid : displays.keySet()) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
                if (player == null) { killDisplay(playerUuid, server); continue; }
                if (!(player.getEntityWorld() instanceof ServerWorld sw)) continue;

                UUID displayId = displays.get(playerUuid);
                Entity entity  = sw.getEntity(displayId);

                if (entity == null || entity.isRemoved()) {
                    // Player changed dimension — remove old, re-spawn in new world
                    for (ServerWorld w : server.getWorlds()) {
                        Entity old = w.getEntity(displayId);
                        if (old != null) { old.remove(Entity.RemovalReason.DISCARDED); break; }
                    }
                    restoreDisplay(player);
                    continue;
                }

                // Keep the entity on the player's right-hand side (world-space)
                float yaw    = player.getYaw();
                double rightX = Math.cos(Math.toRadians(yaw));
                double rightZ = Math.sin(Math.toRadians(yaw));
                entity.setPos(
                    player.getX() + rightX * SIDE_DIST,
                    player.getY() + HEIGHT,
                    player.getZ() + rightZ * SIDE_DIST
                );
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> displays.clear());
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static void spawnDisplay(ServerPlayerEntity player, PetType petType) {
        if (!(player.getEntityWorld() instanceof ServerWorld sw)) return;

        killDisplay(player.getUuid(), sw.getServer());

        DisplayEntity.ItemDisplayEntity display =
            new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, sw);

        float yaw    = player.getYaw();
        double rightX = Math.cos(Math.toRadians(yaw));
        double rightZ = Math.sin(Math.toRadians(yaw));
        display.setPos(
            player.getX() + rightX * SIDE_DIST,
            player.getY() + HEIGHT,
            player.getZ() + rightZ * SIDE_DIST
        );

        display.setItemStack(new ItemStack(petType.spawnEgg));
        display.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
        display.setInvulnerable(true);
        display.setNoGravity(true);
        display.setSilent(true);

        sw.spawnEntity(display);
        displays.put(player.getUuid(), display.getUuid());
    }

    public static void killDisplay(UUID playerUuid, MinecraftServer server) {
        UUID displayId = displays.remove(playerUuid);
        if (displayId == null || server == null) return;
        for (ServerWorld w : server.getWorlds()) {
            Entity e = w.getEntity(displayId);
            if (e != null) { e.remove(Entity.RemovalReason.DISCARDED); break; }
        }
    }

    public static void restoreDisplay(ServerPlayerEntity player) {
        Optional<PetInstance> active =
            PlayerDataManager.get(player.getUuid()).getPetRoster().getActivePet();
        active.ifPresent(pet -> spawnDisplay(player, pet.getType()));
    }
}
