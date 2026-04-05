package com.peakskills.player;

import com.google.gson.*;
import com.peakskills.PeakSkills;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Backup and restore player data to/from timestamped JSON files.
 *
 * Backups live at:  world/peakskills/backup/<uuid>_<timestamp>.json
 *
 * Used for:
 *   - Manual recovery if a player's data gets corrupted
 *   - Migrating data between mod versions
 *   - Admin-initiated snapshots before risky operations
 */
public class PlayerDataFailsafe {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /** 20 ticks/s × 60 s × 60 min = 72 000 ticks per hour */
    private static final int AUTO_BACKUP_INTERVAL = 72_000;

    // ── Auto-backup registration ──────────────────────────────────────────────

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % AUTO_BACKUP_INTERVAL != 0) return;
            // Skip tick 0 (server just started — PlayerDataManager handles initial load)
            if (server.getTicks() == 0) return;

            PeakSkills.LOGGER.info("[Failsafe] Running hourly auto-backup...");
            int count = 0;
            for (UUID uuid : PlayerDataManager.getCachedUuids()) {
                try {
                    backup(uuid, server);
                    count++;
                } catch (IOException e) {
                    PeakSkills.LOGGER.warn("[Failsafe] Auto-backup failed for {}: {}", uuid, e.getMessage());
                }
            }
            PeakSkills.LOGGER.info("[Failsafe] Auto-backup complete — {} players backed up.", count);
        });
    }

    // ── Directory ─────────────────────────────────────────────────────────────

    public static Path backupDir(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT)
            .resolve("peakskills").resolve("backup");
    }

    // ── Backup ────────────────────────────────────────────────────────────────

    /**
     * Writes a timestamped snapshot of the player's current live data.
     * Returns the path of the file that was written.
     */
    public static Path backup(UUID uuid, MinecraftServer server) throws IOException {
        Path dir = backupDir(server);
        Files.createDirectories(dir);

        PlayerData data = PlayerDataManager.get(uuid);
        JsonObject json = PlayerDataManager.toJson(data);

        String ts   = LocalDateTime.now().format(FMT);
        Path   file = dir.resolve(uuid + "_" + ts + ".json");

        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(json, w);
        }

        PeakSkills.LOGGER.info("[Failsafe] Backed up {} → {}", uuid, file.getFileName());
        return file;
    }

    // ── Restore ───────────────────────────────────────────────────────────────

    /**
     * Loads the most recent backup for this UUID and replaces the live cache entry.
     * Stats are NOT re-applied here — callers should do that if the player is online.
     *
     * @return the backup file that was restored, or empty if none found.
     */
    public static Optional<Path> restore(UUID uuid, MinecraftServer server) throws IOException {
        Path dir = backupDir(server);
        if (!Files.exists(dir)) return Optional.empty();

        Optional<Path> latest = Files.list(dir)
            .filter(p -> p.getFileName().toString().startsWith(uuid.toString() + "_"))
            .max(Comparator.comparingLong(p -> p.toFile().lastModified()));

        if (latest.isEmpty()) return Optional.empty();

        try (Reader r = Files.newBufferedReader(latest.get())) {
            JsonObject json = GSON.fromJson(r, JsonObject.class);
            PlayerData restored = PlayerDataManager.fromJson(uuid, json);
            PlayerDataManager.put(uuid, restored);
        }

        PeakSkills.LOGGER.info("[Failsafe] Restored {} from {}", uuid, latest.get().getFileName());
        return latest;
    }

    // ── List backups ──────────────────────────────────────────────────────────

    /** Returns all backup files for a UUID, newest first. */
    public static List<Path> listBackups(UUID uuid, MinecraftServer server) throws IOException {
        Path dir = backupDir(server);
        if (!Files.exists(dir)) return List.of();

        return Files.list(dir)
            .filter(p -> p.getFileName().toString().startsWith(uuid.toString() + "_"))
            .sorted(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed())
            .toList();
    }
}
