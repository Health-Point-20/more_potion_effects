package com.yixi_xun.more_potion_effects.mixin;

import com.yixi_xun.more_potion_effects.api.IEffectAccessor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import javax.annotation.Nullable;

@Mixin(LivingEntity.class)
public interface LivingEntityMixin extends IEffectAccessor {

    @Invoker("onEffectAdded")
    void callOnEffectAdded(MobEffectInstance effect, @Nullable Entity source);

    @Invoker("onEffectUpdated")
    void callOnEffectUpdated(MobEffectInstance effect, boolean flag, @Nullable Entity source);

    @Invoker("onEffectRemoved")
    void callOnEffectRemoved(MobEffectInstance effectInstance);

    // 使用@Accessor访问字段，方法名符合规则
    @Accessor("effectsDirty")
    void setEffectsDirty(boolean dirty);
}
