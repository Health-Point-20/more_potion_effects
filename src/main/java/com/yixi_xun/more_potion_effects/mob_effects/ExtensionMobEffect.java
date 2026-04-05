package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.MPEConfig;
import com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ExtensionMobEffect extends MobEffect {
	public ExtensionMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -2631);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
		if (MPEConfig.EXTENSION_METHOD.get() == 1) {
			ArrayList<MobEffectInstance> effectList = new ArrayList<>(entity.getActiveEffects());
			effectList.remove(entity.getEffect(MorePotionEffectsModMobEffects.EXTENSION));

			MobEffectInstance effect;
			for (int i = amplifier + 1; !effectList.isEmpty() && i-- > 0; ) {
				effect = effectList.remove(entity.getRandom().nextInt(effectList.size()));
				effect.update(new MobEffectInstance(effect.getEffect(), effect.getDuration() + 1, effect.getAmplifier()));
			}
		}
		return true;
	}
}
