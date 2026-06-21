package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class BleedingMobEffect extends MobEffect {

    public BleedingMobEffect() {
        super(MobEffectCategory.HARMFUL, -6750208);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        float maxHealth = entity.getMaxHealth();
        float damage = Math.min(maxHealth * 0.005f * (amplifier + 1), 25f * (amplifier + 1)) + (amplifier + 1);
        entity.hurt(entity.damageSources().generic(), damage);
        return true;
    }
}