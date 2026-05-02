
package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.yixi_xun.more_potion_effects.api.ConfigHelper;
import com.yixi_xun.more_potion_effects.MPEConfig;
import org.jetbrains.annotations.NotNull;

public class ShatteredHeartMobEffect extends MobEffect {
	public ShatteredHeartMobEffect() {
		super(MobEffectCategory.HARMFUL, -10084834);
	}
	
	private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("more_potion_effects", "effect.shattered_heart");

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public void addAttributeModifiers(@NotNull AttributeMap attributeMap, int amplifier) {
		super.addAttributeModifiers(attributeMap, amplifier);
		AttributeInstance maxHealth = attributeMap.getInstance(Attributes.MAX_HEALTH);
		if (maxHealth != null) {
		 	double value = ConfigHelper.evaluate(
					 MPEConfig.SHATTERED_HEART_REDUCED_HEALTH.get()
					, "maxHealth", maxHealth.getBaseValue()
					, "effectLevel", amplifier + 1);
			 maxHealth.addPermanentModifier(new AttributeModifier(MODIFIER_ID, -value, AttributeModifier.Operation.ADD_VALUE));
		}
	}
	
	@Override
	public void removeAttributeModifiers(@NotNull AttributeMap attributeMap) {
		super.removeAttributeModifiers(attributeMap);
		AttributeInstance maxHealth = attributeMap.getInstance(Attributes.MAX_HEALTH);
		if (maxHealth != null) {
			maxHealth.removeModifier(MODIFIER_ID);
		}
	}
}
