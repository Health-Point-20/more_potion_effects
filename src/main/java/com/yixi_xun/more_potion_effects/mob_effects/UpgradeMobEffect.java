package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.MPEConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.UPGRADE;

public class UpgradeMobEffect extends MobEffect {

    public UpgradeMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -3368449);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return false;

        int appliedLevel = amplifier + 1;
        Set<String> exclusionSet = new HashSet<>(MPEConfig.UPGRADE_EXCLUSION.get());

        entity.getActiveEffects().stream()
                .filter(e -> !e.getEffect().equals(UPGRADE))
                .filter(e -> e.getAmplifier() < appliedLevel)
                .forEach(effect -> {
                    ResourceLocation effectKey = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
                    if (effectKey != null && !exclusionSet.contains(effectKey.toString())) {
                        int newAmplifier = entity.getRandom().nextInt(effect.getAmplifier() + 1, appliedLevel + 1);
                        effect.update(new MobEffectInstance(
                                effect.getEffect(),
                                effect.getDuration(),
                                newAmplifier,
                                effect.isAmbient(),
                                effect.isVisible(),
                                effect.showIcon()
                        ));
                    }
                });
        entity.removeEffect(UPGRADE);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}