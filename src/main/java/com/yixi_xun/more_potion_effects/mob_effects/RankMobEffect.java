package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.api.IMobEffectRemovable;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.*;
import static com.yixi_xun.more_potion_effects.MPEConfig.RANK_EFFECTS_ENABLED;

public class RankMobEffect extends MobEffect implements IMobEffectRemovable {
    private static final int MAX_RANK = 6;
    private static final int WEAKENED_EFFECT_DURATION = 60;
    private static final double WEAKENED_EFFECT_FACTOR = 0.5;
    private static final int PARTICLE_INTERVAL = 5;
    private static final double HEAL_FACTOR = 0.5;
    private static final double CROWN_ROTATION_SPEED = 0.02;

    private static final ParticleOptions[] TIER_PARTICLES = {
            ParticleTypes.ELECTRIC_SPARK,
            ParticleTypes.GLOW,
            ParticleTypes.SCULK_SOUL,
            ParticleTypes.REVERSE_PORTAL,
            ParticleTypes.DRAGON_BREATH,
            ParticleTypes.END_ROD
    };

    private static final ParticleOptions[] HALO_PARTICLES = {
            ParticleTypes.GLOW,
            ParticleTypes.SCULK_SOUL,
            ParticleTypes.SOUL_FIRE_FLAME,
            ParticleTypes.DRAGON_BREATH,
            ParticleTypes.END_ROD
    };

    private static final Map<Integer, AttributeModifiers> ATTRIBUTE_MODIFIERS = Map.of(
            1, new AttributeModifiers(2.0, 4.0, 1.1, 1.5, 1.2, 0.5),
            2, new AttributeModifiers(3.0, 6.0, 1.25, 2.0, 1.35, 0.75),
            3, new AttributeModifiers(5.0, 10.0, 1.5, 3.5, 1.5, 0.8),
            4, new AttributeModifiers(7.5, 15.0, 2.0, 5.0, 1.7, 0.9),
            5, new AttributeModifiers(10.0, 20.0, 3.0, 7.0, 2.0, 0.95),
            6, new AttributeModifiers(20.0, 40.0, 5.0, 10.0, 3.0, 0.99)
    );

    // 效果池缓存 - 使用已移植的效果
    private static final Map<Integer, List<MobEffectInstance>> EFFECT_POOL_CACHE = new HashMap<>();

