
package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import com.yixi_xun.more_potion_effects.api.IMoreMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.jetbrains.annotations.NotNull;

import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.*;

public class StaticLifeMobEffect extends MobEffect implements IMoreMobEffect {
	public StaticLifeMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -13261);
	}

	@Override
	public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {

		if (!(entity instanceof Player player)) return true;

		// 获取食物数据并判空
		FoodData foodData = player.getFoodData();

        // 检查药水效果持续时间
		var instance = entity.getEffect(STATIC_LIFE);
        if (instance != null && instance.getDuration() < 20) return true;

        // 获取或初始化计时器
		int foodTickTimer = entity.getPersistentData().getInt("foodTickTimer");

		// 高饱食度逻辑
		if (foodData.getFoodLevel() >= 20) {
			foodTickTimer += 1;
			if (foodTickTimer >= 10) {
				float saturation = Math.min(foodData.getSaturationLevel(), 6.0f);
				entity.heal(saturation * (1.0f / 6.0f));
				player.causeFoodExhaustion(saturation);
				foodTickTimer = 0;
			}
		}
		// 中等饱食度逻辑
		else if (foodData.getFoodLevel() > 18) {
			foodTickTimer += 1;
			if (foodTickTimer >= 80) {
				entity.heal(1.0f);
				player.causeFoodExhaustion(6.0f);
				foodTickTimer = 0;
			}
		}

		// 更新计时器
		entity.getPersistentData().putInt("foodTickTimer", foodTickTimer);
		return true;
	}

	@Override
	public void onEffectRemoved(LivingEntity entity, MobEffectInstance instance) {
		if (instance != null && entity.getPersistentData().getDouble("static_damage") >= 0) {
			MorePotionEffectsMod.queueServerWork(0, () -> {
				DamageSource staticDamageSource = new DamageSource(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("more_potion_effects:static_damage"))));
				float damage = Math.min((float) entity.getPersistentData().getDouble("static_damage"), Float.MAX_VALUE);

				entity.hurt(staticDamageSource, damage);
				entity.getPersistentData().remove("static_damage");
			});
		} else {
			entity.setHealth(entity.getHealth() - (float) entity.getPersistentData().getDouble("static_damage"));
			entity.getPersistentData().remove("static_damage");
		}
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
