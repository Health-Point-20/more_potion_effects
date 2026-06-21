package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class SlaughterMobEffect extends MobEffect {

    public SlaughterMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -16711681);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}