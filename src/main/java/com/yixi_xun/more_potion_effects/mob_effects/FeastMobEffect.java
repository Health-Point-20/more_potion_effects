package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.food.FoodData;

public class FeastMobEffect extends MobEffect {
    private static final String timerKey = "feast_food_tick_timer";
    private static final String saturationKey = "feast_overflow_saturation";
    private static final String nutritionKey = "feast_overflow_nutrition";

    public FeastMobEffect() {
        super(MobEffectCategory.NEUTRAL, -26368);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return false;
        if (!(entity instanceof Player player)) return false;

        int effectLevel = amplifier + 1;
        FoodData foodData = player.getFoodData();
        CompoundTag persistentData = player.getPersistentData();

        // 获取玩家饱食度
        int foodLevel = foodData.getFoodLevel();

        if (foodLevel > 8 && entity.getHealth() < entity.getMaxHealth()) {
            // 计时器逻辑
            int currentTick = persistentData.getInt(timerKey);
            currentTick++;

            if (currentTick >= (80 / effectLevel)) {
                persistentData.putInt(timerKey, 0); // 重置计时器

                // 计算总消耗，以“疲劳值”为统一单位
                // 原版每次回血消耗 4.0 疲劳，这里按等级倍率放大
                float totalExhaustionCost = 6.0F * effectLevel;
                float remainingExhaustion = totalExhaustionCost;

                // 1. 消耗 NBT 溢出饱和度 (1 点 NBT饱和度 = 1 点疲劳值)
                float storedSat = persistentData.getFloat(saturationKey);
                if (storedSat > 0 && remainingExhaustion > 0) {
                    float costFromNbtSat = Math.min(storedSat, remainingExhaustion);
                    storedSat -= costFromNbtSat;
                    remainingExhaustion -= costFromNbtSat;
                }

                // 2. 消耗 NBT 溢出饥饿值 (1 点 NBT饥饿值 = 4 点疲劳值)
                int storedNut = persistentData.getInt(nutritionKey);
                if (storedNut > 0 && remainingExhaustion > 0) {
                    // 需要消耗的 NBT 饥饿值数量
                    float nutNeeded = remainingExhaustion / 4.0F;
                    float actualNutCost = Math.min(storedNut, nutNeeded);

                    storedNut -= (int) actualNutCost;
                    // 将实际消耗的饥饿值转回疲劳值扣除
                    remainingExhaustion -= actualNutCost * 4.0F;
                }

                // 写回并清理 NBT 数据
                if (storedSat > 0) {
                    persistentData.putFloat(saturationKey, storedSat);
                } else {
                    persistentData.remove(saturationKey);
                }
                if (storedNut > 0) {
                    persistentData.putInt(nutritionKey, storedNut);
                } else {
                    persistentData.remove(nutritionKey);
                }

                // 3. 消耗实际饱和度和饥饿值：通过增加疲劳值
                if (remainingExhaustion > 0) {
                    foodData.addExhaustion(remainingExhaustion);
                }

                // 回血（只要 NBT 或者原版值有消耗，就允许回血）
                // 因为 addExhaustion 不返回消耗结果，通过判断剩余疲劳值来确认
                if (remainingExhaustion < totalExhaustionCost || totalExhaustionCost == 0) {
                    float healAmount = 1.0F + effectLevel;
                    entity.heal(healAmount);
                }
            } else {
                persistentData.putInt(timerKey, currentTick);
            }
        }

        // 饱食度不足时的惩罚逻辑
        if (foodLevel <= 6) {
            entity.hurt(entity.level().damageSources().starve(), (float) effectLevel);
            player.displayClientMessage(Component.literal("§c对盛宴的渴望使你的饥饿加重了..."), true);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}