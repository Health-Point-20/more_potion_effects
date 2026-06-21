package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.jetbrains.annotations.NotNull;

public class FeastMobEffect extends MobEffect {
    private static final double TICK_THRESHOLD_BASE = 80.0;
    private static final float BASE_HEAL = 1.0f;
    private static final float HEAL_PER_LEVEL = 0.5f;
    private static final int FOOD_THRESHOLD = 6;

    public FeastMobEffect() {
        super(MobEffectCategory.NEUTRAL, -26368);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        int effectLevel = amplifier + 1;

        if (entity instanceof Player player) {
            int foodLevel = player.getFoodData().getFoodLevel();
            
            if (foodLevel >= FOOD_THRESHOLD && entity.getHealth() < entity.getMaxHealth()) {
                double timer = entity.getPersistentData().getDouble("foodTickTimer");
                timer += 1;
                entity.getPersistentData().putDouble("foodTickTimer", timer);

                double threshold = TICK_THRESHOLD_BASE / effectLevel;
                if (timer >= threshold) {
                    entity.getPersistentData().putDouble("foodTickTimer", 0);
                    float healAmount = BASE_HEAL + HEAL_PER_LEVEL * effectLevel;
                    entity.heal(healAmount);

                    int storedOverflowNutrition = player.getPersistentData().getInt("feast_overflow_nutrition");
                    float storedOverflowSaturation = player.getPersistentData().getFloat("feast_overflow_saturation");

                    if (storedOverflowNutrition > 0 || storedOverflowSaturation > 0) {
                        int consumeAmount = Math.min(storedOverflowNutrition, 6 * effectLevel);
                        player.getPersistentData().putInt("feast_overflow_nutrition", Math.max(0, storedOverflowNutrition - consumeAmount));

                        if (player.getPersistentData().getInt("feast_overflow_nutrition") <= 0) {
                            player.getPersistentData().remove("feast_overflow_nutrition");
                            player.getPersistentData().remove("feast_overflow_saturation");
                        }
                    }
                }
            }

            if (foodLevel <= FOOD_THRESHOLD) {
                DamageSource damageSource = new DamageSource(
                        entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.STARVE),
                        entity
                );
                entity.hurt(damageSource, effectLevel);
                if (!player.level().isClientSide()) {
                    player.displayClientMessage(Component.literal("§c对盛宴的渴望使你的饥饿加重了..."), true);
                }
            }
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}