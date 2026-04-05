package com.peakskills.gear;

import com.peakskills.skill.Skill;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Defines skill level requirements for tools and armor.
 * Uses direct item matching to avoid unstable material API changes.
 */
public class GearRequirements {

    public record Requirement(Skill skill, int level) {}

    private static final Map<Predicate<Item>, Requirement> RULES = new LinkedHashMap<>();

    static {
        // --- ARMOR (Defense) ---
        add(i -> i == Items.NETHERITE_HELMET || i == Items.NETHERITE_CHESTPLATE
               || i == Items.NETHERITE_LEGGINGS || i == Items.NETHERITE_BOOTS,
            Skill.DEFENSE, 99);

        add(i -> i == Items.DIAMOND_HELMET || i == Items.DIAMOND_CHESTPLATE
               || i == Items.DIAMOND_LEGGINGS || i == Items.DIAMOND_BOOTS,
            Skill.DEFENSE, 75);

        add(i -> i == Items.IRON_HELMET || i == Items.IRON_CHESTPLATE
               || i == Items.IRON_LEGGINGS || i == Items.IRON_BOOTS,
            Skill.DEFENSE, 50);

        add(i -> i == Items.CHAINMAIL_HELMET || i == Items.CHAINMAIL_CHESTPLATE
               || i == Items.CHAINMAIL_LEGGINGS || i == Items.CHAINMAIL_BOOTS,
            Skill.DEFENSE, 35);

        add(i -> i == Items.TURTLE_HELMET, Skill.DEFENSE, 30);

        add(i -> i == Items.GOLDEN_HELMET || i == Items.GOLDEN_CHESTPLATE
               || i == Items.GOLDEN_LEGGINGS || i == Items.GOLDEN_BOOTS,
            Skill.DEFENSE, 10);

        // --- PICKAXES (Mining) ---
        add(i -> i == Items.NETHERITE_PICKAXE, Skill.MINING, 70);
        add(i -> i == Items.DIAMOND_PICKAXE,   Skill.MINING, 50);
        add(i -> i == Items.IRON_PICKAXE,      Skill.MINING, 20);
        add(i -> i == Items.STONE_PICKAXE,     Skill.MINING, 5);
        add(i -> i == Items.GOLDEN_PICKAXE,    Skill.MINING, 5);

        // --- AXES (Woodcutting) ---
        add(i -> i == Items.NETHERITE_AXE, Skill.WOODCUTTING, 70);
        add(i -> i == Items.DIAMOND_AXE,   Skill.WOODCUTTING, 50);
        add(i -> i == Items.IRON_AXE,      Skill.WOODCUTTING, 20);
        add(i -> i == Items.STONE_AXE,     Skill.WOODCUTTING, 5);
        add(i -> i == Items.GOLDEN_AXE,    Skill.WOODCUTTING, 5);

        // --- SHOVELS (Excavating) ---
        add(i -> i == Items.NETHERITE_SHOVEL, Skill.EXCAVATING, 70);
        add(i -> i == Items.DIAMOND_SHOVEL,   Skill.EXCAVATING, 50);
        add(i -> i == Items.IRON_SHOVEL,      Skill.EXCAVATING, 20);
        add(i -> i == Items.STONE_SHOVEL,     Skill.EXCAVATING, 5);
        add(i -> i == Items.GOLDEN_SHOVEL,    Skill.EXCAVATING, 5);

        // --- HOES (Farming) ---
        add(i -> i == Items.NETHERITE_HOE, Skill.FARMING, 70);
        add(i -> i == Items.DIAMOND_HOE,   Skill.FARMING, 50);
        add(i -> i == Items.IRON_HOE,      Skill.FARMING, 20);
        add(i -> i == Items.STONE_HOE,     Skill.FARMING, 5);
        add(i -> i == Items.GOLDEN_HOE,    Skill.FARMING, 5);

        // --- SWORDS (Slaying) ---
        add(i -> i == Items.NETHERITE_SWORD, Skill.SLAYING, 70);
        add(i -> i == Items.DIAMOND_SWORD,   Skill.SLAYING, 50);
        add(i -> i == Items.IRON_SWORD,      Skill.SLAYING, 20);
        add(i -> i == Items.STONE_SWORD,     Skill.SLAYING, 5);
        add(i -> i == Items.GOLDEN_SWORD,    Skill.SLAYING, 5);
    }

    private static void add(Predicate<Item> predicate, Skill skill, int level) {
        RULES.put(predicate, new Requirement(skill, level));
    }

    /** Returns the requirement for this item, or null if unrestricted. */
    public static Requirement getRequirement(Item item) {
        for (var entry : RULES.entrySet()) {
            if (entry.getKey().test(item)) return entry.getValue();
        }
        return null;
    }
}
