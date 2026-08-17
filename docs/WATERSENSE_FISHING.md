# Watersense Fishing

Watersense is PeakSkills' server-side fishing progression layer for Minecraft
26.1.2. It keeps the vanilla one-cast, one-reel interaction and requires no client
mod.

## Catch context

Every successful bite inspects at most 75 nearby water cells and 32 blocks below the
bobber. The resulting context includes:

- Shallow, Riverbed, Deep Water, Abyssal, or Ancient depth
- biome, rain, and time of day
- a regional ten-minute mood: Calm Waters, Feeding Frenzy, Treasure Ripple, Murky
  Wake, or Abyss Stir
- Fishing level, the bounded Luck stat contribution, and Luck of the Sea

Fishing level and depth are hard eligibility gates. Luck only changes weights among
already eligible results. This prevents shallow one-block pools and extreme stats
from unlocking endgame loot.

## Progression and compatibility

The catch pool contains fish, materials, treasures, and relics across five rarities.
Rarity determines Fishing XP. A Fishing XP multiplier of zero awards no skill XP.
Vanilla hooked-entity behavior, rod durability, the fishing advancement trigger,
`fish_caught` statistic, and experience orbs are retained.

The permanent Watersense Journal records catches, landed item quantity, category
counts, discovered loot, depths, moods, and biomes. Open it with `/fishing`,
`/fishingjournal`, or the shortcut in `/skills`.

Collections always count legitimate item quantity exactly once. Fishing uses the
caught stack size; block collections observe Minecraft's already-generated drop list
so Fortune and Silk Touch quantities are not rerolled. Column growers include the
upper blocks broken by the same action.

## Module boundary

PeakSkills owns this progression engine and journal. Custom rods, accessories, bait,
and their abilities belong to PeakGear and should integrate through an optional
compatibility boundary. Sea-creature encounters are a later gameplay phase and are
not part of this foundation.
