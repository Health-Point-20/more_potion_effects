package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class CombustionMobEffect extends MobEffect {

    public CombustionMobEffect() {
        super(MobEffectCategory.HARMFUL, -494765);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (!entity.isInWater() && !entity.level().isRainingAt(entity.blockPosition())) {
            entity.setRemainingFireTicks(2 * 20 * (amplifier + 1));
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}