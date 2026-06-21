package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.jetbrains.annotations.NotNull;

public class PotionAntagonismMobEffect extends MobEffect {

    public PotionAntagonismMobEffect() {
        super(MobEffectCategory.NEUTRAL, -10927562);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(@NotNull net.minecraft.world.entity.LivingEntity entity, int amplifier) {
        // 药水拮抗的逻辑在 EffectEvent 中实现
        return true;
    }
}