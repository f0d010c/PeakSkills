package com.peakskills.xp;

import com.peakskills.collection.CollectionRegistry;
import com.peakskills.collection.CollectionRewardHandler;
import com.peakskills.collection.CollectionTier;
import com.peakskills.combat.CombatDropTracker;
import com.peakskills.gear.GearRequirements;
import com.peakskills.pet.PetEggHandler;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import com.peakskills.skill.SkillAbilityRegistry;
import com.peakskills.world.PlacedBlocksState;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SkillEvents {

    // Tracks how many ticks each player has been sprinting/swimming
    private static final Map<UUID, Integer> agilityTicks = new HashMap<>();

    // Pre-counted sugar-cane blocks above the broken block (captured in BEFORE, consumed in AFTER)
    private static final Map<UUID, Integer> pendingCaneExtra = new HashMap<>();

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
            if (state.getBlock() == net.minecraft.block.Blocks.SUGAR_CANE
                    && player instanceof ServerPlayerEntity sp) {
                int extra = 0;
                BlockPos scan = pos.up();
                while (world.getBlockState(scan).getBlock() == net.minecraft.block.Blocks.SUGAR_CANE) {
                    extra++;
                    scan = scan.up();
                }
                if (extra > 0) pendingCaneExtra.put(sp.getUuid(), extra);
            }
            return true; // never cancel
        });

        PlayerBlockBreakEvents.AFTER.register(
            (World world, PlayerEntity player, BlockPos pos, net.minecraft.block.BlockState state,
             net.minecraft.block.entity.BlockEntity be) -> {

                if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
                if (!(world instanceof ServerWorld)) return;

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
                ServerWorld sw = (ServerWorld) world;
                if (skill != Skill.FARMING && PlacedBlocksState.get(sw.getServer()).consumeIfPlaced(pos.asLong())) return;
                // ──────────────────────────────────────────────────────────

                // ── Column-grower base check ──────────────────────────────
                // Sugar cane, bamboo, and cactus placed by the player sit directly
                // on soil/sand. Naturally-grown blocks always have the same block
                // type below them. Skip XP for the player-placed base block only.
                net.minecraft.block.Block b = state.getBlock();
                if (b == net.minecraft.block.Blocks.SUGAR_CANE
                        || b == net.minecraft.block.Blocks.BAMBOO
                        || b == net.minecraft.block.Blocks.CACTUS) {
                    if (world.getBlockState(pos.down()).getBlock() != b) return; // base block
                }
                // ──────────────────────────────────────────────────────────

                xp = applyBlockAbilityBonus(serverPlayer, skill, xp, world.getRandom());
                XpManager.addXp(serverPlayer, skill, xp);

                // ── Extra sugar-cane blocks that auto-broke above ──────────────
                if (state.getBlock() == net.minecraft.block.Blocks.SUGAR_CANE) {
                    Integer extra = pendingCaneExtra.remove(serverPlayer.getUuid());
                    if (extra != null && extra > 0) {
                        for (int i = 0; i < extra; i++) {
                            long extraXp = applyBlockAbilityBonus(serverPlayer, Skill.FARMING,
                                blockXp(state), world.getRandom());
                            XpManager.addXp(serverPlayer, Skill.FARMING, extraXp);
                        }
                    }
                }

                // ── Collections ───────────────────────────────────────────────
                CollectionRegistry.fromBlock(state).ifPresent(colType -> {
                    List<CollectionTier> newTiers = PlayerDataManager.get(serverPlayer.getUuid())
                        .getCollections().increment(colType, 1);
                    CollectionRewardHandler.apply(serverPlayer, colType, newTiers, PlayerDataManager.getServer());
                });
            }
        );
    }

    // -------------------------------------------------------------------------
    // TOOL RESTRICTION — block attack with insufficient skill level
    // -------------------------------------------------------------------------
    private static void registerToolRestriction() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

            net.minecraft.item.ItemStack held = player.getMainHandStack();
            GearRequirements.Requirement req = GearRequirements.getRequirement(held.getItem());
            if (req == null) return ActionResult.PASS;

            int level = PlayerDataManager.get(serverPlayer.getUuid()).getLevel(req.skill());
            if (level < req.level()) {
                serverPlayer.sendMessage(
                    net.minecraft.text.Text.literal("Requires ")
                        .formatted(net.minecraft.util.Formatting.RED)
                        .append(net.minecraft.text.Text.literal(
                            req.skill().getDisplayName() + " level " + req.level())
                            .formatted(net.minecraft.util.Formatting.YELLOW)),
                    true
                );
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }

    /** Classify a broken block into the correct skill. */
    private static Skill classifyBlock(net.minecraft.block.BlockState state) {
        net.minecraft.block.Block b = state.getBlock();
        if (b instanceof CropBlock) return Skill.FARMING;
        // Non-CropBlock farming blocks — explicit checks
        if (b == net.minecraft.block.Blocks.SUGAR_CANE
         || b == net.minecraft.block.Blocks.PUMPKIN
         || b == net.minecraft.block.Blocks.MELON
         || b == net.minecraft.block.Blocks.NETHER_WART
         || b == net.minecraft.block.Blocks.COCOA
         || b == net.minecraft.block.Blocks.SWEET_BERRY_BUSH
         || b == net.minecraft.block.Blocks.CAVE_VINES
         || b == net.minecraft.block.Blocks.CAVE_VINES_PLANT
         || b == net.minecraft.block.Blocks.BAMBOO
         || b == net.minecraft.block.Blocks.CACTUS
         || b == net.minecraft.block.Blocks.KELP
         || b == net.minecraft.block.Blocks.KELP_PLANT
         || b == net.minecraft.block.Blocks.CHORUS_FLOWER
         || b == net.minecraft.block.Blocks.CHORUS_PLANT) return Skill.FARMING;
        // Nether stems are NOT in BlockTags.LOGS — explicit check
        if (b == net.minecraft.block.Blocks.CRIMSON_STEM
         || b == net.minecraft.block.Blocks.WARPED_STEM
         || b == net.minecraft.block.Blocks.STRIPPED_CRIMSON_STEM
         || b == net.minecraft.block.Blocks.STRIPPED_WARPED_STEM) return Skill.WOODCUTTING;
        if (state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LEAVES)) return Skill.WOODCUTTING;
        if (state.isIn(BlockTags.SHOVEL_MINEABLE)) return Skill.EXCAVATING;
        if (state.isIn(BlockTags.PICKAXE_MINEABLE)) return Skill.MINING;
        return null;
    }

    /** XP per block, derived from rarity + effort required to reach the block. */
    private static long blockXp(net.minecraft.block.BlockState state) {
        net.minecraft.block.Block b = state.getBlock();

        // ── Farming — differentiated XP by cultivation difficulty ─────────
        // CropBlocks: check specific types first for tuned values
        if (b == net.minecraft.block.Blocks.WHEAT)
            return ((CropBlock) b).isMature(state) ? 40 : 0;    // cheap seeds, fast
        if (b == net.minecraft.block.Blocks.CARROTS || b == net.minecraft.block.Blocks.POTATOES)
            return ((CropBlock) b).isMature(state) ? 42 : 0;
        if (b == net.minecraft.block.Blocks.BEETROOTS)
            return ((CropBlock) b).isMature(state) ? 50 : 0;    // 4 stages, slower
        if (b instanceof CropBlock crop) return crop.isMature(state) ? 45 : 0; // torchflower etc.
        // Column growers — fast/stackable, low effort
        if (b == net.minecraft.block.Blocks.SUGAR_CANE)    return 22;
        if (b == net.minecraft.block.Blocks.BAMBOO)         return 9;
        if (b == net.minecraft.block.Blocks.CACTUS)         return 15;
        if (b == net.minecraft.block.Blocks.KELP
         || b == net.minecraft.block.Blocks.KELP_PLANT)     return 18;
        // One-per-stem plants
        if (b == net.minecraft.block.Blocks.PUMPKIN)        return 65;
        if (b == net.minecraft.block.Blocks.MELON)          return 55;
        // Bush/vine harvests — check maturity
        if (b == net.minecraft.block.Blocks.SWEET_BERRY_BUSH) {
            int age = state.get(net.minecraft.block.SweetBerryBushBlock.AGE);
            return age >= 2 ? 50 : 0;
        }
        if (b == net.minecraft.block.Blocks.CAVE_VINES || b == net.minecraft.block.Blocks.CAVE_VINES_PLANT) {
            boolean berries = state.get(net.minecraft.block.CaveVines.BERRIES);
            return berries ? 55 : 0;
        }
        // Special-location crops — harder to set up
        if (b == net.minecraft.block.Blocks.NETHER_WART) {
            int age = state.get(net.minecraft.block.NetherWartBlock.AGE);
            return age == 3 ? 80 : 0;
        }
        if (b == net.minecraft.block.Blocks.COCOA) {
            int age = state.get(net.minecraft.block.CocoaBlock.AGE);
            return age == 2 ? 68 : 0;
        }
        // End farming
        if (b == net.minecraft.block.Blocks.CHORUS_FLOWER)  return 70;
        if (b == net.minecraft.block.Blocks.CHORUS_PLANT)   return 60;

        // ── Deepslate ores (checked before tags — tags cover both variants) ─
        if (b == net.minecraft.block.Blocks.DEEPSLATE_COAL_ORE)     return 97;
        if (b == net.minecraft.block.Blocks.DEEPSLATE_COPPER_ORE)   return 225;
        if (b == net.minecraft.block.Blocks.DEEPSLATE_IRON_ORE)     return 174;
        if (b == net.minecraft.block.Blocks.DEEPSLATE_GOLD_ORE)     return 376;
        if (b == net.minecraft.block.Blocks.DEEPSLATE_LAPIS_ORE)    return 444;
        if (b == net.minecraft.block.Blocks.DEEPSLATE_REDSTONE_ORE) return 315;
        if (b == net.minecraft.block.Blocks.DEEPSLATE_DIAMOND_ORE)  return 672;
        if (b == net.minecraft.block.Blocks.DEEPSLATE_EMERALD_ORE)  return 992;

        // ── Regular ores ──────────────────────────────────────────────────
        if (state.isIn(BlockTags.COAL_ORES))     return 85;
        if (state.isIn(BlockTags.COPPER_ORES))   return 196;
        if (state.isIn(BlockTags.IRON_ORES))     return 152;
        if (state.isIn(BlockTags.GOLD_ORES))     return 327;
        if (state.isIn(BlockTags.LAPIS_ORES))    return 386;
        if (state.isIn(BlockTags.REDSTONE_ORES)) return 274;
        if (state.isIn(BlockTags.DIAMOND_ORES))  return 585;
        if (state.isIn(BlockTags.EMERALD_ORES))  return 863;

        // ── Nether special blocks (checked before pickaxe tag fallthrough) ─
        if (b == net.minecraft.block.Blocks.ANCIENT_DEBRIS)         return 1000;
        if (b == net.minecraft.block.Blocks.NETHER_QUARTZ_ORE)      return 62;
        if (b == net.minecraft.block.Blocks.NETHER_GOLD_ORE)        return 106;

        // ── Stone / mining ────────────────────────────────────────────────
        if (b == net.minecraft.block.Blocks.DEEPSLATE
         || b == net.minecraft.block.Blocks.COBBLED_DEEPSLATE)      return 14;
        if (state.isIn(BlockTags.PICKAXE_MINEABLE))                  return 11;

        // ── Logs — rarer wood = more XP ───────────────────────────────────
        if (b == net.minecraft.block.Blocks.DARK_OAK_LOG
         || b == net.minecraft.block.Blocks.MANGROVE_LOG
         || b == net.minecraft.block.Blocks.CHERRY_LOG)              return 17;
        if (b == net.minecraft.block.Blocks.CRIMSON_STEM
         || b == net.minecraft.block.Blocks.WARPED_STEM
         || b == net.minecraft.block.Blocks.BAMBOO_BLOCK)            return 20;
        if (state.isIn(BlockTags.LOGS))                              return 13;
        if (state.isIn(BlockTags.LEAVES))                            return 3;

        // ── Excavating — softer but accessible materials ─────────────────
        if (b == net.minecraft.block.Blocks.SOUL_SAND
         || b == net.minecraft.block.Blocks.SOUL_SOIL)               return 12;
        if (b == net.minecraft.block.Blocks.CLAY)                    return 11;
        if (b == net.minecraft.block.Blocks.GRAVEL)                  return 10;
        if (b == net.minecraft.block.Blocks.SAND
         || b == net.minecraft.block.Blocks.RED_SAND)                return 9;
        if (state.isIn(BlockTags.SHOVEL_MINEABLE))                   return 8;

        return 0;
    }

    // -------------------------------------------------------------------------
    // ENTITY KILLS — Slaying, Ranged
    // -------------------------------------------------------------------------
    private static void registerSlaying() {
        ServerLivingEntityEvents.AFTER_DEATH.register(
            (LivingEntity entity, DamageSource source) -> {
                if (!(source.getAttacker() instanceof ServerPlayerEntity player)) return;
                if (entity instanceof PlayerEntity) return; // no PvP XP

                long xp = mobXp(entity);
                if (xp <= 0) return;

                net.minecraft.item.ItemStack held = player.getMainHandStack();
                boolean isRanged = held.getItem() instanceof BowItem
                    || held.getItem() instanceof CrossbowItem
                    || held.getItem() instanceof TridentItem;

                Skill combatSkill = isRanged ? Skill.RANGED : Skill.SLAYING;
                // Ranged gets 50% of melee XP — distance reduces engagement risk
                if (isRanged) xp = (long)(xp * 0.5);
                xp = applyFlatAbilityBonus(player, combatSkill, xp);
                XpManager.addXp(player, combatSkill, xp);

                CombatDropTracker.recordKill(entity.getUuid(), player.getUuid());

                // Tag item entities that already spawned from this mob's loot so
                // ItemPickupMixin can credit the killer. AFTER_DEATH fires after
                // LivingEntity.drop(), so items are already present in the world.
                if (entity.getEntityWorld() instanceof ServerWorld sw) {
                    double x = entity.getX(), y = entity.getY(), z = entity.getZ();
                    net.minecraft.util.math.Box box =
                        new net.minecraft.util.math.Box(x - 6, y - 4, z - 6, x + 6, y + 4, z + 6);
                    sw.getEntitiesByClass(net.minecraft.entity.ItemEntity.class, box,
                        item -> CollectionRegistry.fromCombatDrop(item.getStack().getItem()).isPresent()
                    ).forEach(item -> CombatDropTracker.tagItemEntity(item.getUuid(), player.getUuid()));
                }

                PetEggHandler.tryDrop(entity, player);
            }
        );
    }

    /** XP based on entity max HP — tougher mobs give more. Bosses have flat overrides. */
    private static long mobXp(LivingEntity entity) {
        // Boss overrides — flat values so they feel rewarding without being trivially farmable
        if (entity instanceof net.minecraft.entity.boss.WitherEntity)        return 9_000;
        if (entity instanceof net.minecraft.entity.boss.dragon.EnderDragonEntity) return 9_000;
        if (entity instanceof net.minecraft.entity.mob.ElderGuardianEntity)   return 9_000;

        float maxHp = entity.getMaxHealth();
        return Math.max(8, Math.round(maxHp * 4.74f));
    }

    // -------------------------------------------------------------------------
    // AGILITY — sprinting and swimming
    // -------------------------------------------------------------------------
    private static void registerAgility() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                boolean active = player.isSprinting() || player.isSwimming();
                if (!active) {
                    agilityTicks.remove(player.getUuid());
                    continue;
                }
                int ticks = agilityTicks.merge(player.getUuid(), 1, Integer::sum);
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
    private static long applyBlockAbilityBonus(ServerPlayerEntity player, Skill skill, long xp,
                                               net.minecraft.util.math.random.Random rng) {
        int level = PlayerDataManager.get(player.getUuid()).getLevel(skill);
        double chance = SkillAbilityRegistry.getDoubleXpChance(skill, level);
        if (chance > 0 && rng.nextDouble() < chance) return xp * 2;
        double mult = SkillAbilityRegistry.getFlatXpMultiplier(skill, level);
        return mult != 1.0 ? (long)(xp * mult) : xp;
    }

    /** Flat multiplier for Slaying, Ranged, and other non-chance skills. */
    private static long applyFlatAbilityBonus(ServerPlayerEntity player, Skill skill, long xp) {
        int level = PlayerDataManager.get(player.getUuid()).getLevel(skill);
        double mult = SkillAbilityRegistry.getFlatXpMultiplier(skill, level);
        return (long)(xp * mult);
    }

}
