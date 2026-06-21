package com.yixi_xun.more_potion_effects.event.handler;

import com.yixi_xun.more_potion_effects.mob_effects.CompanionMobEffect;
import com.yixi_xun.more_potion_effects.mob_effects.DeathMobEffect;
import com.yixi_xun.more_potion_effects.mob_effects.VeiledPresenceMobEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
        if (event.getEntity() instanceof net.minecraft.world.entity.projectile.Arrow arrow 
                && arrow.getOwner() instanceof LivingEntity shooter 
                && !(event.getEntity() instanceof com.yixi_xun.more_potion_effects.entity.HomingArrowEntity)) {
            MobEffectInstance homingInstance = shooter.getEffect(HOMING);
            if (homingInstance != null) {
                int level = homingInstance.getAmplifier() + 1;
                com.yixi_xun.more_potion_effects.entity.HomingArrowEntity homingArrow = 
                        new com.yixi_xun.more_potion_effects.entity.HomingArrowEntity(event.getLevel(), shooter, level);
                homingArrow.setOwner(shooter);
                homingArrow.setPos(arrow.getX(), arrow.getY(), arrow.getZ());
                homingArrow.setBaseDamage(arrow.getBaseDamage());
                homingArrow.setDeltaMovement(arrow.getDeltaMovement());
                homingArrow.setCritArrow(arrow.isCritArrow());
                homingArrow.pickup = arrow.pickup;
                event.setCanceled(true);
                event.getLevel().addFreshEntity(homingArrow);
            }
        }

        if (!(event.getEntity() instanceof ServerPlayer) && DeathMobEffect.getDeathEntity().contains(event.getEntity().getType())) {
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

    private static void handleCompanionEffect(LevelAccessor world, LivingEntity entity) {
        if (entity == null) return;

        List<LivingEntity> nearbyEntities = new ArrayList<>(world.getEntitiesOfClass(LivingEntity.class,
                entity.getBoundingBox().inflate(32),
                e -> true
        ).stream().sorted(Comparator.comparingDouble(target -> target.distanceToSqr(entity))).toList());

        if (entity instanceof OwnableEntity ownableEntity) {
            LivingEntity owner = ownableEntity.getOwner();
            if (owner != null) {
                nearbyEntities.add(owner);
            }
        }

        for (LivingEntity living : nearbyEntities) {
            MobEffectInstance instance = living.getEffect(COMPANION);
            if (instance != null && !living.equals(entity)) {
                int amplifier = instance.getAmplifier();
                if (amplifier + 3 >= living.distanceToSqr(entity)) {
                    int deathCount = living.getPersistentData().getInt("companion_death_num");

                    if (deathCount < amplifier + 2) {
                        living.getPersistentData().putInt("companion_death_num", deathCount + 1);

                        if (living instanceof net.minecraft.world.entity.player.Player player) {
                            CompanionMobEffect.addCompanionEffect(player, amplifier, deathCount);
                        }

                        float healAmount = (float) (living.getMaxHealth() * 0.25 * (amplifier + 1));
                        living.heal(healAmount);

                        if (world instanceof ServerLevel level) {
                            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, living.getX(), living.getY(), living.getZ(), 20, 0.2, 0.2, 0.2, 0.2);
                        }

                        if (world instanceof Level level) {
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
            // 禁锢效果 - 阻止所有传送
            if (target.hasEffect(IMPRISON)) {
                event.setTargetX(event.getPrevX());
                event.setTargetY(event.getPrevY());
                event.setTargetZ(event.getPrevZ());
                return;
            }
            // 空间锚定 - 阻止传送
            MobEffectInstance spatialAnchor = target.getEffect(SPATIAL_ANCHOR);
            if (spatialAnchor != null) {
                int level = spatialAnchor.getAmplifier() + 1;
                // 等级越高阻止的传送方式越多
                // 等级1-3: 阻止末影珍珠传送
                // 等级4+: 阻止所有传送
                if (level >= 4 || event instanceof net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderPearl) {
                    event.setCanceled(true);
                }
            }
        }
    }

    public static void onEntityTravelToDimensionHandler(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof LivingEntity target) {
            // 禁锢效果 - 阻止跨维度
            if (target.hasEffect(IMPRISON)) {
                event.setCanceled(true);
                return;
            }
            // 空间锚定 - 等级>=2阻止跨维度
            MobEffectInstance spatialAnchor = target.getEffect(SPATIAL_ANCHOR);
            if (spatialAnchor != null && spatialAnchor.getAmplifier() >= 1) {
                event.setCanceled(true);
            }
        }
    }
}