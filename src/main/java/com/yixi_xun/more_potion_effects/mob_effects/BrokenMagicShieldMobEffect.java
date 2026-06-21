package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class BrokenMagicShieldMobEffect extends MobEffect {

    public BrokenMagicShieldMobEffect() {
        super(MobEffectCategory.HARMFUL, -10066330);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}