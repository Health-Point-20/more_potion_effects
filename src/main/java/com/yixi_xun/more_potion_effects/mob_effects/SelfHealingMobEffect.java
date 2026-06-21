package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.jetbrains.annotations.NotNull;

public class SelfHealingMobEffect extends MobEffect {
    private static final double BASE_HEAL_FACTOR = 0.001;
    private static final double MIN_HEAL_AMOUNT = 0.05;

    public SelfHealingMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7CFC00);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return false;

        if (!entity.isAlive()) return false;

        double maxHealth = entity.getMaxHealth();
        double currentHealth = entity.getHealth();

        if (currentHealth >= maxHealth) return false;

        // 动态恢复：生命值越低恢复越快
        double healthRatio = currentHealth / maxHealth;
        double healAmount = (maxHealth * BASE_HEAL_FACTOR * healthRatio * (amplifier + 1)) + MIN_HEAL_AMOUNT;

        entity.heal((float) healAmount);

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}