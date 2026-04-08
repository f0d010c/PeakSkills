package com.peakskills.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which player killed which mob so combat collection credit
 * goes to the killer, not whoever picks up the drops.
 *
 * Entry lifetime: 200 ticks (~10s) — long enough to collect loot,
 * short enough that we don't accumulate stale entries forever.
 */
public class CombatDropTracker {

    private static final int EXPIRY_TICKS = 200;

    private record Entry(UUID killerUuid, long expireAt) {}

    /** mobUUID → killer entry */
    private static final Map<UUID, Entry> KILLS = new ConcurrentHashMap<>();

    /** Current server tick — updated each tick from PeakSkills tick event. */
    private static long currentTick = 0;

    public static void tick() {
        currentTick++;
        if (currentTick % 40 == 0) {
            KILLS.entrySet().removeIf(e -> e.getValue().expireAt() <= currentTick);
        }
    }

    /** Called when a player kills a mob. */
    public static void recordKill(UUID mobUuid, UUID killerUuid) {
        KILLS.put(mobUuid, new Entry(killerUuid, currentTick + EXPIRY_TICKS));
    }

    /**
     * Returns the killer UUID for a mob drop, if the mob was killed by a player recently.
     * @param throwerUuid the UUID stored on the ItemEntity (the mob that dropped it)
     */
    public static UUID getKiller(UUID throwerUuid) {
        if (throwerUuid == null) return null;
        Entry entry = KILLS.get(throwerUuid);
        if (entry == null || entry.expireAt() <= currentTick) return null;
        return entry.killerUuid();
    }
}
