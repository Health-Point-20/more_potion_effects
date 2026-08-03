package com.yixi_xun.more_potion_effects.event.handler;

import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects;
import com.yixi_xun.more_potion_effects.mixin.AbstractArrowAccessor;
import com.yixi_xun.more_potion_effects.mob_effects.KineticMobEffect;
import com.yixi_xun.more_potion_effects.mob_effects.VeiledPresenceMobEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.joml.Vector3f;

import java.text.DecimalFormat;
import java.util.*;

import static com.yixi_xun.more_potion_effects.MPEConfig.*;
import static com.yixi_xun.more_potion_effects.MorePotionEffectsMod.MOD_ID;
import static com.yixi_xun.more_potion_effects.api.ConfigHelper.evaluate;
import static com.yixi_xun.more_potion_effects.api.HurtManager.extraHurt;
import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.*;

public class MPECombatHandler {

    // ==================== LivingAttackEvent (LivingIncomingDamageEvent in NeoForge) ====================

    public static void onAttackHandler(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;

        if (attacker == null) return;

        CompoundTag targetTags = target.getPersistentData();

        // === 适应 ===
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

        // === 闪避 ===
        MobEffectInstance evasion = target.getEffect(EVASION);
        if (evasion != null) {
            int level = evasion.getAmplifier() + 1;
            double probability = level * EVASION_PROBABILITY.get();
            if (probability > 100 || target.getRandom().nextDouble() * 100 <= probability) {
                event.setCanceled(true);
                target.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1, 1);
                if (target instanceof Player player) {
                    player.displayClientMessage(Component.literal("闪避！"), true);
                }
                // 弹开
                Vec3 away = target.position().subtract(attacker.position()).normalize().scale(0.5);
                target.setDeltaMovement(away.x, 0.2, away.z);
                return;
            }
        }

        // === 真伤 ===
        MobEffectInstance trueDamageEffect = attacker.getEffect(TRUE_DAMAGE);
        if (trueDamageEffect != null) {
            targetTags.putFloat("incoming_damage", event.getAmount());
            event.setCanceled(false);
        }

