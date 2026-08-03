package com.yixi_xun.more_potion_effects.event.handler;

import com.yixi_xun.more_potion_effects.MPEConfig;
import com.yixi_xun.more_potion_effects.api.EffectUtils;
import com.yixi_xun.more_potion_effects.api.HurtManager;
import com.yixi_xun.more_potion_effects.init.MorePotionEffectsModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import static com.yixi_xun.more_potion_effects.MPEConfig.*;
import static com.yixi_xun.more_potion_effects.api.ConfigHelper.evaluate;
import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.*;

public class MPEEnchantmentHandler {

    // ==================== Equipment Change (Flying, Vibrant) ====================

    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        EquipmentSlot slot = event.getSlot();
        if (slot != EquipmentSlot.CHEST) return;

        LivingEntity entity = event.getEntity();
        ItemStack newItem = event.getTo();
        ItemStack oldItem = event.getFrom();

        handleFlyingEnchant(entity, oldItem, newItem);
        handleVibrantEnchant(entity, oldItem, newItem);
    }

    private static void handleFlyingEnchant(LivingEntity entity, ItemStack oldItem, ItemStack newItem) {
        int oldLevel = MorePotionEffectsModEnchantments.getEnchantmentLevel(oldItem, MorePotionEffectsModEnchantments.FLYING);
        int newLevel = MorePotionEffectsModEnchantments.getEnchantmentLevel(newItem, MorePotionEffectsModEnchantments.FLYING);

        if (oldLevel > 0 && newLevel == 0) {
            entity.removeEffect(FLIGHT);
        } else if (newLevel > 0) {
            MobEffectInstance current = entity.getEffect(FLIGHT);
            if (current == null || current.getAmplifier() != newLevel - 1) {
                entity.addEffect(new MobEffectInstance(FLIGHT, -1, newLevel - 1, false, false, true));
            }
        }
    }

    private static void handleVibrantEnchant(LivingEntity entity, ItemStack oldItem, ItemStack newItem) {
        int oldLevel = MorePotionEffectsModEnchantments.getEnchantmentLevel(oldItem, MorePotionEffectsModEnchantments.VIBRANT);
        int newLevel = MorePotionEffectsModEnchantments.getEnchantmentLevel(newItem, MorePotionEffectsModEnchantments.VIBRANT);

        if (oldLevel > 0 && newLevel == 0) {
            entity.removeEffect(STRONG_HEART);
        } else if (newLevel > 0) {
            MobEffectInstance current = entity.getEffect(STRONG_HEART);
            if (current == null || current.getAmplifier() != newLevel - 1) {
                entity.addEffect(new MobEffectInstance(STRONG_HEART, -1, newLevel - 1, false, false, true));
            }
        }
    }

    // ==================== Effect Removal Protection (Flying, Vibrant) ====================

    public static void onEffectRemove(MobEffectEvent.Remove event) {
        LivingEntity entity = event.getEntity();

        if (event.getEffect() == FLIGHT.get()) {
            ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
            if (MorePotionEffectsModEnchantments.getEnchantmentLevel(chest, MorePotionEffectsModEnchantments.FLYING) > 0) {
                event.setCanceled(true);
            }
        }

        if (event.getEffect() == STRONG_HEART.get()) {
            ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
            if (MorePotionEffectsModEnchantments.getEnchantmentLevel(chest, MorePotionEffectsModEnchantments.VIBRANT) > 0) {
                event.setCanceled(true);
            }
        }
    }

    // ==================== Weapon Enchantments on Hurt ====================

    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        LivingEntity target = event.getEntity();

        if (CannotTriggerEffect(attacker, target)) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();

        handleEliminationEffect(target, weapon);
        handleHighlyToxicBlade(target, weapon);
        handleInflictionCorrosion(target, weapon);
        handleInhibitTherapy(target, weapon);
        handlePotionPunisher(attacker, target, weapon, event);
        handleSourceOfBlessing(attacker, weapon);
        handleSourceOfCurses(attacker, target, weapon);
        handleSunderArmor(attacker, target, weapon);
    }

    private static boolean CannotTriggerEffect(Entity attacker, Entity victim) {
        if (attacker instanceof Player player) {
            boolean isAttackReady = player.getAttackStrengthScale(0.5F) >= 0.95F;
            if (!isAttackReady) {
                // 攻击未冷却完毕时不触发
                return true;
            }
            // 检查是否仅对主目标生效
            if (!MPEConfig.ENCHANT_MAIN_TARGET_ONLY.get()) {
                // 不限制仅主目标时可以触发
                return false;
            }
            // 检查为当前目标是否是主目标
            return HurtManager.getMainTarget(player) != victim.getUUID();
        } else {
            // 其他实体默认可以触发
            return false;
        }
    }

    private static void handleEliminationEffect(LivingEntity target, ItemStack weapon) {
        int level = MorePotionEffectsModEnchantments.getEnchantmentLevel(weapon, MorePotionEffectsModEnchantments.ELIMINATION_EFFECT);
        if (level <= 0) return;
        if (target.hasEffect(LOCK)) return;

        double probability = evaluate(ELIMINATION_EFFECT_PROBABILITY.get(), "EnchantLevel", level);
        if (Math.random() >= probability) return;

        target.getActiveEffects().stream()
                .filter(e -> e.getEffect() != LOCK.get())
                .skip((int) (Math.random() * target.getActiveEffects().size()))
                .findFirst()
                .ifPresent(e -> target.removeEffect(e.getEffect()));
    }

    private static void handleHighlyToxicBlade(LivingEntity target, ItemStack weapon) {
        int level = MorePotionEffectsModEnchantments.getEnchantmentLevel(weapon, MorePotionEffectsModEnchantments.HIGHLY_TOXIC_BLADE);
        if (level <= 0) return;

        double probability = evaluate(ADMINISTER_POISON_PROBABILITY.get(), "EnchantLevel", level);
        if (Math.random() >= probability) return;

        MobEffectInstance effect = target.getEffect(HIGHLY_TOXIC);
        int existingLevel = effect != null ? effect.getAmplifier() + 1 : 0;
        int newLevel = Math.min(existingLevel + 1, level);
        if (newLevel > 0) {
            target.addEffect(new MobEffectInstance(HIGHLY_TOXIC, 40 * level, newLevel - 1));
        }
    }

    private static void handleInflictionCorrosion(LivingEntity target, ItemStack weapon) {
        int level = MorePotionEffectsModEnchantments.getEnchantmentLevel(weapon, MorePotionEffectsModEnchantments.INFLICTION_CORROSION);
        if (level <= 0) return;

        double probability = evaluate(INFLICTION_CORROSION_PROBABILITY.get(), "EnchantLevel", level);
        if (Math.random() >= probability) return;

        MobEffectInstance effect = target.getEffect(CORROSION);
        int existingLevel = effect != null ? effect.getAmplifier() + 1 : 0;
        int newLevel = Math.min(existingLevel + 1, level);
        if (newLevel > 0) {
            target.addEffect(new MobEffectInstance(CORROSION, 100 * level, newLevel - 1));
        }
    }

    private static void handleInhibitTherapy(LivingEntity target, ItemStack weapon) {
        int level = MorePotionEffectsModEnchantments.getEnchantmentLevel(weapon, MorePotionEffectsModEnchantments.INHIBIT_THERAPY);
        if (level <= 0) return;

        double probability = evaluate(INHIBIT_THERAPY_PROBABILITY.get(), "EnchantLevel", level);
        if (Math.random() >= probability) return;

        MobEffectInstance effect = target.getEffect(WEAKENING_RECOVERY);
        int existingLevel = effect != null ? effect.getAmplifier() + 1 : 0;
        int newLevel = Math.min(existingLevel + 1, level);
        if (newLevel > 0) {
            target.addEffect(new MobEffectInstance(WEAKENING_RECOVERY, 100 * level, newLevel - 1));
        }
    }

    private static void handlePotionPunisher(@SuppressWarnings("unused") LivingEntity attacker, LivingEntity target, ItemStack weapon, LivingIncomingDamageEvent event) {
        int level = MorePotionEffectsModEnchantments.getEnchantmentLevel(weapon, MorePotionEffectsModEnchantments.POTION_PUNISHER);
        if (level <= 0) return;

        int effectCount = target.getActiveEffects().size();
        if (effectCount == 0) return;

        float damage = event.getAmount();
        float multiplier = 1 + level * effectCount * 0.05f;
        float extraDamage = level * Math.min(effectCount, level + 3);
        event.setAmount(damage * multiplier + extraDamage);
    }

    private static void handleSourceOfBlessing(LivingEntity attacker, ItemStack weapon) {
        if (!SOURCE_OF_BLESSING.get()) return;
        int level = MorePotionEffectsModEnchantments.getEnchantmentLevel(weapon, MorePotionEffectsModEnchantments.SOURCE_OF_BLESSING);
        if (level <= 0) return;

        double probability = evaluate(SOURCE_OF_BLESSING_PROBABILITY.get(), "EnchantLevel", level);
        if (Math.random() >= probability) return;

        Holder<MobEffect> randomEffect = EffectUtils.getRandomGoodEffect();
        int randomLevel = (int) (Math.random() * level);
        int duration = randomEffect.value().isInstantenous() ? level : level * 100;
        attacker.addEffect(new MobEffectInstance(randomEffect, duration, randomLevel));
    }

    private static void handleSourceOfCurses(@SuppressWarnings("unused") LivingEntity attacker, LivingEntity target, ItemStack weapon) {
        if (!SOURCE_OF_CURSES.get()) return;
        int level = MorePotionEffectsModEnchantments.getEnchantmentLevel(weapon, MorePotionEffectsModEnchantments.SOURCE_OF_CURSES);
        if (level <= 0) return;

        double probability = evaluate(SOURCE_OF_CURSES_PROBABILITY.get(), "EnchantLevel", level);
        if (Math.random() >= probability) return;

        Holder<MobEffect> randomEffect = EffectUtils.getRandomBadEffect();
        int randomLevel = (int) (Math.random() * level);
        int duration = randomEffect.value().isInstantenous() ? level : level * 100;
        target.addEffect(new MobEffectInstance(randomEffect, duration, randomLevel));
    }

    private static void handleSunderArmor(LivingEntity attacker, LivingEntity target, ItemStack weapon) {
        int level = MorePotionEffectsModEnchantments.getEnchantmentLevel(weapon, MorePotionEffectsModEnchantments.SUNDER_ARMOR);
        if (level <= 0) return;

        double probability = evaluate(SUNDER_ARMOR_PROBABILITY.get(), "EnchantLevel", level);
        if (Math.random() >= probability) return;

        MobEffectInstance effect = target.getEffect(ARMOR_BROKEN);
        int existingLevel = effect != null ? effect.getAmplifier() + 1 : 0;
        int newLevel = Math.min(existingLevel + 1, level);
        if (newLevel > 0) {
            target.addEffect(new MobEffectInstance(ARMOR_BROKEN, 80 * level, newLevel - 1));
        }

        int armorValue = target.getArmorValue();
        int durabilityCost = Math.min(level * 4, armorValue);
        if (durabilityCost > 0) {
            weapon.hurtAndBreak(durabilityCost, attacker, EquipmentSlot.MAINHAND);
        }
    }

    // ==================== Unyielding Enchant Death Handler ====================

    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        ItemStack chest = target.getItemBySlot(EquipmentSlot.CHEST);
        int level = MorePotionEffectsModEnchantments.getEnchantmentLevel(chest, MorePotionEffectsModEnchantments.UNYIELDING);
        if (level <= 0) return;

        CompoundTag data = target.getPersistentData();
        double storedDamage = data.getDouble("unyielding_stored_damage");
        data.putDouble("unyielding_stored_damage", storedDamage + event.getOriginalDamage());

        if (target instanceof Player player) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§6你将在不屈触发后受到§c" +
                            String.format("%.1f", storedDamage + event.getOriginalDamage()) + "§6点伤害"),
                    true);
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        ItemStack chest = target.getItemBySlot(EquipmentSlot.CHEST);
        int level = MorePotionEffectsModEnchantments.getEnchantmentLevel(chest, MorePotionEffectsModEnchantments.UNYIELDING);
        if (level <= 0) return;

        CompoundTag data = target.getPersistentData();
        long cooldown = data.getLong("unyielding_cooldown");
        if (target.level().getGameTime() < cooldown) return;

        event.setCanceled(true);
        target.setHealth(1);

        target.level().playSound(null, target.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1, 1);

        int duration = (int) Math.pow(2, level - 1) * 100;
        target.addEffect(new MobEffectInstance(STATIC_LIFE, duration, 0));

        int cooldownTicks = 180 * 20 / (level - 1);
        data.putLong("unyielding_cooldown", target.level().getGameTime() + cooldownTicks);
        data.putDouble("unyielding_stored_damage", 0);
    }
}