package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.api.IMoreMobEffect;
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

public class RankMobEffect extends MobEffect implements IMoreMobEffect {

    private static final int MAX_RANK = 6;
    private static final int WEAKENED_EFFECT_DURATION = 60;
    private static final double WEAKENED_EFFECT_FACTOR = 0.5;
    private static final int PARTICLE_INTERVAL = 5;
    private static final double HEAL_FACTOR = 0.5;

    /*
     * 粒子相关参数
     *
     * 设计目标：
     * 1. 低等级只有轻微光点
     * 2. 中等级出现稳定光环
     * 3. 高等级增加点缀和皇冠
     * 4. 玩家自身粒子减量，避免第一人称挡视野
     * 5. 尽量避免使用大面积、深色、容易糊屏的粒子
     */
    private static final int BODY_PARTICLE_INTERVAL = 6;
    private static final int ACCENT_PARTICLE_INTERVAL = 9;
    private static final int CROWN_PARTICLE_INTERVAL = 5;

    private static final double HALO_ROTATION_SPEED = 0.045D;
    private static final double CROWN_ROTATION_SPEED = 0.05D;

    // 每个阶级对应的光环粒子数量，索引为 rankLevel - 1
    private static final int[] HALO_COUNTS = {4, 5, 6, 7, 8, 9};

    // 每个阶级对应的身体粒子数量，索引为 rankLevel - 1
    private static final int[] BODY_COUNTS = {2, 3, 4, 5, 6, 7};

    /*
     * 身体粒子类型。
     *
     * 这里尽量使用视觉体积较小、较干净的粒子：
     * GLOW、ELECTRIC_SPARK、SCULK_SOUL、REVERSE_PORTAL、END_ROD
     *
     * 不建议高频使用：
     * DRAGON_BREATH、GLOW_SQUID_INK、FIREWORK
     * 因为它们容易遮挡视野或显得杂乱。
     */
    private static final ParticleOptions[] TIER_PARTICLES = {
            ParticleTypes.GLOW,
            ParticleTypes.ELECTRIC_SPARK,
            ParticleTypes.SCULK_SOUL,
            ParticleTypes.REVERSE_PORTAL,
            ParticleTypes.END_ROD,
            ParticleTypes.END_ROD
    };

