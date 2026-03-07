
package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class StrongHeartMobEffect extends MobEffect {
	public StrongHeartMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -26266);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