        // === 近战领域 ===
        MobEffectInstance meleeDomain = target.getEffect(MELEE_DOMAIN);
        if (meleeDomain != null) {
            int effectLevel = meleeDomain.getAmplifier() + 1;
            double distance = attacker.distanceTo(target);
            if (distance <= evaluate(MELEE_DOMAIN_DISTANCE.get(), "damage", event.getAmount(), "effectLevel", effectLevel)) {
                event.setCanceled(true);
            }
        }
    }

    // ==================== LivingHurtEvent (LivingIncomingDamageEvent in NeoForge, same handler) ====================

    public static void onHurtHandler(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        // Handle projectile sources
        if (attacker == null && source.getDirectEntity() instanceof Projectile proj && proj.getOwner() instanceof LivingEntity owner) {
            attacker = owner;
        }
        LivingEntity target = event.getEntity();
        float damage = event.getAmount();

        if (attacker == null) return;

        // 潜匿 - 攻击时建立关系
        VeiledPresenceMobEffect.onAttack(attacker, target);


        // 屠戮
        MobEffectInstance slaughter = attacker.getEffect(SLAUGHTER);
        if (slaughter != null && target.getHealth() > 0f) {
            int level = slaughter.getAmplifier() + 1;
            double missingRatio = (target.getMaxHealth() - target.getHealth()) / target.getMaxHealth();
            damage += (float) (damage * Math.pow(missingRatio * level, SLAUGHTER_DAMAGE.get()));
        }

        // 巨力
        MobEffectInstance hugeForce = attacker.getEffect(HUGE_FORCE);
        if (hugeForce != null && source.getDirectEntity() == attacker) {
            int level = hugeForce.getAmplifier() + 1;
            damage *= (float) (level * HUGE_FORCE_DAMAGE.get() + 1);
        }

        // 溃力
        MobEffectInstance wane = attacker.getEffect(WANE);
        if (wane != null) {
            int level = wane.getAmplifier() + 1;
            damage = (float) (damage * Math.pow(1 - WANE_REDUCE_DAMAGE.get(), level) - level);
            damage = Math.max(0, damage);
        }

        // 吸血
        MobEffectInstance leeching = attacker.getEffect(LEECHING);
        if (leeching != null) {
            int attackerLevel = leeching.getAmplifier() + 1;
            int targetBleedingImmunity = 0;
            MobEffectInstance bi = target.getEffect(BLEEDING_IMMUNITY);
            if (bi != null) targetBleedingImmunity = bi.getAmplifier() + 1;
            int levelDiff = Math.max(0, attackerLevel - targetBleedingImmunity + 1);
            float healAmount = (float) (damage * levelDiff * LEECHING_HEALTH.get() + 1);
            attacker.heal(healAmount);
            target.addEffect(new MobEffectInstance(BLEEDING, 30 * (levelDiff + 1), attackerLevel - 1));
        }

        // 燃命
        MobEffectInstance healthSacrifice = attacker.getEffect(HEALTH_SACRIFICE);
        if (healthSacrifice != null) {
            int level = healthSacrifice.getAmplifier() + 1;
            int time = attacker.getPersistentData().getInt("health_sacrifice_time");
            damage *= level + 3 + time * 0.0025f;
        }

        // 伤势累积 (攻击触发)
        MobEffectInstance injuryAcc = target.getEffect(INJURY_ACCUMULATION);
        if (injuryAcc != null) {
            int level = injuryAcc.getAmplifier() + 1;
            float missingHealth = target.getMaxHealth() - target.getHealth();
            float extraDamage = missingHealth * level * INJURY_ACCUMULATION_DAMAGE.get().floatValue();
            DamageSource InternalInjury = new DamageSource(
                    target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("more_potion_effects:internal_injury"))),
                    attacker);
            extraHurt(target, InternalInjury, extraDamage);
        }

        // 精准 (远程)
        boolean isProjectile = source.getDirectEntity() instanceof Projectile && source.getDirectEntity() != attacker;
        if (isProjectile) {
            MobEffectInstance accurate = attacker.getEffect(ACCURATE);
            if (accurate != null) {
                int level = accurate.getAmplifier() + 1;
                damage *= (float) (1 + level * ACCURATE_DAMAGE.get());
            }
        }

        // 失准 (远程)
        if (isProjectile) {
            MobEffectInstance misalignment = attacker.getEffect(MISALIGNMENT);
            if (misalignment != null) {
                int level = misalignment.getAmplifier() + 1;
                damage *= (float) (1 - level * MISALIGNMENT_REDUCE_DAMAGE.get());
            }
        }

        // 魔法伤害修正
        boolean isMagicDamage = source.is(DamageTypeTags.WITCH_RESISTANT_TO);
        if (isMagicDamage) {
            // 魔力聚焦
            MobEffectInstance magicFocus = attacker.getEffect(MAGIC_FOCUS);
            if (magicFocus != null) {
                int level = magicFocus.getAmplifier() + 1;
                damage *= (float) (1 + level * MAGIC_FOCUS_DAMAGE.get());
            }
            // 魔力抑制
            MobEffectInstance magicInhibition = attacker.getEffect(MAGIC_INHIBITION);
            if (magicInhibition != null) {
                int level = magicInhibition.getAmplifier() + 1;
                damage *= (float) (1 - level * MAGIC_INHIBITION_REDUCE_DAMAGE.get());
            }
        }

        // 病毒 (攻击传播)
        MobEffectInstance attackerVirus = attacker.getEffect(VIRUS);
        if (attackerVirus != null) {
            MobEffectInstance targetVirus = target.getEffect(VIRUS);
            int targetAmp = targetVirus != null ? targetVirus.getAmplifier() : -1;
            if (attackerVirus.getAmplifier() > targetAmp) {
                double infection = target.getPersistentData().getDouble("infection");
                target.getPersistentData().putDouble("infection", infection + 10 * (attackerVirus.getAmplifier() + 1));
            }
        }

        // 位格 (攻击时给目标施加负面效果)
        MobEffectInstance attackerRank = attacker.getEffect(RANK);
        if (attackerRank != null) {
            MobEffectInstance targetRank = target.getEffect(RANK);
            int targetRankLevel = targetRank != null ? targetRank.getAmplifier() : -1;
            if (attackerRank.getAmplifier() > targetRankLevel) {
                target.addEffect(new MobEffectInstance(WEAKENING_RECOVERY, 100 * attackerRank.getAmplifier(),
                        (int) Math.floor(attackerRank.getAmplifier() / 2.0)));
            }
        }

        // 赌徒 (攻击方)
        MobEffectInstance gamblerAttacker = attacker.getEffect(GAMBLER);
        if (gamblerAttacker != null) {
            damage *= getGamblerMagnification(attacker, gamblerAttacker.getAmplifier() + 1);
        }

        // === 受击方伤害修正 ===

        // 赌徒 (受击方)
        MobEffectInstance gamblerTarget = target.getEffect(GAMBLER);
        if (gamblerTarget != null && gamblerAttacker == null) {
            damage /= getGamblerMagnification(target, gamblerTarget.getAmplifier() + 1);
        }

        // 坚盾
        MobEffectInstance solidShield = target.getEffect(SOLID_SHIELD);
        if (solidShield != null) {
            int level = solidShield.getAmplifier() + 1;
            float armor = target.getArmorValue();
            damage = damage - level * (2 + armor * 0.1f);
            damage = Math.max(0, damage);
        }

        // 魔法护盾
        if (isMagicDamage) {
            MobEffectInstance magicShield = target.getEffect(MAGIC_SHIELD);
            if (magicShield != null) {
                int level = magicShield.getAmplifier() + 1;
                damage *= (float) (1 - level * MAGIC_SHIELD_REDUCE_DAMAGE.get());
            }
        }

        // 破碎魔抗
        if (isMagicDamage) {
            MobEffectInstance brokenMagicShield = target.getEffect(BROKEN_MAGIC_SHIELD);
            if (brokenMagicShield != null) {
                int level = brokenMagicShield.getAmplifier() + 1;
                damage *= (float) (1 + level * BROKEN_MAGIC_SHIELD_DAMAGE.get());
            }
        }

        // 轻装上阵 (伤害增加)
        MobEffectInstance lightlyLoaded = target.getEffect(LIGHTLY_LOADED);
        if (lightlyLoaded != null) {
            int level = lightlyLoaded.getAmplifier() + 1;
            damage *= (float) (level * 0.2 + 1);
        }

        // 重甲 (伤害减免)
        MobEffectInstance heavyArmor = target.getEffect(HEAVY_ARMOR);
        if (heavyArmor != null) {
            int level = heavyArmor.getAmplifier() + 1;
            damage *= (float) (1 - Math.min(0.999, level * 0.1));
        }

        // 脆弱
        MobEffectInstance fragile = target.getEffect(FRAGILE);
        if (fragile != null) {
            int level = fragile.getAmplifier() + 1;
            damage *= (float) (level * FRAGILE_DAMAGE.get() + 1);
        }

        // 庇护 (受伤粒子)
        MobEffectInstance asylum = target.getEffect(ASYLUM);
        if (asylum != null && target.level() instanceof ServerLevel serverLevel) {
            int particleCount = (int) Math.min(256, Math.round(damage * 0.5));
            if (particleCount > 0) {
                serverLevel.sendParticles(ParticleTypes.HEART,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        particleCount, 0.5, 0.5, 0.5, 0.1);
            }
        }

        event.setAmount(Math.max(0, damage));

        // === 伤害链接 ===
        handleInjuryLink(target, event.getAmount(), event);

        // === 其他效果处理 ===
        handleEffectSiphon(attacker, target);
        handleAttackAoe(attacker, target, source, event.getAmount());
        handleKinetic(attacker, target, event);
        handleHealthConversion(target, event.getAmount(), source);
        handleResonatingStrike(target, attacker, event);
        handleRecoil(target, attacker, source, event.getAmount());
    }

    // ==================== Gambler helper ====================

    private static float getGamblerMagnification(LivingEntity entity, int level) {
        double min = 1.0 / (level + 1);
        double max = level + 1;
        double luck = 0;
        if (entity instanceof Player player) {
            luck = player.getLuck();
        }
        double luckAdjustment = Math.abs(luck) * 0.1 + 1;
        double random = entity.getRandom().nextDouble();
        double biasedRandom;
        if (luck >= 0) {
            biasedRandom = Math.pow(random, 1.0 / luckAdjustment);
        } else {
            biasedRandom = Math.pow(random, luckAdjustment);
        }
        return (float) Mth.lerp(biasedRandom, min, max);
    }

    // ==================== Kinetic ====================

    private static void handleKinetic(LivingEntity attacker, LivingEntity target, LivingIncomingDamageEvent event) {
        MobEffectInstance kineticEffect = attacker.getEffect(KINETIC);
        if (kineticEffect == null) return;
        float speed = (float) calculateRelativeSpeed(attacker, target);
        int effectLevel = kineticEffect.getAmplifier() + 1;
        float damage = event.getAmount();
        Map<String, Number> vars = Map.of("speed", speed, "effectLevel", effectLevel, "damage", damage);
        float damageModifier = (float) evaluate(KINETIC_CALCULATION_FORMULA.get(), vars);
        event.setAmount(damage + damageModifier);
    }

    private static double calculateRelativeSpeed(LivingEntity attacker, LivingEntity target) {
        Vec3 attackerVelocity;
        Vec3 targetVelocity;
        if (attacker instanceof Player player) {
            attackerVelocity = KineticMobEffect.velocities.getOrDefault(player.getUUID(), Vec3.ZERO);
        } else {
            attackerVelocity = attacker.getDeltaMovement();
        }
        if (target instanceof Player player) {
            targetVelocity = KineticMobEffect.velocities.getOrDefault(player.getUUID(), Vec3.ZERO);
        } else {
            targetVelocity = target.getDeltaMovement();
        }
        Vec3 relativeVelocity = attackerVelocity.subtract(targetVelocity);
        double speed = relativeVelocity.length();
        if (speed <= 0.01) return 0;
        return speed;
    }


    private static void handleInjuryLink(LivingEntity target, float damage, LivingIncomingDamageEvent event) {
        MobEffectInstance injuryLink = target.getEffect(INJURY_LINK);
        if (injuryLink == null) return;

        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        int level = injuryLink.getAmplifier() + 1;
        double radius = Math.min(evaluate(INJURY_LINK_RADIUS.get(), "effectLevel", level), 36.0);
        Map<LivingEntity, Integer> partners = new HashMap<>();

        List<LivingEntity> nearbyEntities = target.level().getEntitiesOfClass(
                LivingEntity.class,
                target.getBoundingBox().inflate(radius),
                entity -> entity != target && entity.isAlive() && entity.hasEffect(INJURY_LINK)
        );

        for (LivingEntity entity : nearbyEntities) {
            if (isPartner(entity, target)) {
                var partnerInjuryLink = entity.getEffect(INJURY_LINK);
                if (partnerInjuryLink != null) {
                    partners.put(entity, partnerInjuryLink.getAmplifier() + 1);
                }
            }
        }

        if (partners.isEmpty()) return;

        int totalWeight = level;
        for (int partnerLevel : partners.values()) {
            totalWeight += partnerLevel;
        }

        float targetMaxHealth = target.getMaxHealth();
        float sharableDamage = Math.min(damage, targetMaxHealth);
        float unsharableDamage = damage - sharableDamage;

        // 目标按权重比例承担的伤害
        float targetSharedDamage = sharableDamage * ((float) level / totalWeight);

        for (Map.Entry<LivingEntity, Integer> entry : partners.entrySet()) {
            LivingEntity partner = entry.getKey();
            int partnerWeight = entry.getValue();


            // 伙伴分摊额 = (可分摊总伤害) * (同伴权重 / 总权重)
            float sharedDamage = sharableDamage * ((float) partnerWeight / totalWeight);


            extraHurt(partner, event.getSource(), sharedDamage);

            createConnectionParticles(serverLevel, target, partner, sharedDamage, partnerWeight);
        }

        // 目标受到的伤害 = 目标分摊的份额 + 超出上限无法分摊的份额
        event.setAmount(targetSharedDamage + unsharableDamage);
    }

    private static void createConnectionParticles(ServerLevel serverLevel, LivingEntity from, LivingEntity to, float sharedDamage, int partnerWeight) {
        double startX = from.getX();
        double startY = from.getY() + from.getBbHeight() / 2.0;
        double startZ = from.getZ();

        double endX = to.getX();
        double endY = to.getY() + to.getBbHeight() / 2.0;
        double endZ = to.getZ();

        double dx = endX - startX;
        double dy = endY - startY;
        double dz = endZ - startZ;

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // 修复粒子数量计算：确保小伤害也有粒子，且避免负数
        int particleCount = Math.max(1, (int) Math.min(sharedDamage * 3.0, 128));
        float particleScale = Math.min((float) (0.5 + Math.log10(sharedDamage + 1)), 1.5f);

        DustParticleOptions redDust = new DustParticleOptions(
                new Vector3f(1.0f, 0.0f, 0.0f),
                particleScale
        );

        RandomSource random = serverLevel.random;

        // 生成连线粒子
        if (distance > 0.1) { // 避免距离太近导致视觉异常
            for (int i = 0; i < particleCount; i++) {
                double t = particleCount > 1 ? (double) i / (particleCount - 1) : 0.5;
                double px = startX + dx * t;
                double py = startY + dy * t;
                double pz = startZ + dz * t;

                double offsetX = (random.nextDouble() - 0.5) * 0.1;
                double offsetY = (random.nextDouble() - 0.5) * 0.1;
                double offsetZ = (random.nextDouble() - 0.5) * 0.1;

                serverLevel.sendParticles(redDust, px + offsetX, py + offsetY, pz + offsetZ, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        // 在接收者位置生成爆发粒子
        int burstCount = Math.max(3, partnerWeight * 2); // 确保至少有几个粒子
        for (int i = 0; i < burstCount; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.6;
            double offsetY = (random.nextDouble() - 0.5) * 0.6;
            double offsetZ = (random.nextDouble() - 0.5) * 0.6;
            serverLevel.sendParticles(redDust,
                    endX + offsetX, endY + offsetY, endZ + offsetZ,
                    1, 0.05, 0.05, 0.05, 0.0); // 速度设为0，依靠偏移散开即可
        }
    }

    private static boolean isPartner(LivingEntity entity, LivingEntity target) {
        // 判断是否有主仆关系
        if (entity instanceof OwnableEntity ownable && ownable.getOwner() == target
                || target instanceof OwnableEntity ownableTarget && ownableTarget.getOwner() == entity) {
            return true;
        }

        // 团队关系
        if (target.getTeam() != null && target.getTeam().isAlliedTo(entity.getTeam())) {
            return true;
        }

        // 种族关系
        return entity.getType() == target.getType();
    }

    private static void handleEffectSiphon(LivingEntity attacker, LivingEntity target) {
        if (!attacker.hasEffect(EFFECT_SIPHON) || target.hasEffect(LOCK)) return;
        if (attacker instanceof Player player && player.getAttackStrengthScale(0.5F) <= 0.95F) return;
        MobEffectInstance siphonEffect = attacker.getEffect(EFFECT_SIPHON);
        if (siphonEffect == null) return;
        int effectLevel = siphonEffect.getAmplifier() + 1;
        double stealChance = BASE_STEAL_CHANCE.get() + (effectLevel * 0.1);
        if (attacker.getRandom().nextFloat() >= stealChance) return;
        List<MobEffectInstance> stealableEffects = target.getActiveEffects().stream()
                .filter(e -> e.getEffect() != EFFECT_SIPHON.get()).toList();
        if (stealableEffects.isEmpty()) return;
        MobEffectInstance chosenEffect = stealableEffects.get(attacker.getRandom().nextInt(stealableEffects.size()));
        int stolenLevel = chosenEffect.getAmplifier();
        int adjustedDuration = (int) (chosenEffect.getDuration() * (1 - Math.pow(DURATION_RATIO.get(), effectLevel)));
        CompoundTag attackerData = attacker.getPersistentData();
        attackerData.putBoolean("EffectSiphonProcessing", true);
        target.removeEffect(chosenEffect.getEffect());
        attacker.addEffect(new MobEffectInstance(chosenEffect.getEffect(), adjustedDuration,
                Math.min(stolenLevel, effectLevel - 1), chosenEffect.isAmbient(), chosenEffect.isVisible(), chosenEffect.showIcon()));
        attackerData.putBoolean("EffectSiphonProcessing", false);
    }

    private static void handleAttackAoe(LivingEntity attacker, LivingEntity target, DamageSource source, float damage) {
        MobEffectInstance aoeEffect = attacker.getEffect(ATTACK_AOE);
        long aoeTime = attacker.level().getGameTime();
        CompoundTag attackerData = attacker.getPersistentData();
        if (aoeEffect == null) return;
        if (attackerData.getLong("AoeTime") == aoeTime) {
            MorePotionEffectsMod.queueServerWork(0, () -> attackerData.remove("AoeTime"));
            return;
        }
        int amplifier = aoeEffect.getAmplifier();
        double range = 1.0 + amplifier * 0.5;
        List<LivingEntity> nearbyEntities = attacker.level().getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(range),
                entity -> entity != attacker && entity != target && entity.isAlive());
        attackerData.putLong("AoeTime", aoeTime);
        for (LivingEntity entity : nearbyEntities) {
            if (entity.getPersistentData().contains("AttackAoeProcessing")) continue;
            entity.getPersistentData().putBoolean("AttackAoeProcessing", true);
            try {
                entity.hurt(source, damage * amplifier * 0.25F);
            } finally {
                entity.getPersistentData().remove("AttackAoeProcessing");
            }
        }
    }

    private static void handleHealthConversion(LivingEntity target, float damage, DamageSource source) {
        MobEffectInstance instance = target.getEffect(HEALTH_CONVERSION);
        if (instance != null && !source.typeHolder().is(DamageTypeTags.BYPASSES_ARMOR)) {
            float healthRatio = (float) ((instance.getAmplifier() + 1) * HEALTH_CONVERSION_RATIO.get());
            target.heal(damage * healthRatio);
        }
    }

    private static void handleResonatingStrike(LivingEntity target, LivingEntity attacker, LivingIncomingDamageEvent event) {
        MobEffectInstance resonatingStrike = attacker.getEffect(RESONATING_STRIKE);
        if (resonatingStrike != null) {
            int level = resonatingStrike.getAmplifier() + 1;
            extraHurt(target, event.getSource(), event.getAmount() * (0.5f + level * 0.25f));
        }
    }

    private static void handleRecoil(LivingEntity target, LivingEntity attacker, DamageSource source, float damage) {
        MobEffectInstance recoilEffect = target.getEffect(MorePotionEffectsModMobEffects.RECOIL);
        if (recoilEffect != null && attacker != target) {
            int recoilLevel = recoilEffect.getAmplifier() + 1;
            extraHurt(attacker, new DamageSource(source.typeHolder(), target), damage * 0.2f * recoilLevel);
        }
    }

    // ==================== LivingDamageEvent ====================

    public static void onDamagePreHandler(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity ? (LivingEntity) source.getEntity() : null;
        CompoundTag data = target.getPersistentData();
        float damage = event.getNewDamage();

        // === 限伤 ===
        MobEffectInstance injuryLimitation = target.getEffect(INJURY_LIMITATION);
        if (injuryLimitation != null) {
            int level = injuryLimitation.getAmplifier() + 1;
            float maxAllowed = target.getMaxHealth() / ((level + 2) * (level + 3) * 0.5f);
            if (damage > maxAllowed) {
                event.setNewDamage(maxAllowed);
                damage = maxAllowed;
            }
        }

        // === 速攻 ===
        if (attacker != null) {
            MobEffectInstance fastAttack = attacker.getEffect(FAST_ATTACK);
            if (fastAttack != null) {
                int level = fastAttack.getAmplifier() + 1;
                target.invulnerableTime = target.invulnerableTime / (level + 2) + 10;
            }
        }

        handleUnyieldingWillpower(target, event, damage);
        if (attacker != null) {
            handleAdaptation(target, attacker, event, data, damage);
            handleTrueDamage(target, attacker, event, data, damage, source);
        }

        // === 生命静止 ===
        if (target.hasEffect(STATIC_LIFE)) {
            data.putDouble("static_incoming_damage", event.getNewDamage());
            event.setNewDamage(0);
        }
    }

    public static void onDamagePostHandler(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        CompoundTag data = target.getPersistentData();

        if (target.hasEffect(STATIC_LIFE)) {
            double incomingDamage = data.getDouble("static_incoming_damage");
            data.remove("static_incoming_damage");
            double accumulatedDamage = data.getDouble("static_damage") + incomingDamage;
            data.putDouble("static_damage", accumulatedDamage);
            if (target instanceof Player player && !player.level().isClientSide()) {
                Component message;
                if (accumulatedDamage > 0) {
                    message = Component.translatable("text.static_life_be_hurt", new DecimalFormat("0.0").format(accumulatedDamage));
                } else {
                    message = Component.translatable("text.static_life_be_treated", new DecimalFormat("0.0").format(-accumulatedDamage));
                }
                player.displayClientMessage(message, true);
            }
        }
    }

    // === 不屈意志 ===
    private static void handleUnyieldingWillpower(LivingEntity target, LivingDamageEvent.Pre event, float damage) {
        MobEffectInstance instance = target.getEffect(UNYIELDING_WILLPOWER);
        double currentHealth = target.getHealth();
        if (instance == null || event.getSource().typeHolder().is(ResourceLocation.parse("more_potion_effects:static_damage"))) return;
        if (damage < currentHealth) return;

        Level level = target.level();
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4));
        target.addEffect(new MobEffectInstance(IMPRISON, 100, 0));

        if (!level.isClientSide()) {
            String baseMessage = Component.translatable("text_unyielding_willpower_message").getString();
            String[] messages = {baseMessage + ".", baseMessage + "..", baseMessage + "..."};
            for (int i = 0; i < 3; i++) {
                int finalI = i;
                int effectLevel = instance.getAmplifier() + 1;
                MorePotionEffectsMod.queueServerWork(finalI * 30 + 5, () -> {
                    if (target instanceof Player player) player.displayClientMessage(Component.literal(messages[finalI]), true);
                    if (finalI == 2) {
                        double chance = evaluate(UNYIELDING_CHANCE.get(), "effectLevel", effectLevel);
                        // 概率判定
                        if (Math.random() >= chance) return;
                        if (!target.isAlive()) return;

                        level.playSound(null, BlockPos.containing(target.getX(), target.getY(), target.getZ()), SoundEvents.ENDER_DRAGON_HURT, SoundSource.PLAYERS, 1.0f, 1.0f);
                        double absorption = Math.min(16 * effectLevel, effectLevel * 4 + damage * 0.25 * effectLevel);
                        var absorptionAttr = target.getAttribute(Attributes.MAX_ABSORPTION);
                        if (absorptionAttr != null) {
                        absorptionAttr.addOrUpdateTransientModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath(MOD_ID, "unyielding_willpower"), absorption, AttributeModifier.Operation.ADD_VALUE));
                        target.setAbsorptionAmount((float) absorption);
                        }
                        target.setHealth(1);
                        if (!level.isClientSide()) target.addEffect(new MobEffectInstance(IMMUNE, 1, 3));
                        int count = target.getPersistentData().getInt("Unyielding_Count") + 1;
                        target.getPersistentData().putInt("Unyielding_Count", count);
                        if (count >= effectLevel) {
                            target.removeEffect(UNYIELDING_WILLPOWER);
                            target.getPersistentData().remove("Unyielding_Count");
                        } else {
                            if (level instanceof ServerLevel world) {
                                world.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§e" + target.getDisplayName().getString() + " §6的意志使他抗拒了死亡！"), false);
                            }
                            if (target instanceof Player player && !player.level().isClientSide()) {
                                player.displayClientMessage(Component.literal("§6你的意志最多还能支撑§c" + (effectLevel - count) + "§6次！"), true);
                            }
                        }
                    }
                });
            }
        }
        event.setNewDamage(0);
    }

    private static void handleAdaptation(LivingEntity target, LivingEntity attacker, LivingDamageEvent.Pre event, CompoundTag data, float damage) {
        MobEffectInstance ins = target.getEffect(ADAPTATION);
        if (ins != null && !attacker.hasEffect(TRUE_DAMAGE)) {
            int effectLevel = ins.getAmplifier() + 1;
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
                event.setNewDamage(event.getOriginalDamage());
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

    // ==================== Heal handler ====================

    public static void onHealHandler(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        CompoundTag data = entity.getPersistentData();

        // 强心
        MobEffectInstance strongHeart = entity.getEffect(STRONG_HEART);
        if (strongHeart != null) {
            int level = strongHeart.getAmplifier() + 1;
            event.setAmount((float) (event.getAmount() * (1 + level * STRONG_HEART_RECOVERY.get())));
        }

        // 弱效恢复
        MobEffectInstance weakeningRecovery = entity.getEffect(WEAKENING_RECOVERY);
        if (weakeningRecovery != null) {
            int level = weakeningRecovery.getAmplifier() + 1;
            event.setAmount(Math.max(0, (float) (event.getAmount() * (1 - level * WEAKENING_RECOVERY_AMOUNT.get()))));
        }

        // 过量治疗
        MobEffectInstance overdose = entity.getEffect(OVERDOSE_TREATMENT);
        if (overdose != null) {
            int level = overdose.getAmplifier() + 1;
            float overflow = entity.getHealth() + event.getAmount() - entity.getMaxHealth();
            if (overflow > 0) {
                double overHealing = data.getDouble("overHealing") + overflow;
                data.putDouble("overHealing", overHealing);
                double absorption = Math.min((entity.getMaxHealth() * 0.25 + 5) * level, Math.pow(overHealing * level, 0.6));
                var absorptionAttr = entity.getAttribute(Attributes.MAX_ABSORPTION);
                if (absorptionAttr != null) {
                    absorptionAttr.addOrUpdateTransientModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath(MOD_ID, "overdose_treatment"), absorption, AttributeModifier.Operation.ADD_VALUE));
                }
                entity.setAbsorptionAmount((float) absorption);
            }
        }

        // 燃命
        MobEffectInstance healthSacrifice = entity.getEffect(HEALTH_SACRIFICE);
        if (healthSacrifice != null) {
            int level = healthSacrifice.getAmplifier() + 1;
            event.setAmount(event.getAmount() / level);
        }

        // 生命静止 (治疗延迟)
        if (entity.hasEffect(STATIC_LIFE)) {
            double accumulated = data.getDouble("static_damage") - event.getAmount();
            data.putDouble("static_damage", accumulated);
            if (entity instanceof Player player && !player.level().isClientSide()) {
                Component message;
                if (accumulated > 0) {
                    message = Component.translatable("text.static_life_be_hurt", new DecimalFormat("0.0").format(accumulated));
                } else {
                    message = Component.translatable("text.static_life_be_treated", new DecimalFormat("0.0").format(-accumulated));
                }
                player.displayClientMessage(message, true);
            }
            event.setCanceled(true);
        }
    }

    // ==================== Death handler ====================

    public static void onLivingDeathHandler(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // 亡命之债
        MobEffectInstance lifeDebt = entity.getEffect(LIFE_DEBT);
        if (lifeDebt != null) {
            DamageSource source = event.getSource();
            LivingEntity killer = source.getEntity() instanceof LivingEntity living ? living : null;
            if (killer != null && killer != entity) {
                int level = lifeDebt.getAmplifier() + 1;
                float debtDamage = entity.getMaxHealth() * level * 0.5f + killer.getHealth() * Math.min(0.1f * level, 0.95f) + level;
                killer.hurt(entity.damageSources().magic(), debtDamage);
                killer.addEffect(new MobEffectInstance(CURSE, 100 * level, level - 1));
            }
        }

        // 不朽
        MobEffectInstance immortal = entity.getEffect(IMMORTAL);
        if (immortal == null) return;
        int amplifier = immortal.getAmplifier();
        int duration = immortal.getDuration();

        event.setCanceled(true);
        entity.getPersistentData().putBoolean("locking", true);
        entity.removeEffect(IMMORTAL);
        entity.getPersistentData().remove("locking");
        entity.setHealth(entity.getMaxHealth() * amplifier * 0.1f + 1);
        entity.heal(Math.max(0, entity.getMaxHealth() * ((amplifier + 1) * 0.1f - 1) + 1));

        Level level = entity.level();
        level.playSound(null, entity.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1, 1);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY(), entity.getZ(), 8, 1, 1, 1, 0.15);
        }
        if (entity instanceof Player player) player.closeContainer();

        if (amplifier - 1 >= 0) {
            entity.addEffect(new MobEffectInstance(IMMORTAL, duration, amplifier - 1));
        }
        entity.addEffect(new MobEffectInstance(OVERDOSE_TREATMENT, 100, amplifier));
        entity.addEffect(new MobEffectInstance(SELF_HEALING, 100, amplifier));
    }

    public static void onProjectileImpactHandler(ProjectileImpactEvent event) {
        // 穿透效果
        if (event.getRayTraceResult() instanceof EntityHitResult hitResult
                && event.getProjectile() instanceof AbstractArrow arrow
                && hitResult.getEntity() instanceof LivingEntity
                && arrow.getOwner() instanceof LivingEntity shooter) {
            MobEffectInstance pierceEffect = shooter.getEffect(PIERCE);
            if (pierceEffect != null) {
                CompoundTag data = arrow.getPersistentData();
                if (!data.contains("extra_pierce")) {
                    data.putBoolean("extra_pierce", true);
                    ((AbstractArrowAccessor) arrow).callOnSetPierceLevel((byte) (arrow.getPierceLevel() + pierceEffect.getAmplifier() + 1));
                }
            }
        }
    }
}