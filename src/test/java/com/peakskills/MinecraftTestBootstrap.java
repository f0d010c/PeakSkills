package com.peakskills;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;

public final class MinecraftTestBootstrap {

    private MinecraftTestBootstrap() {
    }

    public static void initializeRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInRegistries.ITEM.stream()
            .map(BuiltInRegistries.ITEM::wrapAsHolder)
            .filter(holder -> holder instanceof Holder.Reference<?> reference && !reference.areComponentsBound())
            .map(holder -> (Holder.Reference<?>) holder)
            .forEach(holder -> holder.bindComponents(DataComponentMap.EMPTY));
    }
}
