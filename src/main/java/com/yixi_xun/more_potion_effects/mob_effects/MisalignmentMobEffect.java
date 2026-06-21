package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class MisalignmentMobEffect extends MobEffect {

    public MisalignmentMobEffect() {
        super(MobEffectCategory.HARMFUL, -13421773);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}