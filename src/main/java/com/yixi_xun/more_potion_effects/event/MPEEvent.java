package com.yixi_xun.more_potion_effects.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import static com.yixi_xun.more_potion_effects.event.handler.MPECombatHandler.*;
import static com.yixi_xun.more_potion_effects.event.handler.MPEEntityHandler.onEntityJoinWorldHandler;
import static com.yixi_xun.more_potion_effects.event.handler.MPEEntityHandler.onEntityJumpHandler;
import static com.yixi_xun.more_potion_effects.event.handler.MPEEntityHandler.onEntityTeleportHandler;
import static com.yixi_xun.more_potion_effects.event.handler.MPEEntityHandler.onEntityTravelToDimensionHandler;
import static com.yixi_xun.more_potion_effects.event.handler.MPEEntityHandler.onLivingSetAttackTargetHandler;
import static com.yixi_xun.more_potion_effects.event.handler.MPEEnchantmentHandler.*;
import static com.yixi_xun.more_potion_effects.event.handler.MPEPlayerHandler.*;
import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.PIERCE;

@EventBusSubscriber
public class MPEEvent {

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        onHealHandler(event);
    }

    @SubscribeEvent
    public static void onEntityAttacked(LivingIncomingDamageEvent event) {
        onAttackHandler(event);
        if (!event.isCanceled()) {
            onHurtHandler(event);
        }
    }

    @SubscribeEvent
    public static void onEntityDamage(LivingDamageEvent.Pre event) {
        onDamageHandler(event);
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        com.yixi_xun.more_potion_effects.event.handler.MPEEntityHandler.onLivingDeathHandler(event);
        com.yixi_xun.more_potion_effects.event.handler.MPECombatHandler.onLivingDeathHandler(event);
    }

    @SubscribeEvent
    public static void onLivingUseItemTick(LivingEntityUseItemEvent.Tick event) {
        onPlayerUseItemTickHandler(event);
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        onEntityJoinWorldHandler(event);
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        onItemTossHandler(event);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        onPlayerLoggedOutHandler(event);
    }

    @SubscribeEvent
    public static void onEntityJump(LivingEvent.LivingJumpEvent event) {
        onEntityJumpHandler(event);
    }

    @SubscribeEvent
    public static void onLivingSetAttackTarget(LivingChangeTargetEvent event) {
        onLivingSetAttackTargetHandler(event);
    }

    @SubscribeEvent
    public static void onLivingEating(LivingEntityUseItemEvent.Finish event) {
        onLivingEatingHandler(event);
    }

    @SubscribeEvent
    public static void onEntityTeleport(EntityTeleportEvent event) {
        onEntityTeleportHandler(event);
    }

    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        onEntityTravelToDimensionHandler(event);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        onBreakHandler(event);
    }

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        onHarvestCheckHandler(event);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        // 穿透效果
        if (event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hitResult
                && event.getProjectile() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow
                && hitResult.getEntity() instanceof LivingEntity
                && arrow.getOwner() instanceof LivingEntity shooter) {
            MobEffectInstance pierceEffect = shooter.getEffect(PIERCE);
            if (pierceEffect != null) {
                CompoundTag data = arrow.getPersistentData();
                if (!data.contains("extra_pierce")) {
                    data.putBoolean("extra_pierce", true);
                    ((com.yixi_xun.more_potion_effects.mixin.AbstractArrowAccessor) arrow)
                            .invokeSetPierceLevel((byte) (arrow.getPierceLevel() + pierceEffect.getAmplifier() + 1));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // 服务器刻处理
    }

    // ==================== 附魔事件 ====================

    @SubscribeEvent
    public static void onEquipmentChangeEvent(LivingEquipmentChangeEvent event) {
        onEquipmentChange(event);
    }

    @SubscribeEvent
    public static void onEnchantmentHurt(LivingIncomingDamageEvent event) {
        onLivingHurt(event);
    }

    @SubscribeEvent
    public static void onEnchantmentDamageEvent(LivingDamageEvent.Pre event) {
        onLivingDamage(event);
    }

    @SubscribeEvent
    public static void onEnchantmentDeathEvent(LivingDeathEvent event) {
        onLivingDeath(event);
    }

    @SubscribeEvent
    public static void onEnchantmentEffectRemove(MobEffectEvent.Remove event) {
        onEffectRemove(event);
    }
}
