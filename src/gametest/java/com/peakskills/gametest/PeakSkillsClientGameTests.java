package com.peakskills.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public class PeakSkillsClientGameTests implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        boolean clientReady = context.computeOnClient(client ->
            client.getWindow() != null && client.getResourceManager() != null
        );
        if (!clientReady) {
            throw new AssertionError("Minecraft client did not finish initializing");
        }
    }
}
