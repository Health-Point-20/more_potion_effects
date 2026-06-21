
package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class GluttonyMobEffect extends MobEffect {
	public GluttonyMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -3394274);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
