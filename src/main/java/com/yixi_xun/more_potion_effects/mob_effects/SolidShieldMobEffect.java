package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class SolidShieldMobEffect extends MobEffect {

    public SolidShieldMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -154);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}