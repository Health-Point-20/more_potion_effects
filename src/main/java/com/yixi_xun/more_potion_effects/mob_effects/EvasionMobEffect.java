package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class EvasionMobEffect extends MobEffect {

    public EvasionMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -10066330);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}