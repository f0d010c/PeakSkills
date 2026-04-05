package com.peakskills.pet;

import com.peakskills.skill.Skill;
import com.peakskills.stat.Stat;

import java.util.List;
import java.util.Map;

/**
 * Each pet grants a large XP bonus for its primary affinity skill and thematic stat bonuses.
 *
 * XP_BONUS base rate: 0.008/level
 *   COMMON  lv50  → +40% XP       COMMON  lv99 → +79% XP
 *   LEGENDARY lv50 → +120% XP     LEGENDARY lv99 → +237% XP
 *
 * This makes matching the right pet to the right skill very impactful.
 */
public class PetAbilityRegistry {

    private static final Map<PetType, List<PetAbility>> ABILITIES = Map.ofEntries(

        // ── Gathering ──────────────────────────────────────────────────────────
        Map.entry(PetType.IRON_GOLEM,
            List.of(new PetAbility(Skill.MINING, 0.008),                // big Mining XP
                    new PetAbility(Stat.DEFENSE,  0.04),                 // iron armor
                    new PetAbility(Stat.HEALTH,   0.08))),               // iron constitution

        Map.entry(PetType.BAT,
            List.of(new PetAbility(Skill.SMITHING,  0.008),             // big Smithing XP
                    new PetAbility(Stat.TOUGHNESS,  0.006))),            // metalwork toughness

        Map.entry(PetType.FOX,
            List.of(new PetAbility(Skill.WOODCUTTING, 0.008),           // big Woodcutting XP
                    new PetAbility(Stat.SWIFTNESS,    0.0002))),         // forest quickness

        Map.entry(PetType.RABBIT,
            List.of(new PetAbility(Skill.EXCAVATING, 0.008),            // big Excavating XP
                    new PetAbility(Stat.SWIFTNESS,   0.0003))),          // quick digger

        Map.entry(PetType.BEE,
            List.of(new PetAbility(Skill.FARMING, 0.008),               // big Farming XP
                    new PetAbility(Stat.LUCK,     0.008))),              // lucky harvests

        Map.entry(PetType.AXOLOTL,
            List.of(new PetAbility(Skill.FISHING, 0.008),               // big Fishing XP
                    new PetAbility(Stat.LUCK,     0.006))),              // lucky catches

        Map.entry(PetType.DOLPHIN,
            List.of(new PetAbility(Skill.FISHING,   0.008),             // big Fishing XP (2nd option)
                    new PetAbility(Stat.SWIFTNESS,  0.0003))),           // swimming speed

        // ── Combat ────────────────────────────────────────────────────────────
        Map.entry(PetType.WOLF,
            List.of(new PetAbility(Skill.SLAYING, 0.008),               // big Slaying XP
                    new PetAbility(Stat.STRENGTH, 0.025))),              // pack alpha strength

        Map.entry(PetType.SPIDER,
            List.of(new PetAbility(Skill.RANGED,  0.008),               // big Ranged XP
                    new PetAbility(Stat.STRENGTH, 0.015))),              // predator attack

        Map.entry(PetType.TURTLE,
            List.of(new PetAbility(Skill.DEFENSE,  0.008),              // big Defense XP
                    new PetAbility(Stat.DEFENSE,   0.03),                // shell armor
                    new PetAbility(Stat.KNOCKBACK_RESISTANCE, 0.002),    // immovable
                    new PetAbility(Stat.HEALTH,    0.06))),              // shell health

        // ── Mastery ───────────────────────────────────────────────────────────
        Map.entry(PetType.ENDERMAN,
            List.of(new PetAbility(Skill.ENCHANTING, 0.008),            // big Enchanting XP
                    new PetAbility(Stat.LUCK,        0.01))),            // arcane luck

        Map.entry(PetType.MOOSHROOM,
            List.of(new PetAbility(Skill.ALCHEMY,  0.008),              // big Alchemy XP
                    new PetAbility(Stat.HEALTH,    0.1))),               // mushroom stew = health

        Map.entry(PetType.CHICKEN,
            List.of(new PetAbility(Skill.COOKING, 0.008),               // big Cooking XP
                    new PetAbility(Stat.HEALTH,   0.06))),               // well-fed health

        Map.entry(PetType.SHEEP,
            List.of(new PetAbility(Skill.CRAFTING, 0.008),              // big Crafting XP
                    new PetAbility(Stat.LUCK,      0.007))),             // resourceful luck

        Map.entry(PetType.CAT,
            List.of(new PetAbility(Skill.AGILITY,  0.008),              // big Agility XP
                    new PetAbility(Stat.SWIFTNESS, 0.0004))),            // feline grace

        Map.entry(PetType.HORSE,
            List.of(new PetAbility(Skill.AGILITY,  0.008),              // big Agility XP (2nd option)
                    new PetAbility(Stat.SWIFTNESS, 0.0005),              // best speed pet
                    new PetAbility(Stat.HEALTH,    0.05))),              // mount stamina

        Map.entry(PetType.ALLAY,
            List.of(new PetAbility(Skill.TAMING, 0.008),                // big Taming XP
                    new PetAbility(Stat.LUCK,    0.012))),               // magical companion

        Map.entry(PetType.PARROT,
            List.of(new PetAbility(Skill.TRADING, 0.008),               // big Trading XP
                    new PetAbility(Stat.LUCK,     0.012)))               // merchant's luck
    );

    public static List<PetAbility> getAbilities(PetType type) {
        return ABILITIES.getOrDefault(type, List.of());
    }
}
