
package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.api.IEffectAccessor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DispelMobEffect extends MobEffect {
	public DispelMobEffect() {
		super(MobEffectCategory.HARMFUL, -13434829);
	}

	@Override
	public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
		if (entity.level().isClientSide()) return false;

		int dispelLevel = amplifier + 1;
		List<MobEffectInstance> effectsToProcess = entity.getActiveEffects().stream()
				.filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL)
				.filter(effect -> effect.getEffect().value() != DispelMobEffect.this)
				.toList();

		for (MobEffectInstance effect : effectsToProcess) {
			int newLevel = effect.getAmplifier() - dispelLevel;
			if (newLevel >= 0) {
				effect.update(new MobEffectInstance(
						effect.getEffect(),
						effect.getDuration(),
						newLevel,
						effect.isAmbient(),
						effect.isVisible(),
						effect.showIcon()
				));
				((IEffectAccessor) entity).callOnEffectUpdated(effect, true, entity);
			} else {
				entity.removeEffect(effect.getEffect());
			}
		}
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % 20 == 0;
	}
}
