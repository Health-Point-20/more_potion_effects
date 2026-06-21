package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 空间锚定效果：效果等级越高，禁止的传送方式越多。
 * 传送阻止逻辑由 MPEEntityHandler 中的事件处理器实现。
 */
public class SpatialAnchorMobEffect extends MobEffect {

    public SpatialAnchorMobEffect() {
        super(MobEffectCategory.HARMFUL, -13434880);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}