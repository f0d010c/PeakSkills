package com.peakskills;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributes;

public class PeakSkillsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PeakSkills.LOGGER.info("PeakSkills client initializing...");

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;
            if (client.options.hudHidden) return;
            if (client.currentScreen != null) return;

            float  health    = client.player.getHealth();
            float  maxHealth = client.player.getMaxHealth();
            double armor     = client.player.getAttributeValue(EntityAttributes.ARMOR);

            String text = String.format("❤ %.1f / %.1f    ❋ %.0f", health, maxHealth, armor);

            // Render just above the vanilla health bar (bottom-left area)
            int x = 2;
            int y = drawContext.getScaledWindowHeight() - 59;

            drawContext.drawText(client.textRenderer, text, x, y, 0xFFFFFFFF, true);
        });
    }
}
