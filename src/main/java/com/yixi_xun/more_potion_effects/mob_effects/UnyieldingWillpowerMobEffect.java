
package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class UnyieldingWillpowerMobEffect extends MobEffect {
	public UnyieldingWillpowerMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -41682);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
