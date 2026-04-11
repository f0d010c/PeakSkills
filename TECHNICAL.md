# PeakSkills — Technical Reference

Deep-dive for developers and future sessions. Covers architecture, data flow, key classes, and how to extend the mod.

---

## Package Layout

```
com.peakskills
├── PeakSkills.java              — mod entry point, registers all systems
├── skill/
│   ├── Skill.java               — enum of all 16 skills
│   ├── XPTable.java             — OSRS XP curve formula
│   └── SkillEvents.java         — Fabric event listeners that award XP
├── xp/
│   └── XpManager.java           — addXp(), level-up logic, stat bar, taming passive
├── player/
│   ├── PlayerData.java          — per-player state (XP map, pet roster, collections, petsVisible)
│   ├── PlayerDataManager.java   — load/save JSON, in-memory cache, lazy server ref
│   └── PlacedBlocksState.java   — tracks player-placed blocks to prevent dupe XP
├── stat/
│   ├── StatManager.java         — applies vanilla attribute modifiers from all sources
│   └── StatRegistry.java        — defines valuePerLevel for each Skill→Stat mapping
├── gui/
│   ├── SkillsScreenHandler.java — custom ScreenHandler with left/right/shift-click maps
│   ├── SkillsGui.java           — skill list GUI (54-slot chest)
│   ├── SkillDetailGui.java      — single skill detail view
│   ├── ProfileGui.java          — stat overview + skill grid
│   ├── PetMenuGui.java          — pet roster GUI with filter tabs + visibility toggle
│   ├── PetBreederGui.java       — craft pet eggs GUI
│   ├── CollectionsGui.java      — collection category picker
│   ├── CollectionCategoryGui.java
│   └── CollectionDetailGui.java
├── pet/
│   ├── PetType.java             — enum of all 18 pet types (icon, spawnEgg, affinity, stat)
│   ├── PetRarity.java           — enum: COMMON→LEGENDARY (levelCap, xpMultiplier, color)
│   ├── PetInstance.java         — single owned pet (id, type, rarity, xp, active)
│   ├── PetRoster.java           — player's list of pets (max 21), active pet tracking
│   ├── PetDisplayManager.java   — spawns/kills/moves ItemDisplayEntity per active pet
│   ├── PetXPTable.java          — pet level←→XP formula (40*(L-1)^1.3 * rarityMult)
│   ├── PetAbility.java          — interface for stat-scaling ability display
│   ├── PetAbilityRegistry.java  — maps PetType → List<PetAbility>
│   ├── PetEggHandler.java       — right-click hatch logic, NBT read/clamp
│   └── PetUpgradeHandler.java   — shift-click upgrade, cost validation
├── collection/
│   ├── CollectionType.java      — enum of all collection categories + tiers
│   ├── CollectionRegistry.java  — reg() tier defs, fromBlock()/fromEntity() lookup
│   └── CollectionData.java      — per-player counters with saturating increment
├── crafting/
│   ├── PeakIngredient.java      — record(item, count, gridSlot)
│   ├── PeakRecipe.java          — record(id, displayName, category, ingredients, Supplier<ItemStack>)
│   ├── PeakRecipeResult.java    — static factory methods for recipe results (lazy, registry-safe)
│   ├── PeakRecipeRegistry.java  — registers all custom recipes, validates at startup
│   └── PeakCraftingGui.java     — Skyblock-style /craft GUI (list + detail views)
├── enchantment/
│   └── ReplenishEnchantment.java — custom Replenish enchant, TAGGED_DROPS magnet security
└── command/
    ├── SkillsCommand.java       — /skills, /profile, /skilltop, /skillrank + admin subcommands
    └── PetsCommand.java         — /pets, admin pet xp
    └── PeakCraftingCommand.java — /craft
```

---

## Core Data Flow

### XP Award
```
Event fires (e.g. block break)
  → SkillEvents checks PlacedBlocksState / distance guard / gear level
  → XpManager.addXp(player, skill, amount)
      → PlayerData.addXp()  [saturating, returns leveledUp bool]
      → if leveled up: StatManager.applyStats(), send level-up message
      → if TAMING active pet + affinity match: award pet XP
      → every 40 ticks: sendStatBar()
```

### Stat Calculation
```
StatManager.applyStats(player)
  → for each Stat: sum contributions from
      1. skill levels × valuePerLevel  (StatRegistry)
      2. collection tier rewards        (CollectionRegistry)
      3. active pet bonus               (PetAbilityRegistry)
      4. base health (20.0)
  → apply as ADD_VALUE attribute modifier with UUID keyed to each source
```

### Player Data Save/Load
```
PlayerDataManager
  → load: world/peakskills/<uuid>.json → gson → PlayerData
  → save: triggered on disconnect + periodic autosave
  → fields: skills{}, pets[], collections{}, petsVisible, mana, maxMana
```

