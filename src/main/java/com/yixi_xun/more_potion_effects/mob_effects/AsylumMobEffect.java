package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

import static com.yixi_xun.more_potion_effects.MorePotionEffectsMod.MOD_ID;

public class AsylumMobEffect extends MobEffect {

    public AsylumMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -16711936);
        addAttributeModifier(Attributes.MAX_ABSORPTION, ResourceLocation.fromNamespaceAndPath(MOD_ID ,"asylum"), 0, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        int level = amplifier + 1;
        CompoundTag tag = entity.getPersistentData();
        tag.putDouble("asylum_time", Math.min(level * 150, tag.getDouble("asylum_time") + Math.pow(level, 0.6)));

        if (tag.getDouble("asylum_time") >= 100) {
            if (entity.getAbsorptionAmount() < level * 4) {
                var absorptionAttr = entity.getAttribute(Attributes.MAX_ABSORPTION);
                if (absorptionAttr != null) {
                    absorptionAttr.addOrUpdateTransientModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath(MOD_ID, "asylum"), level * 4, AttributeModifier.Operation.ADD_VALUE));
                }
                entity.setAbsorptionAmount((float) (level * 4));
                entity.getPersistentData().putDouble("asylum_time", (entity.getPersistentData().getDouble("asylum_time") - 100));
            }
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}