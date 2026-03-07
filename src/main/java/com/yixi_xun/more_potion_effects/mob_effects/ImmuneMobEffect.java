package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.yixi_xun.more_potion_effects.MPEConfig.*;
import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.IMMUNE;

public class ImmuneMobEffect extends MobEffect {
	public ImmuneMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -103);
	}
	private static List<MobEffect> getEffectsFromConfig(List<? extends String> effectList) {
		return effectList.stream()
				.map(effectName -> BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.tryParse(effectName)))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public static List<Holder<MobEffect>> getImmuneEffects(int amplifier) {

		List<MobEffect> immuneEffects = new ArrayList<>();

        // 从配置中获取各等级免疫效果
		if (amplifier >= 0) {
			immuneEffects.addAll(getEffectsFromConfig(LEVEL_1_LIST.get()));
		}
		if (amplifier >= 1) {
			immuneEffects.addAll(getEffectsFromConfig(LEVEL_2_LIST.get()));
		}
		if (amplifier >= 2) {
			immuneEffects.addAll(getEffectsFromConfig(LEVEL_3_LIST.get()));
		}
		if (amplifier >= 3) {
			immuneEffects.addAll(getEffectsFromConfig(LEVEL_4_LIST.get()));
		}

		// 处理特殊等级的免疫
		if (amplifier == 4) {
			immuneEffects.clear();
			immuneEffects.addAll(BuiltInRegistries.MOB_EFFECT.stream()
					.filter(e -> e.getCategory() == MobEffectCategory.HARMFUL)
					.toList());
		}
		if (amplifier == 5) {
			immuneEffects.clear();
			immuneEffects.addAll(BuiltInRegistries.MOB_EFFECT.stream()
					.filter(e -> e.getCategory() != MobEffectCategory.BENEFICIAL)
					.toList());
		}
		if (amplifier >= 6) {
			immuneEffects.clear();
			immuneEffects.addAll(BuiltInRegistries.MOB_EFFECT.stream().toList());
		}

		return immuneEffects.stream()
				.filter(e -> e != IMMUNE.get())
				.map(Holder::direct)
				.collect(Collectors.toList());
	}

	@Override
	public void onEffectAdded(@NotNull LivingEntity entity, int amplifier) {
		super.onEffectAdded(entity, amplifier);
		// 添加时立即清理已有负面效果
		clearHarmfulEffects(entity, amplifier);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true; // 每 tick 生效一次
	}

	private void clearHarmfulEffects(LivingEntity entity, int amplifier) {
		List<Holder<MobEffect>> immuneEffects = getImmuneEffects(amplifier);
		for (Holder<MobEffect> effect : immuneEffects) {
			if (entity.hasEffect(effect)) {
				entity.removeEffect(effect);
			}
		}
	}
}