package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

public class MoreRangeMobEffect extends MobEffect {

    private static final ResourceLocation ENTITY_INTERACTION_RANGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("more_potion_effects", "effect.more_range_entity_interaction");

    public MoreRangeMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -16777216);
    }

    @Override
    public void addAttributeModifiers(@NotNull AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(attributeMap, amplifier);
        int level = amplifier + 1;

        AttributeInstance entityInteractionRangeInstance = attributeMap.getInstance(Attributes.ENTITY_INTERACTION_RANGE);
        if (entityInteractionRangeInstance != null) {
            AttributeModifier modifier = new AttributeModifier(
                    ENTITY_INTERACTION_RANGE_MODIFIER_ID,
                    1.0 * level,
                    AttributeModifier.Operation.ADD_VALUE
            );
            if (!entityInteractionRangeInstance.hasModifier(modifier.id())) {
                entityInteractionRangeInstance.addTransientModifier(modifier);
            }
        }
    }

    @Override
    public void removeAttributeModifiers(@NotNull AttributeMap attributeMap) {
        super.removeAttributeModifiers(attributeMap);

        AttributeInstance entityInteractionRangeInstance = attributeMap.getInstance(Attributes.ENTITY_INTERACTION_RANGE);
        if (entityInteractionRangeInstance != null) {
            entityInteractionRangeInstance.removeModifier(ENTITY_INTERACTION_RANGE_MODIFIER_ID);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}