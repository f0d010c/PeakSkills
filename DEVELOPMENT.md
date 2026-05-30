# PeakSkills Development Workflow

This document is the default process for adding, updating, or removing PeakSkills
features.

## Feature Planning

Create or update a short plan before large changes. Use `docs/features/` for feature
notes that should live with the public repo.

Minimum plan:

- Goal: what player or server problem this solves.
- Player loop: what the player repeatedly does.
- Systems touched: XP, collections, pets, crafting, GUI, commands, data, config.
- Risks: dupes, permission abuse, bad data, crashes, performance, compatibility.
- Test plan: exact manual steps to verify in game.

## Definition Of Done

A change is done when:

- `.\gradlew.bat test` passes.
- `.\gradlew.bat compileJava` passes.
- `.\gradlew.bat build` passes before release.
- Command permission gates use the `isOp()` pattern for admin actions.
- Numeric command arguments have explicit bounds.
- GUI actions that consume items or grant rewards have cooldown/race checks.
- Player data changes are backward compatible with missing old fields.
- `CHANGELOG.md` or the relevant feature note is updated when appropriate.
- The test server has exactly one PeakSkills jar in its `mods` folder.
- In-game smoke steps are written or updated for player-facing behavior.

## Verification Commands

From the PeakSkills folder:

```bat
.\gradlew.bat test
.\gradlew.bat compileJava
.\gradlew.bat build
```

Or run:

```powershell
.\scripts\verify-peakskills.ps1
```

## Deploying To A Local Test Server

Build and copy the newest non-sources PeakSkills jar to a server `mods` folder:

```powershell
.\scripts\deploy-latest-peakskills.ps1 -ModsDir "C:\path\to\server\mods" -Build
```

The script removes old `peakskills-*.jar` files first so Fabric cannot load multiple
versions at once.

## Manual Smoke Tests

Smoke checklists live in `tests/smoke/`. Update them when a feature adds or changes a
player-facing flow.

Current important smoke tests:

- `tests/smoke/core.md`
- `tests/smoke/fishing.md`

## Release Notes

Public releases should be tagged and released only after build/test verification and
local smoke testing for player-facing changes.
