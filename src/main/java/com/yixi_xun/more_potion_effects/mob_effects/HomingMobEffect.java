package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class HomingMobEffect extends MobEffect {

    public HomingMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -2015699);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        // 追踪效果的核心逻辑在 MPECombatHandler / 投射物 tick 事件中实现
        // 此处仅标记效果需要每 tick 处理
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}