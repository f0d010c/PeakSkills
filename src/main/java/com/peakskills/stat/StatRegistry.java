package com.peakskills.stat;

import com.peakskills.skill.Skill;

import java.util.List;

/**
 * Every skill grants meaningful stat bonuses, Skyblock-inspired.
 * Values shown are per level; multiply by 99 for max contribution.
 */
public class StatRegistry {

    public static final List<SkillStatSource> SOURCES = List.of(

        // ── Gathering ────────────────────────────────────────────────────────
        // Mining: hardened by stone → Defense + Toughness
        new SkillStatSource(Skill.MINING,      Stat.DEFENSE,              0.02),
        new SkillStatSource(Skill.MINING,      Stat.TOUGHNESS,            0.005),

        // Woodcutting: axe work builds muscle → Strength
        new SkillStatSource(Skill.WOODCUTTING, Stat.STRENGTH,             0.01),

        // Excavating: heavy digging → Strength + Toughness
        new SkillStatSource(Skill.EXCAVATING,  Stat.STRENGTH,             0.008),
        new SkillStatSource(Skill.EXCAVATING,  Stat.TOUGHNESS,            0.005),

        // Farming: well-fed → Health + Luck
        new SkillStatSource(Skill.FARMING,     Stat.HEALTH,               0.08),
        new SkillStatSource(Skill.FARMING,     Stat.LUCK,                 0.005),

        // Fishing: patience and nutrition → Luck + Health
        new SkillStatSource(Skill.FISHING,     Stat.LUCK,                 0.01),
        new SkillStatSource(Skill.FISHING,     Stat.HEALTH,               0.04),

        // ── Combat ───────────────────────────────────────────────────────────
        // Defense: battle-hardened → Defense + Health + Toughness + KBR
        new SkillStatSource(Skill.DEFENSE,     Stat.DEFENSE,              0.04),
        new SkillStatSource(Skill.DEFENSE,     Stat.HEALTH,               0.1),
        new SkillStatSource(Skill.DEFENSE,     Stat.TOUGHNESS,            0.01),
        new SkillStatSource(Skill.DEFENSE,     Stat.KNOCKBACK_RESISTANCE, 0.002),

        // Slaying: combat experience → Strength + Health
        new SkillStatSource(Skill.SLAYING,     Stat.STRENGTH,             0.02),
        new SkillStatSource(Skill.SLAYING,     Stat.HEALTH,               0.05),

        // Ranged: keen eye, light feet → Strength + Swiftness
        new SkillStatSource(Skill.RANGED,      Stat.STRENGTH,             0.012),
        new SkillStatSource(Skill.RANGED,      Stat.SWIFTNESS,            0.0002),

        // ── Mastery ───────────────────────────────────────────────────────────
        // Enchanting: arcane knowledge → Luck + Toughness
        new SkillStatSource(Skill.ENCHANTING,  Stat.LUCK,                 0.015),
        new SkillStatSource(Skill.ENCHANTING,  Stat.TOUGHNESS,            0.005),

        // Alchemy: potions improve constitution → Health
        new SkillStatSource(Skill.ALCHEMY,     Stat.HEALTH,               0.12),

        // Smithing: forging skill → Toughness + Defense
        new SkillStatSource(Skill.SMITHING,    Stat.TOUGHNESS,            0.01),
        new SkillStatSource(Skill.SMITHING,    Stat.DEFENSE,              0.02),

        // Cooking: best food = best health → Health (largest single source)
        new SkillStatSource(Skill.COOKING,     Stat.HEALTH,               0.15),

        // Crafting: resourceful → Luck + Strength
        new SkillStatSource(Skill.CRAFTING,    Stat.LUCK,                 0.01),
        new SkillStatSource(Skill.CRAFTING,    Stat.STRENGTH,             0.005),

        // Agility: nimble and grounded → Swiftness + Knockback Resist
        new SkillStatSource(Skill.AGILITY,     Stat.SWIFTNESS,            0.0004),
        new SkillStatSource(Skill.AGILITY,     Stat.KNOCKBACK_RESISTANCE, 0.001),

        // Taming: bond with pets → Luck + Health
        new SkillStatSource(Skill.TAMING,      Stat.LUCK,                 0.02),
        new SkillStatSource(Skill.TAMING,      Stat.HEALTH,               0.04),

        // Trading: shrewd merchant → Luck + Swiftness
        new SkillStatSource(Skill.TRADING,     Stat.LUCK,                 0.015),
        new SkillStatSource(Skill.TRADING,     Stat.SWIFTNESS,            0.0001)
    );
}
