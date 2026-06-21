package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.jetbrains.annotations.NotNull;

public class InjuryAccumulationMobEffect extends MobEffect {
    private static final int DAMAGE_INTERVAL = 40;
    private static final double DAMAGE_FACTOR = 0.1;

    public InjuryAccumulationMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4444);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return false;

        double injuryAccumulation = entity.getPersistentData().getDouble("injury_accumulation");
        injuryAccumulation += amplifier + 1;
        entity.getPersistentData().putDouble("injury_accumulation", injuryAccumulation);

        if (injuryAccumulation >= DAMAGE_INTERVAL) {
            double maxHealth = entity.getMaxHealth();
            double currentHealth = entity.getHealth();
            double missingHealth = maxHealth - currentHealth;

            // 内伤机制：越受伤越痛
            float damage = (float) ((amplifier + 1) * missingHealth * DAMAGE_FACTOR);

            DamageSource damageSource = new DamageSource(
                    entity.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC),
                    entity
            );
            entity.hurt(damageSource, damage);

            entity.getPersistentData().putDouble("injury_accumulation", 0);
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}