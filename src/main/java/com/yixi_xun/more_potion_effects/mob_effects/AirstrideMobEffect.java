package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class AirstrideMobEffect extends MobEffect {

    public AirstrideMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -16776961);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            if (player.getAbilities().flying) return true;
        }
        if (!entity.onGround() && !entity.isInWater() && !entity.isInLava()) {
            entity.setOnGround(true);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}