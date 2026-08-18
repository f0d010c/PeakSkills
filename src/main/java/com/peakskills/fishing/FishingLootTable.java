package com.peakskills.fishing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/** World-reactive Watersense loot engine. Level and depth are hard gates; Luck shifts weights. */
public final class FishingLootTable {
    private static final List<Entry> POOL = new ArrayList<>();

    static {
        add("cod", 1, FishingDepth.SHALLOW, 200, Items.COD, 1, 3,
            "Raw Fish", Rarity.COMMON, FishingOutcomeCategory.FISH);
        add("salmon", 1, FishingDepth.SHALLOW, 180, Items.SALMON, 1, 2,
            "Raw Salmon", Rarity.COMMON, FishingOutcomeCategory.FISH);
        add("lily_pad", 1, FishingDepth.SHALLOW, 120, Items.LILY_PAD, 1, 3,
            "Lily Pad", Rarity.COMMON, FishingOutcomeCategory.MATERIAL);
        add("ink_sac", 1, FishingDepth.SHALLOW, 100, Items.INK_SAC, 1, 4,
            "Ink Sac", Rarity.COMMON, FishingOutcomeCategory.MATERIAL);
        add("seagrass", 1, FishingDepth.SHALLOW, 90, Items.SEAGRASS, 1, 3,
            "Seagrass", Rarity.COMMON, FishingOutcomeCategory.MATERIAL);
        addCustom("driftwood", 1, FishingDepth.SHALLOW, 50, Items.STICK, 2, 5,
            "Driftwood", Rarity.COMMON, FishingOutcomeCategory.MATERIAL, 1001);

        add("tropical_fish", 10, FishingDepth.RIVERBED, 80, Items.TROPICAL_FISH, 1, 1,
            "Tropical Fish", Rarity.UNCOMMON, FishingOutcomeCategory.FISH);
        add("pufferfish", 10, FishingDepth.RIVERBED, 70, Items.PUFFERFISH, 1, 1,
            "Pufferfish", Rarity.UNCOMMON, FishingOutcomeCategory.FISH);
        addCustom("fish_bones", 10, FishingDepth.SHALLOW, 55, Items.BONE, 1, 3,
            "Fish Bones", Rarity.UNCOMMON, FishingOutcomeCategory.MATERIAL, 1003);
        addCustom("tangled_line", 15, FishingDepth.RIVERBED, 50, Items.STRING, 1, 4,
            "Tangled Line", Rarity.UNCOMMON, FishingOutcomeCategory.MATERIAL, 1002);
        addCustom("waterlogged_hide", 20, FishingDepth.RIVERBED, 45, Items.LEATHER, 1, 2,
            "Waterlogged Hide", Rarity.UNCOMMON, FishingOutcomeCategory.MATERIAL, 1004);
        addCustom("river_clay", 12, FishingDepth.RIVERBED, 50, Items.CLAY_BALL, 2, 5,
            "River Clay", Rarity.UNCOMMON, FishingOutcomeCategory.MATERIAL, 1005);
        addCustom("tarnished_buckle", 15, FishingDepth.RIVERBED, 35, Items.IRON_NUGGET, 1, 3,
            "Tarnished Buckle", Rarity.UNCOMMON, FishingOutcomeCategory.MATERIAL, 1006);
        addCustom("murk_sac", 18, FishingDepth.RIVERBED, 25, Items.SLIME_BALL, 1, 2,
            "Murk Sac", Rarity.UNCOMMON, FishingOutcomeCategory.MATERIAL, 1007);
        addCustom("river_pearl", 20, FishingDepth.RIVERBED, 10, Items.ENDER_PEARL, 1, 1,
            "River Pearl", Rarity.RARE, FishingOutcomeCategory.TREASURE, 1008);

        add("nautilus_shell", 25, FishingDepth.DEEP_WATER, 35, Items.NAUTILUS_SHELL, 1, 1,
            "Nautilus Shell", Rarity.RARE, FishingOutcomeCategory.TREASURE);
        add("prismarine_shard", 25, FishingDepth.DEEP_WATER, 30, Items.PRISMARINE_SHARD, 2, 5,
            "Prismarine Shard", Rarity.RARE, FishingOutcomeCategory.MATERIAL);
        addCustom("sunken_scrap", 30, FishingDepth.DEEP_WATER, 25, Items.IRON_INGOT, 1, 3,
            "Sunken Scrap", Rarity.RARE, FishingOutcomeCategory.MATERIAL, 1009);
        addCustom("sea_crystal", 35, FishingDepth.DEEP_WATER, 22, Items.PRISMARINE_CRYSTALS, 1, 3,
            "Sea Crystal", Rarity.RARE, FishingOutcomeCategory.TREASURE, 1010);
        addCustom("drowned_coin", 40, FishingDepth.DEEP_WATER, 18, Items.GOLD_NUGGET, 2, 6,
            "Drowned Coin", Rarity.RARE, FishingOutcomeCategory.RELIC, 1011);
        addCustom("broken_compass", 38, FishingDepth.DEEP_WATER, 12, Items.COMPASS, 1, 1,
            "Broken Compass", Rarity.RARE, FishingOutcomeCategory.RELIC, 1012);
        addCustom("sealed_bottle", 40, FishingDepth.DEEP_WATER, 8, Items.GLASS_BOTTLE, 1, 1,
            "Sealed Bottle", Rarity.RARE, FishingOutcomeCategory.RELIC, 1013);

        addCustom("sea_diamond", 50, FishingDepth.DEEP_WATER, 10, Items.DIAMOND, 1, 1,
            "Sea Diamond", Rarity.EPIC, FishingOutcomeCategory.TREASURE, 1014);
        addCustom("ancient_scale", 55, FishingDepth.ABYSSAL, 8, Items.TURTLE_SCUTE, 1, 1,
            "Ancient Scale", Rarity.EPIC, FishingOutcomeCategory.RELIC, 1015);
        addCustom("abyss_ink", 60, FishingDepth.ABYSSAL, 7, Items.GLOW_INK_SAC, 1, 2,
            "Abyss Ink", Rarity.EPIC, FishingOutcomeCategory.RELIC, 1016);
        addCustom("pearl_shard", 65, FishingDepth.ABYSSAL, 5, Items.PRISMARINE_CRYSTALS, 1, 1,
            "Pearl Shard", Rarity.EPIC, FishingOutcomeCategory.RELIC, 1017);
        addCustom("abyssal_membrane", 65, FishingDepth.ABYSSAL, 5, Items.PHANTOM_MEMBRANE, 1, 1,
            "Abyssal Membrane", Rarity.EPIC, FishingOutcomeCategory.RELIC, 1018);
        addCustom("rusted_hook", 70, FishingDepth.ABYSSAL, 4, Items.TRIPWIRE_HOOK, 1, 1,
            "Rusted Hook", Rarity.EPIC, FishingOutcomeCategory.RELIC, 1019);

        addCustom("ancient_trident", 75, FishingDepth.ABYSSAL, 3, Items.TRIDENT, 1, 1,
            "Ancient Trident", Rarity.LEGENDARY, FishingOutcomeCategory.TREASURE, 1020);
        addCustom("heart_fragment", 75, FishingDepth.ANCIENT, 4, Items.HEART_OF_THE_SEA, 1, 1,
            "Heart Fragment", Rarity.LEGENDARY, FishingOutcomeCategory.RELIC, 1021);
        addCustom("totem_of_the_deep", 80, FishingDepth.ANCIENT, 2, Items.TOTEM_OF_UNDYING, 1, 1,
            "Totem of the Deep", Rarity.LEGENDARY, FishingOutcomeCategory.TREASURE, 1022);
        addCustom("abyssal_star", 90, FishingDepth.ANCIENT, 1, Items.NETHER_STAR, 1, 1,
            "Abyssal Star", Rarity.LEGENDARY, FishingOutcomeCategory.RELIC, 1023);
        addCustom("oceans_memory", 99, FishingDepth.ANCIENT, 1, Items.WRITTEN_BOOK, 1, 1,
            "Ocean's Memory", Rarity.LEGENDARY, FishingOutcomeCategory.RELIC, 1024);
    }

