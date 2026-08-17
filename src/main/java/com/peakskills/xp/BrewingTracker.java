package com.peakskills.xp;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;

/**
 * Tracks brewing stand positions that just completed a brew.
 * SkillEvents polls this each tick to find nearby players and award Alchemy XP.
 */
public class BrewingTracker {

    private static final Set<BlockPos> brewed = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void markBrewed(BlockPos pos) {
        brewed.add(pos.immutable());
    }

    public static Set<BlockPos> drainBrewed() {
        Set<BlockPos> snapshot = Set.copyOf(brewed);
        brewed.clear();
        return snapshot;
    }
}
