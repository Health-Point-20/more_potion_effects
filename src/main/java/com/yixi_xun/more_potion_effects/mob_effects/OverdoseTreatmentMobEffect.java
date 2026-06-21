package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class OverdoseTreatmentMobEffect extends MobEffect {

    public OverdoseTreatmentMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -8355712);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}