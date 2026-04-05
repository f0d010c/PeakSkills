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
 * Spawns and manages floating ItemDisplayEntity pets that orbit their owner.
 * One display entity per player — tracks across dimension changes.
 */
public class PetDisplayManager {

    /** Maps player UUID → active display entity UUID. */
    private static final Map<UUID, UUID> displays = new ConcurrentHashMap<>();

    /** Orbital radius in blocks. */
    private static final double RADIUS = 1.2;
    /** Ticks per full orbit (200 ticks = 10 seconds). */
    private static final double TICKS_PER_ORBIT = 200.0;
    /** Height above player's feet. */
    private static final double HEIGHT = 0.9;

    public static void register() {

        // Restore display after player data is loaded (JOIN fires after PlayerDataManager.JOIN)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            restoreDisplay(handler.player)
        );

        // Remove display when player disconnects
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            killDisplay(handler.player.getUuid(), server)
        );

        // Each tick: orbit the display around the player
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long tick = server.getTicks();
            for (UUID playerUuid : displays.keySet()) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
                if (player == null) {
                    killDisplay(playerUuid, server);
                    continue;
                }
                if (!(player.getEntityWorld() instanceof ServerWorld sw)) continue;

                UUID displayId = displays.get(playerUuid);
                Entity entity = sw.getEntity(displayId);

                if (entity == null || entity.isRemoved()) {
                    // Player probably changed dimension — find the old entity, kill it, re-spawn
                    for (ServerWorld w : server.getWorlds()) {
                        Entity old = w.getEntity(displayId);
                        if (old != null) { old.remove(Entity.RemovalReason.DISCARDED); break; }
                    }
                    restoreDisplay(player);
                    continue;
                }

                // Smooth orbit
                double angle = (tick * Math.PI * 2.0) / TICKS_PER_ORBIT;
                entity.setPos(
                    player.getX() + Math.cos(angle) * RADIUS,
                    player.getY() + HEIGHT,
                    player.getZ() + Math.sin(angle) * RADIUS
                );
            }
        });

        // Clear the tracking map on server stop (entities are cleaned up with the world)
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> displays.clear());
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Spawns (or replaces) the floating display for the given player's pet.
     * Safe to call when there is already an existing display.
     */
    public static void spawnDisplay(ServerPlayerEntity player, PetType petType) {
        if (!(player.getEntityWorld() instanceof ServerWorld sw)) return;

        // Kill any existing display first
        killDisplay(player.getUuid(), sw.getServer());

        DisplayEntity.ItemDisplayEntity display =
            new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, sw);

        display.setPos(player.getX() + RADIUS, player.getY() + HEIGHT, player.getZ());
        display.setItemStack(new ItemStack(petType.icon));
        display.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
        display.setGlowing(true);
        display.setInvulnerable(true);
        display.setNoGravity(true);
        display.setSilent(true);

        sw.spawnEntity(display);
        displays.put(player.getUuid(), display.getUuid());
    }

    /**
     * Kills the floating display for the given player.
     */
    public static void killDisplay(UUID playerUuid, MinecraftServer server) {
        UUID displayId = displays.remove(playerUuid);
        if (displayId == null || server == null) return;
        for (ServerWorld w : server.getWorlds()) {
            Entity e = w.getEntity(displayId);
            if (e != null) { e.remove(Entity.RemovalReason.DISCARDED); break; }
        }
    }

    /**
     * Re-spawns the display for a player based on their currently active pet.
     * Called on join and after dimension changes.
     */
    public static void restoreDisplay(ServerPlayerEntity player) {
        Optional<PetInstance> active =
            PlayerDataManager.get(player.getUuid()).getPetRoster().getActivePet();
        active.ifPresent(pet -> spawnDisplay(player, pet.getType()));
    }
}
