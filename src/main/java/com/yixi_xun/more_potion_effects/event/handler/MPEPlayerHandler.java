package com.yixi_xun.more_potion_effects.event.handler;

import com.yixi_xun.more_potion_effects.mob_effects.KineticMobEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import static com.yixi_xun.more_potion_effects.api.ConfigHelper.evaluate;
import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.*;
import static com.yixi_xun.more_potion_effects.MPEConfig.GLUTTONY_SPEED_MULTIPLIER;
import static com.yixi_xun.more_potion_effects.MPEConfig.FEAST_FOOD_ENHANCED;

public class MPEPlayerHandler {

    public static void onPlayerUseItemTickHandler(LivingEntityUseItemEvent.Tick event) {
        handleGluttony(event.getEntity(), event.getItem(), event);
        handleQuickDraw(event.getEntity(), event.getItem(), event);
    }

    private static void handleGluttony(LivingEntity entity, ItemStack stack, LivingEntityUseItemEvent.Tick event) {
        MobEffectInstance gluttony = entity.getEffect(GLUTTONY);
        if (gluttony != null) {
            UseAnim anim = stack.getUseAnimation();
            if (anim == UseAnim.EAT || anim == UseAnim.DRINK) {
                int level = gluttony.getAmplifier() + 1;
                double reductionPerTick = evaluate(GLUTTONY_SPEED_MULTIPLIER.get(), "effectLevel", level);

                CompoundTag persistentData = entity.getPersistentData();
                String accumulatorKey = "GluttonyAccumulator";
                double accumulator = persistentData.getDouble(accumulatorKey);

                accumulator += reductionPerTick;

                int ticksToReduce = 0;
                while (accumulator >= 1.0) {
                    ticksToReduce++;
                    accumulator -= 1.0;
                }

                persistentData.putDouble(accumulatorKey, accumulator);

                if (ticksToReduce > 0) {
                    int newDuration = Math.max(event.getDuration() - ticksToReduce, 0);
                    event.setDuration(newDuration);
                }
            }
        }
    }

    private static void handleQuickDraw(LivingEntity entity, ItemStack stack, LivingEntityUseItemEvent.Tick event) {
        MobEffectInstance quickDraw = entity.getEffect(QUICK_DRAW);
        if (quickDraw != null && stack.getItem() instanceof BowItem) {
            int level = quickDraw.getAmplifier() + 1;
            CompoundTag tag = entity.getPersistentData();
            String key = "quickDrawCount";

            int counter = tag.getInt(key) + level;
            int reduceDuration = counter / 2;
            tag.putInt(key, counter % 2);

            int newDuration = Math.max(event.getDuration() - reduceDuration, 20);
            event.setDuration(newDuration);
        }
    }

    public static void onBreakHandler(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        MobEffectInstance enhanceDigging = player.getEffect(ENHANCE_DIGGING);

        if (enhanceDigging != null) {
            int amplifier = enhanceDigging.getAmplifier();
            float speedMultiplier = 1.0f + (amplifier + 1) * 0.25f;
            event.setNewSpeed(event.getOriginalSpeed() * speedMultiplier);
        }
    }

    public static void onHarvestCheckHandler(PlayerEvent.HarvestCheck event) {
        Player player = event.getEntity();
        BlockState state = event.getTargetBlock();
        MobEffectInstance enhanceDigging = player.getEffect(ENHANCE_DIGGING);

        if (enhanceDigging != null) {
            Item tool = player.getMainHandItem().getItem();
            
            if (isRightTool(state, tool)) {
                event.setCanHarvest(true);
            }
        }
    }

    private static boolean isRightTool(BlockState state, Item tool) {
        if (state.requiresCorrectToolForDrops()) {
            if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return tool instanceof PickaxeItem;
            if (state.is(BlockTags.MINEABLE_WITH_AXE)) return tool instanceof AxeItem;
            if (state.is(BlockTags.MINEABLE_WITH_HOE)) return tool instanceof HoeItem;
            if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return tool instanceof ShovelItem;
        }
        return true;
    }

    public static void onItemTossHandler(ItemTossEvent event) {
        Player player = event.getPlayer();
        ItemStack tossedItem = event.getEntity().getItem();
        if (tossedItem.isEmpty()) return;

        if (player.hasEffect(SLOT_LOCK) && !player.level().isClientSide()) {
            event.setCanceled(true);
            boolean placed = player.getInventory().add(tossedItem);
            if (!placed) {
                player.displayClientMessage(Component.literal("§0物品被虚空吞噬了！"), true);
            } else {
                player.displayClientMessage(Component.literal("物品栏已被锁定，无法丢弃物品！"), true);
            }
        }
    }

    public static void onPlayerLoggedOutHandler(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        KineticMobEffect.previousPos.remove(player.getUUID());
        KineticMobEffect.velocities.remove(player.getUUID());
    }

    public static void onLivingEatingHandler(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        ItemStack itemStack = event.getItem();
        Item item = itemStack.getItem();
        FoodProperties foodProperties = item.getFoodProperties(itemStack, entity);

        if (entity instanceof Player player && foodProperties != null) {
            MobEffectInstance instance = player.getEffect(FEAST);
            if (instance != null) {
                int level = instance.getAmplifier() + 1;
                FoodData foodData = player.getFoodData();
                CompoundTag persistentData = player.getPersistentData();

                // 获取当前玩家饱食度状态
                int currentFoodLevel = foodData.getFoodLevel();
                float currentSaturationLevel = foodData.getSaturationLevel();

                // 计算食物提供的营养值
                int foodNutrition = foodProperties.nutrition();
                float foodSaturationModifier = foodProperties.saturation();

                // 增幅后的营养值
                float modified = (float) evaluate(FEAST_FOOD_ENHANCED.get(), "effectLevel", level);
                int enhancedNutrition = (int) (foodNutrition * modified);
                float enhancedSaturation = foodNutrition * foodSaturationModifier * 2.0F * modified;

                // 获取之前累积的溢出值
                int storedOverflowNutrition = persistentData.getInt("feast_overflow_nutrition");
                float storedOverflowSaturation = persistentData.getFloat("feast_overflow_saturation");

                // 计算食用后的理论总饱食度（包括已存储的溢出值）
                int totalNutrition = currentFoodLevel + enhancedNutrition + storedOverflowNutrition;
                float totalSaturation = currentSaturationLevel + enhancedSaturation + storedOverflowSaturation;

                // 存储溢出饱食度
                if (totalNutrition > 19) {
                    // 设置为19使玩家可以持续进食
                    foodData.setFoodLevel(19);
                    persistentData.putInt("feast_overflow_nutrition", totalNutrition - 19);
                } else {
                    // 填补饱食度
                    foodData.setFoodLevel(totalNutrition);
                    persistentData.remove("feast_overflow_nutrition");
                }

                // 存储溢出饱和度
                int newFoodLevel = foodData.getFoodLevel();
                if (totalSaturation > newFoodLevel) {
                    foodData.setExhaustion(0);
                    foodData.setSaturation(newFoodLevel);    // 顶到当前上限
                    persistentData.putFloat("feast_overflow_saturation", totalSaturation - newFoodLevel);
                } else {
                    foodData.setSaturation(totalSaturation); // 没超限，直接设置
                    persistentData.remove("feast_overflow_saturation");
                }
            }
        }
    }
}