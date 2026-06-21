package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class AccurateMobEffect extends MobEffect {

    public AccurateMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -16776961);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}