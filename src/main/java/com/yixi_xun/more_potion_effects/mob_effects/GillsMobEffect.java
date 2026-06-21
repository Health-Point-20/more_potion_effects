package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class GillsMobEffect extends MobEffect {
    private static final int AIR_RESTORE_AMOUNT = 5;
    private static final int AIR_DRAIN_BASE = 4;

    public GillsMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x00CED1);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return false;

        int level = amplifier + 1;

        if (entity.isInWater()) {
            // 水下：恢复氧气
            entity.setAirSupply(entity.getAirSupply() + AIR_RESTORE_AMOUNT);
        } else if (!entity.hasEffect(MobEffects.WATER_BREATHING)) {
            // 陆地：消耗更多氧气（没有水肺效果时）
            int airDrain = level + AIR_DRAIN_BASE;
            entity.setAirSupply(entity.getAirSupply() - airDrain);
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}