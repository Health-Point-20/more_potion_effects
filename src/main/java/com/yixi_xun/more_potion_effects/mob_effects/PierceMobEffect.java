package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.jetbrains.annotations.NotNull;

public class PierceMobEffect extends MobEffect {

    public PierceMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -2013626);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(@NotNull net.minecraft.world.entity.LivingEntity entity, int amplifier) {
        return true;
    }
}