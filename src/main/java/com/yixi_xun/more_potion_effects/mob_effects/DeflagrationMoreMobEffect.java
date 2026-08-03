package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.api.IMoreMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class DeflagrationMoreMobEffect extends MobEffect implements IMoreMobEffect {

    public DeflagrationMoreMobEffect() {
        super(MobEffectCategory.HARMFUL, -494765);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        int level = amplifier + 1;
        float fireDuration = entity.getRemainingFireTicks();
        if (fireDuration > 0) {
            float damage = fireDuration * 0.05f * level;
            entity.hurt(entity.damageSources().onFire(), damage);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onEffectRemoved(@NotNull LivingEntity entity, MobEffectInstance instance) {
        if (instance.getDuration() <= 0) return;
        int level = instance.getAmplifier() + 1;
        Level world = entity.level();
        if (!world.isClientSide()) {
            // 爆炸
            world.explode(null, entity.getX(), entity.getY(), entity.getZ(), level, Level.ExplosionInteraction.NONE);
            // 额外火焰伤害
            float fireDuration = entity.getRemainingFireTicks();
            if (fireDuration > 0) {
                entity.hurt(entity.damageSources().onFire(), fireDuration * 0.5f * level);
            }
            entity.clearFire();
        }
    }
}