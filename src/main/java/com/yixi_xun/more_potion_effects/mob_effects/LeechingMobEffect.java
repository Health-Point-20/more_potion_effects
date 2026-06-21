package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class LeechingMobEffect extends MobEffect {

    public LeechingMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -6750208);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}