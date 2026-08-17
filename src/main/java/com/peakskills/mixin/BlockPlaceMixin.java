package com.peakskills.mixin;

import com.peakskills.world.PlacedBlocksState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
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

    @Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN"))
    private void onPlace(BlockPlaceContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        // Only care about successful placements on the server
        if (!cir.getReturnValue().consumesAction()) return;
        if (!(ctx.getLevel() instanceof ServerLevel sw)) return;

        PlacedBlocksState.get(sw.getServer()).markPlaced(ctx.getClickedPos().asLong());
    }
}
