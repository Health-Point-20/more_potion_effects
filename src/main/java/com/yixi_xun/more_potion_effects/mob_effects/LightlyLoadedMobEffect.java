package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

public class LightlyLoadedMobEffect extends MobEffect {

    private static final ResourceLocation MOVEMENT_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("more_potion_effects", "effect.lightly_loaded_speed");
    private static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("more_potion_effects", "effect.lightly_loaded_armor");
    private static final ResourceLocation ARMOR_TOUGHNESS_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("more_potion_effects", "effect.lightly_loaded_armor_toughness");

    public LightlyLoadedMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -5592406);
    }

    @Override
    public void addAttributeModifiers(@NotNull AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(attributeMap, amplifier);
        int level = amplifier + 1;

        AttributeInstance speedInstance = attributeMap.getInstance(Attributes.MOVEMENT_SPEED);
        if (speedInstance != null) {
            AttributeModifier modifier = new AttributeModifier(
                    MOVEMENT_SPEED_MODIFIER_ID,
                    0.2 * level,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            if (!speedInstance.hasModifier(modifier.id())) {
                speedInstance.addTransientModifier(modifier);
            }
        }

        AttributeInstance armorInstance = attributeMap.getInstance(Attributes.ARMOR);
        if (armorInstance != null) {
            AttributeModifier modifier = new AttributeModifier(
                    ARMOR_MODIFIER_ID,
                    -2.0 * level,
                    AttributeModifier.Operation.ADD_VALUE
            );
            if (!armorInstance.hasModifier(modifier.id())) {
                armorInstance.addTransientModifier(modifier);
            }
        }

        AttributeInstance armorToughnessInstance = attributeMap.getInstance(Attributes.ARMOR_TOUGHNESS);
        if (armorToughnessInstance != null) {
            AttributeModifier modifier = new AttributeModifier(
                    ARMOR_TOUGHNESS_MODIFIER_ID,
                    -2.0 * level,
                    AttributeModifier.Operation.ADD_VALUE
            );
            if (!armorToughnessInstance.hasModifier(modifier.id())) {
                armorToughnessInstance.addTransientModifier(modifier);
            }
        }
    }

    @Override
    public void removeAttributeModifiers(@NotNull AttributeMap attributeMap) {
        super.removeAttributeModifiers(attributeMap);

        AttributeInstance speedInstance = attributeMap.getInstance(Attributes.MOVEMENT_SPEED);
        if (speedInstance != null) {
            speedInstance.removeModifier(MOVEMENT_SPEED_MODIFIER_ID);
        }

        AttributeInstance armorInstance = attributeMap.getInstance(Attributes.ARMOR);
        if (armorInstance != null) {
            armorInstance.removeModifier(ARMOR_MODIFIER_ID);
        }

        AttributeInstance armorToughnessInstance = attributeMap.getInstance(Attributes.ARMOR_TOUGHNESS);
        if (armorToughnessInstance != null) {
            armorToughnessInstance.removeModifier(ARMOR_TOUGHNESS_MODIFIER_ID);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}