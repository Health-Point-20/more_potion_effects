package com.yixi_xun.more_potion_effects.event.handler;

import com.yixi_xun.more_potion_effects.MPEConfig;
import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.text.DecimalFormat;
import java.util.List;

import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.*;

public class MPECombatHandler {
    public static void onAttackHandler(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity ? (LivingEntity) source.getEntity() : null;
        LivingEntity target = event.getEntity();
        float damage = event.getAmount();


        if (attacker != null) {
            CompoundTag targetTags = target.getPersistentData();

            MobEffectInstance adaptationEffect = target.getEffect(ADAPTATION);
            if (adaptationEffect != null) {
                int effectLevel = adaptationEffect.getAmplifier() + 1;
                int requiredHurtTime = 10 - effectLevel;

                if (target.hurtTime >= requiredHurtTime) {
                    float currentDamage = event.getAmount();
                    float lastDamage = targetTags.getFloat("last_damage");
                    boolean adaptDamageFlag = targetTags.getBoolean("adapt_damage");

                    if (adaptDamageFlag && lastDamage >= currentDamage) {
                        event.setCanceled(true);
                    } else {
                        targetTags.putFloat("last_damage", currentDamage);
                    }
                    targetTags.putBoolean("adapt_damage", true);
                } else {
                    targetTags.putBoolean("adapt_damage", false);
                    targetTags.putFloat("last_damage", 0);
                }
            }

            MobEffectInstance trueDamageEffect = attacker.getEffect(TRUE_DAMAGE);
            if (trueDamageEffect != null) {
                targetTags.putFloat("incoming_damage", event.getAmount());
                event.setCanceled(false);
            }

            handleEffectSiphon(attacker, target);
            handleEffectAoe(attacker, target, source, damage);
        }
    }

    private static void handleEffectSiphon(LivingEntity attacker, LivingEntity target) {
        if (attacker.hasEffect(EFFECT_SIPHON) &&
                !target.hasEffect(LOCK)) {

            CompoundTag attackerData = attacker.getPersistentData();

            if (attacker instanceof Player player && player.getAttackStrengthScale(0.5F) <= 0.95F) {
                return;
            }

            MobEffectInstance siphonEffect = attacker.getEffect(EFFECT_SIPHON);
            if (siphonEffect == null) return;

            int effectLevel = siphonEffect.getAmplifier() + 1;

            // 计算窃取概率
            double stealChance = MPEConfig.BASE_STEAL_CHANCE.get() + (effectLevel * 0.1);
            if (attacker.getRandom().nextFloat() >= stealChance) {
                return;
            }

            List<MobEffectInstance> stealableEffects = target.getActiveEffects().stream()
                    .filter(e -> e.getEffect() != EFFECT_SIPHON.get())
                    .toList();

            if (stealableEffects.isEmpty()) {
                return;
            }

            MobEffectInstance chosenEffect = stealableEffects.get(attacker.getRandom().nextInt(stealableEffects.size()));

            // 计算窃取后的参数
            int stolenLevel = chosenEffect.getAmplifier();
            int adjustedDuration = (int) (chosenEffect.getDuration() * (1 - Math.pow(MPEConfig.DURATION_RATIO.get(), effectLevel)));

            // 应用窃取到的效果到攻击者身上
            attackerData.putBoolean("EffectSiphonProcessing", true);
            target.removeEffect(chosenEffect.getEffect());
            attacker.addEffect(new MobEffectInstance(
                    chosenEffect.getEffect(),
                    adjustedDuration,
                    Math.min(stolenLevel, effectLevel - 1),
                    chosenEffect.isAmbient(),
                    chosenEffect.isVisible(),
                    chosenEffect.showIcon()
            ));
            attackerData.putBoolean("EffectSiphonProcessing", false);
        }
    }

    private static void handleEffectAoe(LivingEntity attacker, LivingEntity target, DamageSource source, float damage) {
        MobEffectInstance aoeEffect = attacker.getEffect(ATTACK_AOE);
        long aoeTime = attacker.level().getGameTime();
        CompoundTag attackerData = attacker.getPersistentData();

        if (aoeEffect == null) return;
        if (attackerData.getLong("AoeTime") == aoeTime) {
            MorePotionEffectsMod.queueServerWork(1, () -> {
                attackerData.remove("AoeTime");
            });
            return;
        }

        int amplifier = aoeEffect.getAmplifier();
        // 定义AOE范围
        double range = 1.0 + amplifier * 0.5;

        // 获取范围内的所有实体
        List<LivingEntity> nearbyEntities = attacker.level().getEntitiesOfClass(
                LivingEntity.class,
                target.getBoundingBox().inflate(range),
                entity -> entity != attacker && entity != target && entity.isAlive()
        );

        attackerData.putLong("AoeTime", aoeTime);
        // 对范围内的实体造成伤害
        for (LivingEntity entity : nearbyEntities) {
            // 避免递归
            if (entity.getPersistentData().contains("AttackAoeProcessing")) {
                continue;
            }

            entity.getPersistentData().putBoolean("AttackAoeProcessing", true);

            try {
                float aoeDamage = damage * amplifier * 0.25F;
                entity.hurt(source, aoeDamage);
            } finally {
                entity.getPersistentData().remove("AttackAoeProcessing");
            }
        }
    }

