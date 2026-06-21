package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.api.IMobEffectRemovable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.FRAGILE;

public class HealthSacrificeMobEffect extends MobEffect implements IMobEffectRemovable {

    public HealthSacrificeMobEffect() {
        super(MobEffectCategory.NEUTRAL, -52480);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        int level = amplifier + 1;

        // 累积计数
        entity.getPersistentData().putDouble("health_sacrifice",
                entity.getPersistentData().getDouble("health_sacrifice") + 1);
        entity.getPersistentData().putDouble("health_sacrifice_time",
                entity.getPersistentData().getDouble("health_sacrifice_time") + 1);

        // 每20tick造成一次伤害
        if (entity.getPersistentData().getDouble("health_sacrifice") >= 20) {
            float damage = (float) (level * entity.getMaxHealth() * 0.025);
            if (entity.getHealth() > damage) {
                entity.hurt(entity.damageSources().generic(), damage);
            }
            entity.getPersistentData().putDouble("health_sacrifice", 0);
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onEffectRemoved(@NotNull LivingEntity entity, MobEffectInstance instance) {
        double sacrificeTime = entity.getPersistentData().getDouble("health_sacrifice_time");
        if (sacrificeTime > 0 && !entity.level().isClientSide()) {
            int duration = (int) (sacrificeTime * 3);
            int level = instance.getAmplifier();
            entity.addEffect(new MobEffectInstance(FRAGILE, duration, level));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, level));
        }
        entity.getPersistentData().remove("health_sacrifice_time");
        entity.getPersistentData().putDouble("health_sacrifice", 0);
    }
}