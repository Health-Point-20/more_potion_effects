package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class FearMobEffect extends MobEffect {
    private static final int COOLDOWN_INTERVAL = 30;
    private static final int COOLDOWN_DURATION = 20;

    public FearMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x4A0E4E);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return false;

        int level = amplifier + 1;
        double fearTime = entity.getPersistentData().getDouble("fear");
        fearTime += level;
        entity.getPersistentData().putDouble("fear", fearTime);

        // 武器冷却 (1.20.1: 每30tick给物品加20tick冷却)
        if (fearTime >= COOLDOWN_INTERVAL) {
            if (entity instanceof Player player) {
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();

                if (!mainHand.isEmpty()) {
                    player.getCooldowns().addCooldown(mainHand.getItem(), COOLDOWN_DURATION);
                }
                if (!offHand.isEmpty()) {
                    player.getCooldowns().addCooldown(offHand.getItem(), COOLDOWN_DURATION);
                }
            }
            entity.getPersistentData().putDouble("fear", 0);
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}