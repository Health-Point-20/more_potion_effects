package com.yixi_xun.more_potion_effects.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import static com.yixi_xun.more_potion_effects.event.handler.MPECombatHandler.*;

@EventBusSubscriber
public class MPEEvent {

    // 这些方法将在后续版本中逐步实现

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        onHealHandler(event);
    }

    @SubscribeEvent
    public static void onEntityAttacked(LivingIncomingDamageEvent event) {
        onAttackHandler(event);
    }

    @SubscribeEvent
    public static void onEntityDamage(LivingDamageEvent.Pre event) {
        onDamageHandler(event);
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        onLivingDeathHandler(event);
    }



    // 核心事件处理方法保留
    
    @SubscribeEvent
    public static void onItemToolTip(ItemTooltipEvent event) {
        // 物品提示处理逻辑 - 待实现
    }

    @SubscribeEvent
    public static void onLivingSetAttackTarget(LivingChangeTargetEvent event) {
        // 攻击目标变更处理逻辑 - 待实现
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        // 实体加入世界处理逻辑 - 待实现
    }

    @SubscribeEvent
    public static void onGameStopping(GameShuttingDownEvent event) {
        // 游戏关闭处理逻辑 - 待实现
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        // 物品投掷处理逻辑 - 待实现
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // 服务器刻处理逻辑 - 待实现
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 玩家登出处理逻辑 - 待实现
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        // 投射物撞击处理逻辑 - 待实现
    }

    @SubscribeEvent
    public static void onEntityJump(LivingEvent.LivingJumpEvent event) {
        // 实体跳跃处理逻辑 - 待实现
    }

    @SubscribeEvent
    public static void onLivingEating(LivingEntityUseItemEvent.Finish event) {
        // 生物进食处理逻辑 - 待实现
    }

    @SubscribeEvent
    public static void onEntityTeleport(EntityTeleportEvent event) {
        // 实体传送处理逻辑 - 待实现
    }

    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        // 实体跨维度旅行处理逻辑 - 待实现
    }

}
