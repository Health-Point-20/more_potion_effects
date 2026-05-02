package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jetbrains.annotations.NotNull;

public class ImprisonMobEffect extends MobEffect {
	// 使用常量定义ResourceLocation和修饰符名称
	private static final ResourceLocation MOVE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("more_potion_effects", "effect.imprison_move");
	private static final ResourceLocation SWIM_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("more_potion_effects", "effect.imprison_swim");
	private static final ResourceLocation FLY_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("more_potion_effects", "effect.imprison_fly");

	public ImprisonMobEffect() {
		super(MobEffectCategory.HARMFUL, -16751002);
	}

	@Override
	public void addAttributeModifiers(AttributeMap attributeMap, int amplifier) {
		AttributeModifier moveModifier = new AttributeModifier(
				MOVE_MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		);

		AttributeInstance moveInstance = attributeMap.getInstance(Attributes.MOVEMENT_SPEED);
		if (moveInstance != null && !moveInstance.hasModifier(moveModifier.id())) {
			moveInstance.addTransientModifier(moveModifier);
		}

		Holder<Attribute> swimSpeed = NeoForgeMod.SWIM_SPEED;
		AttributeInstance swimInstance = attributeMap.getInstance(swimSpeed);
		if (swimInstance != null) {
			AttributeModifier swimModifier = new AttributeModifier(
					SWIM_MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
			);
			if (!swimInstance.hasModifier(swimModifier.id())) {
				swimInstance.addTransientModifier(swimModifier);
			}
		}

		AttributeInstance flyInstance = attributeMap.getInstance(Attributes.FLYING_SPEED);
		if (flyInstance != null) {
			AttributeModifier flyModifier = new AttributeModifier(
					FLY_MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
			);
			if (!flyInstance.hasModifier(flyModifier.id())) {
				flyInstance.addTransientModifier(flyModifier);
			}
		}
	}

	@Override
	public void onEffectAdded(@NotNull LivingEntity entity, int amplifier) {
		super.onEffectAdded(entity, amplifier);
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		entity.setDeltaMovement(Vec3.ZERO);

		if (entity.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(
					ParticleTypes.CRIT,
					entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
					1,
					0.25, 0.5, 0.25,
					0.05
			);
		}
		return true;
	}

	@Override
	public void removeAttributeModifiers(@NotNull AttributeMap attributeMap) {
		super.removeAttributeModifiers(attributeMap);

		AttributeInstance moveInstance = attributeMap.getInstance(Attributes.MOVEMENT_SPEED);
		if (moveInstance != null) {
			moveInstance.removeModifier(MOVE_MODIFIER_ID);
		}

		Holder<Attribute> swimSpeed = NeoForgeMod.SWIM_SPEED;
		AttributeInstance swimInstance = attributeMap.getInstance(swimSpeed);
		if (swimInstance != null) {
			swimInstance.removeModifier(SWIM_MODIFIER_ID);
		}

		AttributeInstance flyInstance = attributeMap.getInstance(Attributes.FLYING_SPEED);
		if (flyInstance != null) {
			flyInstance.removeModifier(FLY_MODIFIER_ID);
		}

	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}