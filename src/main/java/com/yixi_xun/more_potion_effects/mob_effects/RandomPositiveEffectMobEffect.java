
package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.api.EffectUtils;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.RANDOM_POSITIVE_EFFECT;

public class RandomPositiveEffectMobEffect extends MobEffect {
	public RandomPositiveEffectMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -24930);
	}

	@Override
	public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
		EffectUtils.addRandomEffect(entity, RANDOM_POSITIVE_EFFECT, EffectUtils::getRandomGoodEffect);
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
