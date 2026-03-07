
package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class ImmortalMobEffect extends MobEffect {
	public ImmortalMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -256);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