    public static void onDamageHandler(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity ? (LivingEntity) source.getEntity() : null;
        CompoundTag data = target.getPersistentData();
        float damage = event.getOriginalDamage();

        if (attacker != null) {
            handleAdaptation(target, attacker, event, data, damage);
            handleTrueDamage(target, attacker, event, data, damage, source);
            handleStaticLife(target, event, data, damage);
        }
    }

    private static void handleStaticLife(LivingEntity target, LivingDamageEvent.Pre event, CompoundTag data, float damage) {
        if (!target.hasEffect(STATIC_LIFE)) return;

        // 累积伤害值
        double accumulatedDamage = data.getDouble("static_damage") + damage;
        data.putDouble("static_damage", accumulatedDamage);

        // 向玩家显示累积的伤害信息
        if (target instanceof Player player && !player.level().isClientSide()) {
            String message;

            if (accumulatedDamage > 0) {
                // 正数表示即将受到的伤害
                if (accumulatedDamage < Float.MAX_VALUE) {
                    String damageText = new DecimalFormat("0.0").format(accumulatedDamage);
                    message = "§6你将在生命重新流动后受到§c" + damageText + "§6点伤害！";
                } else {
                    // 超出数值上限
                    message = "§6你将在生命重新流动后受到§4∞§6点伤害！";
                }
            } else {
                // 负数表示即将获得的治疗
                String healText = new DecimalFormat("0.0").format(-accumulatedDamage);
                message = "§6你将在生命重新流动后得到§a" + healText + "§6点治疗！";
            }

            player.displayClientMessage(Component.literal(message), true);
        }

        // 完全抵消当前伤害
        event.setNewDamage(0);
    }

    private static void handleAdaptation(LivingEntity target, LivingEntity attacker, LivingDamageEvent.Pre event, CompoundTag data, float damage) {
        MobEffectInstance ins = target.getEffect(ADAPTATION);
        int effectLevel;
        if (ins != null && !attacker.hasEffect(TRUE_DAMAGE)) {
            effectLevel = ins.getAmplifier() + 1;
            boolean isAdapting = data.getBoolean("adapt_damage");
            float lastHurtDamage = data.getFloat("last_hurt_damage");

            target.invulnerableTime = (int) (target.invulnerableTime * (effectLevel * 0.5 + 1));

            if (isAdapting && damage > lastHurtDamage) {
                data.putFloat("last_hurt_damage", damage);
                event.setNewDamage(damage - lastHurtDamage);
            } else {
                target.getPersistentData().putFloat("last_hurt_damage", 0);
            }
        }
    }

    private static void handleTrueDamage(LivingEntity target, LivingEntity attacker, LivingDamageEvent.Pre event, CompoundTag data, float damage, DamageSource source) {
        MobEffectInstance ins = attacker.getEffect(TRUE_DAMAGE);
        if (ins != null) {
            int effectLevel = ins.getAmplifier() + 1;

            if (effectLevel >= 5 || !target.hasEffect(STATIC_LIFE)) {
                event.setNewDamage(0);
            }

            float originalDamage = data.getFloat("incoming_damage");
            if (originalDamage == 0) return;
            float rate = damage / originalDamage;
            float trueDamage = originalDamage * Math.min(effectLevel * 0.25f, 1f);
            float finalDamage = (originalDamage - trueDamage) * rate + trueDamage;

            if (target.getHealth() - finalDamage <= 0) {
                target.die(source);
                target.setHealth(0);
            }

            event.setNewDamage(finalDamage);
        }
    }

    public static void onHealHandler(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();

        handleStrongHeart(entity, event);
    }

    private static void handleStrongHeart(LivingEntity entity, LivingHealEvent event) {
        MobEffectInstance instance = entity.getEffect(STRONG_HEART);
        if (instance != null) {
            int level = instance.getAmplifier() + 1;
            event.setAmount(event.getAmount() * (1 + 0.25f * level));
        }
    }

    public static void onLivingDeathHandler(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        MobEffectInstance instance = entity.getEffect(IMMORTAL);
        if (instance == null) return;
        int amplifier = instance.getAmplifier();
        int duration = instance.getDuration();

        event.setCanceled(true);
        //移除效果
        entity.getPersistentData().putBoolean("locking", true);
        entity.removeEffect(IMMORTAL);
        entity.getPersistentData().remove("locking");
        //恢复生命
        entity.setHealth(entity.getMaxHealth() * 0.1f * (amplifier + 1));
        entity.heal(entity.getMaxHealth() * 0.15f * (amplifier + 1));

        Level level = entity.level();
        SoundEvent sound = SoundEvents.TOTEM_USE;
        //播放声音
        level.playSound(null, entity.blockPosition(), sound, SoundSource.PLAYERS, 1, 1);
        //发送粒子
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY(), entity.getZ(), 8, 1, 1, 1, 0.15);
        }
        //关闭界面
        if (entity instanceof Player player) {
            player.closeContainer();
        }
        //重新添加效果
        if (amplifier > 0) {
            entity.addEffect(new MobEffectInstance(IMMORTAL, duration , amplifier - 1));
        }
    }
}
