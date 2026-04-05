package com.peakskills.collection;

import com.peakskills.stat.Stat;

import java.util.*;

/**
 * Per-player collection counters and unlocked tiers.
 * Stored inside PlayerData and persisted via PlayerDataManager.
 */
public class CollectionData {

    private final Map<CollectionType, Long>    counts        = new EnumMap<>(CollectionType.class);
    private final Map<CollectionType, Integer> unlockedTiers = new EnumMap<>(CollectionType.class);

    // ── Public API ────────────────────────────────────────────────────────────

    public long getCount(CollectionType type) {
        return counts.getOrDefault(type, 0L);
    }

    public int getUnlockedTier(CollectionType type) {
        return unlockedTiers.getOrDefault(type, 0);
    }

    /**
     * Increments a collection counter by {@code amount}.
     * Returns the list of tiers that were newly unlocked (empty if none).
     */
    public List<CollectionTier> increment(CollectionType type, long amount) {
        long newCount = counts.merge(type, amount, Long::sum);
        int currentTier = getUnlockedTier(type);
        List<CollectionTier> tiers = CollectionRegistry.getTiers(type);
        List<CollectionTier> unlocked = new ArrayList<>();

        for (int i = currentTier; i < tiers.size(); i++) {
            if (newCount >= tiers.get(i).threshold()) {
                unlocked.add(tiers.get(i));
                unlockedTiers.put(type, i + 1);
            } else {
                break;
            }
        }
        return unlocked;
    }

    /**
     * Sums all raw attribute bonuses from every unlocked collection tier.
     * Used by StatManager to include collection contributions.
     */
    public Map<Stat, Double> computeStatBonuses() {
        Map<Stat, Double> totals = new EnumMap<>(Stat.class);
        for (CollectionType type : CollectionType.values()) {
            int tier = getUnlockedTier(type);
            List<CollectionTier> tiers = CollectionRegistry.getTiers(type);
            for (int i = 0; i < tier; i++) {
                for (CollectionReward reward : tiers.get(i).rewards()) {
                    if (reward instanceof CollectionReward.StatBonus sb) {
                        totals.merge(sb.stat(), sb.rawValue(), Double::sum);
                    }
                }
            }
        }
        return totals;
    }

    // ── Serialization hooks (used by PlayerDataManager) ───────────────────────

    public Map<CollectionType, Long>    getCounts()        { return counts; }
    public Map<CollectionType, Integer> getUnlockedTiers() { return unlockedTiers; }
}
