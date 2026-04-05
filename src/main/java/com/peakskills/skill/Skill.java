package com.peakskills.skill;

public enum Skill {
    MINING,
    WOODCUTTING,
    EXCAVATING,
    FARMING,
    FISHING,
    DEFENSE,
    SLAYING,
    RANGED,
    ENCHANTING,
    ALCHEMY,
    SMITHING,
    COOKING,
    CRAFTING,
    AGILITY,
    TAMING,
    TRADING;

    public static final int MAX_LEVEL = 99;

    public String getId() {
        return name().toLowerCase();
    }

    public String getDisplayName() {
        String n = name();
        return n.charAt(0) + n.substring(1).toLowerCase();
    }

    /** One-line description of what this skill does. */
    public String getDescription() {
        return switch (this) {
            case MINING      -> "Grants Defense and Toughness. Double ore drop chance at milestones.";
            case WOODCUTTING -> "Grants Strength. Double log drop chance at milestones.";
            case EXCAVATING  -> "Grants Strength and Toughness. Double drop chance at milestones.";
            case FARMING     -> "Grants Health and Luck. Bonus crop yield at milestones.";
            case FISHING     -> "Grants Luck and Health. Bonus fish at milestones.";
            case DEFENSE     -> "Grants Defense, Health, Toughness, and Knockback Resistance.";
            case SLAYING     -> "Grants Strength and Health. Bonus mob XP at milestones.";
            case RANGED      -> "Grants Strength and Swiftness. Bonus mob XP at milestones.";
            case ENCHANTING  -> "Grants Luck and Toughness.";
            case ALCHEMY     -> "Grants Health.";
            case SMITHING    -> "Grants Toughness and Defense.";
            case COOKING     -> "Grants Health — the largest single source.";
            case CRAFTING    -> "Grants Luck and Strength.";
            case AGILITY     -> "Grants Swiftness and Knockback Resistance.";
            case TAMING      -> "Grants Luck and Health. Powers up your pet's XP gain.";
            case TRADING     -> "Grants Luck and Swiftness.";
        };
    }

    /** Short tip on how to train this skill. */
    public String getTrainingTip() {
        return switch (this) {
            case MINING      -> "Mine stone, ores, and deepslate.";
            case WOODCUTTING -> "Chop any log or leaf block.";
            case EXCAVATING  -> "Dig dirt, sand, gravel, clay, or soul sand.";
            case FARMING     -> "Harvest fully grown crops.";
            case FISHING     -> "Catch fish with a fishing rod.";
            case DEFENSE     -> "Wear armor and take hits from mobs.";
            case SLAYING     -> "Kill mobs with swords or axes.";
            case RANGED      -> "Kill mobs with bows, crossbows, or tridents.";
            case ENCHANTING  -> "Enchant items at an enchanting table.";
            case ALCHEMY     -> "Complete a brew in a brewing stand.";
            case SMITHING    -> "Use the anvil or smithing table.";
            case COOKING     -> "Cook raw food in a furnace or smoker.";
            case CRAFTING    -> "Craft items at a crafting table.";
            case AGILITY     -> "Sprint or swim continuously.";
            case TAMING      -> "Keep any pet active while playing.";
            case TRADING     -> "Complete trades with villagers.";
        };
    }
}
