package com.peakskills.pet;

import com.peakskills.skill.Skill;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * All 18 pet types. Each has a primary skill affinity that earns a large XP bonus
 * when the pet is active while the player uses that skill.
 *
 * All 16 skills are covered by at least one pet.
 */
public enum PetType {

    // ── Gathering ──────────────────────────────────────────────────────────────
    IRON_GOLEM ("Iron Golem", Skill.MINING,      Items.IRON_INGOT),    // lives underground, iron = mining
    BAT        ("Bat",        Skill.SMITHING,     Items.COAL),          // cave ore → smithing
    FOX        ("Fox",        Skill.WOODCUTTING,  Items.SWEET_BERRIES), // forest creature
    RABBIT     ("Rabbit",     Skill.EXCAVATING,   Items.CARROT),        // burrow digger
    BEE        ("Bee",        Skill.FARMING,      Items.HONEYCOMB),     // pollinator
    AXOLOTL    ("Axolotl",   Skill.FISHING,      Items.TROPICAL_FISH), // water hunter
    DOLPHIN    ("Dolphin",    Skill.FISHING,      Items.COD),           // second fishing pet

    // ── Combat ────────────────────────────────────────────────────────────────
    WOLF       ("Wolf",       Skill.SLAYING,      Items.BONE),          // apex predator
    SPIDER     ("Spider",     Skill.RANGED,        Items.STRING),        // web = projectile traps
    TURTLE     ("Turtle",     Skill.DEFENSE,       Items.TURTLE_SCUTE),  // living shield

    // ── Mastery ───────────────────────────────────────────────────────────────
    ENDERMAN   ("Enderman",   Skill.ENCHANTING,   Items.ENDER_PEARL),   // mystical, arcane
    MOOSHROOM  ("Mooshroom",  Skill.ALCHEMY,       Items.RED_MUSHROOM),  // mushroom = potions
    CHICKEN    ("Chicken",    Skill.COOKING,       Items.EGG),           // farm animal, food source
    SHEEP      ("Sheep",      Skill.CRAFTING,      Items.WHEAT),         // wool = crafting material
    CAT        ("Cat",        Skill.AGILITY,       Items.COD),           // nimble and quick
    HORSE      ("Horse",      Skill.AGILITY,       Items.WHEAT),         // second agility pet (speed)
    ALLAY      ("Allay",      Skill.TAMING,        Items.ALLIUM),        // magical companion
    PARROT     ("Parrot",     Skill.TRADING,       Items.COOKIE);        // merchant's bird

    public final String displayName;
    public final Skill affinity;  // primary skill — gives large XP bonus when active during this skill
    public final Item  icon;

    PetType(String displayName, Skill affinity, Item icon) {
        this.displayName = displayName;
        this.affinity    = affinity;
        this.icon        = icon;
    }
}
