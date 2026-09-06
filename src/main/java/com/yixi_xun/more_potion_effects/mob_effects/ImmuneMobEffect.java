package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
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
		List<? extends String> effectsConfig = IMMUNE_EFFECTS.get();
		Map<Holder<MobEffect>,Integer> immuneList = new HashMap<>();

		if (effectsConfig.isEmpty()) return immuneList;

		// 防止数组越界
		int max = Math.min(amplifier, IMMUNE_EFFECTS.get().size() - 1);
		for (int i = 0; i <= max; i++) {
			// 获取免疫效果的配置列联表
			Arrays.asList(IMMUNE_EFFECTS.get().get(i).split(",")).forEach(effectConfig -> {
				String[] parts = effectConfig.split("-");
				if (parts.length == 2) {
					String effectName = parts[0].trim();
					int level = Integer.parseInt(parts[1].trim());
					Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(effectName));
                    effect.ifPresent(mobEffectReference -> immuneList.put(mobEffectReference, level));
				} else if (parts.length == 1){
					Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(parts[0].trim()));
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

		if (immuneMap.isEmpty()) return;

		// 遍历实体身上的效果，判断是否在配置Map中
		Collection<MobEffectInstance> activeEffects = entity.getActiveEffects();

		new ArrayList<>(activeEffects).forEach(instance -> {
			Holder<MobEffect> effect = instance.getEffect();

			// 判断实体身上的效果是否在配置列表中
			if (immuneMap.containsKey(effect)) {
				int immuneAmplifier = immuneMap.get(effect);
				// 当前等级 >= 免疫阈值，或者阈值为 -1 (无条件免疫)
				if (amplifier >= immuneAmplifier || immuneAmplifier == -1) {
					entity.removeEffect(effect);
				}
			}
		});


		Collection<MobEffectInstance> effects = entity.getActiveEffects().stream().toList();
		if (amplifier >= immuneMap.size() + 2) {
			effects.stream().filter(effect -> effect.getEffect() != IMMUNE)
					.forEach(effect -> entity.removeEffect(effect.getEffect()));
		} else if (amplifier >= immuneMap.size() + 1) {
			effects.stream().filter(effect -> !effect.getEffect().value().isBeneficial())
					.forEach(effect -> entity.removeEffect(effect.getEffect()));
		} else if (amplifier >= immuneMap.size()) {
			effects.stream().filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
					.forEach(effect -> entity.removeEffect(effect.getEffect()));
		}
	}
}