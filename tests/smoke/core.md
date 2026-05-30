# Core Smoke Test

Run after changes touching commands, player data, GUI, stats, pets, or releases.

## Setup

1. Build PeakSkills.
2. Deploy exactly one `peakskills-*.jar` to the test server `mods` folder.
3. Start the server and join with an operator account.

## Checks

1. Run `/skills`.
   - Expected: menu opens without disconnecting or logging errors.
2. Run `/profile`.
   - Expected: profile opens and total level/stat values display.
3. Run `/collections`.
   - Expected: collections menu opens.
4. Run `/pets`.
   - Expected: pet menu opens.
5. Run `/settings`.
   - Expected: settings menu opens and toggles refresh in place.
6. Run `/skilltop`.
   - Expected: leaderboard command responds.
7. Restart the server.
   - Expected: player data loads and no save/load errors appear.

## Safety Checks

1. Join as a non-operator if possible.
2. Try admin subcommands such as `/skills addxp`.
   - Expected: command is denied.
3. Confirm the server `mods` folder has only one PeakSkills jar.
