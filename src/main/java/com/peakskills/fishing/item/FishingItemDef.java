package com.peakskills.fishing.item;

import net.minecraft.item.Item;
import net.minecraft.util.Formatting;

public record FishingItemDef(
    String id,
    String displayName,
    Item baseItem,
    Formatting color,
    int requiredFishingLevel,
    double fishingXpBonus,
    int effectiveLevelBonus,
    int eventContributionBonus
) {}
