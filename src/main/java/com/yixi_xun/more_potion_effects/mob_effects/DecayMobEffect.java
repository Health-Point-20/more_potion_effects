package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.api.IMoreMobEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class DecayMobEffect extends MobEffect implements IMoreMobEffect {

    private static final ResourceLocation HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("more_potion_effects", "decay_health");

    public DecayMobEffect() {
        super(MobEffectCategory.HARMFUL, -13421773);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return true;

        // 累积计时
        entity.getPersistentData().putDouble("decay_time", entity.getPersistentData().getDouble("decay_time") + 1);

        // 发送粒子效果
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, entity.getX(), entity.getY(), entity.getZ(), 1, 0.1, 0.1, 0.1, 0.1);
        }

        // 每20tick处理一次
        if (entity.getPersistentData().getDouble("decay_time") >= 20) {
            int level = amplifier + 1;
            double decayHealth = entity.getPersistentData().getDouble("decay_health");
            double currentMaxHealth = entity.getAttributeValue(Attributes.MAX_HEALTH);

            if (currentMaxHealth - Math.pow(2, level) > 0) {
                decayHealth += Math.pow(2, level);
            } else {
                decayHealth += currentMaxHealth - 0.01;
            }

            entity.getPersistentData().putDouble("decay_health", decayHealth);

            // 更新最大生命值属性 - 使用ResourceLocation方式
            AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.removeModifier(HEALTH_MODIFIER_ID);
                maxHealth.addTransientModifier(
                        new AttributeModifier(HEALTH_MODIFIER_ID, -decayHealth, AttributeModifier.Operation.ADD_VALUE));
            }

            // 如果当前生命值超过最大生命值，调整生命值
            if (entity.getHealth() > entity.getAttributeValue(Attributes.MAX_HEALTH)) {
                entity.setHealth((float) entity.getAttributeValue(Attributes.MAX_HEALTH));
            }

            entity.getPersistentData().putDouble("decay_time", 0);
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onEffectRemoved(@NotNull LivingEntity entity, MobEffectInstance instance) {
        // 移除属性修改器
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(HEALTH_MODIFIER_ID);
        }
        // 重置持久化数据
        entity.getPersistentData().putDouble("decay_health", 0);
        entity.getPersistentData().putDouble("decay_time", 0);
    }
}