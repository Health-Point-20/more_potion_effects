package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class PercentageRecoveryMobEffect extends MobEffect {

    public PercentageRecoveryMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -65281);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        float healAmount = entity.getMaxHealth() * 0.01f * (amplifier + 1);
        entity.heal(healAmount);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }
}