package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KineticMobEffect extends MobEffect {
    public static final Map<UUID, Vec3> previousPos = new HashMap<>();
    public static final Map<UUID, Vec3> velocities = new HashMap<>();

    public KineticMobEffect() {
        super(MobEffectCategory.NEUTRAL, -13090992);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(@NotNull net.minecraft.world.entity.LivingEntity entity, int amplifier) {
        return true;
    }
}