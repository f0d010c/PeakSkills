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
    IRON_GOLEM ("Iron Golem", Skill.MINING,      Items.IRON_INGOT,    Items.IRON_GOLEM_SPAWN_EGG),
    BAT        ("Bat",        Skill.SMITHING,     Items.COAL,          Items.BAT_SPAWN_EGG),
    FOX        ("Fox",        Skill.WOODCUTTING,  Items.SWEET_BERRIES, Items.FOX_SPAWN_EGG),
    RABBIT     ("Rabbit",     Skill.EXCAVATING,   Items.CARROT,        Items.RABBIT_SPAWN_EGG),
    BEE        ("Bee",        Skill.FARMING,      Items.HONEYCOMB,     Items.BEE_SPAWN_EGG),
    AXOLOTL    ("Axolotl",    Skill.FISHING,      Items.TROPICAL_FISH, Items.AXOLOTL_SPAWN_EGG),
    DOLPHIN    ("Dolphin",    Skill.FISHING,      Items.COD,           Items.DOLPHIN_SPAWN_EGG),

    // ── Combat ────────────────────────────────────────────────────────────────
    WOLF       ("Wolf",       Skill.SLAYING,      Items.BONE,          Items.WOLF_SPAWN_EGG),
    SPIDER     ("Spider",     Skill.RANGED,        Items.STRING,        Items.SPIDER_SPAWN_EGG),
    TURTLE     ("Turtle",     Skill.DEFENSE,       Items.TURTLE_SCUTE,  Items.TURTLE_SPAWN_EGG),

    // ── Mastery ───────────────────────────────────────────────────────────────
    ENDERMAN   ("Enderman",   Skill.ENCHANTING,   Items.ENDER_PEARL,   Items.ENDERMAN_SPAWN_EGG),
    MOOSHROOM  ("Mooshroom",  Skill.ALCHEMY,       Items.RED_MUSHROOM,  Items.MOOSHROOM_SPAWN_EGG),
    CHICKEN    ("Chicken",    Skill.COOKING,       Items.EGG,           Items.CHICKEN_SPAWN_EGG),
    SHEEP      ("Sheep",      Skill.CRAFTING,      Items.WHEAT,         Items.SHEEP_SPAWN_EGG),
    CAT        ("Cat",        Skill.AGILITY,       Items.COD,           Items.CAT_SPAWN_EGG),
    HORSE      ("Horse",      Skill.AGILITY,       Items.WHEAT,         Items.HORSE_SPAWN_EGG),
    ALLAY      ("Allay",      Skill.TAMING,        Items.ALLIUM,        Items.ALLAY_SPAWN_EGG),
    PARROT     ("Parrot",     Skill.TRADING,       Items.COOKIE,        Items.PARROT_SPAWN_EGG);

    public final String displayName;
    public final Skill affinity;
    public final Item  icon;
    public final Item  spawnEgg;

    PetType(String displayName, Skill affinity, Item icon, Item spawnEgg) {
        this.displayName = displayName;
        this.affinity    = affinity;
        this.icon        = icon;
        this.spawnEgg    = spawnEgg;
    }
}
