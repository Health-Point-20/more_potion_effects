package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class EruditeMobEffect extends MobEffect {

    public EruditeMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -16777216);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}