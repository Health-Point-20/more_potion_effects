
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.yixi_xun.more_potion_effects.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import com.yixi_xun.more_potion_effects.api.PotionBrewingSystem;

import java.util.List;

public class MorePotionEffectsModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MorePotionEffectsMod.MOD_ID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MORE_POTION_EFFECTS = REGISTRY.register("more_potion_effects",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.more_potion_effects")).icon(() -> new ItemStack(Items.POTION)).displayItems((parameters, tabData) -> {
				List<ItemStack> generatedPotions = PotionBrewingSystem.getCustomsPotionStacks();
				for (ItemStack potionStack : generatedPotions) {
					if (!potionStack.isEmpty()) {
						tabData.accept(potionStack);
					}
				}
			})

					.build());
}
