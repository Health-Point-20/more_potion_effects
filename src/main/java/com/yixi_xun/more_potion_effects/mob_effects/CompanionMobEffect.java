package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class CompanionMobEffect extends MobEffect {

    public CompanionMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -13159);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    public static void removeCompanionAttributes(LivingEntity entity) {
    }

    public static void addCompanionEffect(Player player, int amplifier, int deathCount) {
        Holder<MobEffect> effect = getRandomGoodEffect();
        player.addEffect(new MobEffectInstance(effect, (amplifier + deathCount) * 1200, amplifier));
    }

    private static Holder<MobEffect> getRandomGoodEffect() {
        return MobEffects.REGENERATION;
    }
}