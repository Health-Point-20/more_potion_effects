package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class MagicInhibitionMobEffect extends MobEffect {

    public MagicInhibitionMobEffect() {
        super(MobEffectCategory.HARMFUL, -3355444);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}