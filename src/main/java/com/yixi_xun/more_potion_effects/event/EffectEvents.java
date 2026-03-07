package com.yixi_xun.more_potion_effects.event;

import com.yixi_xun.more_potion_effects.api.EffectUtils;
import com.yixi_xun.more_potion_effects.mob_effects.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.yixi_xun.more_potion_effects.MPEConfig.*;
import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.*;
import static net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result.*;

@EventBusSubscriber
public class EffectEvents {
    private static final Collection<Runnable> removeQueue = new ConcurrentLinkedQueue<>();

    // 当效果移除时处理的队列
    public static void onEffectRemove(Runnable action) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
            removeQueue.add(action);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effectToApply = event.getEffectInstance();
        CompoundTag persistentData = entity.getPersistentData();

        // 获取效果和实体的注册名
        String effectId = Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.getKey(effectToApply.getEffect().value())).toString();
        String entityId = Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())).toString();

        /*// 避免递归
        if (persistentData.getBoolean("dispelling_in_progress") || persistentData.getBoolean("Upgrading")) {
            return;
        }*/

        // 免疫检查
        var immune = entity.getEffect(IMMUNE);
        if (immune != null && ImmuneMobEffect.getImmuneEffects(immune.getAmplifier()).contains(effectToApply.getEffect())) {
            event.setResult(DO_NOT_APPLY);
        }

        if (FORCE_EFFECTS.get().contains(effectId) && !BAN_LIST.get().contains(effectId)) {
            EffectUtils.forceAddEffect(entity, effectToApply, null);
            event.setResult(APPLY);
        }

        // 实体白名单检查
        if (ENTITY_LIST.get().contains(entityId)) {
            event.setResult(APPLY);
        }

        /*if (entity.hasEffect(DISPEL.get()) &&
                effectToApply.getEffect().getCategory() == MobEffectCategory.BENEFICIAL &&
                !effectToApply.getEffect().equals(DISPEL.get())) {

            int dispelLevel = entity.getEffect(DISPEL.get()).getAmplifier();
            int newLevel = effectToApply.getAmplifier() - dispelLevel;

            event.setResult(DO_NOT_APPLY);

            if (newLevel >= 0) {
                persistentData.putBoolean("dispelling_in_progress", true);
                entity.addEffect(new MobEffectInstance(
                        effectToApply.getEffect(),
                        effectToApply.getDuration(),
                        newLevel,
                        effectToApply.isAmbient(),
                        effectToApply.isVisible(),
                        effectToApply.showIcon()
                ));
                persistentData.putBoolean("dispelling_in_progress", false);
            }
        }

        if (effectToApply.getEffect() == UPGRADE.get()) {
            int upgradeLevel = effectToApply.getAmplifier();
            Set<String> exclusionSet = new HashSet<>(UPGRADE_EXCLUSION.get());

            // 遍历实体上所有效果并尝试升级
            entity.getActiveEffects().stream()
                    .filter(e -> e.getEffect() != UPGRADE.get())
                    .filter(e -> e.getAmplifier() < upgradeLevel)
                    .filter(e -> !exclusionSet.contains(Objects.requireNonNull(ForgeRegistries.MOB_EFFECTS.getKey(e.getEffect())).toString()))
                    .forEach(effect -> {
                        int newAmplifier = entity.getRandom().nextInt(effect.getAmplifier() + 1, upgradeLevel + 1);
                        effect.update(new MobEffectInstance(
                                effect.getEffect(),
                                effect.getDuration(),
                                newAmplifier,
                                effect.isAmbient(),
                                effect.isVisible(),
                                effect.showIcon()
                        ));
                    });
            entity.removeEffect(UPGRADE.get());
        }*/

        // 禁用列表检查
        if (BAN_LIST.get().contains(effectId)) {
            event.setResult(DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onEffectRemove(MobEffectEvent.Remove event) {
        LivingEntity entity = event.getEntity();
        CompoundTag nbt = entity.getPersistentData();
        // 处理效果移除
        removeQueue.forEach(Runnable::run);

        handleLockEffectRemoval(entity, nbt, event.getEffectInstance(), event);

    }

    private static void handleLockEffectRemoval(LivingEntity entity, CompoundTag persistentData, MobEffectInstance effectInstance, MobEffectEvent.Remove event) {
        if (persistentData.getBoolean("locking")) {
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

                // 手动更新 Lock 效果：减少持续时间
                persistentData.putBoolean("locking", true);
                entity.removeEffect(LOCK);
                entity.addEffect(new MobEffectInstance(
                        LOCK,
                        (int) (effectInstance.getDuration() - 1200f / (effectInstance.getAmplifier() + 1f)), // 进行浮点除法计算
                        effectInstance.getAmplifier(),
                        effectInstance.isAmbient(),
                        effectInstance.isVisible(),
                        effectInstance.showIcon()
                ));
                persistentData.remove("locking");
            }
        }
    }
}
