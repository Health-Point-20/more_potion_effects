package com.yixi_xun.more_potion_effects.api;

import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class HurtManager {

    // 存储额外伤害信息的Map
    private static final Map<LivingEntity, List<ExtraHurtData>> extraHurtQueue = new ConcurrentHashMap<>();

    // 存储玩家横扫攻击的主要目标
    private static final Map<Player, UUID> mainTargetMap = new ConcurrentHashMap<>();

    private static boolean onDealExtraDamage = false;

    public record ExtraHurtData(DamageSource source, float damage) {}

    /**
     * 获取玩家横扫攻击的主要目标
     */
    public static UUID getMainTarget(Player player) {
        return mainTargetMap.getOrDefault(player, null);
    }

    /**
     * 添加额外伤害到队列中
     */
    public static void extraHurt(LivingEntity target, DamageSource source, float damage) {
       if (!onDealExtraDamage && target.isAlive() && !target.level().isClientSide()) {
            extraHurtQueue.computeIfAbsent(target, k -> new ArrayList<>()).add(new ExtraHurtData(source, damage));
        }
    }
    
    /**
     * 处理单个实体的额外伤害
     */
    private static void processExtraHurt(LivingEntity target, ExtraHurtData data) {
        int origInvulnerableTime = target.invulnerableTime;

        try {
            target.invulnerableTime = 0;
            target.hurt(data.source(), data.damage());
        } finally {
            // 恢复原始状态
            target.invulnerableTime = origInvulnerableTime;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTick(ServerTickEvent.Post event) {
        onDealExtraDamage = true;
        for (LivingEntity target : new HashSet<>(extraHurtQueue.keySet())) {
            if (!target.isAlive()) {
                extraHurtQueue.remove(target);
            } else {
                List<ExtraHurtData> extraHurtDataList = extraHurtQueue.get(target);

                if (extraHurtDataList == null) continue;

                for (ExtraHurtData extraHurtData : extraHurtDataList) {
                    if (extraHurtData != null) {
                        // 处理额外伤害
                        processExtraHurt(target, extraHurtData);
                    }
                }
                // 清除此实体的额外伤害数据
                extraHurtQueue.remove(target);
            }
        }
        onDealExtraDamage = false;
    }

    /**
     * 获取玩家横扫攻击的主要目标
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        mainTargetMap.put(event.getEntity(), event.getTarget().getUUID());
        // 清理主要攻击目标
        MorePotionEffectsMod.queueServerWork(1, () -> mainTargetMap.remove(event.getEntity()));
    }
}