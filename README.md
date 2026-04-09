# PeakSkills

A Fabric mod for Minecraft 1.21.x that adds an OSRS-inspired skill progression system to vanilla survival. Sixteen skills level up through normal gameplay, unlocking passive stat bonuses, abilities, and a pet system — without replacing or bypassing any vanilla mechanics.

---

## Table of Contents

- [Overview](#overview)
- [Installation](#installation)
- [Commands](#commands)
- [Skills](#skills)
  - [Gathering](#gathering-skills)
  - [Combat](#combat-skills)
  - [Mastery](#mastery-skills)
- [Stats](#stats)
- [Gear Requirements](#gear-requirements)
- [Pets](#pets)
- [Fishing](#fishing)
- [Collections](#collections)
- [Design Philosophy](#design-philosophy)
- [Technical Reference](#technical-reference)

---

## Overview

PeakSkills runs entirely server-side. Players earn XP in each skill by doing what they already do in vanilla Minecraft — mining stone earns Mining XP, killing mobs earns Slaying XP, fishing earns Fishing XP, and so on. Every skill reaches a maximum level of **99**.

As skills level up they grant passive **stat bonuses** (health, defense, strength, etc.) and at levels 50 and 99 unlock a **skill ability** — a meaningful passive effect unique to that skill.

A live **action bar** above your hearts shows your current health and defense at all times.

---

## Installation

**Requirements**
- Minecraft 1.21.x (Fabric)
- Fabric Loader 0.16+
- Fabric API

**Server-side only.** Players do not need the mod installed on their client. Drop the jar into your server's `mods/` folder and restart.

---

## Commands

| Command | Description |
|---|---|
| `/skills` | Open your skills menu |
| `/skills <player>` | View another player's skills |
| `/profile` | Open your profile (stats overview + skill grid) |
| `/profile <player>` | View another player's profile |
| `/pets` | Open your pet roster |
| `/collections` | View your collections progress |

---

## Skills

All 16 skills share the same XP curve (see [XP Table](#xp-table)). Each skill has:
- A source of XP (how you earn it)
- Passive stat bonuses that scale linearly from level 1 to 99
- An ability unlocked at level 50 and an upgraded version at level 99

### Gathering Skills

---

#### ⛏ Mining
Earn XP by breaking stone, ores, and other mineable blocks.

**Stat Bonuses per level**
- Defense +0.02 → max +1.98 at level 99
- Toughness +0.005 → max +0.495

**Abilities**
| Level | Name | Effect |
|---|---|---|
| 50 | Ore Sense | 25% chance to earn double XP per block |
| 99 | Master Miner | 50% chance to earn double XP per block |

**Gear Requirements**
| Tool | Min Level |
|---|---|
| Stone / Gold Pickaxe | 5 |
| Iron Pickaxe | 20 |
| Diamond Pickaxe | 50 |
| Netherite Pickaxe | 70 |

---

#### 🪓 Woodcutting
Earn XP by chopping logs (all wood types including crimson and warped stems).

**Stat Bonuses per level**
- Strength +0.01 → max +0.99

**Abilities**
| Level | Name | Effect |
|---|---|---|
| 50 | Lumberjack | 25% chance to earn double XP per log |
| 99 | Arborist | 50% chance to earn double XP per log |

**Gear Requirements** — same tier thresholds as Mining, applied to axes.

---

#### 🪣 Excavating
Earn XP by breaking dirt, sand, gravel, clay, soul sand, and similar soft blocks.

**Stat Bonuses per level**
- Strength +0.008 → max +0.792
- Toughness +0.005 → max +0.495

**Abilities**
| Level | Name | Effect |
|---|---|---|
| 50 | Deep Digger | 25% chance to earn double XP per block |
| 99 | Treasure Hunter | 50% chance to earn double XP per block |

**Gear Requirements** — same tier thresholds as Mining, applied to shovels.

---

#### 🌾 Farming
Earn XP by harvesting fully grown crops. XP is only awarded when a crop is at max maturity; immature crops give nothing. Breaking a crop you planted yourself gives no XP — only crops that grew naturally do.

Column-growing plants (sugar cane, bamboo, cactus) give XP for every naturally grown block above the base you planted.

**XP per crop (fully mature)**
| Crop | XP |
|---|---|
| Wheat | 40 |
| Carrots | 42 |
| Potatoes | 42 |
| Beetroot | 50 |
| Sugar Cane (per grown block) | 22 |
| Bamboo (per grown block) | 18 |
| Cactus (per grown block) | 15 |
| Kelp / Kelp Plant | 18 |
| Pumpkin | 65 |
| Melon | 55 |
| Sweet Berry Bush (age ≥ 2) | 50 |
| Cave Vines (with berries) | 55 |
| Nether Wart (age 3) | 80 |
| Cocoa Beans (age 2) | 68 |
| Chorus Flower | 70 |
| Chorus Plant | 60 |

**Stat Bonuses per level**
- Health +0.08 → max +7.92
- Luck +0.005 → max +0.495

**Abilities**
| Level | Name | Effect |
|---|---|---|
| 50 | Green Thumb | 25% chance to earn double XP per harvest |
| 99 | Master Farmer | 50% chance to earn double XP per harvest |

---

#### 🎣 Fishing
Earn XP by catching fish using the custom loot system (requires the bobber to be in water). XP scales with the rarity of the catch.

**Stat Bonuses per level**
- Luck +0.01 → max +0.99
- Health +0.04 → max +3.96

**Abilities**
| Level | Name | Effect |
|---|---|---|
| 50 | Lucky Cast | +25% fishing XP |
| 99 | Ocean Master | +50% fishing XP |

See the [Fishing](#fishing) section for the full loot table.

---

#### 🔨 Smithing
Earn XP by using an anvil or smithing table.

**Stat Bonuses per level**
- Toughness +0.01 → max +0.99
- Defense +0.015 → max +1.485

**Abilities**
| Level | Name | Effect |
|---|---|---|
| 50 | Forge Master | +25% Smithing XP |
| 99 | Legendary Smith | +50% Smithing XP |

---

---

#### 🌱 Replenish (Enchantment)
A custom enchantment for hoes and axes. When you harvest a crop, it is automatically replanted — one seed is consumed from the drop. Works on all cropblock types: Wheat, Carrots, Potatoes, Beetroot, Nether Wart, Cocoa Beans, and more.

**Unlock**: Reach Farming level 30. The crafting recipe is unlocked in your recipe book automatically.

**Recipe**: Craft using a **Book** (center), **Wheat Seeds** (corners), and **Bone Meal** (edges) in a 3×3 crafting grid. Produces an enchanted book with Replenish I, which can be applied at an anvil.

---

### Combat Skills

---

#### 🛡 Defense
Earn XP by taking damage while wearing armor.

**Stat Bonuses per level**
- Defense +0.04 → max +3.96
- Health +0.1 → max +9.9
- Toughness +0.01 → max +0.99
- Knockback Resistance +0.002 → max +0.198

**Abilities**
| Level | Name | Effect |
|---|---|---|
| 50 | Iron Will | Heal for 10% of incoming damage after it lands |
| 99 | Titan | Heal for 20% of incoming damage after it lands |

> Iron Will and Titan work as retroactive healing — full damage is taken first, then a fraction is immediately restored.

**Gear Requirements**
| Armor | Min Level |
|---|---|
| Golden Armor | 10 |
| Chainmail Armor | 30 |
| Turtle Helmet | 30 |
| Iron Armor | 35 |
| Diamond Armor | 40 |
| Netherite Armor | 99 |

---

#### ⚔ Slaying
Earn XP by killing mobs in melee.

**Stat Bonuses per level**
- Strength +0.02 → max +1.98
- Health +0.05 → max +4.95

**Abilities**
| Level | Name | Effect |
|---|---|---|
| 50 | Executioner | +50% melee kill XP |
| 99 | Warlord | +100% melee kill XP |

**Gear Requirements** — same tier thresholds as Mining, applied to swords.

---

#### 🏹 Ranged
Earn XP by killing mobs with bows, crossbows, or tridents. No gear requirements.

**Stat Bonuses per level**
- Strength +0.012 → max +1.188
- Swiftness +0.0002 → max +0.0198

**Abilities**
| Level | Name | Effect |
|---|---|---|
| 50 | Hawk Eye | +50% ranged kill XP |
| 99 | Sniper | +100% ranged kill XP |

---

### Mastery Skills

---

#### ✨ Enchanting
Earn XP by using an enchantment table.

**Stat Bonuses per level**
- Luck +0.015 → max +1.485
- Toughness +0.005 → max +0.495

**Abilities**
| Level | Name | Effect |
|---|---|---|
| 50 | Arcane Mind | +25% Enchanting XP |
| 99 | Grand Enchanter | +50% Enchanting XP |

---

#### 🧪 Alchemy
Earn XP by brewing potions.

**Stat Bonuses per level**
- Health +0.12 → max +11.88

**Abilities**
| Level | Name | Effect |
|---|---|---|
| 50 | Brewmaster | +25% Alchemy XP |
| 99 | Potion Savant | +50% Alchemy XP |

---

#### 🍳 Cooking
Earn XP by smelting food in a furnace or smoker.

**Stat Bonuses per level**
- Health +0.15 → max +14.85

**Abilities**
| Level | Name | Effect |
|---|---|---|
| 50 | Chef's Kiss | +25% Cooking XP |
| 99 | Master Chef | +50% Cooking XP |

---

#### 🔧 Crafting
Earn XP by using a crafting table.

**Stat Bonuses per level**
- Luck +0.01 → max +0.99
- Strength +0.015 → max +1.485

**Abilities**
| Level | Name | Effect |
|---|---|---|
| 50 | Artisan | +25% Crafting XP |
| 99 | Mastercraftsman | +50% Crafting XP |

---

#### 💨 Agility
Earn XP by sprinting and swimming.

**Stat Bonuses per level**
- Swiftness +0.0004 → max +0.0396
- Knockback Resistance +0.001 → max +0.099

**Abilities**
| Level | Name | Effect |
|---|---|---|
| 50 | Parkour Pro | +25% Agility XP |
| 99 | Wind Walker | +50% Agility XP |

---

#### 🐾 Taming
Earn XP by having an active pet when that pet's affinity skill earns XP.

**Stat Bonuses per level**
- Luck +0.02 → max +1.98
- Health +0.04 → max +3.96

**Abilities**
| Level | Name | Effect |
|---|---|---|
| 50 | Beast Bond | Active pet earns 2× XP |
| 99 | Pet Whisperer | Active pet earns 3× XP |

---

#### 💰 Trading
Earn XP by trading with villagers.

**Stat Bonuses per level**
- Luck +0.015 → max +1.485
- Swiftness +0.0001 → max +0.0099

**Abilities**
| Level | Name | Effect |
|---|---|---|
| 50 | Haggler | +25% Trading XP |
| 99 | Trade Baron | +50% Trading XP |

---

## Stats

Seven stats are computed from skill levels, collection rewards, and active pet bonuses. They map directly to vanilla Minecraft entity attributes.

| Stat | Icon | Vanilla Attribute | Display Scale |
|---|---|---|---|
| Health | ❤ | `max_health` | ×10 |
| Defense | ❋ | `armor` | ×10 |
| Toughness | ◈ | `armor_toughness` | ×100 |
| Strength | ⚔ | `attack_damage` | ×100 |
| Swiftness | ⚡ | `movement_speed` | ×10000 |
| Knockback Resist | ⚓ | `knockback_resistance` | ×1000 |
| Luck | ✦ | `luck` | ×100 |

Stats are recalculated and applied every time a skill levels up, a pet is activated or deactivated, or the player joins the server.

The action bar above your hearts shows live health and defense:
```
❤ 22 / 35    ❋ 14
```

---

## Gear Requirements

Higher-tier tools and weapons require a minimum skill level to use. Attempting to attack a block with an insufficient tool sends a warning and cancels the action.

| Tool Tier | Mining / Woodcutting / Excavating | Slaying (Swords) |
|---|---|---|
| Stone / Gold | Level 5 | Level 5 |
| Iron | Level 20 | Level 20 |
| Diamond | Level 50 | Level 50 |
| Netherite | Level 70 | Level 70 |

| Armor Tier | Defense Level |
|---|---|
| Golden | 10 |
| Chainmail | 30 |
| Turtle Helmet | 30 |
| Iron | 35 |
| Diamond | 40 |
| Netherite | 99 |

---

## Pets

Pets are companions that earn XP alongside the player and provide passive stat bonuses. Only one pet can be active at a time.

### Getting Pets

**From mob drops** — Every mob has a corresponding pet type. When you kill a mob, there is a chance (3% base, +0.05% per Taming level, up to ~8%) of dropping a Common pet egg. Higher rarities can drop too at lower rates (Uncommon: any drop, Rare/Epic via lucky rolls).

**From the Pet Breeder** — Open `/pets` → click "Craft Pet Eggs" to craft Common eggs using materials from your inventory. Each pet type has a unique recipe (e.g. Wolf requires 8 Bones + 1 Lead).

### Hatching

Right-click a pet egg in your hand to hatch it. The pet is added to your roster (max 21 slots). An active pet floats visibly at your right side.

### Rarity Tiers

| Rarity | Level Cap | Upgrade Cost |
|---|---|---|
| Common | 20 | 16 Gold Ingots |
| Uncommon | 40 | 8 Diamonds |
| Rare | 60 | 16 Emeralds + 4 Diamonds |
| Epic | 80 | 4 Netherite Ingots |
| Legendary | 99 | — (max) |

When a pet reaches its level cap, **shift-clicking** it in the roster upgrades it to the next rarity. Upgrading resets XP to 0 but raises the level cap.

### Pet XP

Active pets earn XP whenever their **affinity skill** earns XP:
- Same skill as affinity: 100% of the skill XP earned
- Different skill: 10% of the skill XP earned

The Taming ability multiplies this further (2× at Beast Bond, 3× at Pet Whisperer).

Each rarity has a different XP multiplier that increases XP requirements per level:

| Rarity | XP Multiplier | Approx. XP to cap |
|---|---|---|
| Common | 1.0× | ~16,000 |
| Uncommon | 1.2× | ~95,000 |
| Rare | 1.4× | ~288,000 |
| Epic | 1.6× | ~644,000 |
| Legendary | 1.8× | ~1,190,000 |

### Removing Pets

Right-click a pet in the roster to remove it. The pet is returned to your inventory as an egg with its XP preserved — re-hatching it restores the same level.

### All 18 Pets

| Pet | Affinity | Stat Bonus |
|---|---|---|
| Iron Golem | Mining | Defense |
| Bat | Smithing | Defense |
| Fox | Woodcutting | Strength |
| Rabbit | Excavating | Strength |
| Bee | Farming | Health |
| Axolotl | Fishing | Health |
| Dolphin | Fishing | Luck |
| Wolf | Slaying | Strength |
| Spider | Ranged | Strength |
| Turtle | Defense | Defense |
| Enderman | Enchanting | Luck |
| Mooshroom | Alchemy | Health |
| Chicken | Cooking | Health |
| Sheep | Crafting | Luck |
| Cat | Agility | Swiftness |
| Horse | Agility | Swiftness |
| Allay | Taming | Luck |
| Parrot | Trading | Luck |

---

## Fishing

The vanilla fishing loot is replaced with a custom loot table. XP and item quality scale with your Fishing level. The bobber must be in water — reeling items off the ground gives nothing.

### Loot Tiers

| Rarity | Min Level | XP | Example Drops |
|---|---|---|---|
| Common | 1 | 30 | Raw Cod, Salmon, Lily Pad, Ink Sac, Seagrass |
| Uncommon | 10 | 80 | Tropical Fish, Pufferfish, Nautilus Shell |
| Rare | 25 | 194 | Prismarine Shard, Iron Scrap, Sea Crystal, Ancient Coin |
| Epic | 50 | 477 | Sea Diamond, Deep Treasure, Loot Orb |
| Legendary | 75 | 1,283 | Ancient Trident, Totem of the Deep, Abyssal Star |

Higher Fishing levels increase the weight of better rarity tiers. Luck stat further shifts the table toward rarer catches.

---

## Collections

Breaking blocks and killing mobs increments collection counters. Reaching collection milestones unlocks permanent rewards (recipe unlocks, stat bonuses, items).

**Mining**: Cobblestone, Coal, Granite, Diorite, Andesite, Deepslate

**Woodcutting**: Oak, Birch, Spruce, Dark Oak, Jungle, Acacia, Mangrove

**Excavating**: Dirt, Sand, Gravel, Clay, Soul Sand

**Farming**: Wheat, Carrot, Potato, Sugar Cane, Pumpkin, Melon, Bamboo

**Fishing**: Cod, Salmon, Pufferfish, Tropical Fish, Lily Pad, Ink Sac, Nautilus Shell, Prismarine

**Combat**: Rotten Flesh, Bone, Spider Eye, Gunpowder, Ender Pearl, Blaze Rod, Ghast Tear, Wither Skeleton Skull, Gold Nugget (Piglin) — credit goes to the player who killed the mob, not whoever picks up the drop

---

## Design Philosophy

PeakSkills is designed to feel like a natural extension of vanilla Minecraft rather than a parallel MMO system layered on top of it.

- **Skills amplify what you already do** — there are no dedicated grind spots or skill-specific minigames. A player who explores, builds, and fights naturally levels every skill.
- **Bonuses are incremental, not transformative** — a level 99 player is noticeably stronger than a level 1 player but is still playing Minecraft, not a different game.
- **No custom item tiers** — swords are swords, pickaxes are pickaxes. Skills affect how well you use them, not what they are.
- **Access is never gated, only efficiency** — gear requirements prevent early use of end-game tools, but any area of the world remains accessible. A low-level player can still enter the Nether; they just cannot swing a Netherite sword yet.

---

## Technical Reference

### XP Table

The XP curve is based on the Old School RuneScape formula. XP required for level `L` is:

```
xpForLevel(L) = floor( sum_{k=1}^{L-1} floor(k + 300 * 2^(k/7)) ) / 4
```

Key milestones:

| Level | Total XP Required |
|---|---|
| 1 | 0 |
| 10 | 1,154 |
| 25 | 7,842 |
| 50 | 101,333 |
| 75 | 1,210,421 |
| 99 | 13,034,431 |

### Stat Scaling

Each skill grants a flat bonus per level applied as an `ADD_VALUE` modifier to the corresponding vanilla attribute. The formula for a single stat source is:

```
bonus = level × valuePerLevel
```

All sources for a given stat are summed before being applied. Stats are recalculated fresh on every level-up and on join.

The displayed value in `/profile` and the action bar is:
```
displayed = rawAttributeBonus × displayScale
```

For Health and Defense, `displayScale` is 10. The action bar shows rounded integers.

### Pet XP Formula

XP required for a pet to reach level `L` at a given rarity:

```
xpForLevel(L) = 40 × (L - 1)^1.3 × rarityMultiplier
```

Where `rarityMultiplier` is 1.0 (Common), 1.2 (Uncommon), 1.4 (Rare), 1.6 (Epic), or 1.8 (Legendary).

### Data Persistence

All player data (skill XP, pet roster, collection counts) is stored server-side in a `PersistentState` NBT file per player UUID under the world's `data/` folder. It survives server restarts.

Placed block positions (used to prevent farming block duplication exploits) are stored in a separate `peakskills_placed_blocks.dat` file in the overworld's `data/` folder as a flat list of packed `BlockPos` longs.
