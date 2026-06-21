package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RepairMobEffect extends MobEffect {

    public RepairMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -8388608);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        int repair = amplifier + 1;

        // 修复盔甲
        repairItem(entity, EquipmentSlot.HEAD, repair);
        repairItem(entity, EquipmentSlot.CHEST, repair);
        repairItem(entity, EquipmentSlot.LEGS, repair);
        repairItem(entity, EquipmentSlot.FEET, repair);

        // 修复主手物品
        repairItem(entity, EquipmentSlot.MAINHAND, repair);

        return true;
    }

    private void repairItem(LivingEntity entity, EquipmentSlot slot, int repair) {
        ItemStack itemStack = entity.getItemBySlot(slot);
        if (!itemStack.isEmpty() && itemStack.isDamaged()) {
            int newDamage = itemStack.getDamageValue() - repair;
            itemStack.setDamageValue(Math.max(0, newDamage));
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}