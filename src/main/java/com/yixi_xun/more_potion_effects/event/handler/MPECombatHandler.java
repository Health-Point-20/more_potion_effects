package com.yixi_xun.more_potion_effects.event.handler;

import com.yixi_xun.more_potion_effects.MPEConfig;
import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import com.yixi_xun.more_potion_effects.api.HurtManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yixi_xun.more_potion_effects.MPEConfig.*;
import static com.yixi_xun.more_potion_effects.api.ConfigHelper.evaluate;
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
            handleInjuryLink(target, damage, event);
        }
    }

    private static void handleInjuryLink(LivingEntity target, float damage, LivingIncomingDamageEvent event) {
        MobEffectInstance injuryLink = target.getEffect(INJURY_LINK);
        if (injuryLink == null) return;

        if (target.level().isClientSide()) return;

        int level = injuryLink.getAmplifier() + 1;
        double radius = Math.max(evaluate(INJURY_LINK_RADIUS.get(), "level", level), 36.0);
        Map<LivingEntity, Integer> partners = new HashMap<>();

        List<LivingEntity> nearbyEntities = target.level().getEntitiesOfClass(
                LivingEntity.class,
                target.getBoundingBox().inflate(radius),
                entity -> entity != target && entity.isAlive()  && entity.hasEffect(INJURY_LINK)
        );

        for (LivingEntity entity : nearbyEntities) {
            if (isPartner(entity, target)) {
                var entityInjuryLink = entity.getEffect(INJURY_LINK);
                if (entityInjuryLink != null) {
                    int entityLevel = entityInjuryLink.getAmplifier() + 1;
                    partners.put(entity, entityLevel);
                } else {
                    partners.put(entity, 0);
                }

            }
        }

        int totalWeight = level;
        for (int partnerLevel : partners.values()) {
            totalWeight += partnerLevel;
        }

        float targetMaxHealth = target.getMaxHealth();

        float remainingDamage = Math.min(damage, targetMaxHealth);
        float targetDamage = damage * ((float) level / totalWeight);
        remainingDamage -= targetDamage;

        for (Map.Entry<LivingEntity, Integer> entry : partners.entrySet()) {
            LivingEntity partner = entry.getKey();
            int partnerLevel = entry.getValue();

            float sharedDamage = remainingDamage * ((float) partnerLevel / (totalWeight - level));

            HurtManager.extraHurt(partner, event.getSource(), sharedDamage);
        }

        if (damage > targetMaxHealth) {
            event.setAmount(targetDamage + damage - targetMaxHealth);
        } else {
            event.setAmount(targetDamage);
        }

    }

    private static boolean isPartner(LivingEntity entity, LivingEntity target) {
        // 判断是否有主仆关系
        if (entity instanceof OwnableEntity ownable && ownable.getOwner() == target
                || target instanceof OwnableEntity ownableTarget && ownableTarget.getOwner() == entity) {
            return true;
        }

        // 团队关系
        if (target.getTeam() != null) {
            String teamName = target.getTeam().getName();
            return entity.getTeam() != null && entity.getTeam().getName().equals(teamName);
        }

        // 种族关系
        return entity.getType() == target.getType();
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
            MorePotionEffectsMod.queueServerWork(0, () -> attackerData.remove("AoeTime"));
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

        handleUnyieldingWillpower(target, event, damage);
        handleStaticLife(target, event, data, damage);
        if (attacker != null) {
            handleAdaptation(target, attacker, event, data, damage);
            handleTrueDamage(target, attacker, event, data, damage, source);
        }
    }

    private static void handleUnyieldingWillpower(LivingEntity target, LivingDamageEvent.Pre event, float damage) {
        MobEffectInstance instance = target.getEffect(UNYIELDING_WILLPOWER);
        double currentHealth = target.getHealth();

        if (instance == null ||
                event.getSource().is(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("more_potion_effects:static_damage")))) {
            return;
        }

        if (damage < currentHealth) {
            return;
        }

        Level level = target.level();

        // 添加发光、黑暗、抗性、禁锢效果
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4));
        target.addEffect(new MobEffectInstance(IMPRISON, 100, 0));

        if (!level.isClientSide()) {
            String baseMessage = Component.translatable("text_unyielding_willpower_message").getString();
            String[] messages = {
                    baseMessage + ".",
                    baseMessage + "..",
                    baseMessage + "...",
            };

            for (int i = 0; i < 3; i++) {
                int finalI = i;
                MorePotionEffectsMod.queueServerWork(finalI * 30 + 5, () -> {
                    // 如果是玩家，显示文本
                    if (target instanceof Player player) {
                        player.displayClientMessage(Component.literal(messages[finalI]), true);
                    }

                    if (finalI == 2) {
                        int effectLevel = instance.getAmplifier() + 1;
                        double chance = evaluate(UNYIELDING_CHANCE.get(), "effectLevel", effectLevel);

                        // 概率判定
                        if (Math.random() >= chance) {
                            target.setHealth(Math.max(0.001f, target.getHealth() - damage));
                            target.hurt(event.getSource(), damage);
                            return;
                        }

                        // 实体由于意外因素死亡
                        if (!target.isAlive()) return;

                        // 播放音效
                        SoundEvent sound = SoundEvents.ENDER_DRAGON_HURT;
                        level.playSound(null, BlockPos.containing(target.getX(), target.getY(), target.getZ()), sound, SoundSource.PLAYERS, 1.0f, 1.0f);


                        // 计算吸收护盾值
                        double absorption = Math.min(
                                16 * effectLevel,
                                effectLevel * 4 + damage * 0.25 * effectLevel
                        );
                        target.setAbsorptionAmount((float) absorption);

                        // 设置生命值和免疫效果
                        target.setHealth(1);
                        if (!level.isClientSide()) {
                            target.addEffect(new MobEffectInstance(IMMUNE, 1, 3));
                        }

                        // 更新使用次数
                        int count = target.getPersistentData().getInt("Unyielding_Count") + 1;
                        target.getPersistentData().putInt("Unyielding_Count", count);

                        // 检查是否需要移除效果
                        if (count >= effectLevel) {
                            target.removeEffect(UNYIELDING_WILLPOWER);
                            target.getPersistentData().remove("Unyielding_Count");
                        } else {
                            if (level instanceof ServerLevel world) {
                                Component broadcastMessage = Component.literal("§e" + target.getDisplayName().getString() + " §6的意志使他抗拒了死亡！");
                                world.getServer().getPlayerList().broadcastSystemMessage(broadcastMessage, false);
                            }
                            // 显示剩余次数
                            if (target instanceof Player player && !player.level().isClientSide()) {
                                int remaining = effectLevel - count;
                                player.displayClientMessage(
                                        Component.literal("§6你的意志最多还能支撑§c" + remaining + "§6次！"),
                                        true
                                );
                            }
                        }
                    }
                });
            }
        }
        event.setNewDamage(0);
    }


    private static void handleStaticLife(LivingEntity target, LivingDamageEvent.Pre event, CompoundTag data, float damage) {
        if (!target.hasEffect(STATIC_LIFE)) return;

        // 累积伤害值
        double accumulatedDamage = data.getDouble("static_damage") + damage;
        data.putDouble("static_damage", accumulatedDamage);

        // 向玩家显示累积的伤害信息
        if (target instanceof Player player && !player.level().isClientSide()) {
            Component message;

            if (accumulatedDamage > 0) {
                // 正数表示即将受到的伤害
                String damageText = new DecimalFormat("0.0").format(accumulatedDamage);
                message = Component.translatable("text.static_life_be_hurt", damageText);
            } else {
                // 负数表示即将获得的治疗
                String healText = new DecimalFormat("0.0").format(-accumulatedDamage);
                message = Component.translatable("text.static_life_be_treated", healText);
            }

            player.displayClientMessage(message, true);
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
