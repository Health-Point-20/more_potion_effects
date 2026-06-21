package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.MPEConfig;
import com.yixi_xun.more_potion_effects.api.EffectUtils;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.*;

public class CurseMobEffect extends MobEffect {

    private static final List<List<Holder<MobEffect>>> CURSE_EFFECT_POOL = new ArrayList<>();

    static {
        // 1.20.1 Curse_StartProcedure tiers
        // Tier 0: 中毒/凋零/虚弱/霉运/饥饿
        CURSE_EFFECT_POOL.add(List.of(
                MobEffects.POISON, MobEffects.WITHER, MobEffects.WEAKNESS,
                MobEffects.UNLUCK, MobEffects.HUNGER
        ));
        // Tier 1: +ARMOR_BROKEN/CORROSION/FRAGILE/失明
        CURSE_EFFECT_POOL.add(List.of(
                ARMOR_BROKEN, CORROSION, FRAGILE, MobEffects.BLINDNESS
        ));
        // Tier 2: +HEAVY/SUFFOCATION/WEAKENING_RECOVERY/黑暗
        CURSE_EFFECT_POOL.add(List.of(
                HEAVY, SUFFOCATION, WEAKENING_RECOVERY, MobEffects.DARKNESS
        ));
        // Tier 3: +FEAR/HIGHLY_TOXIC/INJURY_ACCUMULATION
        CURSE_EFFECT_POOL.add(List.of(
                FEAR, HIGHLY_TOXIC, INJURY_ACCUMULATION
        ));
        // Tier 4: +IMPRISON/VIRUS/DECAY
        CURSE_EFFECT_POOL.add(List.of(
                IMPRISON, VIRUS, DECAY
        ));
    }

    public CurseMobEffect() {
        super(MobEffectCategory.HARMFUL, -10066330);
    }

    @Override
    public void onEffectAdded(@NotNull LivingEntity entity, int amplifier) {
        super.onEffectAdded(entity, amplifier);
        if (entity.level().isClientSide()) return;

        int count = MPEConfig.CURSE_COUNT.get().intValue();
        if (count <= 0) return;

        // 收集可用的负面效果
        List<Holder<MobEffect>> available = new ArrayList<>();
        for (int i = 0; i <= Math.min(amplifier, CURSE_EFFECT_POOL.size() - 1); i++) {
            available.addAll(CURSE_EFFECT_POOL.get(i));
        }

        if (available.isEmpty()) return;

        // 随机选择
        Collections.shuffle(available, new Random(entity.getRandom().nextLong()));
        int toApply = Math.min(count, available.size());

        for (int i = 0; i < toApply; i++) {
            Holder<MobEffect> effect = available.get(i);
            entity.addEffect(new MobEffectInstance(effect, 200 * (amplifier + 1), amplifier));
        }
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return false;

        // 1.20.1: 阻止其他负面效果持续时间减少
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
                    && !effect.getEffect().equals(CURSE)
                    && !effect.getEffect().value().isInstantenous()) {
                effect.update(new MobEffectInstance(effect.getEffect(),
                        effect.getDuration() + 1, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            }
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}