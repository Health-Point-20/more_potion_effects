
package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class AdaptationMobEffect extends MobEffect {
	public AdaptationMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -103);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
