
package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class SideEffectMobEffect extends MobEffect {
	public SideEffectMobEffect() {
		super(MobEffectCategory.HARMFUL, -15640303);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
