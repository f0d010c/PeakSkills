package com.peakskills.enchantment;

import com.peakskills.PeakSkills;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReplenishEnchantment {

    /** itemEntityUUID → playerUUID who broke the crop that spawned it. */
    private static final Map<UUID, UUID> TAGGED_DROPS = new ConcurrentHashMap<>();

    public static final ResourceKey<Enchantment> REPLENISH = ResourceKey.create(
        Registries.ENCHANTMENT,
        Identifier.fromNamespaceAndPath(PeakSkills.MOD_ID, "replenish")
    );

    private static final int MIN_FARMING_LEVEL = 30;

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register(ReplenishEnchantment::onBlockBreak);
    }

    /** Called from XpManager when the Farming skill levels up. */
    public static void onFarmingLevelUp(ServerPlayer player, int from, int to) {
        if (from < MIN_FARMING_LEVEL && to >= MIN_FARMING_LEVEL) {
            giveRecipeBook(player);
            player.sendSystemMessage(
                Component.literal("  ✦ Recipe Unlocked: ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("Replenish I Book").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                    .append(Component.literal(" — use /craft to make it. A recipe guide was added to your inventory.")
                        .withStyle(ChatFormatting.GRAY)),
                false);
        }
    }

    private static void giveRecipeBook(ServerPlayer player) {
        ItemStack book = new ItemStack(Items.BOOK);
        book.set(DataComponents.CUSTOM_NAME,
            Component.literal("Replenish I — Recipe Guide").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        book.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  Craft via /craft").withStyle(ChatFormatting.GOLD),
            Component.empty(),
            Component.literal("  Ingredients (16× each):").withStyle(ChatFormatting.GRAY),
            Component.literal("  • Wheat").withStyle(ChatFormatting.GREEN),
            Component.literal("  • Carrot").withStyle(ChatFormatting.GREEN),
            Component.literal("  • Potato").withStyle(ChatFormatting.GREEN),
            Component.literal("  • Nether Wart").withStyle(ChatFormatting.GREEN),
            Component.literal("  • Bone Meal").withStyle(ChatFormatting.GREEN),
            Component.empty(),
            Component.literal("  Apply to a Hoe or Axe at an Anvil").withStyle(ChatFormatting.DARK_GRAY)
        )));
        player.getInventory().add(book);
    }

    private static void onBlockBreak(Level world, Player player, BlockPos pos,
                                     BlockState state, net.minecraft.world.level.block.entity.BlockEntity be) {
        if (!(world instanceof ServerLevel serverWorld)) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Must be holding a hoe with Replenish
        ItemStack tool = serverPlayer.getMainHandItem();
        if (!hasReplenish(tool, serverWorld)) return;

        // Farming 30 required
        if (PlayerDataManager.get(serverPlayer.getUUID()).getLevel(Skill.FARMING) < MIN_FARMING_LEVEL) return;

        // Anti-desync: block must be within 8 blocks of the player
        // Prevents a client spoofing a break packet for a distant crop to trigger magnet collection
        if (pos.distToCenterSqr(serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ()) > 64) return;

        // Works on any age — replants immediately
        Block block = state.getBlock();
        Item seed = seedFor(block);
        if (seed == null) return;

        // Defer to next tick so item entities from the break are fully registered
        // in the world's entity list before we query/collect them.
        BlockState replantState = resetAge(block, state);
        serverWorld.getServer().execute(() -> {
            // Tag all crop item entities near the broken block as belonging to this player
            UUID playerUuid = serverPlayer.getUUID();
            AABB tagBox = new AABB(pos).inflate(2.0);
            serverWorld.getEntities(net.minecraft.world.entity.EntityType.ITEM, tagBox,
                e -> isCropDrop(e.getItem().getItem()))
                .forEach(e -> TAGGED_DROPS.put(e.getUUID(), playerUuid));

            // Consume seed from ground drops first, then player inventory as fallback
            if (consumeSeedDrop(serverWorld, pos, seed, serverPlayer)) {
                serverWorld.setBlockAndUpdate(pos, replantState);
            }
            magnetCollect(serverWorld, serverPlayer);
        });
    }

    private static boolean hasReplenish(ItemStack stack, ServerLevel world) {
        if (stack.isEmpty()) return false;
        Holder.Reference<Enchantment> entry =
            world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(REPLENISH);
        return EnchantmentHelper.getItemEnchantmentLevel(entry, stack) > 0;
    }

    private static boolean isMatureCrop(Block block, BlockState state) {
        if (block instanceof CropBlock crop) return crop.isMaxAge(state);
        if (block == Blocks.NETHER_WART) return state.getValue(NetherWartBlock.AGE) == 3;
        if (block == Blocks.COCOA)       return state.getValue(CocoaBlock.AGE) == 2;
        return false;
    }

    private static Item seedFor(Block block) {
        if (block == Blocks.WHEAT)      return Items.WHEAT_SEEDS;
        if (block == Blocks.CARROTS)    return Items.CARROT;
        if (block == Blocks.POTATOES)   return Items.POTATO;
        if (block == Blocks.BEETROOTS)  return Items.BEETROOT_SEEDS;
        if (block == Blocks.NETHER_WART) return Items.NETHER_WART;
        if (block == Blocks.COCOA)      return Items.COCOA_BEANS;
        return null;
    }

    private static BlockState resetAge(Block block, BlockState state) {
        if (block instanceof CropBlock)        return state.setValue(CropBlock.AGE, 0);
        if (block == Blocks.NETHER_WART)       return state.setValue(NetherWartBlock.AGE, 0);
        if (block == Blocks.COCOA)             return state.setValue(CocoaBlock.AGE, 0);
        return block.defaultBlockState();
    }

    /**
     * Pulls all item entities within 0.75 blocks of the broken crop directly into
     * the player's inventory. Any items that don't fit are left with pickup delay 0
     * so the player can walk over them normally.
     */
    /**
     * Collects all crop drops within 8 blocks of the player directly into their inventory.
     * Scanned from the player position so an entire farm row is covered in one sweep.
     * Only pulls items that match a known crop drop (seeds, produce) to avoid vacuuming
     * unrelated items off the ground.
     */
    private static void magnetCollect(ServerLevel world, ServerPlayer player) {
        UUID playerUuid = player.getUUID();
        double x = player.getX(), y = player.getY(), z = player.getZ();
        AABB box = new AABB(x - 8, y - 2, z - 8, x + 8, y + 4, z + 8);
        world.getEntities(net.minecraft.world.entity.EntityType.ITEM, box,
            e -> !e.isRemoved()
                && isCropDrop(e.getItem().getItem())
                && playerUuid.equals(TAGGED_DROPS.get(e.getUUID())))
            .forEach(entity -> {
                TAGGED_DROPS.remove(entity.getUUID());
                ItemStack stack = entity.getItem();
                player.getInventory().add(stack);
                if (stack.isEmpty()) {
                    entity.discard();
                } else {
                    entity.setPickUpDelay(0);
                }
            });
    }

    /** Returns true if this item is a known crop produce or seed. */
    private static boolean isCropDrop(Item item) {
        return item == Items.WHEAT          || item == Items.WHEAT_SEEDS
            || item == Items.CARROT
            || item == Items.POTATO
            || item == Items.BEETROOT       || item == Items.BEETROOT_SEEDS
            || item == Items.NETHER_WART
            || item == Items.COCOA_BEANS;
    }

    /**
     * Consumes one seed for replanting.
     * Priority: ground drops near the block first, then player inventory as fallback.
     * Returns true if a seed was successfully consumed.
     */
    private static boolean consumeSeedDrop(ServerLevel world, BlockPos pos, Item seed, ServerPlayer player) {
        // 1. Try ground drops first (mature crop drops a seed itself)
        AABB box = new AABB(pos).inflate(2.0);
        List<ItemEntity> nearby = world.getEntities(
            net.minecraft.world.entity.EntityType.ITEM, box,
            e -> e.getItem().is(seed));
        if (!nearby.isEmpty()) {
            ItemEntity entity = nearby.get(0);
            ItemStack stack = entity.getItem();
            if (stack.getCount() <= 1) entity.discard();
            else stack.shrink(1);
            return true;
        }
        // 2. Fallback: consume from player inventory (immature crops have no drop)
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (invStack.is(seed)) {
                invStack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
