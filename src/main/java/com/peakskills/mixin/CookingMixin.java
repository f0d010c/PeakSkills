package com.peakskills.mixin;

import com.peakskills.skill.Skill;
import com.peakskills.xp.XpManager;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public class CookingMixin {

    @Shadow @Final
    private Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed;

    @Inject(method = "awardUsedRecipesAndPopExperience", at = @At("HEAD"))
    private void onFurnaceOutput(ServerPlayer player, CallbackInfo ci) {
        long cookedCount = 0;
        for (Reference2IntMap.Entry<ResourceKey<Recipe<?>>> entry : recipesUsed.reference2IntEntrySet()) {
            cookedCount += Math.max(0, entry.getIntValue());
        }
        if (cookedCount <= 0) return;

        XpManager.addXp(player, Skill.COOKING, cookedCount * 113L);
    }
}
