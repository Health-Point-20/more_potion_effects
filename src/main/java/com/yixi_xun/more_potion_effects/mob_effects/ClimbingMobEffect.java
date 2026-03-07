
package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ClimbingMobEffect extends MobEffect {
	public ClimbingMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -13159635);
	}
	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		if (entity.horizontalCollision) {
			Vec3 initialVec = entity.getDeltaMovement();
			Vec3 climbVec = new Vec3(initialVec.x, Math.min(0.3D,0.1D * (amplifier + 1)), initialVec.z);
			entity.setDeltaMovement(climbVec);
		}
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
