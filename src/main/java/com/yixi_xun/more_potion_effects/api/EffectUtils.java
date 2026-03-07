package com.yixi_xun.more_potion_effects.api;

import com.yixi_xun.more_potion_effects.MPEConfig;
import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import com.yixi_xun.more_potion_effects.mixin.LivingEntityMixin;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@EventBusSubscriber
public class EffectUtils {
    private static final List<Holder<MobEffect>> ALL_EFFECTS = new ArrayList<>();
    private static final List<Holder<MobEffect>> GOOD_EFFECTS = new ArrayList<>();
    private static final List<Holder<MobEffect>> BAD_EFFECTS = new ArrayList<>();

    @SubscribeEvent
    public static void fillLists(LevelEvent.Load event) {
        Set<ResourceLocation> exclusionSet = new HashSet<>();
        MPEConfig.RANDOM_EFFECT_EXCLUSION.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .forEach(exclusionSet::add);
        MPEConfig.BAN_LIST.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .forEach(exclusionSet::add);

        // 清空列表
        ALL_EFFECTS.clear();
        GOOD_EFFECTS.clear();
        BAD_EFFECTS.clear();

        // 遍历注册表
        for (Holder.Reference<MobEffect> holder : BuiltInRegistries.MOB_EFFECT.holders().toList()) {
            ResourceLocation id = holder.key().location();
            if (exclusionSet.contains(id)) continue;

            ALL_EFFECTS.add(holder);

            MobEffectCategory category = holder.value().getCategory();
            if (category == MobEffectCategory.BENEFICIAL) {
                GOOD_EFFECTS.add(holder);
            } else if (category == MobEffectCategory.HARMFUL) {
                BAD_EFFECTS.add(holder);
            }
        }
    }

    public static Holder<MobEffect> getRandomGoodEffect() {
        return getRandomFromList(GOOD_EFFECTS);
    }

    public static Holder<MobEffect> getRandomBadEffect() {
        return getRandomFromList(BAD_EFFECTS);
    }

    public static Holder<MobEffect> getRandomAllEffect() {
        return getRandomFromList(ALL_EFFECTS);
    }

    private static Holder<MobEffect> getRandomFromList(List<Holder<MobEffect>> list) {
        if (list.isEmpty()) return null;
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    public static void forceAddEffect(LivingEntity entity, MobEffectInstance effect, @Nullable Entity source) {
        if (entity == null || effect == null) return;

        LivingEntityMixin accessor = (LivingEntityMixin) entity;
        Map<Holder<MobEffect>, MobEffectInstance> effectMap = entity.getActiveEffectsMap();
        Holder<MobEffect> effectKey = effect.getEffect();

        MobEffectInstance existing = effectMap.get(effectKey);
        effectMap.put(effectKey, effect);

        if (existing == null) {
            accessor.callOnEffectAdded(effect, source);
        } else if (existing.update(effect)) {
            accessor.callOnEffectUpdated(existing, true, source);
        }
        accessor.setEffectsDirty(true);
    }

    public static void forceRemoveEffect(LivingEntity entity, Holder<MobEffect> effect) {
        if (entity == null || effect == null) return;

        LivingEntityMixin accessor = (LivingEntityMixin) entity;
        Map<Holder<MobEffect>, MobEffectInstance> effectMap = entity.getActiveEffectsMap();

        MobEffectInstance removed = effectMap.remove(effect);
        if (removed != null) {
            accessor.callOnEffectRemoved(removed);
            accessor.setEffectsDirty(true);
        }
    }

    public static void addRandomEffect(Entity entity, Holder<MobEffect> targetEffect, Supplier<Holder<MobEffect>> effectSupplier) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        MobEffectInstance currentEffect = livingEntity.getEffect(targetEffect);

        if (currentEffect == null) return;
        int duration = currentEffect.getDuration();

        // 等级递减逻辑：每个等级递减1，最低为0
        int amplifierLevel = Math.max(currentEffect.getAmplifier() - 1, 0);

        // 移除原有效果
        livingEntity.removeEffect(targetEffect);

        // 根据原效果的等级应用相应数量的随机效果
        for (int i = 0; i <= currentEffect.getAmplifier(); i++) {
            if (!livingEntity.level().isClientSide()) {
                Holder<MobEffect> randomEffect = effectSupplier.get();

                if (randomEffect == null) {
                    MorePotionEffectsMod.LOGGER.error("Failed to get random effect.");
                    continue;
                }
                livingEntity.addEffect(new MobEffectInstance(
                        randomEffect,
                        duration,
                        amplifierLevel,
                        true,
                        false  // 不显示图标
                ));
            }
        }
    }
}