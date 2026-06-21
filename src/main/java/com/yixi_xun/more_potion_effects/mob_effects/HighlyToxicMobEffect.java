package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class HighlyToxicMobEffect extends MobEffect {

    public HighlyToxicMobEffect() {
        super(MobEffectCategory.HARMFUL, -3355444);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        float damage = Math.min(entity.getMaxHealth() * 0.01f * (amplifier + 1), 10f * (amplifier + 1));
        entity.hurt(entity.damageSources().generic(), damage);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}