package com.peakskills.collection;

import java.util.List;

/**
 * A single tier within a collection.
 *
 * @param tier      1-indexed tier number (1–9)
 * @param threshold Number of items required to reach this tier
 * @param rewards   Rewards granted on first unlock
 */
public record CollectionTier(int tier, long threshold, List<CollectionReward> rewards) {

    /** Roman numeral label for display (I–IX). */
    public String tierLabel() {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            default -> String.valueOf(tier);
        };
    }
}
