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

    /** mobUUID → killer entry (used internally to tag item drops) */
    private static final Map<UUID, Entry> KILLS = new ConcurrentHashMap<>();

    /**
     * itemEntityUUID → killer entry.
     * Populated in AFTER_DEATH by scanning item entities near the corpse.
     * ItemPickupMixin looks up items here — avoids relying on ItemEntity.getOwner()
     * which is never set for mob loot drops.
     */
    private static final Map<UUID, Entry> ITEM_DROPS = new ConcurrentHashMap<>();

    /** Current server tick — updated each tick from PeakSkills tick event. */
    private static long currentTick = 0;

    public static void tick() {
        currentTick++;
        if (currentTick % 40 == 0) {
            KILLS.entrySet().removeIf(e -> e.getValue().expireAt() <= currentTick);
            ITEM_DROPS.entrySet().removeIf(e -> e.getValue().expireAt() <= currentTick);
        }
    }

    /** Called when a player kills a mob. */
    public static void recordKill(UUID mobUuid, UUID killerUuid) {
        KILLS.put(mobUuid, new Entry(killerUuid, currentTick + EXPIRY_TICKS));
    }

    /** Tags an item entity as belonging to a specific killer's combat drops. */
    public static void tagItemEntity(UUID itemEntityUuid, UUID killerUuid) {
        ITEM_DROPS.put(itemEntityUuid, new Entry(killerUuid, currentTick + EXPIRY_TICKS));
    }

    /**
     * Returns the killer UUID credited for picking up this item entity.
     * Returns null if the item wasn't tagged (i.e. not a combat drop from a recent kill).
     */
    public static UUID getKillerForItem(UUID itemEntityUuid) {
        if (itemEntityUuid == null) return null;
        Entry entry = ITEM_DROPS.get(itemEntityUuid);
        if (entry == null || entry.expireAt() <= currentTick) return null;
        return entry.killerUuid();
    }
}
