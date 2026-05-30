# Fishing Smoke Test

Run after changes touching fishing, loot, collections, stats, pets, or XP.

## Setup

1. Build and deploy PeakSkills.
2. Start the test server.
3. Join with a fishing rod.
4. Stand near real water.

## Checks

1. Fish in water and reel a successful catch.
   - Expected: custom catch message appears.
   - Expected: Fishing XP bar appears.
   - Expected: item spawns near the bobber and travels toward the player.
2. Catch a collection item such as cod, salmon, pufferfish, tropical fish, lily pad,
   ink sac, nautilus shell, or prismarine shard.
   - Expected: matching collection increments.
3. Cast on dry land or reel a loose ground item.
   - Expected: no Fishing XP and no custom loot.
4. Give yourself Fishing level 50 or 99 when testing ability changes.
   - Expected: Fishing ability behavior matches the feature plan.
5. Activate an Axolotl or Dolphin pet when testing pet interactions.
   - Expected: Fishing XP bonus/pet XP behavior still works.

## Regression Notes

- Keep fishing one cast, one reel.
- Verify no duplicate loot is awarded from one bobber.
- Verify old and new fishing drops do not bypass collection or XP expectations.
