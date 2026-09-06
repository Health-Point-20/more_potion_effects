package com.yixi_xun.more_potion_effects.editor;

import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 按 {@code PotionBrewingSystem.registerRecipe} 的规则，从编辑中的配方构建预览物品栈。
 * 仅用于 GUI 展示，不参与实际注册。
 */
public final class RecipePreview {

    private RecipePreview() {
    }

    public static ItemStack build(RecipeDoc.RecipeEntry entry) {
        List<MobEffectInstance> instances = new ArrayList<>();
        for (RecipeDoc.EffectEntry fx : entry.effects) {
            String fid = fx.effectId == null ? "" : fx.effectId.trim();
            ResourceLocation rid = fid.isEmpty() ? null : ResourceLocation.tryParse(fid);
            if (rid == null) {
                continue;
            }
            Optional<Holder.Reference<MobEffect>> holder = BuiltInRegistries.MOB_EFFECT.getHolder(rid);
            if (holder.isEmpty()) {
                continue;
            }
            instances.add(new MobEffectInstance(holder.get(),
                    fx.duration().orElse(0),
                    fx.amplifier().orElse(0),
                    fx.ambient, fx.visible, fx.showIcon));
        }

        ItemStack stack = new ItemStack(Items.POTION);
        Optional<Integer> color = RecipeDoc.parseHexColor(entry.customColor);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), color, instances));

        String customBase = entry.customBase == null ? "" : entry.customBase.trim();
        if (!customBase.isEmpty()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("BasePotion", customBase);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        String customName = entry.customName == null ? "" : entry.customName.trim();
        if (!customName.isEmpty()) {
            // 与加载器一致
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(customName));
        }
        return stack;
    }

    /** 材料物品栈（用于 GUI 图标展示）。 */
    public static ItemStack ingredientStack(String ingredientId) {
        try {
            ResourceLocation rl = ResourceLocation.tryParse(ingredientId == null ? "" : ingredientId.trim());
            if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) {
                return new ItemStack(BuiltInRegistries.ITEM.get(rl));
            }
        } catch (Exception e) {
            MorePotionEffectsMod.LOGGER.debug("preview ingredient failed", e);
        }
        return ItemStack.EMPTY;
    }
}
