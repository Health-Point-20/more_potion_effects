package com.yixi_xun.more_potion_effects.mob_effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class KineticMobEffect extends MobEffect {
    public static final Map<Entity, Vec3> previousPos = new HashMap<>();
    public static final Map<Entity, Vec3> velocities = new HashMap<>();

    public KineticMobEffect() {
        super(MobEffectCategory.NEUTRAL, -13090992);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(@NotNull net.minecraft.world.entity.LivingEntity entity, int amplifier) {
        // 动能效果的逻辑在 MPEPlayerHandler 中实现
        return true;
    }
}