    public static RollResult roll(FishingContext context, RandomSource random) {
        return roll(context, FishingModifiers.NONE, random);
    }

    public static RollResult roll(FishingContext context, FishingModifiers modifiers, RandomSource random) {
        List<WeightedEntry> eligible = new ArrayList<>();
        long totalWeight = 0;
        for (Entry entry : POOL) {
            if (entry.id.equals("oceans_memory") && modifiers.discoveries().contains(entry.id)) continue;
            if (context.fishingLevel() < entry.minLevel || context.depth().ordinal() < entry.minDepth.ordinal()) {
                continue;
            }
            int weight = adjustedWeight(entry, context, modifiers);
            if (weight <= 0) continue;
            eligible.add(new WeightedEntry(entry, weight));
            totalWeight += weight;
        }
        if (eligible.isEmpty() || totalWeight <= 0) return null;

        long roll = Math.floorMod(random.nextLong(), totalWeight);
        long cursor = 0;
        for (WeightedEntry weighted : eligible) {
            cursor += weighted.weight;
            if (roll < cursor) {
                Entry entry = weighted.entry;
                int count = entry.minCount == entry.maxCount ? entry.minCount
                    : entry.minCount + random.nextInt(entry.maxCount - entry.minCount + 1);
                if (context.mood() == FishingMood.FEEDING_FRENZY
                    && entry.category == FishingOutcomeCategory.FISH && count < entry.item.getDefaultMaxStackSize()) {
                    count++;
                }
                if (entry.category == FishingOutcomeCategory.MATERIAL
                        && random.nextDouble() < modifiers.satchelChance()
                        && count < entry.item.getDefaultMaxStackSize()) count++;
                int copies = !entry.id.equals("oceans_memory")
                    && random.nextDouble() < modifiers.doubleHookChance() ? 2 : 1;
                return new RollResult(entry.id, buildStack(entry, count), entry.rarity.xp,
                    entry.rarity, entry.category, copies);
            }
        }
        return null;
    }

