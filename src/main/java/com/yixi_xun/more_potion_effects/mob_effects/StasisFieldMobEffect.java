package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StasisFieldMobEffect extends MobEffect {
	// 基础参数
	private static final double BASE_RADIUS = 3.0;          // 基础作用半径(格)
	private static final double RADIUS_PER_LEVEL = 1.0;     // 每级增加的半径
	private static final double BASE_DECAY_RATE = 0.1;     // 基础衰减率(5%/tick)
	private static final double DECAY_PER_LEVEL = 0.05;     // 每级增加的衰减率
	private static final double MAX_DECAY_RATE = 0.5;      // 衰减率上限(25%/tick)
	private static final double STOP_THRESHOLD = 0.01;     // 速度归零判定阈值
	private static final int PARTICLE_TICK_INTERVAL = 5; // 每 5 tick 生成一次粒子
	private static final int MAX_PARTICLES_PER_CYCLE = 8; // 单次循环最多生成粒子数

	public StasisFieldMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -8470785);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
		// 仅服务端执行
		if (!(entity.level() instanceof ServerLevel serverLevel)) return false;

		// 根据等级计算动态参数
		double radius = BASE_RADIUS + (amplifier * RADIUS_PER_LEVEL);
		double decayRate = Math.min(MAX_DECAY_RATE, BASE_DECAY_RATE + (amplifier * DECAY_PER_LEVEL));
		double radiusSq = radius * radius;

		// 快速筛选范围内的弹射物 (使用 AABB 粗筛 + 距离平方精筛)
		double searchRange = radius + 1.0;
		var searchBox = entity.getBoundingBox().inflate(searchRange);

		List<Projectile> projectiles = serverLevel.getEntitiesOfClass(
				Projectile.class,
				searchBox,
				proj -> proj.isAlive() && proj.distanceToSqr(entity.position()) <= radiusSq
		);

		// 对每个有效弹射物应用指数衰减
		for (Projectile proj : projectiles) {
			Vec3 currentDelta = proj.getDeltaMovement();
			double speedSqr = currentDelta.lengthSqr();

			// 低于阈值直接归零，防止浮点数精度漂移
			if (speedSqr <= STOP_THRESHOLD * STOP_THRESHOLD) {
				proj.setDeltaMovement(Vec3.ZERO);
				proj.hurtMarked = true;
				continue;
			}

			// 指数衰减: V_new = V_old * (1 - rate)
			double newSpeed = speedSqr * (1.0 - decayRate);
			Vec3 newDelta = currentDelta.normalize().scale(newSpeed);

			// 仅当速度发生有效变化时才同步
			if (!newDelta.equals(currentDelta)) {
				proj.setDeltaMovement(newDelta);
				proj.hurtMarked = true;
			}

			// 粒子生成
			if (serverLevel.getGameTime() % PARTICLE_TICK_INTERVAL == 0) {
				spawnStasisParticles(serverLevel, entity, amplifier);
			}
		}
		return true;
	}

	private void spawnStasisParticles(ServerLevel level, LivingEntity entity, int amplifier) {
		int points = Math.min(4 + amplifier, MAX_PARTICLES_PER_CYCLE); // 等级越高粒子略多，但有上限

		Vec3 center = entity.position();
		double baseY = center.y + entity.getBbHeight() * 0.5;

		for (int i = 0; i < points; i++) {
			double angle = (2 * Math.PI * i) / points + (level.getGameTime() * 0.08);
			double x = center.x + 3 * Math.cos(angle);
			double z = center.z + 3 * Math.sin(angle);
			double y = baseY + (level.getRandom().nextGaussian() * 0.2);

			// 服务端生成粒子
			level.addParticle(
					ParticleTypes.REVERSE_PORTAL,
					false,
					x, y, z,
					0.0, 0.05, 0.0
			);
		}
	}
}