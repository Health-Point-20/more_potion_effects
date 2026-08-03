package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.api.IMoreMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class InjuryOutburstMobEffect extends MobEffect implements IMoreMobEffect {

    public InjuryOutburstMobEffect() {
        super(MobEffectCategory.HARMFUL, -16711681);
    }

    @Override
    public void onEffectRemoved(@NotNull LivingEntity entity, MobEffectInstance instance) {
        if (entity.level().isClientSide() || !entity.isAlive()) return;

        int level = instance.getAmplifier() + 1;
        // 1.20.1 formula: (maxHealth - currentHealth) * 0.3 * level
        float damage = (entity.getMaxHealth() - entity.getHealth()) * 0.3f * level;

        DamageSource damageSource = new DamageSource(
                entity.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MAGIC)
        );
        entity.hurt(damageSource, damage);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}