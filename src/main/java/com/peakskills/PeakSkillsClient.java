package com.peakskills;

import net.fabricmc.api.ClientModInitializer;

public class PeakSkillsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PeakSkills.LOGGER.info("PeakSkills client initializing...");
    }
}
