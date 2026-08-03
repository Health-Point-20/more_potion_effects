
package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import static com.yixi_xun.more_potion_effects.MorePotionEffectsMod.MOD_ID;

public class UnyieldingWillpowerMobEffect extends MobEffect {
	public UnyieldingWillpowerMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -41682);
		addAttributeModifier(Attributes.MAX_ABSORPTION, ResourceLocation.fromNamespaceAndPath(MOD_ID, "unyielding_willpower"), 0, AttributeModifier.Operation.ADD_VALUE);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
