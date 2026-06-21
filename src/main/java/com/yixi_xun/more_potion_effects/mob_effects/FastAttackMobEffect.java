package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class FastAttackMobEffect extends MobEffect {

    public FastAttackMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -65281);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}