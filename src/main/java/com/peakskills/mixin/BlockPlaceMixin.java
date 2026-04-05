package com.peakskills.mixin;

import com.peakskills.world.PlacedBlocksState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts every successful block placement and records the position
 * in PlacedBlocksState so SkillEvents can skip XP on those blocks.
 */
@Mixin(BlockItem.class)
public class BlockPlaceMixin {

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("RETURN"))
    private void onPlace(ItemPlacementContext ctx, CallbackInfoReturnable<ActionResult> cir) {
        // Only care about successful placements on the server
        if (!cir.getReturnValue().isAccepted()) return;
        if (!(ctx.getWorld() instanceof ServerWorld sw)) return;

        PlacedBlocksState.get(sw.getServer()).markPlaced(ctx.getBlockPos().asLong());
    }
}
