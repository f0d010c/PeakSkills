package com.peakskills.pet;

import com.peakskills.player.PlayerDataManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
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

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Clean up orphaned display entities when a player joins and their chunks load.
            // Matches entities tagged by us OR carrying our specific combination of flags
            // (covers pre-tag entities from older sessions).
            for (ServerLevel world : server.getAllLevels()) {
                List<Entity> orphans = new ArrayList<>();
                for (Entity e : world.getAllEntities()) {
                    if (!(e instanceof Display.ItemDisplay)) continue;
                    boolean isOurs = e.entityTags().contains(TAG)
                        || (e.isInvulnerable() && e.isNoGravity() && e.isSilent());
                    if (isOurs && !displays.containsValue(e.getUUID())) {
                        orphans.add(e);
                    }
                }
                orphans.forEach(e -> e.remove(Entity.RemovalReason.DISCARDED));
            }
            restoreDisplay(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            killDisplay(handler.player.getUUID(), server)
        );

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            List<UUID> toKill    = new ArrayList<>();
            List<ServerPlayer> toRestore = new ArrayList<>();

            for (UUID playerUuid : displays.keySet()) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
                if (player == null) { toKill.add(playerUuid); continue; }
                if (!(player.level() instanceof ServerLevel sw)) continue;

                UUID displayId = displays.get(playerUuid);
                if (displayId == null) continue;
                Entity entity = sw.getEntity(displayId);

                if (entity == null || entity.isRemoved()) {
                    displays.remove(playerUuid);
                    toRestore.add(player);
                    continue;
                }

                float yaw     = player.getYRot();
                double rightX = Math.cos(Math.toRadians(yaw));
                double rightZ = Math.sin(Math.toRadians(yaw));
                entity.setPosRaw(
                    player.getX() + rightX * SIDE_DIST,
                    player.getY() + HEIGHT,
                    player.getZ() + rightZ * SIDE_DIST
                );
            }

            for (UUID uuid : toKill)               killDisplay(uuid, server);
            for (ServerPlayer p : toRestore) restoreDisplay(p);
        });

        // Kill all display entities before the world is saved on shutdown,
        // so they are not persisted and do not reappear as frozen duplicates.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            new ArrayList<>(displays.keySet()).forEach(uuid -> killDisplay(uuid, server));
        });
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static void spawnDisplay(ServerPlayer player, PetType petType) {
        if (!(player.level() instanceof ServerLevel sw)) return;

        killDisplay(player.getUUID(), sw.getServer());

        Display.ItemDisplay display =
            new Display.ItemDisplay(EntityType.ITEM_DISPLAY, sw);

        float yaw    = player.getYRot();
        double rightX = Math.cos(Math.toRadians(yaw));
        double rightZ = Math.sin(Math.toRadians(yaw));
        display.setPosRaw(
            player.getX() + rightX * SIDE_DIST,
            player.getY() + HEIGHT,
            player.getZ() + rightZ * SIDE_DIST
        );

        display.setItemStack(new ItemStack(petType.spawnEgg));
        display.setBillboardConstraints(Display.BillboardConstraints.CENTER);
        display.setPosRotInterpolationDuration(3);
        display.setInvulnerable(true);
        display.setNoGravity(true);
        display.setSilent(true);
        display.addTag(TAG); // mark for orphan cleanup on next startup

        sw.addFreshEntity(display);
        displays.put(player.getUUID(), display.getUUID());
    }

    public static void killDisplay(UUID playerUuid, MinecraftServer server) {
        UUID displayId = displays.remove(playerUuid);
        if (displayId == null || server == null) return;
        for (ServerLevel w : server.getAllLevels()) {
            Entity e = w.getEntity(displayId);
            if (e != null) { e.remove(Entity.RemovalReason.DISCARDED); break; }
        }
    }

    public static void restoreDisplay(ServerPlayer player) {
        com.peakskills.player.PlayerData data = PlayerDataManager.get(player.getUUID());
        if (!data.isPetsVisible()) return;
        Optional<PetInstance> active = data.getPetRoster().getActivePet();
        active.ifPresent(pet -> spawnDisplay(player, pet.getType()));
    }
}
