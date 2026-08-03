package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.api.IMoreMobEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class HeavyMoreEffect extends MobEffect implements IMoreMobEffect {

    // 属性修饰符ID
    private static final ResourceLocation GRAVITY_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("more_potion_effects", "effect.heavy_gravity");
    private static final ResourceLocation MOVEMENT_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("more_potion_effects", "effect.heavy_speed");
    private static final ResourceLocation STEP_HEIGHT_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("more_potion_effects", "effect.heavy_step_height");

    public HeavyMoreEffect() {
        super(MobEffectCategory.HARMFUL, -10092544);
    }

    @Override
    public void addAttributeModifiers(@NotNull AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(attributeMap, amplifier);
        int level = amplifier + 1;

        // 增加重力 (GRAVITY是原版属性)
        AttributeInstance gravityInstance = attributeMap.getInstance(Attributes.GRAVITY);
        if (gravityInstance != null) {
            AttributeModifier gravityModifier = new AttributeModifier(
                    GRAVITY_MODIFIER_ID,
                    0.25 * level,  // 增加0.25重力每级
                    AttributeModifier.Operation.ADD_VALUE
            );
            if (!gravityInstance.hasModifier(gravityModifier.id())) {
                gravityInstance.addTransientModifier(gravityModifier);
            }
        }

        // 减少移动速度
        AttributeInstance speedInstance = attributeMap.getInstance(Attributes.MOVEMENT_SPEED);
        if (speedInstance != null) {
            AttributeModifier speedModifier = new AttributeModifier(
                    MOVEMENT_SPEED_MODIFIER_ID,
                    -0.33 * level,  // 减少33%移动速度每级
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            if (!speedInstance.hasModifier(speedModifier.id())) {
                speedInstance.addTransientModifier(speedModifier);
            }
        }

        // 减少台阶攀爬高度 (使用Minecraft 1.21.1原版属性STEP_HEIGHT)
        AttributeInstance stepHeightInstance = attributeMap.getInstance(Attributes.STEP_HEIGHT);
        if (stepHeightInstance != null) {
            AttributeModifier stepHeightModifier = new AttributeModifier(
                    STEP_HEIGHT_MODIFIER_ID,
                    -0.5 * level,  // 减少0.5台阶高度每级
                    AttributeModifier.Operation.ADD_VALUE
            );
            if (!stepHeightInstance.hasModifier(stepHeightModifier.id())) {
                stepHeightInstance.addTransientModifier(stepHeightModifier);
            }
        }
    }

    @Override
    public void removeAttributeModifiers(@NotNull AttributeMap attributeMap) {
        super.removeAttributeModifiers(attributeMap);

        // 移除重力修饰符
        AttributeInstance gravityInstance = attributeMap.getInstance(Attributes.GRAVITY);
        if (gravityInstance != null) {
            gravityInstance.removeModifier(GRAVITY_MODIFIER_ID);
        }

        // 移除移动速度修饰符
        AttributeInstance speedInstance = attributeMap.getInstance(Attributes.MOVEMENT_SPEED);
        if (speedInstance != null) {
            speedInstance.removeModifier(MOVEMENT_SPEED_MODIFIER_ID);
        }

        // 移除台阶高度修饰符
        AttributeInstance stepHeightInstance = attributeMap.getInstance(Attributes.STEP_HEIGHT);
        if (stepHeightInstance != null) {
            stepHeightInstance.removeModifier(STEP_HEIGHT_MODIFIER_ID);
        }
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        // 停止疾跑
        entity.setSprinting(false);

        // 反转向上速度（快速下落）
        Vec3 motion = entity.getDeltaMovement();
        if (motion.y > 0) {
            entity.setDeltaMovement(new Vec3(motion.x, Math.abs(motion.y) * (-1), motion.z));
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onEffectRemoved(@NotNull LivingEntity entity, MobEffectInstance instance) {
        // 属性修饰符已在removeAttributeModifiers中移除，无需额外处理
    }
}