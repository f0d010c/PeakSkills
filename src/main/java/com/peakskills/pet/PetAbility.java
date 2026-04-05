package com.peakskills.pet;

import com.peakskills.skill.Skill;
import com.peakskills.stat.Stat;

/**
 * A single passive ability on a pet.
 * Either adds a flat stat bonus or multiplies XP gain for a skill.
 */
public class PetAbility {

    public enum Type { STAT_BONUS, XP_BONUS }

    public final Type  type;
    public final Stat  stat;          // non-null when STAT_BONUS
    public final Skill skill;         // non-null when XP_BONUS
    public final double basePerLevel; // value per level at COMMON rarity

    /** STAT_BONUS constructor */
    public PetAbility(Stat stat, double basePerLevel) {
        this.type = Type.STAT_BONUS;
        this.stat = stat;
        this.skill = null;
        this.basePerLevel = basePerLevel;
    }

    /** XP_BONUS constructor */
    public PetAbility(Skill skill, double basePerLevel) {
        this.type = Type.XP_BONUS;
        this.stat = null;
        this.skill = skill;
        this.basePerLevel = basePerLevel;
    }

    /** Computed value at the given level and rarity. */
    public double compute(int level, PetRarity rarity) {
        return basePerLevel * level * rarityMultiplier(rarity);
    }

    public static double rarityMultiplier(PetRarity rarity) {
        return switch (rarity) {
            case COMMON    -> 1.0;
            case UNCOMMON  -> 1.3;
            case RARE      -> 1.7;
            case EPIC      -> 2.2;
            case LEGENDARY -> 3.0;
        };
    }

    public String label() {
        if (type == Type.STAT_BONUS) {
            return "+" + "{v} " + statName(stat);
        } else {
            return "+" + "{v}% " + skill.getDisplayName() + " XP";
        }
    }

    /** Format the ability value nicely for display. */
    public String formatValue(int level, PetRarity rarity) {
        double v = compute(level, rarity);
        if (type == Type.XP_BONUS) {
            return String.format("%.1f%%", v * 100);
        }
        return v < 1
            ? String.format("%.3f", v).replaceAll("0+$","").replaceAll("\\.$","")
            : String.format("%.2f", v).replaceAll("0+$","").replaceAll("\\.$","");
    }

    public String displayLine(int level, PetRarity rarity) {
        String v = formatValue(level, rarity);
        if (type == Type.XP_BONUS) {
            return "+" + v + " " + skill.getDisplayName() + " XP";
        } else {
            return "+" + v + " " + statName(stat);
        }
    }

    private static String statName(Stat stat) {
        return switch (stat) {
            case STRENGTH             -> "Strength";
            case DEFENSE              -> "Defense";
            case TOUGHNESS            -> "Toughness";
            case HEALTH               -> "Health";
            case SWIFTNESS            -> "Speed";
            case KNOCKBACK_RESISTANCE -> "Knockback Res";
            case LUCK                 -> "Luck";
        };
    }
}
