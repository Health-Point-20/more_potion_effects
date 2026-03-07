
package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class AttackAoeMobEffect extends MobEffect {
	public AttackAoeMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -1754823);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
