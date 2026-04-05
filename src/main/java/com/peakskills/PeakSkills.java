package com.peakskills;

import com.peakskills.command.CollectionsCommand;
import com.peakskills.command.PetsCommand;
import com.peakskills.command.ProfileCommand;
import com.peakskills.command.SkillsCommand;
import com.peakskills.pet.PetDisplayManager;
import com.peakskills.pet.PetEggHandler;
import com.peakskills.player.PlayerDataFailsafe;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.xp.SkillEvents;
import com.peakskills.xp.XpManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeakSkills implements ModInitializer {

    public static final String MOD_ID = "peakskills";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("PeakSkills initializing...");
        PlayerDataManager.register();
        PlayerDataFailsafe.register();
        XpManager.register();
        SkillEvents.register();
        PetEggHandler.register();
        PetDisplayManager.register();
        SkillsCommand.register();
        PetsCommand.register();
        CollectionsCommand.register();
        ProfileCommand.register();
    }
}
