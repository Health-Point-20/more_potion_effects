package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class CorrosionMobEffect extends MobEffect {
    private static final int DAMAGE_INTERVAL_BASE = 20;

    public CorrosionMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF6B6B);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return false;

        double damage = amplifier + 1;
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();

        // 损坏所有装备槽位
        damageEquipment(entity, EquipmentSlot.HEAD, damage, level, pos);
        damageEquipment(entity, EquipmentSlot.CHEST, damage, level, pos);
        damageEquipment(entity, EquipmentSlot.LEGS, damage, level, pos);
        damageEquipment(entity, EquipmentSlot.FEET, damage, level, pos);

        return true;
    }

    private void damageEquipment(LivingEntity entity, EquipmentSlot slot, double damage, Level level, BlockPos pos) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (stack.isEmpty()) return;

        int interval = (int) Math.ceil(DAMAGE_INTERVAL_BASE / damage);
        
        if (entity.tickCount % interval == 0) {
            int damageAmount = (int) damage;
            if (stack.isDamageableItem()) {
                // 使用简化的损坏方式，不使用回调
                int newDamage = stack.getDamageValue() + damageAmount;
                if (newDamage >= stack.getMaxDamage()) {
                    // 物品损坏，播放音效并移除
                    level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 2.0F, 1.0F);
                    stack.shrink(1);
                } else {
                    stack.setDamageValue(newDamage);
                    // 播放腐蚀音效
                    level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2.0F, 1.0F);
                }
            }
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}