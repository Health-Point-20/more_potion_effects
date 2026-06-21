package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import org.jetbrains.annotations.NotNull;

import static com.yixi_xun.more_potion_effects.MPEConfig.*;

public class ConfusionMobEffect extends MobEffect {
    // 常量配置
    private static final double BASE_RESISTANCE = 0.2;
    private static final double RESISTANCE_PER_LEVEL = 0.15;
    private static final double BASE_RANDOMNESS = 0.05;
    private static final double RANDOMNESS_PER_LEVEL = 0.03;
    private static final double MAX_RESISTANCE = 0.95;
    private static final double VERTICAL_REDUCTION = 0.4;
    private static final int ROTATION_INTERVAL_BASE = 15;
    private static final int ROTATION_INTERVAL_REDUCTION_PER_LEVEL = 2;

    public ConfusionMobEffect() {
        super(MobEffectCategory.HARMFUL, -10092391);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        // 只影响移动中的实体
        if (entity.getDeltaMovement().lengthSqr() < 0.0001) {
            return true;
        }

        // 计算当前效果强度（范围0-1）
        float strength = calculateEffectStrength(amplifier);

        // 应用移动阻力
        applyMovementResistance(entity, strength);

        // 添加方向干扰
        applyDirectionalInterference(entity, amplifier, strength);

        return true;
    }

    private float calculateEffectStrength(int amplifier) {
        // 将放大器级别映射到范围0-1
        return Mth.clamp((amplifier + 1) / 3.0f, 0.0f, 1.0f);
    }

    private void applyMovementResistance(LivingEntity entity, float strength) {
        // 计算阻力因子：随等级增加而增大
        double resistanceFactor = BASE_RESISTANCE + (RESISTANCE_PER_LEVEL * strength);
        resistanceFactor = Mth.clamp(resistanceFactor, 0, MAX_RESISTANCE);

        // 应用阻力到水平移动
        Vec3 motion = entity.getDeltaMovement();
        double horizontalFactor = 1.0 - resistanceFactor;
        Vec3 newMotion = new Vec3(
            motion.x * horizontalFactor,
            motion.y,
            motion.z * horizontalFactor
        );

        entity.setDeltaMovement(newMotion);
    }

    private void applyDirectionalInterference(LivingEntity entity, int amplifier, float strength) {
        // 计算随机干扰强度
        double randomness = BASE_RANDOMNESS + (RANDOMNESS_PER_LEVEL * strength);

        // 获取实体当前运动向量
        Vec3 motion = entity.getDeltaMovement();
        RandomSource random = entity.getRandom();

        // 生成随机偏移
        double randX = (random.nextDouble() - 0.5) * 2 * randomness;
        double randY = (random.nextDouble() - 0.5) * 2 * randomness * VERTICAL_REDUCTION;
        double randZ = (random.nextDouble() - 0.5) * 2 * randomness;

        // 应用随机偏移
        Vec3 newMotion = new Vec3(
            motion.x + randX,
            motion.y + randY,
            motion.z + randZ
        );

        entity.setDeltaMovement(newMotion);

        // 高级别时添加旋转干扰
        if (amplifier >= 2) {
            applyRotationInterference(entity, amplifier, strength);
        }
    }

    private void applyRotationInterference(LivingEntity entity, int amplifier, float strength) {
        // 高级别时添加视角旋转干扰
        int rotationTimer = entity.getPersistentData().getInt("confusion_rotation_timer");
        rotationTimer++;

        // 计算干扰间隔
        int interval = Math.max(1, ROTATION_INTERVAL_BASE - (amplifier * ROTATION_INTERVAL_REDUCTION_PER_LEVEL));

        if (rotationTimer < interval) {
            entity.getPersistentData().putInt("confusion_rotation_timer", rotationTimer);
            return;
        }

        // 重置计时器
        entity.getPersistentData().putInt("confusion_rotation_timer", 0);

        RandomSource random = entity.getRandom();
        float rotationChange = (random.nextFloat() - 0.5f) * 30.0f * strength;

        entity.setYRot(entity.getYRot() + rotationChange);
        entity.setXRot(Mth.clamp(
            entity.getXRot() + (random.nextFloat() - 0.5f) * 15.0f * strength,
            -180.0f,
            180.0f
        ));

        entity.yRotO = entity.getYRot();
        entity.xRotO = entity.getXRot();
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}