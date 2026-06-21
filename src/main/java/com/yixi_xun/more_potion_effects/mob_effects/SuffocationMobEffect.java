package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class SuffocationMobEffect extends MobEffect {

    public SuffocationMobEffect() {
        super(MobEffectCategory.HARMFUL, -16737895);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        int level = amplifier + 1;
        int airReduction = level * 2 + 4;

        entity.setAirSupply(entity.getAirSupply() - airReduction);

        if (entity.getAirSupply() <= 0) {
            // 累积窒息时间
            entity.getPersistentData().putDouble("suffocation_time",
                    entity.getPersistentData().getDouble("suffocation_time") + 1);

            // 每10tick造成一次溺水伤害
            if (entity.getPersistentData().getDouble("suffocation_time") >= 10) {
                entity.hurt(entity.damageSources().drown(), level);
                entity.getPersistentData().putDouble("suffocation_time", 0);
            }
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}