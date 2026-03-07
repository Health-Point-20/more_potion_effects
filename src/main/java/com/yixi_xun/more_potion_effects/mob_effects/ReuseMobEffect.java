package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.*;
import java.util.stream.Collectors;

import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.LOCK;
import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.REUSE;

public class ReuseMobEffect extends MobEffect {
	public ReuseMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -3342388);
	}

	@Override
	public boolean isInstantenous() {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		// 检查是否被锁定
		if (entity.hasEffect(LOCK)) {
			return false;
		}

		// 获取当前所有效果
		List<MobEffectInstance> activeEffects = entity.getActiveEffects().stream()
				.filter(e -> e.getEffect() != REUSE)
				.collect(Collectors.toList());
		// 如果没有效果需要转移，则返回
		if (activeEffects.isEmpty()) {
			return false;
		}

		// 创建新的药水瓶
		ItemStack potion = new ItemStack(Items.POTION);

		// 根据效果信息动态设置药水瓶名称和颜色
		String potionName = generatePotionName(entity, activeEffects);
		potion.set(DataComponents.CUSTOM_NAME, Component.literal(potionName));

		// 设置药水
		int potionColor = calculatePotionColor(activeEffects);
		PotionContents coloredContents = new PotionContents(
				Optional.empty(),
				Optional.of(potionColor),
				activeEffects
		);
		potion.set(DataComponents.POTION_CONTENTS, coloredContents);

		// 生成药水瓶
		ItemEntity spawnedPotion = entity.spawnAtLocation(potion);
		if (spawnedPotion != null) {
			spawnedPotion.setNoPickUpDelay();
			// 清除所有效果
			entity.removeAllEffects();
		}

		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	private String generatePotionName(LivingEntity entity, List<MobEffectInstance> effects) {
		String entityName = entity.getName().getString();
		int nameLevel = 0;

		// 统计效果信息
		int effectCount = effects.size();
		int maxAmplifier = effects.stream()
				.mapToInt(MobEffectInstance::getAmplifier)
				.max()
				.orElse(0);
		int totalDuration = effects.stream()
				.mapToInt(MobEffectInstance::getDuration)
				.max()
				.orElse(0);

		// 根据效果数量命名
		String countPrefix;
		if (effectCount == 1) {
			countPrefix = "";
		} else if (effectCount <= 3) {
			countPrefix = "复合";
			nameLevel += 1;
		} else {
			countPrefix = "混沌";
			nameLevel += 2;
		}

		// 根据最高等级命名
		String levelPrefix;
		if (maxAmplifier == 0) {
			levelPrefix = "";
		} else if (maxAmplifier <= 2) {
			levelPrefix = "强化级";
			nameLevel += 1;
		} else if (maxAmplifier <= 4) {
			levelPrefix = "精英级";
			nameLevel += 2;
		} else {
			levelPrefix = "战略级";
			nameLevel += 3;
		}

		// 根据总持续时间命名
		String durationSuffix;
		if (totalDuration < 1200) { // 60秒
			durationSuffix = "短效";
			nameLevel -= 1;
		} else if (totalDuration < 3600) { // 3分钟
			durationSuffix = "";
		} else if (totalDuration < 12000) { // 10分钟
			durationSuffix = "长效";
			nameLevel += 1;
		} else {
			durationSuffix = "不竭";
			nameLevel += 2;
		}

		long beneficialCount = effects.stream()
				.filter(effect -> !effect.getEffect().value().isBeneficial())
				.count();
		long harmfulCount = effects.stream()
				.filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
				.count();

		if (harmfulCount - beneficialCount > 0) {
			nameLevel -= 1;
		} else if (harmfulCount - beneficialCount > 2) {
			nameLevel -=2;
		}
		else if (beneficialCount - harmfulCount > 0) {
			nameLevel += 1;
		} else if (beneficialCount - harmfulCount > 2){
			nameLevel += 2;
		}

		// 组合名称
		return String.format("%s%s的%s%s%s药水",getNameColor(nameLevel) , entityName, countPrefix, levelPrefix, durationSuffix);
	}

	private static String getNameColor(int nameLevel) {
		String nameColor;
		if (nameLevel <= -2) {
			nameColor = "§4"; // 深红色
		} else if (nameLevel < 0) {
			nameColor = "§8"; // 深灰色
		} else if (nameLevel == 0) {
			nameColor = "§7"; // 灰色
		} else if (nameLevel == 1) {
			nameColor = "§f"; // 白色
		} else if (nameLevel == 2) {
			nameColor = "§a"; // 绿色
		} else if (nameLevel == 3) {
			nameColor = "§b"; // 青色
		} else if (nameLevel == 4) {
			nameColor = "§5"; // 紫色
		} else if (nameLevel == 5) {
			nameColor = "§d"; // 粉色
		} else if (nameLevel == 6) {
			nameColor = "§6"; // 金色
		} else {
			nameColor = "§c"; // 红色
		}
		return nameColor;
	}

	private int calculatePotionColor(List<MobEffectInstance> effects) {
		if (effects.isEmpty()) {
			return 0x385DC7; // 默认药水蓝色
		}

		// 计算所有效果颜色的混合值
		long totalRed = 0, totalGreen = 0, totalBlue = 0;
		long totalWeight = 0;

		for (MobEffectInstance effect : effects) {
			int color = effect.getEffect().value().getColor();
			int red = (color >> 16) & 0xFF;
			int green = (color >> 8) & 0xFF;
			int blue = color & 0xFF;

			double durationFactor = Math.sqrt(Math.max(1, effect.getDuration() / 20.0)); // 转换为秒
			long weight = (long) (effect.getAmplifier() + 1) * Math.max(1, (int) durationFactor);

			totalRed += (long) red * weight;
			totalGreen += (long) green * weight;
			totalBlue += (long) blue * weight;
			totalWeight += weight;
		}

		if (totalWeight > 0) {
			// 使用加权平均计算平均颜色
			int avgRed = (int) (totalRed / totalWeight);
			int avgGreen = (int) (totalGreen / totalWeight);
			int avgBlue = (int) (totalBlue / totalWeight);

			// 确保颜色值在有效范围内
			avgRed = Math.min(255, Math.max(0, avgRed));
			avgGreen = Math.min(255, Math.max(0, avgGreen));
			avgBlue = Math.min(255, Math.max(0, avgBlue));

			// 增强颜色饱和度，避免颜色过于暗淡
			if (avgRed + avgGreen + avgBlue < 100) {
				// 如果颜色太暗，适当提高亮度
				int boost = Math.min(100, 100 - (avgRed + avgGreen + avgBlue) / 3);
				avgRed = Math.min(255, avgRed + boost);
				avgGreen = Math.min(255, avgGreen + boost);
				avgBlue = Math.min(255, avgBlue + boost);
			}

			return (avgRed << 16) | (avgGreen << 8) | avgBlue;
		}

		return 0x385DC7; // 默认颜色
	}

}
