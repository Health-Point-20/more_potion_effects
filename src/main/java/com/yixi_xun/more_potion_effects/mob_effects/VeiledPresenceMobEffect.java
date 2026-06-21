package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.VEILED_PRESENCE;

public class VeiledPresenceMobEffect extends MobEffect {
    public static final Map<UUID, UUID> attackRelations = new HashMap<>();

    public VeiledPresenceMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7F7F7F);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        entity.setInvisible(true);
        if (entity.level().getGameTime() % 100 == 0) {
            cleanupInvalidRelations(entity);
        }
        return true;
    }

    private void cleanupInvalidRelations(LivingEntity entity) {
        attackRelations.entrySet().removeIf(entry ->
                entity.level().getPlayerByUUID(entry.getValue()) == null
        );
    }

    public static void onAttack(LivingEntity attacker, LivingEntity target) {
        if (target.hasEffect(VEILED_PRESENCE)) {
            attackRelations.put(attacker.getUUID(), target.getUUID());

            if (attacker instanceof Mob mob) {
                mob.setTarget(target);
                mob.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            }
        }
    }

    public static boolean canAttack(LivingEntity attacker, LivingEntity target) {
        if (target.hasEffect(VEILED_PRESENCE)) {
            if (attacker instanceof Player) return true;
            return attackRelations.getOrDefault(attacker.getUUID(), UUID.randomUUID())
                    .equals(target.getUUID());
        }
        return true;
    }

    public static void removeRelations(LivingEntity entity) {
        UUID uuid = entity.getUUID();
        attackRelations.values().removeIf(uuid::equals);
        attackRelations.remove(uuid);
    }
}