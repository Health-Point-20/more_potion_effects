package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

public class StepUpMobEffect extends MobEffect {

    private static final ResourceLocation STEP_HEIGHT_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("more_potion_effects", "effect.step_up_step_height");

    public StepUpMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -5592406);
    }

    @Override
    public void addAttributeModifiers(@NotNull AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(attributeMap, amplifier);
        int level = amplifier + 1;

        AttributeInstance stepHeightInstance = attributeMap.getInstance(Attributes.STEP_HEIGHT);
        if (stepHeightInstance != null) {
            AttributeModifier modifier = new AttributeModifier(
                    STEP_HEIGHT_MODIFIER_ID,
                    0.5 * level,
                    AttributeModifier.Operation.ADD_VALUE
            );
            if (!stepHeightInstance.hasModifier(modifier.id())) {
                stepHeightInstance.addTransientModifier(modifier);
            }
        }
    }

    @Override
    public void removeAttributeModifiers(@NotNull AttributeMap attributeMap) {
        super.removeAttributeModifiers(attributeMap);

        AttributeInstance stepHeightInstance = attributeMap.getInstance(Attributes.STEP_HEIGHT);
        if (stepHeightInstance != null) {
            stepHeightInstance.removeModifier(STEP_HEIGHT_MODIFIER_ID);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}