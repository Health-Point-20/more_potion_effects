package com.yixi_xun.more_potion_effects.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

import static com.yixi_xun.more_potion_effects.MorePotionEffectsMod.MOD_ID;
import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.CALMING;

@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && player.hasEffect(CALMING)) {
            player.hurtTime = 0;
        }
    }
}