    /** Compatibility overload retained for tests and external integrations. */
    public static RollResult roll(int fishingLevel, double luckBonus, RandomSource random) {
        return roll(new FishingContext(fishingLevel, luckBonus, 0, FishingDepth.ANCIENT,
            FishingMood.CALM_WATERS, "minecraft:ocean", false, false, 32, 75), random);
    }

    public static List<EntryView> entries() {
        return POOL.stream().map(entry -> new EntryView(entry.id, entry.displayName, entry.item,
            entry.rarity, entry.category, entry.minLevel, entry.minDepth)).toList();
    }

    public static ItemStack preview(String id) {
        return POOL.stream().filter(entry -> entry.id.equals(id)).findFirst()
            .map(entry -> buildStack(entry, 1)).orElse(ItemStack.EMPTY);
    }

    private static int adjustedWeight(Entry entry, FishingContext context, FishingModifiers modifiers) {
        double multiplier = 1.0;
        if (entry.rarity.ordinal() >= Rarity.RARE.ordinal()) {
            multiplier *= 1.0 + context.rareWeightBonus();
        }
        if (context.raining() && entry.category == FishingOutcomeCategory.FISH) multiplier *= 1.20;
        if (context.night() && entry.category == FishingOutcomeCategory.RELIC) multiplier *= 1.25;
        double biomeMultiplier = biomeMultiplier(entry, context.biome());
        multiplier *= biomeMultiplier;
        multiplier *= switch (context.mood()) {
            case FEEDING_FRENZY -> entry.category == FishingOutcomeCategory.FISH ? 1.75 : 0.85;
            case TREASURE_RIPPLE -> entry.category == FishingOutcomeCategory.TREASURE
                || entry.category == FishingOutcomeCategory.RELIC ? 2.0 : 0.90;
            case MURKY_WAKE -> entry.category == FishingOutcomeCategory.MATERIAL ? 1.60
                : entry.category == FishingOutcomeCategory.RELIC ? 1.20 : 0.90;
            case ABYSS_STIR -> entry.rarity.ordinal() >= Rarity.EPIC.ordinal() ? 2.25 : 0.80;
            case CALM_WATERS -> 1.0;
        };
        multiplier *= modifiers.weightMultiplier(entry.id, entry.category, entry.rarity,
            context, biomeMultiplier);
        return Math.max(1, (int) Math.round(entry.weight * multiplier * 100.0));
    }

