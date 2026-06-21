package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class SlotLockMobEffect extends MobEffect {

    public SlotLockMobEffect() {
        super(MobEffectCategory.NEUTRAL, -4081806);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}