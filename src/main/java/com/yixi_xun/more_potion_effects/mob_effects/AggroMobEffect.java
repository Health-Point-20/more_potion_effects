package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class AggroMobEffect extends MobEffect {
	// 基础仇恨范围（等级 I）
	private static final double BASE_RANGE = 16.0D;
	// 每提升一级增加的范围
	private static final double RANGE_PER_LEVEL = 8.0D;

	public AggroMobEffect() {
		super(MobEffectCategory.HARMFUL, -4846066);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		// 每 5 ticks触发一次
		return duration % 5 == 0;
	}

	@Override
	public boolean applyEffectTick(LivingEntity target, int amplifier) {

		if (target.level().isClientSide() || !target.isAlive()) {
			return false;
		}

		double range = BASE_RANGE + amplifier * RANGE_PER_LEVEL;
		ServerLevel serverLevel = (ServerLevel) target.level();

		// 获取范围内符合条件的生物
		serverLevel.getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(range))
				.stream()
				.filter(mob -> mob.isAlive() && mob.canAttack(target))
				.filter(mob -> !(target instanceof Mob) || mob != target)
				.forEach(mob -> mob.setTarget(target));
		return true;
	}
}