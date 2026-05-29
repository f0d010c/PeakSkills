# PeakSkills

PeakSkills is a server-side Fabric RPG progression mod for Minecraft. It adds skills,
levels, stat bonuses, pets, collections, custom fishing rewards, and quality of life
systems built for survival servers.

- [Modrinth](https://modrinth.com/mod/peakskills)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/peakskills)

## Requirements

- Minecraft `1.21.11`
- Fabric Loader `0.18.2` or newer
- Fabric API
- Java 21

PeakSkills is server-side. Players can join without installing the mod on their
client, although the server must have Fabric and PeakSkills installed.

## Features

- RPG-style skill progression with XP, levels, and unlock rewards.
- Persistent player stats including health, defense, strength, speed, crit chance,
  magic find, mining fortune, farming fortune, and more.
- Collection tracking for blocks, items, mobs, fishing loot, pets, and server
  activities.
- Pet leveling, pet menu management, visible companions, and pet stat bonuses.
- Custom fishing loot and fishing-related progression.
- Replenish enchantment for harvesting and replanting crops.
- In-game menus for skills, profiles, pets, collections, crafting, leaderboards, and
  player settings.
- Per-player settings, including a level-up ding guard for burst XP gains.

## Skills

PeakSkills currently includes:

- Mining
- Woodcutting
- Excavating
- Farming
- Fishing
- Defense
- Slaying
- Ranged
- Taming
- Enchanting
- Alchemy
- Smithing
- Cooking
- Crafting
- Agility
- Trading

Each skill has its own XP sources, level rewards, and stat bonuses.

## Commands

Player commands:

| Command | Description |
| --- | --- |
| `/skills` | Opens the skills menu. |
| `/skills <player>` | Views another player's skills. |
| `/profile` | Opens your profile. |
| `/profile <player>` | Views another player's profile. |
| `/collections` | Opens the collections menu. |
| `/pets` | Opens the pet menu. |
| `/craft` | Opens the custom crafting menu. |
| `/settings` | Opens player settings. |
| `/skilltop` | Shows skill leaderboards. |
| `/skillrank` | Shows your rank in every skill. |

Admin commands are available for server operators to manage player data, XP, pet XP,
and server-side progression systems.

## Installation

1. Install Fabric Loader and Fabric API on your server.
2. Download the PeakSkills JAR from Modrinth, CurseForge, or GitHub Releases.
3. Place the JAR in your server's `mods` folder.
4. Start the server.

Player data is stored server-side in the world save and is written with atomic file
replacement to reduce the chance of corrupted saves after crashes or interrupted
shutdowns.

## Building From Source

From the repository root:

```bat
.\gradlew.bat build
```

The built JAR will be created under:

```text
build/libs/
```

For a faster compile check during development:

```bat
.\gradlew.bat compileJava
```

## License

PeakSkills is licensed under the MIT License.
