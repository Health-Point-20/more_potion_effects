package com.yixi_xun.more_potion_effects.event;

import com.yixi_xun.more_potion_effects.api.EffectUtils;
import com.yixi_xun.more_potion_effects.api.IMobEffectRemovable;
import com.yixi_xun.more_potion_effects.mob_effects.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.yixi_xun.more_potion_effects.MPEConfig.*;
import static com.yixi_xun.more_potion_effects.api.ConfigHelper.evaluate;
import static com.yixi_xun.more_potion_effects.api.EffectUtils.forceUpdateEffect;
import static com.yixi_xun.more_potion_effects.api.EffectUtils.getRandomBadEffect;
import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.*;
import static net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result.*;

@EventBusSubscriber
public class EffectEvents {

    public static final Map<UUID, Integer> effectDuration = new HashMap<>();

    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        handleDeathAdded(event.getEntity(), event.getEffectInstance());
        handleExtensionEffect(event.getEntity(), event.getEffectInstance());
        handleSideEffect(event.getEntity(), event.getEffectInstance());
        handleUpgradeEffect(event.getEntity(), event.getEffectInstance());
        handleQuickDrawSkeleton(event.getEntity(), event.getEffectInstance());
    }

    private static void handleSideEffect(LivingEntity entity, MobEffectInstance instance) {
        if (!instance.getEffect().value().isBeneficial()) return;

        long positiveEffectsCount = entity.getActiveEffects().stream()
                .filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL)
                .count();

        CompoundTag data = entity.getPersistentData();

        if (SIDE_EFFECT_LIMIT.get() <= 0 || data.getBoolean("isSideEffect")) {
            return;
        }

        if (positiveEffectsCount > SIDE_EFFECT_LIMIT.get()) {
            int sideLevel = (int) Math.round(Math.sqrt(positiveEffectsCount - SIDE_EFFECT_LIMIT.get()));
            data.putBoolean("isSideEffect", true);
            entity.addEffect(new MobEffectInstance(
                    SIDE_EFFECT,
                    -1,
                    sideLevel
            ));
            data.remove("isSideEffect");
        }

        try {
            MobEffectInstance sideEffect = entity.getEffect(SIDE_EFFECT);
            if (sideEffect != null) {
                int sideLevel = sideEffect.getAmplifier() + 1;
                int newEffectLevel = instance.getAmplifier() + 1;
                int negativeCount = (int)evaluate(NUMBER_OF_SIDE_EFFECTS.get(), "SideEffectLevel", sideLevel, "NewEffectLevel", newEffectLevel);

                data.putBoolean("isSideEffect", true);
                for (int i = 0; i < negativeCount; i++) {
                    Holder<MobEffect> negativeEffect = getRandomBadEffect();
                    int negativeDuration;
                    if (negativeEffect.value().isInstantenous()) {
                        negativeDuration = instance.getDuration() / 20;
                    } else {
                        negativeDuration = instance.getDuration() * sideLevel;
                    }

                    entity.addEffect(new MobEffectInstance(
                            negativeEffect,
                            negativeDuration,
                            Math.max(0, newEffectLevel - 1)
                    ));
                }
            }
        } finally {
            data.remove("isSideEffect");
        }
    }

    private static void handleExtensionEffect(LivingEntity entity, MobEffectInstance newEffect) {
        MobEffectInstance extension = entity.getEffect(EXTENSION);
        if (extension != null && EXTENSION_METHOD.get() == 0) {
            int extensionLevel = extension.getAmplifier() + 1;
            int extraDuration = 600 * extensionLevel;

            if (!newEffect.getEffect().value().isInstantenous()) {
                newEffect.update(new MobEffectInstance(
                        newEffect.getEffect(),
                        newEffect.getDuration() + 600 + extraDuration,
                        newEffect.getAmplifier()
                ));
            } else {
                newEffect.update(new MobEffectInstance(
                        newEffect.getEffect(),
                        newEffect.getDuration() + extensionLevel,
                        newEffect.getAmplifier()
                ));
            }
        }
    }

    private static void handleUpgradeEffect(LivingEntity entity, MobEffectInstance instance) {
        if (instance.getEffect() != UPGRADE) return;

        int appliedLevel = instance.getAmplifier() + 1;
        Set<String> exclusionSet = new HashSet<>(UPGRADE_EXCLUSION.get());

        entity.getActiveEffects().stream()
                .filter(e -> e.getEffect() != UPGRADE)
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
    }

    private static void handleQuickDrawSkeleton(LivingEntity entity, MobEffectInstance instance) {
        if (entity instanceof AbstractSkeleton target && instance.getEffect() == QUICK_DRAW) {
            target.goalSelector.getAvailableGoals().stream()
                    .filter(goal -> goal.getGoal() instanceof RangedBowAttackGoal).findFirst()
                    .ifPresent(goal -> {
                        if (goal.getGoal() instanceof RangedBowAttackGoal<?> rangedGoal) {
                            rangedGoal.setMinAttackInterval(Math.max(1, (int) (20 * Math.pow(0.5, instance.getAmplifier() + 1))));
                        }
                    });
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effectToApply = event.getEffectInstance();
        CompoundTag persistentData = entity.getPersistentData();

        if (persistentData.getBoolean("dispelling_in_progress") || persistentData.getBoolean("Antagonism")) {
            return;
        }

        ResourceLocation effectKey = BuiltInRegistries.MOB_EFFECT.getKey(effectToApply.getEffect().value());

        if (effectKey == null) return;

        String effectId = effectKey.toString();
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();

        handleImmune(entity, effectToApply, event);
        handleDispel(entity, effectToApply);
        handleDispelContinuous(entity, effectToApply, persistentData, event);
        handleFearCalming(entity, effectToApply, event);
        handleBleedingImmunity(entity, effectToApply, event);
        handlePotionAntagonism(entity, effectToApply, persistentData, event);

        if (FORCE_EFFECTS.get().contains(effectId) && !BAN_LIST.get().contains(effectId)) {
            EffectUtils.forceAddEffect(entity, effectToApply, null);
            event.setResult(APPLY);
        }

        if (ENTITY_LIST.get().contains(entityId)) {
            event.setResult(APPLY);
        }

        if (BAN_LIST.get().contains(effectId)) {
            event.setResult(DO_NOT_APPLY);
            EffectUtils.forceRemoveEffect(entity, effectToApply.getEffect());
        }
    }

    private static void handleImmune(LivingEntity entity, MobEffectInstance effectToApply, MobEffectEvent.Applicable event) {
        var immune = entity.getEffect(IMMUNE);
        if (immune != null) {
            Holder<MobEffect> effect = effectToApply.getEffect();
            var immuneMap = ImmuneMobEffect.getImmuneMap(immune.getAmplifier());
            if (immuneMap.containsKey(effect)) {
                int immuneAmplifier = immuneMap.get(effect);
                if (immuneAmplifier >= effectToApply.getAmplifier() || immuneAmplifier == -1) {
                    event.setResult(DO_NOT_APPLY);
                }
            } else if (immune.getAmplifier() > immuneMap.size() + 2) {
                event.setResult(DO_NOT_APPLY);
            } else if (immune.getAmplifier() + 1 > immuneMap.size() && !effectToApply.getEffect().value().isBeneficial()) {
                event.setResult(DO_NOT_APPLY);
            } else if (immune.getAmplifier() > immuneMap.size() && effectToApply.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                event.setResult(DO_NOT_APPLY);
            }
        }
    }

    private static void handleDispel(LivingEntity entity, MobEffectInstance newEffect) {
        if (newEffect.getEffect() == DISPEL) {
            int dispelLevel = newEffect.getAmplifier();
            List<MobEffectInstance> effectsToProcess = entity.getActiveEffects().stream()
                    .filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL)
                    .filter(effect -> effect.getEffect() != DISPEL)
                    .toList();

            for (MobEffectInstance effect : effectsToProcess) {
                int newLevel = effect.getAmplifier() - dispelLevel;
                if (newLevel >= 0) {
                    MobEffectInstance newInstance = new MobEffectInstance(
                            effect.getEffect(),
                            effect.getDuration(),
                            newLevel,
                            effect.isAmbient(),
                            effect.isVisible(),
                            effect.showIcon()
                    );
                    forceUpdateEffect(entity, effect.getEffect(), newInstance, null);
                }
            }
        }
    }

    private static void handleBleedingImmunity(LivingEntity entity, MobEffectInstance effectToApply, MobEffectEvent.Applicable event) {
        MobEffectInstance bleedingImmunity = entity.getEffect(BLEEDING_IMMUNITY);
        if (bleedingImmunity != null) {
            boolean isBleedEffect = effectToApply.getEffect().value().getDescriptionId().contains("bleed") && !effectToApply.getEffect().value().isBeneficial();
            if (isBleedEffect && bleedingImmunity.getAmplifier() + 1 >= effectToApply.getAmplifier() + 1) {
                event.setResult(DO_NOT_APPLY);
            }
        }
    }

    private static void handlePotionAntagonism(LivingEntity entity, MobEffectInstance effectToApply, CompoundTag persistentData, MobEffectEvent.Applicable event) {
        // 防止递归调用导致崩溃
        if (persistentData.getBoolean("Antagonism")) return;

        MobEffectInstance antagonismEffect = entity.getEffect(POTION_ANTAGONISM);
        if (antagonismEffect != null) {
            boolean isHarmful = effectToApply.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
            boolean shouldApply = !isHarmful || NEGATIVE_POTION_ANTAGONISM.get();

            if (shouldApply && effectToApply.getEffect() != POTION_ANTAGONISM) {
                int antagonismLevel = antagonismEffect.getAmplifier() + 1;
                int appliedLevel = effectToApply.getAmplifier() + 1;
                int newLevel = Math.max(0, appliedLevel - antagonismLevel + 2);
                int newDuration = Math.max(1, (int) evaluate(POTION_ANTAGONISM_REDUCE.get(), "duration", effectToApply.getDuration(), "effectLevel", antagonismLevel));

                if (newLevel < 0) return;

                event.setResult(DO_NOT_APPLY);

                persistentData.putBoolean("Antagonism", true);
                try {
                    entity.addEffect(new MobEffectInstance(
                            effectToApply.getEffect(),
                            newDuration,
                            newLevel,
                            effectToApply.isAmbient(),
                            effectToApply.isVisible(),
                            effectToApply.showIcon()
                    ));
                } finally {
                    persistentData.remove("Antagonism");
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEffectRemove(MobEffectEvent.Remove event) {
        LivingEntity entity = event.getEntity();
        CompoundTag nbt = entity.getPersistentData();
        MobEffectInstance instance = event.getEffectInstance();

        if (instance == null) return;

        // NON_REMOVABLE_EFFECTS 检查
        ResourceLocation effectKey = BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value());
        if (effectKey != null && NON_REMOVABLE_EFFECTS.get().contains(effectKey.toString()) && instance.getDuration() > 0) {
            event.setCanceled(true);
            return;
        }

        // 调用 IMobEffectRemovable 接口的移除处理方法
        if (event.getEffect().value() instanceof IMobEffectRemovable effect) {
            effect.onEffectRemoved(entity, instance);
        }

        handleDeathRemoval(entity, instance);
        handleRankEffectRemoval(entity, instance, event);
        handleLockEffectRemoval(entity, nbt, instance, event);

        // 潜匿效果移除时清理攻击关系
        if (event.getEffect() == VEILED_PRESENCE.get()) {
            VeiledPresenceMobEffect.removeRelations(entity);
        }
    }

    private static void handleLockEffectRemoval(LivingEntity entity, CompoundTag persistentData, MobEffectInstance effectInstance, MobEffectEvent.Remove event) {
        if (persistentData.getBoolean("locking_in_progress")) {
            return;
        }

        if (entity.hasEffect(LOCK)) {
            if (event.getEffect() != LOCK && effectInstance.getDuration() > 0) {
                event.setCanceled(true);
                return;
            }

            if (event.getEffect() == LOCK) {
                if (effectInstance.getDuration() <= 1) {
                    return;
                }
                event.setCanceled(true);

                persistentData.putBoolean("locking_in_progress", true);
                entity.removeEffect(LOCK);
                entity.addEffect(new MobEffectInstance(
                        LOCK,
                        (int) (effectInstance.getDuration() - 1200f / (effectInstance.getAmplifier() + 1f)),
                        effectInstance.getAmplifier(),
                        effectInstance.isAmbient(),
                        effectInstance.isVisible(),
                        effectInstance.showIcon()
                ));
                persistentData.remove("locking_in_progress");
            }
        }
    }

    private static void handleDeathAdded(LivingEntity entity, MobEffectInstance instance) {
        if (instance.getEffect() != DEATH) return;
        effectDuration.put(entity.getUUID(), instance.getDuration());
    }

    private static void handleDeathRemoval(LivingEntity entity, MobEffectInstance removedInstance) {
        if (removedInstance.getEffect() != DEATH) return;
        int effectLevel = removedInstance.getAmplifier() + 1;
        if (effectLevel <= 6) {
            int duration = effectDuration.getOrDefault(entity.getUUID(), 100);
            entity.addEffect(new MobEffectInstance(
                    DEATH,
                    duration,
                    effectLevel
            ));
        }
    }

    private static void handleRankEffectRemoval(LivingEntity entity, MobEffectInstance effectInstance, MobEffectEvent.Remove event) {
        if (event.getEffect() != RANK) return;
        int targetAmplifier = effectInstance.getAmplifier() - 1;
        List<Holder<MobEffect>> effectsToRemove = entity.getActiveEffects().stream()
                .filter(e -> e.getAmplifier() == targetAmplifier)
                .map(MobEffectInstance::getEffect)
                .toList();
        effectsToRemove.forEach(entity::removeEffect);
        MobEffectInstance newRank = new MobEffectInstance(
                RANK,
                effectInstance.getDuration() / 2,
                effectInstance.getAmplifier()
        );
        forceUpdateEffect(entity, RANK, newRank, null);
    }

    private static void handleDispelContinuous(LivingEntity entity, MobEffectInstance effectToApply, CompoundTag persistentData, MobEffectEvent.Applicable event) {
        MobEffectInstance dispelEffect = entity.getEffect(DISPEL);
        if (dispelEffect != null && effectToApply.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL && !effectToApply.getEffect().equals(DISPEL)) {
            int dispelLevel = dispelEffect.getAmplifier() + 1;
            int appliedLevel = effectToApply.getAmplifier() + 1;
            int newLevel = appliedLevel - dispelLevel;
            event.setResult(DO_NOT_APPLY);
            if (newLevel < 0) return;
            persistentData.putBoolean("dispelling_in_progress", true);
            try {
                entity.addEffect(new MobEffectInstance(
                        effectToApply.getEffect(),
                        effectToApply.getDuration(),
                        newLevel,
                        effectToApply.isAmbient(),
                        effectToApply.isVisible(),
                        effectToApply.showIcon()
                ));
            } finally {
                persistentData.remove("dispelling_in_progress");
            }
        }
    }

    private static void handleFearCalming(LivingEntity entity, MobEffectInstance effectToApply, MobEffectEvent.Applicable event) {
        if (effectToApply.getEffect() == FEAR) {
            MobEffectInstance calmingEffect = entity.getEffect(CALMING);
            if (calmingEffect != null && calmingEffect.getAmplifier() + 1 >= effectToApply.getAmplifier() + 1) {
                event.setResult(DO_NOT_APPLY);
                if (entity instanceof Player player) {
                    player.displayClientMessage(Component.literal("镇静使你免受恐惧"), true);
                }
            }
        }
    }
}
