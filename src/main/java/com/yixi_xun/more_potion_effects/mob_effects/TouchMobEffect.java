package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

public class TouchMobEffect extends MobEffect {

    private static final ResourceLocation BLOCK_INTERACTION_RANGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("more_potion_effects", "effect.touch_block_interaction");

    public TouchMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -65281);
    }

    @Override
    public void addAttributeModifiers(@NotNull AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(attributeMap, amplifier);
        int level = amplifier + 1;

        AttributeInstance blockInteractionRangeInstance = attributeMap.getInstance(Attributes.BLOCK_INTERACTION_RANGE);
        if (blockInteractionRangeInstance != null) {
            AttributeModifier modifier = new AttributeModifier(
                    BLOCK_INTERACTION_RANGE_MODIFIER_ID,
                    1.0 * level,
                    AttributeModifier.Operation.ADD_VALUE
            );
            if (!blockInteractionRangeInstance.hasModifier(modifier.id())) {
                blockInteractionRangeInstance.addTransientModifier(modifier);
            }
        }
    }

    @Override
    public void removeAttributeModifiers(@NotNull AttributeMap attributeMap) {
        super.removeAttributeModifiers(attributeMap);

        AttributeInstance blockInteractionRangeInstance = attributeMap.getInstance(Attributes.BLOCK_INTERACTION_RANGE);
        if (blockInteractionRangeInstance != null) {
            blockInteractionRangeInstance.removeModifier(BLOCK_INTERACTION_RANGE_MODIFIER_ID);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}