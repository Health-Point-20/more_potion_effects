package com.yixi_xun.more_potion_effects.init;

import com.yixi_xun.more_potion_effects.MPEConfig;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;

@EventBusSubscriber(modid = MorePotionEffectsMod.MOD_ID)
public class MorePotionEffectsModConfigs {
	@SubscribeEvent
	public static void register(FMLConstructModEvent event) {
		event.enqueueWork(() ->
				ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, MPEConfig.SPEC, "more_potion_effects.toml"));
	}
}
