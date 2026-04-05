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

import java.util.ArrayList;
import java.util.List;
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

    /** Command tag applied to every display entity so we can clean up orphans on startup. */
    private static final String TAG = "peakskills_pet_display";

    /** Lateral distance from the player's center. */
    private static final double SIDE_DIST = 0.8;
    /** Height above the player's feet. */
    private static final double HEIGHT    = 0.5;

    public static void register() {

        // Remove any orphaned display entities left over from a previous session
        // (e.g. after a crash where SERVER_STOPPING never fired properly).
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                List<Entity> orphans = new ArrayList<>();
                for (Entity e : world.iterateEntities()) {
                    if (e.getCommandTags().contains(TAG)) orphans.add(e);
                }
                orphans.forEach(e -> e.remove(Entity.RemovalReason.DISCARDED));
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            restoreDisplay(handler.player)
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            killDisplay(handler.player.getUuid(), server)
        );

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            List<UUID> toKill    = new ArrayList<>();
            List<ServerPlayerEntity> toRestore = new ArrayList<>();

            for (UUID playerUuid : displays.keySet()) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
                if (player == null) { toKill.add(playerUuid); continue; }
                if (!(player.getEntityWorld() instanceof ServerWorld sw)) continue;

                UUID displayId = displays.get(playerUuid);
                if (displayId == null) continue;
                Entity entity = sw.getEntity(displayId);

                if (entity == null || entity.isRemoved()) {
                    displays.remove(playerUuid);
                    toRestore.add(player);
                    continue;
                }

                float yaw     = player.getYaw();
                double rightX = Math.cos(Math.toRadians(yaw));
                double rightZ = Math.sin(Math.toRadians(yaw));
                entity.setPos(
                    player.getX() + rightX * SIDE_DIST,
                    player.getY() + HEIGHT,
                    player.getZ() + rightZ * SIDE_DIST
                );
            }

            for (UUID uuid : toKill)               killDisplay(uuid, server);
            for (ServerPlayerEntity p : toRestore) restoreDisplay(p);
        });

        // Kill all display entities before the world is saved on shutdown,
        // so they are not persisted and do not reappear as frozen duplicates.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            new ArrayList<>(displays.keySet()).forEach(uuid -> killDisplay(uuid, server));
        });
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
        display.setTeleportDuration(3);
        display.setInvulnerable(true);
        display.setNoGravity(true);
        display.setSilent(true);
        display.addCommandTag(TAG); // mark for orphan cleanup on next startup

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
