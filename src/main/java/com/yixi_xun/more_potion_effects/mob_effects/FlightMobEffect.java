package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.api.IMobEffectRemovable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class FlightMobEffect extends MobEffect implements IMobEffectRemovable {

    // PERSISTENT_DATA键名
    private static final String ORIGINAL_FLIGHT_SPEED_KEY = "original_flight_speed";
    private static final String FLIGHT_LEVEL_KEY = "flight_level";

    public FlightMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -52);
    }

    @Override
    public void onEffectAdded(@NotNull LivingEntity entity, int amplifier) {
        super.onEffectAdded(entity, amplifier);
        
        if (entity instanceof Player player) {
            int level = amplifier + 1;
            
            // 保存原始飞行速度到PERSISTENT_DATA
            float originalSpeed = player.getAbilities().getFlyingSpeed();
            player.getPersistentData().putFloat(ORIGINAL_FLIGHT_SPEED_KEY, originalSpeed);
            player.getPersistentData().putInt(FLIGHT_LEVEL_KEY, level);
            
            // 允许飞行
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            
            // 增加飞行速度
            float newSpeed = originalSpeed + 0.005f * level;
            player.getAbilities().setFlyingSpeed(newSpeed);
            
            player.onUpdateAbilities();
        }
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            // 确保飞行权限持续生效
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                
                // 恢复飞行速度（基于保存的原始速度）
                int level = amplifier + 1;
                float originalSpeed = player.getPersistentData().getFloat(ORIGINAL_FLIGHT_SPEED_KEY);
                if (originalSpeed > 0) {
                    float currentSpeed = player.getAbilities().getFlyingSpeed();
                    float targetSpeed = originalSpeed + 0.005f * level;
                    // 如果当前速度低于目标速度，逐步增加
                    if (currentSpeed < targetSpeed && currentSpeed < 0.1f) {
                        player.getAbilities().setFlyingSpeed(Math.min(currentSpeed + 0.001f, targetSpeed));
                    }
                }
                
                player.onUpdateAbilities();
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onEffectRemoved(@NotNull LivingEntity entity, MobEffectInstance instance) {
        if (entity instanceof Player player) {
            // 恢复原始飞行速度
            float originalSpeed = player.getPersistentData().getFloat(ORIGINAL_FLIGHT_SPEED_KEY);
            if (originalSpeed > 0) {
                player.getAbilities().setFlyingSpeed(originalSpeed);
            } else {
                // 如果没有保存原始速度，恢复到默认值
                player.getAbilities().setFlyingSpeed(0.05f);
            }
            
            // 移除飞行权限（仅在非创造模式/非观察者模式下）
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
            }
            
            player.onUpdateAbilities();
            
            // 清理PERSISTENT_DATA
            player.getPersistentData().remove(ORIGINAL_FLIGHT_SPEED_KEY);
            player.getPersistentData().remove(FLIGHT_LEVEL_KEY);
        }
    }
}