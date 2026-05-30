# Feature Plan: Watersense Fishing

## Goal

Turn fishing into a world-reactive discovery system while keeping the input simple:
one cast, one reel.

## Player Loop

Players fish in different water contexts, discover entries in a Fishing Journal,
progress custom fishing items, find relics, encounter occasional role-based sea
creatures, and contribute to community fishing events.

## Core Pillars

- World-reactive fishing: biome, depth, weather, time, and water state affect catches.
- Depth tiers: shallow, riverbed, deep water, abyssal, ancient.
- Water events and ecosystem moods: temporary states such as Feeding Frenzy, Treasure
  Ripple, Murky Wake, Abyss Stir, and Calm Waters.
- Fishing Journal: tracks discoveries, relics, trophy catches, water events, sea
  creatures, biomes, depth tiers, and community event participation.
- Custom fishing items: real custom rods/items with traits such as XP bonus, rare
  catch chance, relic chance, event chance, trophy size, and journal discovery chance.
- Role-based sea creatures: fewer creatures with distinct behavior.
- Sunken relics: materials that feed collections, crafting, journal completion, and
  future events.
- Community fishing events: server-wide goals with participation rewards.

## Guardrails

- Keep fishing one cast, one reel.
- Avoid repeated-click minigames.
- Sea creatures support the system but do not dominate it.
- Mechanics should connect to collections, pets, crafting, stats, and server events.

## Existing System

PeakSkills already has custom fishing loot, rarity tiers, rarity-based Fishing XP,
Luck-based effective fishing level, fishing collections, Fishing stat bonuses, and
Axolotl/Dolphin fishing pets.

## First Safe Implementation Slice

To be decided before coding. Good candidates:

- Fishing Journal data model and read-only GUI.
- Custom fishing item definitions.
- Depth/context calculation with debug output only.
- Community event prototype without new drops.
