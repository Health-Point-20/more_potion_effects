package com.yixi_xun.more_potion_effects.api;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public interface IMoreMobEffect {
    default void onEffectRemoved(LivingEntity entity, MobEffectInstance instance) {}
    default void onEffectExpired(LivingEntity entity, MobEffectInstance instance) {}
    default void onEffectAdded(LivingEntity entity, MobEffectInstance instance) {}
}
