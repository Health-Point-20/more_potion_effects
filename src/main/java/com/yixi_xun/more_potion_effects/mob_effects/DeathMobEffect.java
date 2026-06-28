package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import com.yixi_xun.more_potion_effects.api.EffectUtils;
import com.yixi_xun.more_potion_effects.api.IMobEffectRemovable;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.DEATH;
import static com.yixi_xun.more_potion_effects.MPEConfig.SUPER_DEATH_MODE;

public class DeathMobEffect extends MobEffect implements IMobEffectRemovable {
	private static final float MAX_DAMAGE = Float.POSITIVE_INFINITY;
	private static final float NEGATIVE_HEALTH = Float.NEGATIVE_INFINITY;
	private static final List<EntityType<?>> deathEntity = new ArrayList<>();

	public DeathMobEffect() {
		super(MobEffectCategory.HARMFUL, -10925223);
	}

	public static List<EntityType<?>> getDeathEntity() {
		return deathEntity;
	}

	@Override
	public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
		if (!SUPER_DEATH_MODE.get()) return false;

		Level level = entity.level();
		if (level.isClientSide()) return false;

		try {
			if (!deathEntity.contains(entity.getType())) {
				deathEntity.add(entity.getType());
			}

			// 使用服务器任务队列替代多线程逻辑
			switch (amplifier) {
				case 0 -> entity.hurt(entity.damageSources().genericKill(), entity.getMaxHealth() + 1);
				case 1 -> entity.hurt(entity.damageSources().genericKill(), MAX_DAMAGE);
				case 2 -> entity.setHealth(0);
				case 3 -> entity.setHealth(NEGATIVE_HEALTH);
				case 4 -> basicAttack(entity);
				case 5 -> {
					clearAttack(entity);
					forceClearEffect(entity);
					basicAttack(entity);
					attributeAttack(entity);
				}
				case 6 -> {
					clearAttack(entity);
					forceClearEffect(entity);
					basicAttack(entity);
					attributeAttack(entity);
					removeAttack(entity, level);
				}
				case 7 -> // 使用服务器任务队列替代多线程
                        MorePotionEffectsMod.queueServerWork(25, () -> {
                            if (entity.isAlive()) {
                                executeFullRemoval(entity, level);
                            }
                        });
				case 8 -> // 高等级死亡效果，使用任务队列
                        MorePotionEffectsMod.queueServerWork(10, () -> {
                            if (entity.isAlive()) {
                                executeFullRemoval(entity, level);
                            }
                        });
				default -> {}
			}
		} catch (Exception e) {
			// 记录错误日志
            MorePotionEffectsMod.LOGGER.error("Error applying DeathMobEffect: {}", e.getMessage());
		}
		return true;
	}

	private void executeFullRemoval(LivingEntity entity, Level level) {
		clearAttack(entity);
		forceClearEffect(entity);
		basicAttack(entity);
		attributeAttack(entity);
		removeAttack(entity, level);
	}

	private void basicAttack(LivingEntity entity) {
		entity.setHealth(Float.NEGATIVE_INFINITY);
		entity.die(entity.damageSources().genericKill());
	}

	private void clearAttack(LivingEntity entity) {
		if (entity instanceof Player player) {
			player.getInventory().dropAll();
			player.getInventory().clearContent();
		}
		entity.removeAllEffects();
		entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		entity.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
	}

	private void forceClearEffect(LivingEntity entity) {
		entity.getActiveEffects().forEach(effect -> {
			if (!(effect.getEffect() == DEATH)) {
				EffectUtils.forceRemoveEffect(entity, effect.getEffect());
			}
		});
	}

	private void attributeAttack(LivingEntity entity) {
		AttributeInstance maxHealthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealthAttr != null) {
			maxHealthAttr.setBaseValue(0);
			maxHealthAttr.addTransientModifier(new AttributeModifier(
					ResourceLocation.fromNamespaceAndPath("more_potion_effects", "death"),
					-1,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
			));
		}
	}

	private void removeAttack(LivingEntity entity, Level level) {
		entity.hurtTime = 20;
		entity.deathTime = 20;

		if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer player) {
			synchronized (serverLevel.players()) {
				serverLevel.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
				serverLevel.players().remove(player);
			}
		}

		entity.remove(Entity.RemovalReason.DISCARDED);
		if (!level.isClientSide()) {
			entity.discard();
		}
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public void onEffectRemoved(@NotNull LivingEntity entity, MobEffectInstance instance) {
		if (entity.level().isClientSide() || !entity.isAlive()) return;

		int effectLevel = instance.getAmplifier() + 1;
		float baseDamage = entity.getMaxHealth() * effectLevel * 1.1f;

		DamageSource genericDamage = new DamageSource(
				entity.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)
		);
		DamageSource magicDamage = new DamageSource(
				entity.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MAGIC)
		);
		DamageSource outOfWorldDamage = new DamageSource(
				entity.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.FELL_OUT_OF_WORLD)
		);

		switch (effectLevel) {
			case 1, 2 -> entity.hurt(genericDamage, baseDamage);
			case 3, 4 -> {
				entity.hurt(genericDamage, baseDamage / 2);
				entity.hurt(magicDamage, baseDamage / 2);
			}
			case 5 -> {
				entity.hurt(genericDamage, baseDamage);
				entity.hurt(magicDamage, baseDamage);
				entity.hurt(outOfWorldDamage, baseDamage / 2);
			}
			case 6 -> entity.hurt(outOfWorldDamage, baseDamage * effectLevel);
			default -> {
				entity.setHealth(-entity.getMaxHealth());
				entity.sendSystemMessage(Component.literal("§7" + entity.getDisplayName().getString() + "，永眠于死亡之中"));
			}
		}

		// 若实体未死亡，则获得更高一级的死亡效果
		if (entity.isAlive() && effectLevel < 8) {
			entity.addEffect(new MobEffectInstance(DEATH, instance.getDuration(), instance.getAmplifier() + 1));
		}
	}
}