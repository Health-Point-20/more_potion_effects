package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class LifeDebtMobEffect extends MobEffect {

    public LifeDebtMobEffect() {
        super(MobEffectCategory.HARMFUL, -16711936);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}