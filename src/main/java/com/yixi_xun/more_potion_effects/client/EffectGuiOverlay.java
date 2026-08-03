package com.yixi_xun.more_potion_effects.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.text.DecimalFormat;

import static com.yixi_xun.more_potion_effects.MorePotionEffectsMod.MOD_ID;
import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.*;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class EffectGuiOverlay {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##.#");

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "effect_status"),
                (guiGraphics, deltaTracker) -> {
                    Minecraft mc = Minecraft.getInstance();
                    Player player = mc.player;
                    if (player == null) return;
                    renderEffectStatus(guiGraphics, mc.font, player);
                });
    }

    private static void renderEffectStatus(GuiGraphics guiGraphics, Font font, Player player) {
        int yOffset = 14;

        if (player.hasEffect(HEALTH_SACRIFICE)) {
            String text = getHealthSacrificeText(player);
            guiGraphics.drawString(font, text, 14, yOffset, -1, false);
        }
    }

    private static String getHealthSacrificeText(Player player) {
        MobEffectInstance effect = player.getEffect(HEALTH_SACRIFICE);
        if (effect == null) return "";
        int level = effect.getAmplifier() + 1;
        double time = player.getPersistentData().getDouble("health_sacrifice_time");
        double damageBoost = (level + 3 + time * 0.0025);
        return "§4伤害增幅：" + DECIMAL_FORMAT.format(damageBoost) + "x";
    }
}
