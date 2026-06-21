package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class MagicShieldMobEffect extends MobEffect {

    public MagicShieldMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -3407872);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}