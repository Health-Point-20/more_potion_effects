package com.yixi_xun.more_potion_effects.api;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public interface IMobEffectRemovable {
    void onEffectRemoved(LivingEntity entity, MobEffectInstance instance);
}
