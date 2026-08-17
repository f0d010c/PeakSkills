package com.peakskills.fishing;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Bounded permanent discovery record. Item collections remain separate quantity counters. */
public final class FishingJournal {
    public static final int MAX_DISCOVERIES = 256;
    private long totalCatches;
    private long totalItems;
    private final Map<FishingOutcomeCategory, Long> categoryCatches =
        new EnumMap<>(FishingOutcomeCategory.class);
    private final Set<FishingDepth> depths = EnumSet.noneOf(FishingDepth.class);
    private final Set<FishingMood> moods = EnumSet.noneOf(FishingMood.class);
    private final Set<String> lootDiscoveries = new HashSet<>();
    private final Set<String> biomes = new HashSet<>();

    public void record(FishingContext context, FishingLootTable.RollResult result) {
        totalCatches = saturatingAdd(totalCatches, 1);
        totalItems = saturatingAdd(totalItems, result.stack().getCount());
        categoryCatches.merge(result.category(), 1L, FishingJournal::saturatingAdd);
        depths.add(context.depth());
        moods.add(context.mood());
        if (lootDiscoveries.size() < MAX_DISCOVERIES) lootDiscoveries.add(result.entryId());
        if (biomes.size() < MAX_DISCOVERIES) biomes.add(context.biome());
    }

    public long getTotalCatches() { return totalCatches; }
    public long getTotalItems() { return totalItems; }
    public long getCategoryCatches(FishingOutcomeCategory category) {
        return categoryCatches.getOrDefault(category, 0L);
    }
    public Set<FishingDepth> getDepths() { return Collections.unmodifiableSet(depths); }
    public Set<FishingMood> getMoods() { return Collections.unmodifiableSet(moods); }
    public Set<String> getLootDiscoveries() { return Collections.unmodifiableSet(lootDiscoveries); }
    public Set<String> getBiomes() { return Collections.unmodifiableSet(biomes); }
    public boolean hasDiscovered(String id) { return lootDiscoveries.contains(id); }

    public void loadTotals(long catches, long items) {
        totalCatches = Math.max(0, catches);
        totalItems = Math.max(0, items);
    }
    public void loadCategory(FishingOutcomeCategory category, long count) {
        if (count > 0) categoryCatches.put(category, count);
    }
    public void loadDepth(FishingDepth depth) { depths.add(depth); }
    public void loadMood(FishingMood mood) { moods.add(mood); }
    public void loadLoot(String id) {
        if (id != null && id.matches("[a-z0-9_]{1,64}") && lootDiscoveries.size() < MAX_DISCOVERIES) {
            lootDiscoveries.add(id);
        }
    }
    public void loadBiome(String biome) {
        if (biome != null && biome.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
            && biome.length() <= 128 && biomes.size() < MAX_DISCOVERIES) {
            biomes.add(biome);
        }
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return Math.max(0, left + right);
    }
}
