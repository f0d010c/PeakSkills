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

## PeakGear integration

When PeakGear is present, Watersense reads bounded vanilla `CUSTOM_DATA` from the
held rod and carried accessories. It supports two rank I-X traits per rod, bait
charges, rod-specific bonuses, and the following trait effects:

| Trait | Effect per rank |
| --- | --- |
| Double Hook | 4/7/10/13/16/19/22/25/28/32% chance to double the complete catch |
| Treasure Hunter | +6% treasure weight |
| Relic Seeker | +5% relic weight |
| Scholar | +4% Fishing XP |
| Swift Reel | -4% bite time |
| Deepwater | +5% rare deep-water weight |
| Stormcaller | +6% rain bonus |
| Biome Specialist | +5% effective biome bonus |
| Mood Reader | +5% non-calm mood bonus |
| Journalist | +8% undiscovered-entry weight |

Swift Reel and rod bonuses share a 55% bite-time reduction cap. Double Hook applies
to normal loot at every rarity, including Totems and Nether Stars. It does not clone
the one-time Ocean's Memory journal reward. Future boss/creature drops and content
containers must also remain excluded.

Every Watersense-only material has a stable, validated ID inside server-owned custom
data. Recipes never identify a custom material from its vanilla carrier, display
name, or lore, so ordinary vanilla items cannot impersonate it. The optional
resource pack gives those carriers unique visuals; without it, all mechanics and
identities remain correct and the items use their vanilla fallback appearance.

## Module boundary

PeakSkills owns this progression engine, journal, custom catch identity, and loot
selection. PeakGear owns rods, random traits, fusion, accessories, bait, recipes,
and their GUIs. Sea-creature encounters remain a later gameplay phase; Creature Bait
is reserved for that phase and does not currently change normal loot.
