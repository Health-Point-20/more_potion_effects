package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.api.IMobEffectRemovable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.VIRUS;

public class VirusMobEffect extends MobEffect implements IMobEffectRemovable {

	// 基础传染半径系数
	private static final double BASE_RADIUS = 2.0;
	// 感染进度累积基础速度 (每 tick)
	private static final double BASE_INFECTION_SPEED = 1.0;
	// 感染阈值系数 (阈值 = 目标生命值 * THRESHOLD_FACTOR)
	private static final double THRESHOLD_FACTOR = 10.0;
	// 伤害累积时间 (40 ticks = 2秒)
	private static final double DAMAGE_INTERVAL_TICKS = 40.0;
	// 伤害系数 (伤害 = 累积感染伤害 + 最大生命值 * DAMAGE_HEALTH_FACTOR * (amplifier+1))
	private static final double DAMAGE_HEALTH_FACTOR = 0.01;
	// infectedTime 上限系数 (上限 = 最大生命值 * INFECTED_TIME_CAP_FACTOR)
	private static final double INFECTED_TIME_CAP_FACTOR = 3.0;

	public VirusMobEffect() {
		super(MobEffectCategory.HARMFUL, 0xFFCC3333);
	}

	@Override
	public boolean applyEffectTick(@NotNull LivingEntity source, int amplifier) {
		if (source.level().isClientSide) return false;

		Level world = source.level();
		double level = amplifier + 1; // 实际等级从1开始

		// ----- 伤害部分 -----
		double virusTime = source.getPersistentData().getDouble("virus_time");
		virusTime += Math.sqrt(level);
		source.getPersistentData().putDouble("virus_time", virusTime);

		if (virusTime >= DAMAGE_INTERVAL_TICKS) {
			double infectedTime = source.getPersistentData().getDouble("infected_time");
			infectedTime += level * 0.1;

			// 设置 infectedTime 上限
			double maxInfectedTime = source.getMaxHealth() * INFECTED_TIME_CAP_FACTOR;
			if (infectedTime > maxInfectedTime) {
				infectedTime = maxInfectedTime;
			}
			source.getPersistentData().putDouble("infected_time", infectedTime);

			float damage = (float) (infectedTime + source.getMaxHealth() * DAMAGE_HEALTH_FACTOR * level);
			// 使用魔法伤害类型（无视护甲，但受保护附魔和抗性提升影响）
			DamageSource damageSource = new DamageSource(
					world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MAGIC),
					source
			);
			source.hurt(damageSource, damage);
			source.getPersistentData().putDouble("virus_time", 0.0);
		}

		// ----- 传染部分（距离越近越快 + 方块阻挡衰减）-----
		double baseRadius = BASE_RADIUS * level;
		AABB searchBox = source.getBoundingBox().inflate(baseRadius, level, baseRadius);
		List<Entity> nearbyEntities = world.getEntitiesOfClass(Entity.class, searchBox,
				e -> e instanceof LivingEntity && e != source);
		nearbyEntities.sort(Comparator.comparingDouble(source::distanceToSqr));

