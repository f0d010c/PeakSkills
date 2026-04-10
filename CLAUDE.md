# PeakSkills — Claude Development Guide

## Git Rules
- Always commit AND push together — never commit without pushing
- Never add `Co-Authored-By` or any AI attribution lines to commit messages
- Stage specific files by name, never `git add .`

## Before Every Commit
- Build must pass: `.\gradlew.bat compileJava`
- No `return true` permission bypasses left in command requires()

## Command Permissions
- Admin commands (`addxp`, `setlevel`, `reset`, `removexp`, `backup`, `restore`) must use `SkillsCommand::isOp`
- `isOp()` checks the server's `OperatorList` directly — do NOT use the `PermissionPredicate` API (broken in 1.21.11)
- Player-facing commands (`/skills`, `/profile`, `/collections`, `/pets`) require no OP

## Adding New Collections
When adding a new collection, all 3 of these must be updated:
1. `CollectionType.java` — add the enum entry
2. `CollectionRegistry.java` — add `reg(...)` tier definition + block/entity matching in `fromBlock()` or `fromEntity()`
3. Nothing else needed — GUIs read dynamically from the enum

## Adding New Skills
- Add to `Skill.java` enum
- Add XP sources in `SkillEvents.java`
- Add stat bonuses in `StatRegistry.java`
- Register command tab-completions if needed

## Architecture Notes
- **Minecraft version**: 1.21.11, Yarn mappings `1.21.11+build.4`, Fabric Loader 0.18.2
- **Package root**: `com.peakskills`
- `PlacedBlocksState` tracks player-placed blocks to prevent dupe XP — FARMING skill blocks are exempt, column growers (sugar cane/bamboo/cactus) use structural base-block check
- `PetDisplayManager` defers entity spawn/kill to post-tick lists — never spawn/kill entities inside the tick loop
- `sendStatBar` in `XpManager` runs every 40 ticks — shows ❤ health and ❋ defense (defense value × 10, all values Math.round)
- `computeStatTotals` in `ProfileGui` must match `StatManager` exactly: skill contributions + collection bonuses + pet bonuses + base health (20.0)
- `SkillsScreenHandler` supports left-click, right-click, and shift-click (SlotActionType.QUICK_MOVE) handlers — CLONE only fires in creative mode, never use it for survival features

## Documentation
- After every major update (new feature, system change, balance adjustment), update `README.md` to reflect it

## Development Mindset
- When implementing something, check if the same logic applies elsewhere in the project — if it does, ask the user before doing it
- **Every new feature must be reviewed for security vulnerabilities before committing.** Specifically check:
  - Client-controlled input (NBT, command args, slot clicks) — clamp/validate all values
  - Packet desync — add distance/position checks before acting on block/entity events (max ~8 blocks from player)
  - Command arguments — all numeric args must have explicit min AND max bounds (`longArg(1, 10_000_000L)`, not `longArg(1)`)
  - Arithmetic overflow — use saturating math or clamp before storing in player data
  - Race conditions in GUIs — add per-player cooldowns on any action that consumes items or grants rewards

## Security Patterns (established in codebase)
- **Replenish distance check**: `pos.getSquaredDistanceFromCenter(...) > 64` guards against spoofed block break packets
- **Craft cooldown**: `PeakCraftingGui.lastCraftTime` map — 1 second per player, prevents macro spam
- **XP bounds**: `LongArgumentType.longArg(1, 10_000_000L)` on all admin XP commands
- **Taming gain guard**: `amount > 0` check before passive taming grant in `XpManager`
- **Item tagging**: Replenish and CombatDropTracker use UUID maps to ensure only the correct player benefits from item collection
- **NBT clamping**: `PetEggHandler` and `PetInstance` clamp XP to `[0, maxXpForRarity]`

## Permission API (1.21.11 specific)
- `hasPermissionLevel(int)` does NOT exist on `ServerCommandSource` in 1.21.11
- `DefaultPermissions.GAMEMASTERS` exists but `hasPermission()` does not enforce correctly
- Correct OP check: query `server.getPlayerManager().getOpList().get(new PlayerConfigEntry(player.getGameProfile())) != null`
