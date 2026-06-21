package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.jetbrains.annotations.NotNull;

public class ResonatingStrikeMobEffect extends MobEffect {

    public ResonatingStrikeMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -955143);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(@NotNull net.minecraft.world.entity.LivingEntity entity, int amplifier) {
        // 共鸣打击的逻辑在 MPECombatHandler 中实现
        return true;
    }
}