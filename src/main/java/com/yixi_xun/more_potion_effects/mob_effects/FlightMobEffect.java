package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import com.yixi_xun.more_potion_effects.api.IMoreMobEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


public class FlightMobEffect extends MobEffect implements IMoreMobEffect {

    // PERSISTENT_DATA键名
    private static final String FLIGHT_SPEED_DELTA_KEY = "flight_speed_delta";
    // CREATIVE_FLIGHT 属性修改器的 ResourceLocation 标识
    private static final ResourceLocation FLIGHT_EFFECT_ID = ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "flight_effect");

    public FlightMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -52);
    }

    @Override
    public void onEffectAdded(@NotNull LivingEntity entity, int amplifier) {
        super.onEffectAdded(entity, amplifier);
        
        if (entity instanceof Player player) {
            int level = amplifier + 1;
            
            // 记录飞行速度增量到PERSISTENT_DATA
            float speedDelta = 0.005f * level;
            player.getPersistentData().putFloat(FLIGHT_SPEED_DELTA_KEY, speedDelta);
            
            // 通过 NeoForge CREATIVE_FLIGHT 属性允许飞行
            Objects.requireNonNull(player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT)).addPermanentModifier(
                    new AttributeModifier(FLIGHT_EFFECT_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
            player.getAbilities().flying = true;
            
            // 增加飞行速度
            float newSpeed = player.getAbilities().getFlyingSpeed() + speedDelta;
            player.getAbilities().setFlyingSpeed(newSpeed);
            
            player.onUpdateAbilities();
        }
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            // 确保飞行权限持续生效
            if (!player.mayFly()) {
                Objects.requireNonNull(player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT)).addPermanentModifier(
                        new AttributeModifier(FLIGHT_EFFECT_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
                player.getAbilities().flying = true;
                
                // 恢复飞行速度（基于保存的速度增量）
                float speedDelta = player.getPersistentData().getFloat(FLIGHT_SPEED_DELTA_KEY);
                if (speedDelta > 0) {
                    float currentSpeed = player.getAbilities().getFlyingSpeed();
                    float targetSpeed = currentSpeed + speedDelta;
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
            // 减去飞行速度增量
            float speedDelta = player.getPersistentData().getFloat(FLIGHT_SPEED_DELTA_KEY);
            float currentSpeed = player.getAbilities().getFlyingSpeed();
            float restoredSpeed = Math.max(currentSpeed - speedDelta, 0.05f);
            player.getAbilities().setFlyingSpeed(restoredSpeed);
            
            // 移除 CREATIVE_FLIGHT 属性修改器
            Objects.requireNonNull(player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT)).removeModifier(FLIGHT_EFFECT_ID);
            
            // 仅在非创造模式/非观察者模式下停止飞行
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().flying = false;
            }
            
            player.onUpdateAbilities();
            
            // 清理PERSISTENT_DATA
            player.getPersistentData().remove(FLIGHT_SPEED_DELTA_KEY);
        }
    }
}