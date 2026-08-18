package com.peakskills.fishing;

import java.util.Set;

/** Bounded optional modifiers supplied by PeakGear metadata and carried accessories. */
public record FishingModifiers(
    String rodId,
    int doubleHook,
    int treasureHunter,
    int relicSeeker,
    int scholar,
    int deepwater,
    int stormcaller,
    int biomeSpecialist,
    int moodReader,
    int journalist,
    boolean relicTalisman,
    boolean deepwaterCharm,
    boolean fishermanSatchel,
    String bait,
    Set<String> discoveries
) {
    public static final FishingModifiers NONE = new FishingModifiers("", 0, 0, 0, 0, 0, 0, 0, 0, 0,
        false, false, false, "", Set.of());

    public FishingModifiers {
        rodId = rodId == null ? "" : rodId;
        doubleHook = level(doubleHook);
        treasureHunter = level(treasureHunter);
        relicSeeker = level(relicSeeker);
        scholar = level(scholar);
        deepwater = level(deepwater);
        stormcaller = level(stormcaller);
        biomeSpecialist = level(biomeSpecialist);
        moodReader = level(moodReader);
        journalist = level(journalist);
        bait = bait == null ? "" : bait;
        discoveries = discoveries == null ? Set.of() : Set.copyOf(discoveries);
    }

    public double weightMultiplier(String entryId, FishingOutcomeCategory category,
                                   FishingLootTable.Rarity rarity, FishingContext context,
                                   double baseBiomeMultiplier) {
        double value = 1.0;
        if (category == FishingOutcomeCategory.TREASURE) value *= 1.0 + treasureHunter * 0.06;
        if (category == FishingOutcomeCategory.RELIC) value *= 1.0 + relicSeeker * 0.05;
        if (context.depth().ordinal() >= FishingDepth.DEEP_WATER.ordinal()
                && rarity.ordinal() >= FishingLootTable.Rarity.RARE.ordinal()) {
            value *= 1.0 + deepwater * 0.05 + (deepwaterCharm ? 0.15 : 0.0);
        }
        if (context.raining()) value *= 1.0 + stormcaller * 0.06;
        if (baseBiomeMultiplier != 1.0) value *= 1.0 + biomeSpecialist * 0.05;
        if (context.mood() != FishingMood.CALM_WATERS) value *= 1.0 + moodReader * 0.05;
        if (!discoveries.contains(entryId)) value *= 1.0 + journalist * 0.08;
        if (relicTalisman && category == FishingOutcomeCategory.RELIC) value *= 1.10;
        if (bait.equals("fish_bait") && category == FishingOutcomeCategory.FISH) value *= 1.30;
        if (bait.equals("treasure_bait") && category == FishingOutcomeCategory.TREASURE) value *= 1.25;
        if (bait.equals("relic_bait") && category == FishingOutcomeCategory.RELIC) value *= 1.25;
        if (bait.equals("frenzy_bait") && context.mood() == FishingMood.FEEDING_FRENZY) value *= 1.25;
        if (rodId.equals("riverweave_rod") && context.biome().contains("river")
                && category == FishingOutcomeCategory.FISH) value *= 1.10;
        if (rodId.equals("tideglass_rod") && category == FishingOutcomeCategory.TREASURE) value *= 1.15;
        if (rodId.equals("stormwake_rod") && context.raining()) value *= 1.20;
        if (rodId.equals("relic_seekers_rod") && category == FishingOutcomeCategory.RELIC) value *= 1.20;
        if (rodId.equals("abyssal_rod") && context.depth().ordinal() >= FishingDepth.ABYSSAL.ordinal()
                && rarity.ordinal() >= FishingLootTable.Rarity.EPIC.ordinal()) value *= 1.25;
        if (rodId.equals("rod_of_the_first_tide")
                && (category == FishingOutcomeCategory.TREASURE || category == FishingOutcomeCategory.RELIC)) value *= 1.10;
        return Math.min(8.0, value);
    }

    public double xpMultiplier() { return (1.0 + scholar * 0.04) * (rodId.equals("rod_of_the_first_tide") ? 1.10 : 1.0); }
    public double doubleHookChance() {
        if (doubleHook == 0) return 0.0;
        int[] percent = {0, 4, 7, 10, 13, 16, 19, 22, 25, 28, 32};
        return percent[doubleHook] / 100.0;
    }
    public double satchelChance() { return fishermanSatchel ? 0.05 : 0.0; }

    private static int level(int value) { return Math.max(0, Math.min(10, value)); }
}