### Pet Display
```
PetDisplayManager (tick loop — deferred spawn/kill lists)
  → each tick: move existing ItemDisplayEntity to player right side
  → restoreDisplay(): skip if !petsVisible
  → spawnDisplay(): new ItemDisplayEntity, TAG="peakskills_pet_display"
  → killDisplay(): remove entity, clear displays map
  → orphan cleanup on JOIN: scan world for tagged entities not in displays map
```

---

## GUI System

All GUIs use a 54-slot `SimpleInventory` + `SkillsScreenHandler`.

`SkillsScreenHandler` holds three maps:
- `Map<Integer, Runnable> leftHandlers`
- `Map<Integer, Runnable> rightHandlers`
- `Map<Integer, Runnable> middleHandlers`

On slot click the handler fires, then the GUI re-opens (reopening refreshes state). CLONE action type only fires in creative — never use it for survival logic.

---

## Custom Crafting System (`/craft`)

**Adding a new recipe:**

1. Add a static result factory in `PeakRecipeResult.java` (use `PlayerDataManager.getServer().getRegistryManager()` for anything needing registries — must be lazy).
2. Register in `PeakRecipeRegistry.register()`:
```java
recipes.add(new PeakRecipe(
    "my_recipe",           // unique id
    "My Item",             // display name
    "General",             // category
    List.of(
        new PeakIngredient(Items.DIAMOND, 4, 0),   // gridSlot 0-8 = 3x3 grid
        new PeakIngredient(Items.GOLD_INGOT, 8, 4) // center
    ),
    PeakRecipeResult::myItem
));
```

**Grid slot layout:**
```
0 1 2
3 4 5
6 7 8
```

**Security built-in:** 1s per-player cooldown, `buildResult()` before consume (graceful failure), all ingredients validated at startup.

**Crafting grants:** 500 Crafting XP per successful craft.

---

## Security Patterns

| Guard | Location | What it prevents |
|---|---|---|
| Distance check `squaredDist > 64` | ReplenishEnchantment | Spoofed block break packets |
| `TAGGED_DROPS` UUID map | ReplenishEnchantment | Magnet collecting other players' drops |
| Craft cooldown 1s | PeakCraftingGui | Macro spam / dupe |
| `buildResult()` before consume | PeakCraftingGui | Item loss on result failure |
| `longArg(1, 10_000_000L)` | SkillsCommand, PetsCommand | Arithmetic overflow via commands |
| `amount > 0` guard | XpManager taming passive | Negative XP injection |
| NBT clamp `[0, maxXpForRarity]` | PetEggHandler, PetInstance | Overflow from tampered egg NBT |
| Saturating increment | CollectionData | Long overflow in collection counters |
| OP check via OpList | SkillsCommand, PetsCommand | Permission bypass (1.21.11 API broken) |

---

## OP Check Pattern (1.21.11)

`hasPermissionLevel()` and `DefaultPermissions` do not work correctly in 1.21.11.

Correct pattern:
```java
private static boolean isOp(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
    if (player == null) return true; // console always allowed
    MinecraftServer server = ctx.getSource().getServer();
    return server.getPlayerManager().getOpList()
        .get(new PlayerConfigEntry(player.getGameProfile())) != null;
}
```

---

## Adding a New Skill

1. `Skill.java` — add enum entry
2. `SkillEvents.java` — add Fabric event listener awarding XP
3. `StatRegistry.java` — add stat bonus mappings
4. Optionally add gear requirements in the relevant event handler
5. Update `README.md`

## Adding a New Collection

1. `CollectionType.java` — add enum entry
2. `CollectionRegistry.java` — add `reg(...)` tier definition + `fromBlock()` or `fromEntity()` match
3. Nothing else — GUIs read dynamically from the enum

## Adding a New Pet Type

1. `PetType.java` — add enum entry (icon, spawnEgg, affinity, stat, displayName)
2. `PetAbilityRegistry.java` — register abilities for the new type
3. Optionally add a recipe in `PetBreederGui`

---

## Key Constants

| Value | Where | Notes |
|---|---|---|
| Max skill level | `XPTable.MAX_LEVEL = 99` | |
| Max pet slots | `PetRoster.MAX_SLOTS = 21` | |
| Stat bar update interval | `XpManager` every 40 ticks | ~2 seconds |
| Replenish distance cap | 8 blocks (squaredDist 64) | |
| Craft cooldown | 1000ms | `PeakCraftingGui.COOLDOWN_MS` |
| Custom recipe XP | 500 | hardcoded in `tryCraft()` |
| Pet drop base chance | 3% | +0.05% per Taming level |

---

## Minecraft Version Notes

- **1.21.11**, Yarn mappings `1.21.11+build.4`, Fabric Loader 0.18.2
- `hasPermissionLevel(int)` does NOT exist on `ServerCommandSource`
- `getUserCache()` / `GameProfile::getName` API unavailable for offline players — use UUID prefix as fallback display name
- `ItemDisplayEntity` is under `net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity`
- `SlotActionType.CLONE` only fires in creative mode
