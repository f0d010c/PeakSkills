package com.peakskills.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

public class PeakSkillsClientGameTests implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientWorld().waitForChunksDownload();

            boolean commandRegistered = singleplayer.getServer().computeOnServer(server ->
                server.getCommandManager().getDispatcher().getRoot().getChild("skills") != null
            );
            if (!commandRegistered) {
                throw new AssertionError("PeakSkills did not initialize in an integrated server");
            }

            boolean playerReady = context.computeOnClient(client -> client.player != null && client.world != null);
            if (!playerReady) {
                throw new AssertionError("Client did not finish joining the test world");
            }
        }
    }
}