    // 光环粒子类型
    private static final ParticleOptions[] HALO_PARTICLES = {
            ParticleTypes.GLOW,
            ParticleTypes.SCULK_SOUL,
            ParticleTypes.REVERSE_PORTAL,
            ParticleTypes.END_ROD,
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
            spawnParticles(serverLevel, amplifier, entity);
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

        // 使用 Holder<Attribute> 作为键
        Map<Holder<Attribute>, AttributeModifier> modifiers = new HashMap<>();

        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("more_potion_effects", "rank_health"),
                mods.health(),
                AttributeModifier.Operation.ADD_VALUE
        ));

        modifiers.put(Attributes.ARMOR, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("more_potion_effects", "rank_armor"),
                mods.armor(),
                AttributeModifier.Operation.ADD_VALUE
        ));

        modifiers.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("more_potion_effects", "rank_toughness"),
                mods.armor(),
                AttributeModifier.Operation.ADD_VALUE
        ));

        modifiers.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("more_potion_effects", "rank_attack_speed"),
                mods.attackSpeed() - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));

        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("more_potion_effects", "rank_attack_damage"),
                mods.attack() - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));

        modifiers.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("more_potion_effects", "rank_movement"),
                mods.movement() - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));

        modifiers.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("more_potion_effects", "rank_knockback"),
                mods.knockBack(),
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

        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                WEAKENED_EFFECT_DURATION,
                level,
                false,
                false
        ));

        target.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS,
                WEAKENED_EFFECT_DURATION,
                level,
                false,
                false
        ));
    }

    private void applyWeakenedEffects(LivingEntity target, int sourceAmplifier) {
        int weakenedLevel = (int) Math.max(0, sourceAmplifier * WEAKENED_EFFECT_FACTOR);

        target.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                WEAKENED_EFFECT_DURATION,
                weakenedLevel,
                false,
                false
        ));

        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                WEAKENED_EFFECT_DURATION,
                weakenedLevel,
                false,
                false
        ));

        if (sourceAmplifier > 1) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_BOOST,
                    WEAKENED_EFFECT_DURATION,
                    weakenedLevel,
                    false,
                    false
            ));
        }
    }

    private void applyPlayerEffects(LivingEntity player, int amplifier) {
        int roundedLevel = (int) Math.round(amplifier * 0.5);

        player.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                WEAKENED_EFFECT_DURATION,
                roundedLevel,
                false,
                false
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                WEAKENED_EFFECT_DURATION,
                roundedLevel,
                false,
                false
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST,
                WEAKENED_EFFECT_DURATION,
                amplifier,
                false,
                false
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                WEAKENED_EFFECT_DURATION,
                amplifier,
                false,
                false
        ));
    }

    private void applyNonPlayerEffects(LivingEntity entity, int amplifier) {
        int roundedLevel = (int) Math.round(amplifier * 0.5);

        entity.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                WEAKENED_EFFECT_DURATION,
                roundedLevel,
                false,
                false
        ));
    }

    /*
     * 粒子总入口。
     *
     * duration 保留是为了兼容原来的调用方式。
     * 现在粒子间隔主要使用 serverLevel.getGameTime() + entity.getId() 控制。
     */
    private void spawnParticles(ServerLevel serverLevel, int amplifier, LivingEntity entity) {
        int rankLevel = Math.min(amplifier + 1, MAX_RANK);
        if (rankLevel <= 0) return;

        // 加入实体 ID 做相位偏移，避免大量实体在同一 tick 集中生成粒子
        long time = serverLevel.getGameTime() + entity.getId();

        // 头顶光环
        if (Math.floorMod(time, getHaloInterval(rankLevel)) == 0) {
            spawnHaloParticles(serverLevel, rankLevel, entity, time);
        }

        // 身体周围粒子
        if (Math.floorMod(time, BODY_PARTICLE_INTERVAL) == 0) {
            spawnBodyParticles(serverLevel, rankLevel, entity);
        }

        // 高等级点缀粒子
        if (rankLevel >= 4 && Math.floorMod(time, ACCENT_PARTICLE_INTERVAL) == 0) {
            spawnAccentParticles(serverLevel, rankLevel, entity);
        }

        // 6 级专属皇冠
        if (rankLevel == 6 && Math.floorMod(time, CROWN_PARTICLE_INTERVAL) == 0) {
            spawnCrownParticles(serverLevel, entity, time);
        }
    }

    private int getHaloInterval(int rankLevel) {
        // 高等级光环更明显，所以频率稍高
        return rankLevel >= 5 ? 3 : 4;
    }

    private void spawnHaloParticles(ServerLevel serverLevel, int rankLevel, LivingEntity entity, long time) {
        int tier = Math.min(rankLevel - 1, HALO_PARTICLES.length - 1);

        ParticleOptions particle = rankLevel >= MAX_RANK
                ? ParticleTypes.END_ROD
                : HALO_PARTICLES[tier];

        int count = HALO_COUNTS[Math.min(rankLevel - 1, HALO_COUNTS.length - 1)];

        // 玩家自己的光环减量，避免第一人称挡视野
        if (entity instanceof Player) {
            count = Math.max(4, count - 2);
        }

        // 光环半径稍微大一点，但放在头顶上方，不要贴脸
        double radius = 0.32D + rankLevel * 0.035D;

        // 光环高度：头顶上方一段距离
        double y = entity.getY() + entity.getBbHeight() + 0.22D + rankLevel * 0.02D;

        double rotation = time * HALO_ROTATION_SPEED + entity.getId() * 0.35D;

        double x = entity.getX();
        double z = entity.getZ();

        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2D * i) / count + rotation;

            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;

            // 轻微上下浮动，不要太明显
            double py = y + Math.sin(angle * 3D + rotation) * 0.025D;

            /*
             * count = 0：生成单个精确粒子
             * offset 为 0：基本不额外随机运动
             */
            serverLevel.sendParticles(
                    particle,
                    px, py, pz,
                    0,
                    0.0D, 0.0D, 0.0D,
                    1.0D
            );
        }
    }

    private void spawnBodyParticles(ServerLevel serverLevel, int rankLevel, LivingEntity entity) {
        int tier = Math.min(rankLevel - 1, TIER_PARTICLES.length - 1);
        ParticleOptions particle = TIER_PARTICLES[tier];

        int count = BODY_COUNTS[Math.min(rankLevel - 1, BODY_COUNTS.length - 1)];

        // 玩家身体粒子进一步减少
        if (entity instanceof Player) {
            count = Math.max(2, count - 2);
        }

        /*
         * 如果你希望玩家自己完全不被身体粒子遮挡，
         * 可以取消下面这行注释。
         */
        // if (entity instanceof Player) return;

        double radius = entity.getBbWidth() * 0.5D + 0.10D + rankLevel * 0.02D;

        // 中心放在身体中下部，避开头部和玩家视角中心
        double centerY = entity.getY() + entity.getBbHeight() * 0.42D;
        double offsetY = entity.getBbHeight() * 0.28D;

        /*
         * 一次发包生成多个随机粒子。
         *
         * 这比循环调用：
         * serverLevel.sendParticles(..., 1, ...)
         * 性能好很多。
         */
        serverLevel.sendParticles(
                particle,
                entity.getX(), centerY, entity.getZ(),
                count,
                radius, offsetY, radius,
                0.012D
        );
    }

    private void spawnAccentParticles(ServerLevel serverLevel, int rankLevel, LivingEntity entity) {
        int count = rankLevel >= 5 ? 3 : 2;

        if (entity instanceof Player) {
            count = Math.max(1, count - 1);
        }

        ParticleOptions particle = rankLevel >= 6
                ? ParticleTypes.END_ROD
                : ParticleTypes.GLOW;

        double y = entity.getY() + entity.getBbHeight() * (rankLevel >= 6 ? 0.82D : 0.65D);
        double spread = 0.28D + rankLevel * 0.02D;

        serverLevel.sendParticles(
                particle,
                entity.getX(), y, entity.getZ(),
                count,
                spread, 0.18D, spread,
                0.02D
        );
    }

    private void spawnCrownParticles(ServerLevel serverLevel, LivingEntity entity, long time) {
        // 玩家皇冠也减量
        int count = entity instanceof Player ? 6 : 8;

        double radius = Math.max(0.35D, entity.getBbWidth() * 0.5D + 0.18D);

        // 皇冠放在头顶更高处
        double y = entity.getY() + entity.getBbHeight() + 0.35D;

        double rotation = time * CROWN_ROTATION_SPEED + entity.getId() * 0.35D;

        double x = entity.getX();
        double z = entity.getZ();

        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2D * i) / count + rotation;

            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;

            // 皇冠轻微起伏
            double py = y + Math.sin(angle * 4D + rotation) * 0.04D;

            /*
             * END_ROD 比较小、干净，适合做皇冠。
             *
             * 这里给一点点向上速度，让皇冠粒子有轻微漂浮感。
             */
            serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    px, py, pz,
                    0,
                    0.0D, 0.008D, 0.0D,
                    1.0D
            );
        }
    }

    private record AttributeModifiers(
            double health,
            double armor,
            double attackSpeed,
            double attack,
            double movement,
            double knockBack
    ) {
    }
}