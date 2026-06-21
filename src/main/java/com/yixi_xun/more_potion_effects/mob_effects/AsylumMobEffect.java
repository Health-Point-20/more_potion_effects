package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class AsylumMobEffect extends MobEffect {

    public AsylumMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -16711936);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        int level = amplifier + 1;
        float maxAbsorption = level * 4.0f;
        float currentAbsorption = entity.getAbsorptionAmount();
        if (currentAbsorption < maxAbsorption) {
            float newAbsorption = Math.min(currentAbsorption + level * 2.0f, maxAbsorption);
            entity.setAbsorptionAmount(newAbsorption);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 60 == 0;
    }
}