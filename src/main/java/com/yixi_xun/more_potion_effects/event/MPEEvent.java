package com.yixi_xun.more_potion_effects.event;

import com.yixi_xun.more_potion_effects.event.handler.MPECombatHandler;
import com.yixi_xun.more_potion_effects.event.handler.MPEEntityHandler;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import static com.yixi_xun.more_potion_effects.event.handler.MPECombatHandler.*;
import static com.yixi_xun.more_potion_effects.event.handler.MPEEntityHandler.*;
import static com.yixi_xun.more_potion_effects.event.handler.MPEEnchantmentHandler.*;
import static com.yixi_xun.more_potion_effects.event.handler.MPEGameHandler.*;
import static com.yixi_xun.more_potion_effects.event.handler.MPEPlayerHandler.*;

@EventBusSubscriber
public class MPEEvent {

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        onHealHandler(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityAttacked(LivingIncomingDamageEvent event) {
        onAttackHandler(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityHurt(LivingIncomingDamageEvent event) {
        onHurtHandler(event);
    }

    @SubscribeEvent
    public static void onEntityDamage(LivingDamageEvent.Pre event) {
        onDamagePreHandler(event);
    }

    @SubscribeEvent
    public static void onEntityDamage(LivingDamageEvent.Post event) {
        onDamagePostHandler(event);
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        MPECombatHandler.onLivingDeathHandler(event);
        MPEEntityHandler.onLivingDeathHandler(event);
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
        onProjectileImpactHandler(event);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        onServerTickHandler(event);
    }

    @SubscribeEvent
    public static void onGameShutdown(GameShuttingDownEvent event) {
        onGameStoppingHandler(event);
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
