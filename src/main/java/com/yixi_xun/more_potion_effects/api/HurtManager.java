package com.yixi_xun.more_potion_effects.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class HurtManager {

    // 存储额外伤害信息的Map
    private static final Map<LivingEntity, List<ExtraHurtData>> extraHurtQueue = new ConcurrentHashMap<>();

    private static boolean onDealExtraDamage = false;

    public record ExtraHurtData(DamageSource source, float damage) {}
    
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
}