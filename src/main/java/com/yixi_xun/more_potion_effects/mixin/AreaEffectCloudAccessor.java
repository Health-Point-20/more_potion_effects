package com.yixi_xun.more_potion_effects.mixin;

import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AreaEffectCloud.class)
public interface AreaEffectCloudAccessor {
    @Accessor("potionContents")
    PotionContents getPotionContents();
}