    private static double biomeMultiplier(Entry entry, String biome) {
        String id = biome.toLowerCase(java.util.Locale.ROOT);
        if (id.contains("warm_ocean") || id.contains("lukewarm_ocean")) {
            if (entry.id.equals("tropical_fish")) return 1.80;
            if (entry.id.equals("pufferfish")) return 1.40;
        }
        if (id.contains("frozen") || id.contains("cold_ocean")) {
            if (entry.id.equals("salmon")) return 1.35;
            if (entry.id.equals("tropical_fish")) return 0.45;
        }
        if (id.contains("swamp") || id.contains("mangrove")) {
            return entry.category == FishingOutcomeCategory.MATERIAL
                || entry.category == FishingOutcomeCategory.RELIC ? 1.25 : 0.90;
        }
        if (id.contains("river")) {
            return entry.category == FishingOutcomeCategory.FISH ? 1.15
                : entry.category == FishingOutcomeCategory.TREASURE ? 0.85 : 1.0;
        }
        if (id.contains("ocean")) {
            return entry.category == FishingOutcomeCategory.TREASURE
                || entry.category == FishingOutcomeCategory.RELIC ? 1.15 : 1.0;
        }
        return 1.0;
    }

    public enum Rarity {
        COMMON(ChatFormatting.WHITE, "COMMON", 30),
        UNCOMMON(ChatFormatting.GREEN, "UNCOMMON", 80),
        RARE(ChatFormatting.AQUA, "RARE", 194),
        EPIC(ChatFormatting.DARK_PURPLE, "EPIC", 477),
        LEGENDARY(ChatFormatting.GOLD, "LEGENDARY", 1283);

        public final ChatFormatting color;
        public final String label;
        public final long xp;

        Rarity(ChatFormatting color, String label, long xp) {
            this.color = color;
            this.label = label;
            this.xp = xp;
        }
    }

    public record RollResult(String entryId, ItemStack stack, long xp, Rarity rarity,
                             FishingOutcomeCategory category, int copies) {
        public RollResult(String entryId, ItemStack stack, long xp, Rarity rarity,
                          FishingOutcomeCategory category) {
            this(entryId, stack, xp, rarity, category, 1);
        }

        public RollResult {
            copies = Math.max(1, Math.min(2, copies));
        }

        public int totalQuantity() {
            return Math.min(128, stack.getCount() * copies);
        }
    }

    public record EntryView(String id, String displayName, Item icon, Rarity rarity,
                            FishingOutcomeCategory category, int minLevel, FishingDepth minDepth) {}

    private record Entry(String id, int minLevel, FishingDepth minDepth, int weight, Item item,
                         int minCount, int maxCount, String displayName, Rarity rarity,
                         FishingOutcomeCategory category, int modelId) {}
    private record WeightedEntry(Entry entry, int weight) {}

    private static void add(String id, int minLevel, FishingDepth minDepth, int weight, Item item,
                            int minCount, int maxCount, String displayName, Rarity rarity,
                            FishingOutcomeCategory category) {
        POOL.add(new Entry(id, minLevel, minDepth, weight, item, minCount, maxCount,
            displayName, rarity, category, 0));
    }

    private static void addCustom(String id, int minLevel, FishingDepth minDepth, int weight, Item item,
                                  int minCount, int maxCount, String displayName, Rarity rarity,
                                  FishingOutcomeCategory category, int modelId) {
        POOL.add(new Entry(id, minLevel, minDepth, weight, item, minCount, maxCount,
            displayName, rarity, category, modelId));
    }

    private static ItemStack buildStack(Entry entry, int count) {
        ItemStack stack = new ItemStack(entry.item, count);
        if (entry.modelId > 0) WatersenseItemData.mark(stack, entry.id, entry.modelId);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(entry.displayName).withStyle(entry.rarity.color));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal(entry.rarity.label).withStyle(entry.rarity.color, ChatFormatting.BOLD),
            Component.literal(entry.category.name().replace('_', ' ')).withStyle(ChatFormatting.DARK_GRAY),
            Component.literal("Requires " + entry.minDepth.displayName + " · Fishing " + entry.minLevel)
                .withStyle(ChatFormatting.GRAY)
        )));
        return stack;
    }

    private FishingLootTable() {}
}
