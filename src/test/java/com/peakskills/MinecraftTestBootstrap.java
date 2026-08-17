package com.peakskills;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;

public final class MinecraftTestBootstrap {

    private MinecraftTestBootstrap() {
    }

    public static void initializeRegistries() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }
}
