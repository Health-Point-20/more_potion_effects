package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class CalmingMobEffect extends MobEffect {

    public CalmingMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -8474414);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity target, int amplifier) {
        target.hurtTime = 0;
        target.hurtDuration = 0;
        target.hurtMarked = false;
        return true;
    }
}