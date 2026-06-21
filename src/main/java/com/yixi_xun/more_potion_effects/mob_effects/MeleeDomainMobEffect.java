package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.jetbrains.annotations.NotNull;

public class MeleeDomainMobEffect extends MobEffect {

    public MeleeDomainMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -103);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(@NotNull net.minecraft.world.entity.LivingEntity entity, int amplifier) {
        // 近战领域的逻辑在 MPECombatHandler 中实现
        return true;
    }
}