package com.yixi_xun.more_potion_effects.event.handler;

import com.yixi_xun.more_potion_effects.api.EffectUtils;
import com.yixi_xun.more_potion_effects.entity.HomingArrowEntity;
import com.yixi_xun.more_potion_effects.mixin.AbstractArrowAccessor;
import com.yixi_xun.more_potion_effects.mixin.AreaEffectCloudAccessor;
import com.yixi_xun.more_potion_effects.mob_effects.CompanionMobEffect;
import com.yixi_xun.more_potion_effects.mob_effects.DeathMobEffect;
import com.yixi_xun.more_potion_effects.mob_effects.VeiledPresenceMobEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import org.apache.commons.compress.utils.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.*;

public class MPEEntityHandler {

    public static void onLivingSetAttackTargetHandler(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() != null
                && !VeiledPresenceMobEffect.canAttack(event.getEntity(), event.getNewAboutToBeSetTarget())) {
            event.setCanceled(true);
        }
    }

    public static void onEntityJoinWorldHandler(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        // 追踪箭 - 将普通箭转换为追踪箭
        if (event.getEntity() instanceof Arrow arrow
                && arrow.getOwner() instanceof LivingEntity shooter 
                && !(event.getEntity() instanceof HomingArrowEntity)) {
            MobEffectInstance homingInstance = shooter.getEffect(HOMING);
            if (homingInstance != null) {
                int level = homingInstance.getAmplifier() + 1;
                HomingArrowEntity homingArrow = new HomingArrowEntity(event.getLevel(), shooter, level);

                // 复制箭的属性
                homingArrow.setUUID(arrow.getUUID());
                homingArrow.setId(arrow.getId());
                homingArrow.setOwner(shooter);
                homingArrow.setPos(arrow.getX(), arrow.getY(), arrow.getZ());
                homingArrow.setBaseDamage(arrow.getBaseDamage());
                homingArrow.setDeltaMovement(arrow.getDeltaMovement());
                ((AbstractArrowAccessor) homingArrow).callOnSetPierceLevel(arrow.getPierceLevel());
                homingArrow.setCritArrow(arrow.isCritArrow());
                homingArrow.pickup = arrow.pickup;

                // 移除原箭，添加追踪箭
                event.setCanceled(true);
                event.getLevel().addFreshEntity(homingArrow);
            }
        }

        if (event.getEntity() instanceof AreaEffectCloud cloud && cloud instanceof AreaEffectCloudAccessor cloudAccessor) {

            // 获取当前云的效果
            List<MobEffectInstance> effects = Lists.newArrayList(cloudAccessor.getPotionContents().getAllEffects().iterator());

            ParticleOptions particle = cloud.getParticle();

            boolean hasRandomEffect = false;
            List<MobEffectInstance> newEffects = new ArrayList<>();

            for (MobEffectInstance originalEffect : effects) {
                Holder<MobEffect> effectType = originalEffect.getEffect();

                Supplier<Holder<MobEffect>> supplier = null;

                if (effectType == RANDOM_POSITIVE_EFFECT.get()) {
                    supplier = EffectUtils::getRandomGoodEffect;
                } else if (effectType == RANDOM_NEGATIVE_EFFECT.get()) {
                    supplier = EffectUtils::getRandomBadEffect;
                } else if (effectType == RANDOM_EFFECT.get()) {
                    supplier = EffectUtils::getRandomAllEffect;
                }

                if (supplier != null) {
                    hasRandomEffect = true;
                    Holder<MobEffect> randomEffect = supplier.get();
                    newEffects.add(new MobEffectInstance(
                            randomEffect,
                            originalEffect.getDuration(),
                            originalEffect.getAmplifier(),
                            originalEffect.isAmbient(),
                            originalEffect.isVisible()
                    ));
                } else {
                    newEffects.add(originalEffect);
                }
            }

            // 替换效果
            if (hasRandomEffect) {
                PotionContents newContents = new PotionContents(
                        Optional.empty(),
                        Optional.empty(),
                        newEffects
                );
                cloud.setPotionContents(newContents);
                cloud.setParticle(particle);
            }
        }

        if (!(event.getEntity() instanceof Player) && DeathMobEffect.getDeathEntity().contains(event.getEntity().getType())) {
            event.setCanceled(true);
        }
    }

    public static void onLivingDeathHandler(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        if (DeathMobEffect.getDeathEntity().contains(target.getType())) {
            event.setCanceled(false);
        }
        handleCompanionEffect(event.getEntity().level(), event.getEntity());
    }

    private static void handleCompanionEffect(LevelAccessor world, LivingEntity deadEntity) {
        if (deadEntity == null) return;

        List<LivingEntity> nearbyEntities = new ArrayList<>(
                world.getEntitiesOfClass(LivingEntity.class, deadEntity.getBoundingBox().inflate(16))
        );

        if (deadEntity instanceof OwnableEntity ownableEntity) {
            LivingEntity owner = ownableEntity.getOwner();
            if (owner != null && !nearbyEntities.contains(owner)) {
                nearbyEntities.add(owner);
            }
        }

        for (LivingEntity living : nearbyEntities) {
            MobEffectInstance instance = living.getEffect(COMPANION);
            if (instance != null && !living.equals(deadEntity)) {
                int amplifier = instance.getAmplifier();

                double maxDistance = amplifier + 3;
                if (living.distanceToSqr(deadEntity) <= maxDistance * maxDistance) {
                    int deathCount = living.getPersistentData().getInt("companion_death_num");

                    if (deathCount < amplifier + 2) {
                        living.getPersistentData().putInt("companion_death_num", deathCount + 1);

                        if (living instanceof Player player) {
                            CompanionMobEffect.addCompanionEffect(player, amplifier, deathCount);
                        } else {
                            CompanionMobEffect.removeCompanionAttributes(living);

                            CompanionMobEffect.ATTRIBUTE_CONFIGS.forEach((attribute, config) -> {
                                double value = config.valueCalculator().calculate(living, deathCount, amplifier);
                                AttributeInstance attrInstance = living.getAttribute(attribute);
                                if (attrInstance != null) {
                                    attrInstance.addPermanentModifier(new AttributeModifier(
                                            config.id(), value, AttributeModifier.Operation.ADD_VALUE
                                    ));
                                }
                            });
                        }

                        float healAmount = (float) (living.getMaxHealth() * 0.25 * (amplifier + 1));
                        living.heal(healAmount);

                        if (world instanceof ServerLevel level) {
                            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, living.getX(), living.getY(), living.getZ(), 20, 0.2, 0.2, 0.2, 0.2);
                            level.playSound(null, living.getOnPos(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 2, 1);
                        }
                    }
                }
            }
        }
    }

    public static void onEntityJumpHandler(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity().hasEffect(IMPRISON)) {
            event.getEntity().setDeltaMovement(Vec3.ZERO);
        }
    }

    public static void onEntityTeleportHandler(EntityTeleportEvent event) {
        if (event.getEntity() instanceof LivingEntity target) {
            // 空间锚定 - 阻止传送
            MobEffectInstance spatialAnchor = target.getEffect(SPATIAL_ANCHOR);
            if (spatialAnchor != null) {
                int level = spatialAnchor.getAmplifier() + 1;
                if (event instanceof EntityTeleportEvent.TeleportCommand) {
                    if (level >= 3) {
                        event.setCanceled(true);
                    }
                } else {
                    event.setTargetX(event.getPrevX());
                    event.setTargetY(event.getPrevY());
                    event.setTargetZ(event.getPrevZ());
                }
            }
        }
    }

    public static void onEntityTravelToDimensionHandler(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof LivingEntity target) {
            // 空间锚定 - 等级>=2阻止跨维度
            MobEffectInstance spatialAnchor = target.getEffect(SPATIAL_ANCHOR);
            if (spatialAnchor != null && spatialAnchor.getAmplifier() >= 1) {
                event.setCanceled(true);
            }
        }
    }
}