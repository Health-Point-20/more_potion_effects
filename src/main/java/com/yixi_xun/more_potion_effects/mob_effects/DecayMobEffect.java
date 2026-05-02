package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.api.IMobEffectRemovable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;

public class DecayMobEffect extends MobEffect implements IMobEffectRemovable {
	private static final ResourceLocation DECAY_HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("more_potion_effects","effect.decay");
	private static final double TICK_THRESHOLD = 20.0;
	private static final double MIN_HEALTH = 0.01;

	public DecayMobEffect() {
		super(MobEffectCategory.HARMFUL, -13421773);
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		var persistentData = entity.getPersistentData();

        double currentTime = persistentData.getDouble("decay_time");
		persistentData.putDouble("decay_time", currentTime + 1);

		if (entity.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, entity.getX(), entity.getY(), entity.getZ(), 1, 0.1, 0.1, 0.1, 0.1);
		}

		AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
		if (currentTime >= TICK_THRESHOLD && maxHealth != null) {
			double currentMaxHealth = maxHealth.getValue();
			double decayAmount = Math.pow(2, amplifier);

			double newDecayHealth;
			if (currentMaxHealth - decayAmount > 0) {
				newDecayHealth = persistentData.getDouble("decay_health") + decayAmount;
			} else {
				newDecayHealth = persistentData.getDouble("decay_health") + maxHealth.getBaseValue() - MIN_HEALTH;
			}

			persistentData.putDouble("decay_health", newDecayHealth);

			maxHealth.removeModifier(DECAY_HEALTH_MODIFIER_ID);
			maxHealth.addTransientModifier(new AttributeModifier(DECAY_HEALTH_MODIFIER_ID, -newDecayHealth, AttributeModifier.Operation.ADD_VALUE));

			float maxHealthValue = (float) maxHealth.getValue();
			if (entity.getHealth() > maxHealthValue) {
				entity.setHealth(maxHealthValue);
			}

			persistentData.putDouble("decay_health", 0);
		}

		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public void onEffectRemoved(LivingEntity entity, MobEffectInstance instance) {
		if (entity == null)
			return;
		AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null) return;
		maxHealth.removeModifier(DECAY_HEALTH_MODIFIER_ID);
		entity.getPersistentData().remove("decay_health");

	}
}
