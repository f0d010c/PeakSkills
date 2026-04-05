package com.peakskills.skill;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SkillAbilityRegistry {

    private static final Map<Skill, List<SkillAbility>> ABILITIES = new EnumMap<>(Skill.class);

    static {
        ABILITIES.put(Skill.MINING, List.of(
            new SkillAbility(Skill.MINING,      50, "Ore Sense",        "25% chance to earn double Mining XP."),
            new SkillAbility(Skill.MINING,      99, "Master Miner",     "50% chance to earn double Mining XP.")
        ));
        ABILITIES.put(Skill.WOODCUTTING, List.of(
            new SkillAbility(Skill.WOODCUTTING, 50, "Lumberjack",       "25% chance to earn double Woodcutting XP."),
            new SkillAbility(Skill.WOODCUTTING, 99, "Arborist",         "50% chance to earn double Woodcutting XP.")
        ));
        ABILITIES.put(Skill.EXCAVATING, List.of(
            new SkillAbility(Skill.EXCAVATING,  50, "Deep Digger",      "25% chance to earn double Excavating XP."),
            new SkillAbility(Skill.EXCAVATING,  99, "Treasure Hunter",  "50% chance to earn double Excavating XP.")
        ));
        ABILITIES.put(Skill.FARMING, List.of(
            new SkillAbility(Skill.FARMING,     50, "Green Thumb",      "25% chance to earn double Farming XP."),
            new SkillAbility(Skill.FARMING,     99, "Master Farmer",    "50% chance to earn double Farming XP.")
        ));
        ABILITIES.put(Skill.FISHING, List.of(
            new SkillAbility(Skill.FISHING,     50, "Lucky Cast",       "+25% Fishing XP."),
            new SkillAbility(Skill.FISHING,     99, "Ocean Master",     "+50% Fishing XP.")
        ));
        ABILITIES.put(Skill.SLAYING, List.of(
            new SkillAbility(Skill.SLAYING,     50, "Executioner",      "+50% XP from mob kills."),
            new SkillAbility(Skill.SLAYING,     99, "Warlord",          "+100% XP from mob kills.")
        ));
        ABILITIES.put(Skill.RANGED, List.of(
            new SkillAbility(Skill.RANGED,      50, "Hawk Eye",         "+50% XP from ranged kills."),
            new SkillAbility(Skill.RANGED,      99, "Sniper",           "+100% XP from ranged kills.")
        ));
        ABILITIES.put(Skill.DEFENSE, List.of(
            new SkillAbility(Skill.DEFENSE,     50, "Iron Will",        "Incoming damage reduced by 10%."),
            new SkillAbility(Skill.DEFENSE,     99, "Titan",            "Incoming damage reduced by 20%.")
        ));
        ABILITIES.put(Skill.ENCHANTING, List.of(
            new SkillAbility(Skill.ENCHANTING,  50, "Arcane Mind",      "+25% Enchanting XP."),
            new SkillAbility(Skill.ENCHANTING,  99, "Grand Enchanter",  "+50% Enchanting XP.")
        ));
        ABILITIES.put(Skill.ALCHEMY, List.of(
            new SkillAbility(Skill.ALCHEMY,     50, "Brewmaster",       "+25% Alchemy XP."),
            new SkillAbility(Skill.ALCHEMY,     99, "Potion Savant",    "+50% Alchemy XP.")
        ));
        ABILITIES.put(Skill.SMITHING, List.of(
            new SkillAbility(Skill.SMITHING,    50, "Forge Master",     "+25% Smithing XP."),
            new SkillAbility(Skill.SMITHING,    99, "Legendary Smith",  "+50% Smithing XP.")
        ));
        ABILITIES.put(Skill.COOKING, List.of(
            new SkillAbility(Skill.COOKING,     50, "Chef's Kiss",      "+25% Cooking XP."),
            new SkillAbility(Skill.COOKING,     99, "Master Chef",      "+50% Cooking XP.")
        ));
        ABILITIES.put(Skill.CRAFTING, List.of(
            new SkillAbility(Skill.CRAFTING,    50, "Artisan",          "+25% Crafting XP."),
            new SkillAbility(Skill.CRAFTING,    99, "Mastercraftsman",  "+50% Crafting XP.")
        ));
        ABILITIES.put(Skill.AGILITY, List.of(
            new SkillAbility(Skill.AGILITY,     50, "Parkour Pro",      "+25% Agility XP."),
            new SkillAbility(Skill.AGILITY,     99, "Wind Walker",      "+50% Agility XP.")
        ));
        ABILITIES.put(Skill.TAMING, List.of(
            new SkillAbility(Skill.TAMING,      50, "Beast Bond",       "Pet XP gain doubled."),
            new SkillAbility(Skill.TAMING,      99, "Pet Whisperer",    "Pet XP gain tripled.")
        ));
        ABILITIES.put(Skill.TRADING, List.of(
            new SkillAbility(Skill.TRADING,     50, "Haggler",          "+25% Trading XP."),
            new SkillAbility(Skill.TRADING,     99, "Trade Baron",      "+50% Trading XP.")
        ));
    }

    public static List<SkillAbility> getAbilities(Skill skill) {
        return ABILITIES.getOrDefault(skill, List.of());
    }

    /** Returns only the abilities the player has unlocked at the given level. */
    public static List<SkillAbility> getUnlocked(Skill skill, int level) {
        return getAbilities(skill).stream()
            .filter(a -> a.isUnlocked(level))
            .toList();
    }

    /**
     * Returns the XP multiplier bonus for the given skill and level.
     * Used by SkillEvents and XpManager to apply passive bonuses.
     * E.g. Mining lv50 → 25% chance to return 2× XP (handled by caller).
     * For flat bonuses (Slaying, Fishing, etc.) returns a direct multiplier.
     */
    public static double getFlatXpMultiplier(Skill skill, int level) {
        return switch (skill) {
            case FISHING, ENCHANTING, ALCHEMY, SMITHING, COOKING,
                 CRAFTING, AGILITY, TRADING ->
                    level >= 99 ? 1.50 : level >= 50 ? 1.25 : 1.0;
            case SLAYING, RANGED ->
                    level >= 99 ? 2.0  : level >= 50 ? 1.50 : 1.0;
            default -> 1.0; // Mining/Woodcutting/Excavating/Farming use chance-based doubling
        };
    }

    /** For skills with chance-based double XP, returns the chance (0–1). */
    public static double getDoubleXpChance(Skill skill, int level) {
        return switch (skill) {
            case MINING, WOODCUTTING, EXCAVATING, FARMING ->
                    level >= 99 ? 0.50 : level >= 50 ? 0.25 : 0.0;
            default -> 0.0;
        };
    }

    /** Pet XP multiplier from the Taming skill. */
    public static double getPetXpMultiplier(int tamingLevel) {
        if (tamingLevel >= 99) return 3.0;
        if (tamingLevel >= 50) return 2.0;
        return 1.0;
    }
}
