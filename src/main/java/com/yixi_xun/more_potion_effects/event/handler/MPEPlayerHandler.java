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
import org.jetbrains.annotations.Nullable;

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

        if (enhanceDigging == null) return;

        // 如果原本就能挖，不需要干预
        if (event.canHarvest()) return;

        ItemStack heldStack = player.getMainHandItem();
        Item tool = heldStack.getItem();

        // ★ 获取当前工具的 Tier
        Tier currentTier = null;
        if (tool instanceof TieredItem tieredItem) {
            currentTier = tieredItem.getTier();
        }

        int amplifier = enhanceDigging.getAmplifier() + 1;

        // ★ 判断是否为原版挖掘等级
        boolean isVanillaTier = isVanillaTier(currentTier);

        if (!isVanillaTier) {
            // 视为超过下界合金级
            event.setCanHarvest(true);
            return;
        }

        //  下界合金级以内：计算有效挖掘等级
        int toolLevel = getVanillaToolLevel(currentTier);
        int effectiveLevel = toolLevel + amplifier;
        int requiredLevel = getRequiredLevel(state);

        // 有效等级 >= 所需等级 → 允许挖掘
        if (effectiveLevel >= requiredLevel && requiredLevel > 0) {
            event.setCanHarvest(true);
        }
    }


    /**
     * 判断是否为原版 Tier，如果该 Tier 不是原版 Tier则视为超过下界合金
     */
    private static boolean isVanillaTier(@Nullable Tier tier) {
        if (tier == null) return false;
        // 原版 Tiers 枚举中的所有值
        for (Tiers vanilla : Tiers.values()) {
            if (vanilla == tier) return true;
        }
        return false;
    }

    /**
     * 获取原版 Tier 对应的整数等级
     * 仅用于下界合金级以内的计算
     */
    private static int getVanillaToolLevel(@Nullable Tier tier) {
        if (tier == null) return 0;
        // 利用原版 Tiers 枚举的 ordinal 作为等级
        // WOOD=0, STONE=1, IRON=2, DIAMOND=3, NETHERITE=4, GOLD=0
        for (int i = 0; i < Tiers.values().length; i++) {
            if (Tiers.values()[i] == tier) {
                // Gold 特殊处理：虽然 ordinal=5，但实际挖掘等级等同于 Stone
                if (tier == Tiers.GOLD) return 1;
                return i;
            }
        }
        return 0; // 非原版 Tier 不应到达这里
    }

    /**
     * 获取方块所需的最低原版挖掘等级
     * 对于模组方块（不在原版 needs_xxx tag 中），返回 -1 表示"未知"
     */
    private static int getRequiredLevel(BlockState state) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) return 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) return 1;
        if (!state.requiresCorrectToolForDrops()) return 0;
        // 需要正确工具但不在原版 needs_xxx tag 中 → 模组方块
        // 返回 -1 表示无法处理
        return -1;
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