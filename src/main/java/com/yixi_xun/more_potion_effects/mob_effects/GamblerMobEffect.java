package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class GamblerMobEffect extends MobEffect {

    public GamblerMobEffect() {
        super(MobEffectCategory.NEUTRAL, -16776961);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}