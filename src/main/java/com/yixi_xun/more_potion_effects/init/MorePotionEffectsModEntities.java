package com.yixi_xun.more_potion_effects.init;

import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import com.yixi_xun.more_potion_effects.entity.HomingArrowEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

public class MorePotionEffectsModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MorePotionEffectsMod.MOD_ID);
    
    public static final DeferredHolder<EntityType<?>, EntityType<HomingArrowEntity>> HOMING_ARROW = REGISTRY.register("homing_arrow",
            () -> EntityType.Builder.<HomingArrowEntity>of(HomingArrowEntity::new, MobCategory.MISC)
                    .setShouldReceiveVelocityUpdates(true)
                    .setTrackingRange(64)
                    .setUpdateInterval(1)
                    .sized(0.5f, 0.5f)
                    .build("homing_arrow"));
}