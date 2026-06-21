
package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class RecoilMobEffect extends MobEffect {
	public RecoilMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -11931);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
