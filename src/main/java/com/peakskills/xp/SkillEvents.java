package com.peakskills.xp;

import com.peakskills.collection.CollectionRegistry;
import com.peakskills.collection.CollectionRewardHandler;
import com.peakskills.collection.CollectionTier;
import com.peakskills.combat.CombatDropTracker;
import com.peakskills.gear.GearRequirements;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import com.peakskills.skill.SkillAbilityRegistry;
import com.peakskills.world.PlacedBlocksState;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SkillEvents {

    // Tracks how many ticks each player has been sprinting/swimming
    private static final Map<UUID, Integer> agilityTicks = new HashMap<>();

    // Pre-counted sugar-cane blocks above the broken block (captured in BEFORE, consumed in AFTER)
    private static final Map<UUID, Integer> pendingCaneExtra = new HashMap<>();
    private static final Map<UUID, ColumnExtra> pendingColumnCollectionExtra = new HashMap<>();

    // Player-placed block positions are persisted via PlacedBlocksState (PersistentState).
    // Retrieved per-call from the server instance — always up-to-date and restart-safe.

    public static void register() {
        registerBlockBreak();
        registerToolRestriction();
        registerSlaying();
        registerAgility();
        ServerTickEvents.END_SERVER_TICK.register(server -> CombatDropTracker.tick());
    }

    // -------------------------------------------------------------------------
    // BLOCK BREAKING — Mining, Woodcutting, Excavating, Farming
    // -------------------------------------------------------------------------
    private static void registerBlockBreak() {

        // Count sugar-cane blocks above BEFORE they auto-break, so AFTER can award XP for all of them.
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
            net.minecraft.world.level.block.Block column = state.getBlock();
            if ((column == net.minecraft.world.level.block.Blocks.SUGAR_CANE
                    || column == net.minecraft.world.level.block.Blocks.BAMBOO
                    || column == net.minecraft.world.level.block.Blocks.CACTUS)
                    && player instanceof ServerPlayer sp) {
                int extra = 0;
                BlockPos scan = pos.above();
                while (world.getBlockState(scan).getBlock() == column) {
                    extra++;
                    scan = scan.above();
                }
                if (extra > 0) {
                    if (column == net.minecraft.world.level.block.Blocks.SUGAR_CANE) {
                        pendingCaneExtra.put(sp.getUUID(), extra);
                    }
                    com.peakskills.collection.CollectionType type =
                        column == net.minecraft.world.level.block.Blocks.SUGAR_CANE
                            ? com.peakskills.collection.CollectionType.SUGAR_CANE
                            : column == net.minecraft.world.level.block.Blocks.BAMBOO
                                ? com.peakskills.collection.CollectionType.BAMBOO_WOOD
                                : com.peakskills.collection.CollectionType.CACTUS;
                    pendingColumnCollectionExtra.put(sp.getUUID(), new ColumnExtra(column, type, extra));
                }
            }
            return true; // never cancel
        });

        PlayerBlockBreakEvents.AFTER.register(
            (Level world, Player player, BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
             net.minecraft.world.level.block.entity.BlockEntity be) -> {

                if (!(player instanceof ServerPlayer serverPlayer)) return;
                if (!(world instanceof ServerLevel)) return;

                long xp = blockXp(state);
                if (xp <= 0) return;

                Skill skill = classifyBlock(state);
                if (skill == null) return;

                // ── Player-placed check (persistent across restarts) ──────
                // If a player placed this block, consume the record and skip XP.
                // Works cross-player: if A placed it, B breaking it gets no XP.
                // Farming blocks are fully exempt: crops/herbs record the planted
                // position via BlockItem.place which would wrongly block legitimate
                // harvests. Instead, column-growers (sugar cane, bamboo, cactus) are
                // handled below using a structural base-block check.
                ServerLevel sw = (ServerLevel) world;
                if (skill != Skill.FARMING && PlacedBlocksState.get(sw.getServer()).consumeIfPlaced(pos.asLong())) return;
                // ──────────────────────────────────────────────────────────

                // ── Column-grower base check ──────────────────────────────
                // Sugar cane, bamboo, and cactus placed by the player sit directly
                // on soil/sand. Naturally-grown blocks always have the same block
                // type below them. Skip XP for the player-placed base block only.
                net.minecraft.world.level.block.Block b = state.getBlock();
                if (b == net.minecraft.world.level.block.Blocks.SUGAR_CANE
                        || b == net.minecraft.world.level.block.Blocks.BAMBOO
                        || b == net.minecraft.world.level.block.Blocks.CACTUS) {
                    if (world.getBlockState(pos.below()).getBlock() != b) return; // base block
                }
                // ──────────────────────────────────────────────────────────

                xp = applyBlockAbilityBonus(serverPlayer, skill, xp, world.getRandom());
                XpManager.addXp(serverPlayer, skill, xp);

                // ── Extra sugar-cane blocks that auto-broke above ──────────────
                if (state.getBlock() == net.minecraft.world.level.block.Blocks.SUGAR_CANE) {
                    Integer extra = pendingCaneExtra.remove(serverPlayer.getUUID());
                    if (extra != null && extra > 0) {
                        for (int i = 0; i < extra; i++) {
                            long extraXp = applyBlockAbilityBonus(serverPlayer, Skill.FARMING,
                                blockXp(state), world.getRandom());
                            XpManager.addXp(serverPlayer, Skill.FARMING, extraXp);
                        }
                    }
                }

                ColumnExtra columnExtra = pendingColumnCollectionExtra.remove(serverPlayer.getUUID());
                if (columnExtra != null && columnExtra.block() == state.getBlock()
                        && columnExtra.amount() > 0) {
                    List<CollectionTier> newTiers = PlayerDataManager.get(serverPlayer.getUUID())
                        .getCollections().increment(columnExtra.type(), columnExtra.amount());
                    CollectionRewardHandler.apply(serverPlayer, columnExtra.type(), newTiers,
                        PlayerDataManager.getServer());
                }
            }
        );
    }

    private record ColumnExtra(net.minecraft.world.level.block.Block block,
                               com.peakskills.collection.CollectionType type, int amount) {}

    // -------------------------------------------------------------------------
    // TOOL RESTRICTION — block attack with insufficient skill level
    // -------------------------------------------------------------------------
    private static void registerToolRestriction() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

            net.minecraft.world.item.ItemStack held = player.getMainHandItem();
            GearRequirements.Requirement req = GearRequirements.getRequirement(held.getItem());
            if (req == null) return InteractionResult.PASS;

            int level = PlayerDataManager.get(serverPlayer.getUUID()).getLevel(req.skill());
            if (level < req.level()) {
                serverPlayer.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("Requires ")
                        .withStyle(net.minecraft.ChatFormatting.RED)
                        .append(net.minecraft.network.chat.Component.literal(
                            req.skill().getDisplayName() + " level " + req.level())
                            .withStyle(net.minecraft.ChatFormatting.YELLOW)),
                    true
                );
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    /** Classify a broken block into the correct skill. */
    private static Skill classifyBlock(net.minecraft.world.level.block.state.BlockState state) {
        net.minecraft.world.level.block.Block b = state.getBlock();
        if (b instanceof CropBlock) return Skill.FARMING;
        // Non-CropBlock farming blocks — explicit checks
        if (b == net.minecraft.world.level.block.Blocks.SUGAR_CANE
         || b == net.minecraft.world.level.block.Blocks.PUMPKIN
         || b == net.minecraft.world.level.block.Blocks.MELON
         || b == net.minecraft.world.level.block.Blocks.NETHER_WART
         || b == net.minecraft.world.level.block.Blocks.COCOA
         || b == net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH
         || b == net.minecraft.world.level.block.Blocks.CAVE_VINES
         || b == net.minecraft.world.level.block.Blocks.CAVE_VINES_PLANT
         || b == net.minecraft.world.level.block.Blocks.BAMBOO
         || b == net.minecraft.world.level.block.Blocks.CACTUS
         || b == net.minecraft.world.level.block.Blocks.KELP
         || b == net.minecraft.world.level.block.Blocks.KELP_PLANT
         || b == net.minecraft.world.level.block.Blocks.CHORUS_FLOWER
         || b == net.minecraft.world.level.block.Blocks.CHORUS_PLANT) return Skill.FARMING;
        // Nether stems are NOT in BlockTags.LOGS — explicit check
        if (b == net.minecraft.world.level.block.Blocks.CRIMSON_STEM
         || b == net.minecraft.world.level.block.Blocks.WARPED_STEM
         || b == net.minecraft.world.level.block.Blocks.STRIPPED_CRIMSON_STEM
         || b == net.minecraft.world.level.block.Blocks.STRIPPED_WARPED_STEM) return Skill.WOODCUTTING;
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) return Skill.WOODCUTTING;
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return Skill.EXCAVATING;
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return Skill.MINING;
        return null;
    }

    /** XP per block, derived from rarity + effort required to reach the block. */
    private static long blockXp(net.minecraft.world.level.block.state.BlockState state) {
        net.minecraft.world.level.block.Block b = state.getBlock();

        // ── Farming — differentiated XP by cultivation difficulty ─────────
        // CropBlocks: check specific types first for tuned values
        if (b == net.minecraft.world.level.block.Blocks.WHEAT)
            return ((CropBlock) b).isMaxAge(state) ? 40 : 0;    // cheap seeds, fast
        if (b == net.minecraft.world.level.block.Blocks.CARROTS || b == net.minecraft.world.level.block.Blocks.POTATOES)
            return ((CropBlock) b).isMaxAge(state) ? 42 : 0;
        if (b == net.minecraft.world.level.block.Blocks.BEETROOTS)
            return ((CropBlock) b).isMaxAge(state) ? 50 : 0;    // 4 stages, slower
        if (b instanceof CropBlock crop) return crop.isMaxAge(state) ? 45 : 0; // torchflower etc.
        // Column growers — fast/stackable, low effort
        if (b == net.minecraft.world.level.block.Blocks.SUGAR_CANE)    return 22;
        if (b == net.minecraft.world.level.block.Blocks.BAMBOO)         return 9;
        if (b == net.minecraft.world.level.block.Blocks.CACTUS)         return 15;
        if (b == net.minecraft.world.level.block.Blocks.KELP
         || b == net.minecraft.world.level.block.Blocks.KELP_PLANT)     return 18;
        // One-per-stem plants
        if (b == net.minecraft.world.level.block.Blocks.PUMPKIN)        return 65;
        if (b == net.minecraft.world.level.block.Blocks.MELON)          return 55;
        // Bush/vine harvests — check maturity
        if (b == net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH) {
            int age = state.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE);
            return age >= 2 ? 50 : 0;
        }
        if (b == net.minecraft.world.level.block.Blocks.CAVE_VINES || b == net.minecraft.world.level.block.Blocks.CAVE_VINES_PLANT) {
            boolean berries = state.getValue(net.minecraft.world.level.block.CaveVines.BERRIES);
            return berries ? 55 : 0;
        }
        // Special-location crops — harder to set up
        if (b == net.minecraft.world.level.block.Blocks.NETHER_WART) {
            int age = state.getValue(net.minecraft.world.level.block.NetherWartBlock.AGE);
            return age == 3 ? 80 : 0;
        }
        if (b == net.minecraft.world.level.block.Blocks.COCOA) {
            int age = state.getValue(net.minecraft.world.level.block.CocoaBlock.AGE);
            return age == 2 ? 68 : 0;
        }
        // End farming
        if (b == net.minecraft.world.level.block.Blocks.CHORUS_FLOWER)  return 70;
        if (b == net.minecraft.world.level.block.Blocks.CHORUS_PLANT)   return 60;

        // ── Deepslate ores (checked before tags — tags cover both variants) ─
        if (b == net.minecraft.world.level.block.Blocks.DEEPSLATE_COAL_ORE)     return 97;
        if (b == net.minecraft.world.level.block.Blocks.DEEPSLATE_COPPER_ORE)   return 225;
        if (b == net.minecraft.world.level.block.Blocks.DEEPSLATE_IRON_ORE)     return 174;
        if (b == net.minecraft.world.level.block.Blocks.DEEPSLATE_GOLD_ORE)     return 376;
        if (b == net.minecraft.world.level.block.Blocks.DEEPSLATE_LAPIS_ORE)    return 444;
        if (b == net.minecraft.world.level.block.Blocks.DEEPSLATE_REDSTONE_ORE) return 315;
        if (b == net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE)  return 672;
        if (b == net.minecraft.world.level.block.Blocks.DEEPSLATE_EMERALD_ORE)  return 992;

        // ── Regular ores ──────────────────────────────────────────────────
        if (state.is(BlockTags.COAL_ORES))     return 85;
        if (state.is(BlockTags.COPPER_ORES))   return 196;
        if (state.is(BlockTags.IRON_ORES))     return 152;
        if (state.is(BlockTags.GOLD_ORES))     return 327;
        if (state.is(BlockTags.LAPIS_ORES))    return 386;
        if (state.is(BlockTags.REDSTONE_ORES)) return 274;
        if (state.is(BlockTags.DIAMOND_ORES))  return 585;
        if (state.is(BlockTags.EMERALD_ORES))  return 863;

        // ── Nether special blocks (checked before pickaxe tag fallthrough) ─
        if (b == net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS)         return 1000;
        if (b == net.minecraft.world.level.block.Blocks.NETHER_QUARTZ_ORE)      return 62;
        if (b == net.minecraft.world.level.block.Blocks.NETHER_GOLD_ORE)        return 106;

        // ── Stone / mining ────────────────────────────────────────────────
        if (b == net.minecraft.world.level.block.Blocks.DEEPSLATE
         || b == net.minecraft.world.level.block.Blocks.COBBLED_DEEPSLATE)      return 14;
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE))                  return 11;

        // ── Logs — rarer wood = more XP ───────────────────────────────────
        if (b == net.minecraft.world.level.block.Blocks.DARK_OAK_LOG
         || b == net.minecraft.world.level.block.Blocks.MANGROVE_LOG
         || b == net.minecraft.world.level.block.Blocks.CHERRY_LOG)              return 17;
        if (b == net.minecraft.world.level.block.Blocks.CRIMSON_STEM
         || b == net.minecraft.world.level.block.Blocks.WARPED_STEM
         || b == net.minecraft.world.level.block.Blocks.BAMBOO_BLOCK)            return 20;
        if (state.is(BlockTags.LOGS))                              return 13;
        if (state.is(BlockTags.LEAVES))                            return 3;

        // ── Excavating — softer but accessible materials ─────────────────
        if (b == net.minecraft.world.level.block.Blocks.SOUL_SAND
         || b == net.minecraft.world.level.block.Blocks.SOUL_SOIL)               return 12;
        if (b == net.minecraft.world.level.block.Blocks.CLAY)                    return 11;
        if (b == net.minecraft.world.level.block.Blocks.GRAVEL)                  return 10;
        if (b == net.minecraft.world.level.block.Blocks.SAND
         || b == net.minecraft.world.level.block.Blocks.RED_SAND)                return 9;
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL))                   return 8;

        return 0;
    }

    // -------------------------------------------------------------------------
    // ENTITY KILLS — Slaying, Ranged
    // -------------------------------------------------------------------------
    private static void registerSlaying() {
        ServerLivingEntityEvents.AFTER_DEATH.register(
            (LivingEntity entity, DamageSource source) -> {
                if (!(source.getEntity() instanceof ServerPlayer player)) return;
                if (entity instanceof Player) return; // no PvP XP

                long xp = mobXp(entity);
                if (xp <= 0) return;

                net.minecraft.world.item.ItemStack held = player.getMainHandItem();
                boolean isRanged = held.getItem() instanceof BowItem
                    || held.getItem() instanceof CrossbowItem
                    || held.getItem() instanceof TridentItem;

                Skill combatSkill = isRanged ? Skill.RANGED : Skill.SLAYING;
                // Ranged gets 50% of melee XP — distance reduces engagement risk
                if (isRanged) xp = (long)(xp * 0.5);
                xp = applyFlatAbilityBonus(player, combatSkill, xp);
                XpManager.addXp(player, combatSkill, xp);

                CombatDropTracker.recordKill(entity.getUUID(), player.getUUID());

                // Tag item entities that already spawned from this mob's loot so
                // ItemPickupMixin can credit the killer. AFTER_DEATH fires after
                // LivingEntity.drop(), so items are already present in the world.
                if (entity.level() instanceof ServerLevel sw) {
                    double x = entity.getX(), y = entity.getY(), z = entity.getZ();
                    net.minecraft.world.phys.AABB box =
                        new net.minecraft.world.phys.AABB(x - 6, y - 4, z - 6, x + 6, y + 4, z + 6);
                    sw.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box,
                        item -> CollectionRegistry.fromCombatDrop(item.getItem().getItem()).isPresent()
                    ).forEach(item -> CombatDropTracker.tagItemEntity(item.getUUID(), player.getUUID()));
                }
            }
        );
    }

    /** XP based on entity max HP — tougher mobs give more. Bosses have flat overrides. */
    private static long mobXp(LivingEntity entity) {
        // Boss overrides — flat values so they feel rewarding without being trivially farmable
        if (entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss)             return 9_000;
        if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) return 9_000;
        if (entity instanceof net.minecraft.world.entity.monster.ElderGuardian)        return 9_000;
        if (entity instanceof net.minecraft.world.entity.monster.warden.Warden)               return 12_000;

        float maxHp = entity.getMaxHealth();
        return Math.max(8, Math.round(maxHp * 4.74f));
    }

    // -------------------------------------------------------------------------
    // AGILITY — sprinting and swimming
    // -------------------------------------------------------------------------
    private static void registerAgility() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                boolean active = player.isSprinting() || player.isSwimming();
                if (!active) {
                    agilityTicks.remove(player.getUUID());
                    continue;
                }
                int ticks = agilityTicks.merge(player.getUUID(), 1, Integer::sum);
                if (ticks % 40 == 0) {
                    XpManager.addXp(player, Skill.AGILITY, 22);
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // ABILITY BONUS HELPERS
    // -------------------------------------------------------------------------

    /** Chance-based double XP for Mining/Woodcutting/Excavating/Farming. */
    private static long applyBlockAbilityBonus(ServerPlayer player, Skill skill, long xp,
                                               net.minecraft.util.RandomSource rng) {
        int level = PlayerDataManager.get(player.getUUID()).getLevel(skill);
        double chance = SkillAbilityRegistry.getDoubleXpChance(skill, level);
        if (chance > 0 && rng.nextDouble() < chance) return xp * 2;
        double mult = SkillAbilityRegistry.getFlatXpMultiplier(skill, level);
        return mult != 1.0 ? (long)(xp * mult) : xp;
    }

    /** Flat multiplier for Slaying, Ranged, and other non-chance skills. */
    private static long applyFlatAbilityBonus(ServerPlayer player, Skill skill, long xp) {
        int level = PlayerDataManager.get(player.getUUID()).getLevel(skill);
        double mult = SkillAbilityRegistry.getFlatXpMultiplier(skill, level);
        return (long)(xp * mult);
    }

}
