package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.yixi_xun.more_potion_effects.MPEConfig.*;
import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.IMMUNE;

public class ImmuneMobEffect extends MobEffect {
	public ImmuneMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -103);
	}

	public static Map<Holder<MobEffect>, Integer> getImmuneMap(int amplifier) {
		Map<Holder<MobEffect>,Integer> immuneList = new HashMap<>();
		for (int i = 0; i <= amplifier; i++) {
			Arrays.asList(IMMUNE_EFFECTS.get().get(i).split(",")).forEach(effectConfig -> {
				String[] parts = effectConfig.split("-");
				if (parts.length == 2) {
					String effectName = parts[0];
					int level = Integer.parseInt(parts[1]);
					Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(effectName));
                    effect.ifPresent(mobEffectReference -> immuneList.put(mobEffectReference, level));
				} else if (parts.length == 1){
					Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(parts[0]));
                    effect.ifPresent(mobEffectReference -> immuneList.put(mobEffectReference, -1));

				}
			});
		}
		return immuneList;
	}

	@Override
	public void onEffectAdded(@NotNull LivingEntity entity, int amplifier) {
		super.onEffectAdded(entity, amplifier);
		// 添加时立即清理已有负面效果
		clearEffects(entity, amplifier);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true; // 每 tick 生效一次
	}

	private void clearEffects(LivingEntity entity, int amplifier) {
		var immuneMap = ImmuneMobEffect.getImmuneMap(amplifier);
		for (var effect : immuneMap.keySet()) {
			if (entity.hasEffect(effect)) {
				int immuneAmplifier = immuneMap.get(effect);
				if (immuneAmplifier >= amplifier) {
					entity.removeEffect(effect);
				}
			}
		}

		if (amplifier > immuneMap.size() + 2) {
			entity.getActiveEffects().stream().filter(effect -> effect.getEffect() != IMMUNE)
					.forEach(effect -> entity.removeEffect(effect.getEffect()));
		} else if (amplifier + 1 > immuneMap.size()) {
			entity.getActiveEffects().stream().filter(effect -> !effect.getEffect().value().isBeneficial())
					.forEach(effect -> entity.removeEffect(effect.getEffect()));
		} else if (amplifier > immuneMap.size()) {
			entity.getActiveEffects().stream().filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
					.forEach(effect -> entity.removeEffect(effect.getEffect()));
		}
	}
}