    public RankMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -10092442);
    }

    @Override
    public void onEffectAdded(@NotNull LivingEntity entity, int amplifier) {
        super.onEffectAdded(entity, amplifier);
        int rankLevel = Math.min(amplifier + 1, MAX_RANK);
        entity.heal((float) (HEAL_FACTOR * entity.getMaxHealth()));
        if (entity instanceof Player) {
            // 玩家也获得属性加成
            applyAttributeModifiers(entity, rankLevel, true);
        } else {
            updateEntityName(entity, rankLevel);
            applyAttributeModifiers(entity, rankLevel, true);
            if (RANK_EFFECTS_ENABLED.get()) {
                applyRandomEffects(entity, amplifier);
            }
        }
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        MobEffectInstance effectInstance = entity.getEffect(RANK);
        if (effectInstance == null) return false;

        int duration = effectInstance.getDuration();
        Level level = entity.level();

        if (duration % PARTICLE_INTERVAL == 0) {
            if (!(entity instanceof Player)) {
                applyNegativeEffectsToTarget(entity, amplifier);
                applyWeakenedBuffsToSameType(level, entity, amplifier);
            }
            applySelfEffects(entity, amplifier);
            applyAreaNegativeEffects(level, entity, amplifier);
        }

        if (level instanceof ServerLevel serverLevel) {
            spawnParticles(serverLevel, amplifier, entity, duration);
        }
        return true;
    }

    @Override
	public void onEffectRemoved(@NotNull LivingEntity entity, MobEffectInstance instance) {
		int rankLevel = Math.min(instance.getAmplifier() + 1, MAX_RANK);
		applyAttributeModifiers(entity, rankLevel, false);
		if (!(entity instanceof Player)) {
			restoreEntityName(entity, rankLevel);
		}
	}

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    private void updateEntityName(LivingEntity entity, int rankLevel) {
        entity.getPersistentData().putBoolean("hasCustomName", entity.hasCustomName());
        entity.setCustomName(Component.literal(
                Component.translatable("rank_entity_title_" + rankLevel).getString() +
                        entity.getDisplayName().getString()
        ));
    }

    private void restoreEntityName(LivingEntity entity, int rankLevel) {
        String title = Component.translatable("rank_entity_title_" + rankLevel).getString();
        if (entity.getPersistentData().getBoolean("hasCustomName")) {
            Component customName = entity.getCustomName();
            if (customName != null) {
                String displayName = customName.getString();
                if (displayName.startsWith(title)) {
                    entity.setCustomName(Component.literal(displayName.substring(title.length())));
                    entity.setCustomNameVisible(false);
                }
            }
        } else {
            entity.setCustomName(null);
        }
    }

    private int getRankLevel(LivingEntity entity) {
        if (entity == null) return -1;
        MobEffectInstance effect = entity.getEffect(RANK);
        return effect != null ? effect.getAmplifier() : -1;
    }

    private void applyNegativeEffectsToTarget(LivingEntity entity, int amplifier) {
        if (!(entity instanceof Mob mob)) return;
        LivingEntity target = mob.getTarget();
        if (target == null || getRankLevel(target) == -1) return;
        double maxDistance = (amplifier + 1) * 2 + 6;
        double distance = entity.distanceTo(target);
        if (getRankLevel(target) < amplifier && distance <= maxDistance) {
            applyNegativeEffects(target, amplifier - getRankLevel(target));
        }
    }

    private void applyAreaNegativeEffects(Level level, LivingEntity entity, int amplifier) {
        double radius = ((amplifier + 1) * 2 + 4) / 2d;
        AABB area = entity.getBoundingBox().inflate(radius);
        EntityType<?> sourceType = entity.getType();
        for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (livingEntity != entity &&
                    livingEntity.getType() != sourceType &&
                    getRankLevel(livingEntity) < amplifier) {
                applyNegativeEffects(livingEntity, amplifier - getRankLevel(livingEntity));
            }
        }
    }

    private void applySelfEffects(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            applyPlayerEffects(player, amplifier);
        } else {
            applyNonPlayerEffects(entity, amplifier);
        }
    }

    private void applyWeakenedBuffsToSameType(Level level, LivingEntity source, int amplifier) {
        if (amplifier < 1) return;
        double radius = (amplifier + 3) / 2d;
        AABB area = source.getBoundingBox().inflate(radius);
        EntityType<?> sourceType = source.getType();
        for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (livingEntity != source &&
                    livingEntity.getType() == sourceType &&
                    getRankLevel(livingEntity) < 0) {
                applyWeakenedEffects(livingEntity, amplifier);
            }
        }
    }

    private void applyAttributeModifiers(LivingEntity entity, int rankLevel, boolean apply) {
        AttributeModifiers mods = ATTRIBUTE_MODIFIERS.getOrDefault(rankLevel, ATTRIBUTE_MODIFIERS.get(1));

        // 使用Holder<Attribute>作为键
        Map<Holder<Attribute>, AttributeModifier> modifiers = new HashMap<>();

        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("more_potion_effects", "rank_health"),
                mods.health,
                AttributeModifier.Operation.ADD_VALUE
        ));
        modifiers.put(Attributes.ARMOR, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("more_potion_effects", "rank_armor"),
                mods.armor,
                AttributeModifier.Operation.ADD_VALUE
        ));
        modifiers.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("more_potion_effects", "rank_toughness"),
                mods.armor,
                AttributeModifier.Operation.ADD_VALUE
        ));
        modifiers.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("more_potion_effects", "rank_attack_speed"),
                mods.attackSpeed - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("more_potion_effects", "rank_attack_damage"),
                mods.attack - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
        modifiers.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("more_potion_effects", "rank_movement"),
                mods.movement - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
        modifiers.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("more_potion_effects", "rank_knockback"),
                mods.knockBack,
                AttributeModifier.Operation.ADD_VALUE
        ));

        if (apply) {
            for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : modifiers.entrySet()) {
                AttributeInstance attr = entity.getAttribute(entry.getKey());
                if (attr != null) {
                    attr.addTransientModifier(entry.getValue());
                }
            }
            entity.setHealth(entity.getMaxHealth());
        } else {
            for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : modifiers.entrySet()) {
                AttributeInstance attr = entity.getAttribute(entry.getKey());
                if (attr != null) {
                    attr.removeModifier(entry.getValue().id());
                }
            }
        }
    }

    private void applyRandomEffects(LivingEntity entity, int amplifier) {
        int level = Math.max(amplifier - 1, 0);
        List<MobEffectInstance> effectPool = createEffectPool(level);
        int effectCount = Math.min(amplifier + 1, 13);
        RandomSource random = entity.getRandom();
        Set<MobEffectInstance> chosenEffects = new HashSet<>();
        while (chosenEffects.size() < effectCount && !effectPool.isEmpty()) {
            chosenEffects.add(effectPool.get(random.nextInt(effectPool.size())));
        }
        chosenEffects.forEach(entity::addEffect);
    }

    private List<MobEffectInstance> createEffectPool(int level) {
        return EFFECT_POOL_CACHE.computeIfAbsent(level, lvl -> {
            // 使用已移植的效果简化效果池
            List<Holder<MobEffect>> effects = List.of(
                    ADAPTATION,
                    IMMORTAL,
                    INJURY_LINK,
                    IMMUNE,
                    ATTACK_AOE,
                    RECOIL,
                    MobEffects.REGENERATION,
                    MobEffects.FIRE_RESISTANCE,
                    MobEffects.INVISIBILITY,
                    MobEffects.DAMAGE_RESISTANCE,
                    MobEffects.MOVEMENT_SPEED,
                    MobEffects.DAMAGE_BOOST,
                    MobEffects.HEALTH_BOOST,
                    MobEffects.WATER_BREATHING,
                    MobEffects.LUCK
            );
            List<MobEffectInstance> pool = new ArrayList<>(effects.size());
            for (Holder<MobEffect> effectHolder : effects) {
                pool.add(new MobEffectInstance(effectHolder, 2000000, lvl, false, false));
            }
            return pool;
        });
    }

    private void applyNegativeEffects(LivingEntity target, int level) {
        if (level < 0) return;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, WEAKENED_EFFECT_DURATION, level, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKENED_EFFECT_DURATION, level, false, false));
    }

    private void applyWeakenedEffects(LivingEntity target, int sourceAmplifier) {
        int weakenedLevel = (int) Math.max(0, sourceAmplifier * WEAKENED_EFFECT_FACTOR);
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, WEAKENED_EFFECT_DURATION, weakenedLevel, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, WEAKENED_EFFECT_DURATION, weakenedLevel, false, false));
        if (sourceAmplifier > 1) {
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, WEAKENED_EFFECT_DURATION, weakenedLevel, false, false));
        }
    }

    private void applyPlayerEffects(LivingEntity player, int amplifier) {
        int roundedLevel = (int) Math.round(amplifier * 0.5);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, WEAKENED_EFFECT_DURATION, roundedLevel, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, WEAKENED_EFFECT_DURATION, roundedLevel, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, WEAKENED_EFFECT_DURATION, amplifier, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, WEAKENED_EFFECT_DURATION, amplifier, false, false));
    }

    private void applyNonPlayerEffects(LivingEntity entity, int amplifier) {
        int roundedLevel = (int) Math.round(amplifier * 0.5);
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, WEAKENED_EFFECT_DURATION, roundedLevel, false, false));
    }

    private void spawnParticles(ServerLevel serverLevel, int amplifier, LivingEntity entity, int duration) {
        int rankLevel = Math.min(amplifier + 1, MAX_RANK);
        int tier = Math.min(rankLevel - 1, TIER_PARTICLES.length - 1);
        if (tier < 0) return;

        // 光环粒子每tick都生成
        spawnHaloParticles(serverLevel, rankLevel, entity, duration);

        // 身体粒子每PARTICLE_INTERVAL tick生成一次
        if (duration % PARTICLE_INTERVAL == 0) {
            RandomSource random = entity.getRandom();
            spawnBodyParticles(serverLevel, rankLevel, tier, random, entity);
        }
    }

    private void spawnHaloParticles(ServerLevel serverLevel, int rankLevel, LivingEntity entity, int duration) {
        int haloParticles = 16 + rankLevel * 2;
        double haloRadius = 0.2 + rankLevel * 0.05;
        double haloHeight = entity.getY() + entity.getBbHeight() + 0.3;
        int tier = Math.min(rankLevel - 1, HALO_PARTICLES.length - 1);

        ParticleOptions particle;
        if (rankLevel == MAX_RANK) {
            particle = ParticleTypes.END_ROD;
        } else {
            particle = HALO_PARTICLES[tier];
        }

        // 添加旋转效果
        double rotation = duration * CROWN_ROTATION_SPEED;

        for (int i = 0; i < haloParticles; i++) {
            double angle = (Math.PI * 2 * i) / haloParticles + rotation;
            double px = entity.getX() + Math.cos(angle) * haloRadius;
            double pz = entity.getZ() + Math.sin(angle) * haloRadius;
            double py = haloHeight + (Math.sin(angle * 2) * 0.05);
            serverLevel.sendParticles(particle, px, py, pz, 1, 0.0, 0.0, 0.0, 8.0);
        }
    }

    private void spawnBodyParticles(ServerLevel serverLevel, int rankLevel, int tier, RandomSource random, LivingEntity entity) {
        double baseRadius = 0.7 + rankLevel * 0.12;
        int baseParticleCount = 4 + rankLevel * 2;
        ParticleOptions baseType = TIER_PARTICLES[tier];

        for (int i = 0; i < baseParticleCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = baseRadius * (0.6 + random.nextDouble() * 0.4);
            double px = entity.getX() + Math.cos(angle) * distance;
            double pz = entity.getZ() + Math.sin(angle) * distance;
            double py = entity.getY() + 0.1 + (random.nextDouble() * 0.8) * entity.getBbHeight();
            double vy = 0.02 + random.nextDouble() * 0.03;
            serverLevel.sendParticles(baseType, px, py, pz, 1, 0.01, vy, 0.01, 1.0);
        }

        if (rankLevel >= 4) {
            int specialParticleCount = 1 + rankLevel / 2;
            for (int i = 0; i < specialParticleCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = (0.8 + rankLevel * 0.12) * (0.7 + random.nextDouble() * 0.3);
                double px = entity.getX() + Math.cos(angle) * distance;
                double pz = entity.getZ() + Math.sin(angle) * distance;
                double py = entity.getY() + 0.1 + (random.nextDouble() * 0.7) * entity.getBbHeight();
                double vy = 0.03 + random.nextDouble() * 0.04;

                if (rankLevel >= 5 && random.nextDouble() < 0.3) {
                    serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, px, py, pz, 1, 0.03, vy, 0.03, 2.0);
                } else if (random.nextDouble() < 0.4) {
                    serverLevel.sendParticles(ParticleTypes.GLOW, px, py, pz, 1, 0.03, vy * 0.7, 0.03, 1.5);
                }
            }
        }

        // 等级6专属粒子效果
        if (rankLevel >= 6) {
            double crownRadius = 0.5 + entity.getBbWidth() * 0.5;
            double crownY = entity.getY() + entity.getBbHeight() + 0.5;
            int crownParticles = 8 + rankLevel;
            double rotation = (serverLevel.getGameTime() % 100) * CROWN_ROTATION_SPEED;

            for (int i = 0; i < crownParticles; i++) {
                double angle = (Math.PI * 2 * i) / crownParticles + rotation;
                double px = entity.getX() + Math.cos(angle) * crownRadius;
                double pz = entity.getZ() + Math.sin(angle) * crownRadius;
                double py = crownY + (Math.sin(angle * 3) * 0.1);
                serverLevel.sendParticles(ParticleTypes.FIREWORK, px, py, pz, 1, 0, 0, 0, 0);

                if (random.nextDouble() < 0.3) {
                    serverLevel.sendParticles(ParticleTypes.GLOW_SQUID_INK,
                            px, py + 0.1, pz,
                            1,
                            0.02, 0.02, 0.02,
                            0.1);
                }
            }
        }
    }

    private record AttributeModifiers(double health, double armor, double attackSpeed,
                                      double attack, double movement, double knockBack) {}
}