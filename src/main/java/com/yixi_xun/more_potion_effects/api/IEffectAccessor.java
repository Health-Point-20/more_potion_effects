package com.yixi_xun.more_potion_effects.api;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import javax.annotation.Nullable;

public interface IEffectAccessor {
    void callOnEffectAdded(MobEffectInstance effectInstance, @Nullable Entity entity);
    void callOnEffectUpdated(MobEffectInstance effectInstance, boolean flag, @Nullable Entity entity);
    void callOnEffectRemoved(MobEffectInstance effectInstance);
    void setEffectsDirty(boolean dirty);
}