		for (Entity entity : nearbyEntities) {
			if (!(entity instanceof LivingEntity target)) continue;

			// 射线检测，计算阻挡系数
			double blockFactor = computeBlockTransmissionFactor(source, target, world, level);
			if (blockFactor <= 0.0) continue;

			double effectiveRadius = baseRadius * blockFactor;
			double distance = source.distanceTo(target);
			if (distance > effectiveRadius) continue;

			// 距离系数：线性衰减，最近处为1，最远处为0
			double distanceFactor = 1.0 - (distance / effectiveRadius);
			distanceFactor = Math.max(0.0, Math.min(1.0, distanceFactor));

			// 感染速度受等级、阻挡系数、距离系数共同影响
			double infectionSpeed = BASE_INFECTION_SPEED * level * blockFactor * distanceFactor;

			int targetAmplifier = target.hasEffect(VIRUS) ?
					target.getEffect(VIRUS).getAmplifier() : -1;

			// 只有目标等级低于源等级时才能感染
			if (targetAmplifier < amplifier) {
				double infection = target.getPersistentData().getDouble("infection");
				infection += infectionSpeed;
				target.getPersistentData().putDouble("infection", infection);

				double threshold = target.getHealth() * THRESHOLD_FACTOR;
				if (infection >= threshold) {
					MobEffectInstance newEffect = new MobEffectInstance(
							VIRUS,
							(int) (200 * level),
							amplifier
					);
					target.addEffect(newEffect);
					target.getPersistentData().putDouble("infection", 0.0);
				}
			}
		}
		return true;
	}

	/**
	 * 计算从源实体到目标实体的传染衰减系数。
	 * 如果射线被完整方块完全阻挡，返回 0；
	 * 否则根据路径上不完整方块的"不透明度"计算总衰减系数（乘法）。
	 */
	private double computeBlockTransmissionFactor(LivingEntity source, LivingEntity target, Level world, double potionLevel) {
		Vec3 start = source.getEyePosition();
		Vec3 end = target.getEyePosition();
		double distance = start.distanceTo(end);
		if (distance <= 0.5) return 1.0;

		Vec3 direction = end.subtract(start).normalize();
		double step = 0.5;
		double totalFactor = 1.0;
		double currentDist = 0.0;
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

		while (currentDist < distance) {
			Vec3 checkPoint = start.add(direction.scale(currentDist));
			mutablePos.set(checkPoint.x, checkPoint.y, checkPoint.z);
			BlockState blockState = world.getBlockState(mutablePos);
			if (blockState.isAir()) {
				currentDist += step;
				continue;
			}

			boolean isFullBlock = blockState.isCollisionShapeFullBlock(world, mutablePos);
			if (isFullBlock) {
				if (potionLevel < 5) {
					return 0.0; // 低等级无法穿透完整方块
				} else {
					totalFactor *= 0.1; // 高等级可勉强穿透，但衰减极大
				}
			} else {
				// 细化不完整方块的阻挡系数
				double opacity = getRefinedBlockOpacity(blockState, world, mutablePos);
				totalFactor *= (1.0 - opacity);
			}

			if (totalFactor <= 0.01) return 0.0;
			currentDist += step;
		}

		return Math.max(0.0, Math.min(1.0, totalFactor));
	}

	/**
	 * 细化方块不透明度，返回 0～1 之间的值，表示阻挡传播的强度。
	 * 数值越高，阻挡越强。
	 */
	private double getRefinedBlockOpacity(BlockState state, Level world, BlockPos pos) {
		// 树叶、蜘蛛网等疏松但遮挡视线
		if (state.is(BlockTags.LEAVES) || state.getBlock() == Blocks.COBWEB) {
			return 0.5;
		}

		// 半透明装饰性方块（玻璃、冰、栅栏、铁栏杆等）
		if (state.getBlock() == Blocks.GLASS_PANE || state.is(BlockTags.FENCES)
				|| state.is(BlockTags.FENCE_GATES) || state.getBlock() == Blocks.IRON_BARS
				|| state.getBlock() == Blocks.CHAIN) {
			return 0.3;
		}

		// 台阶、楼梯等非完整方块
		if (!state.isCollisionShapeFullBlock(world, pos) && state.isSolid()) {
			return 0.2;
		}
		// 其他非固体方块（如花、草、火把等）几乎不阻挡
		if (!state.isSolid()) {
			return 0.05;
		}
		// 默认完整不透明方块已在 isFullBlock 中处理，这里不应进入，但兜底
		return 0.7;
	}

	@Override
	public void onEffectRemoved(@NotNull LivingEntity entity, MobEffectInstance instance) {
		// 效果结束时将感染进度设为负数，实现临时免疫（后续需要累积正数才能重新感染）
		double resetValue = -((instance.getAmplifier() + 1) * 50.0);
		entity.getPersistentData().putDouble("infection", resetValue);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true; // 每 tick 执行
	}
}