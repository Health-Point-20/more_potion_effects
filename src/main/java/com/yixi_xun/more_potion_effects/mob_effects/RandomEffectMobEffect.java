
package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.api.EffectUtils;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.RANDOM_EFFECT;

public class RandomEffectMobEffect extends MobEffect {
	public RandomEffectMobEffect() {
		super(MobEffectCategory.NEUTRAL, -10551187);
	}

	@Override
	public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
		EffectUtils.addRandomEffect(entity, RANDOM_EFFECT, EffectUtils::getRandomAllEffect);
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
