package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class MagicFocusMobEffect extends MobEffect {

    public MagicFocusMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -6710887);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}