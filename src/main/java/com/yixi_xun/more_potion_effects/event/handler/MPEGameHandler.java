package com.yixi_xun.more_potion_effects.event.handler;

import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.yixi_xun.more_potion_effects.mob_effects.DeathMobEffect;
import com.yixi_xun.more_potion_effects.mob_effects.KineticMobEffect;

public class MPEGameHandler {
    public static void onGameStoppingHandler(GameShuttingDownEvent event) {
        DeathMobEffect.getDeathEntity().clear();
    }

    public static void onServerTickHandler(ServerTickEvent.Post event) {
        event.getServer().getPlayerList().getPlayers().forEach(player -> {
            // 获取玩家位置
            Vec3 currentPos = player.position();
            // 获取玩家初始位置
            Vec3 originalPos = KineticMobEffect.previousPos.getOrDefault(player.getUUID(), currentPos);
            // 计算玩家速度
            Vec3 velocity = currentPos.subtract(originalPos);
            // 存储玩家当前速度和位置
            KineticMobEffect.previousPos.put(player.getUUID(), currentPos);
            KineticMobEffect.velocities.put(player.getUUID(), velocity);
        });
    }
}
