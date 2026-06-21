package com.yixi_xun.more_potion_effects.init;

import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public class MorePotionEffectsModEnchantments {
    public static final ResourceLocation SOURCE_OF_BLESSING = ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "source_of_blessing");
    public static final ResourceLocation SOURCE_OF_CURSES = ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "source_of_curses");
    public static final ResourceLocation ELIMINATION_EFFECT = ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "elimination_effect");
    public static final ResourceLocation UNYIELDING = ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "unyielding");
    public static final ResourceLocation SUNDER_ARMOR = ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "sunder_armor");
    public static final ResourceLocation POTION_PUNISHER = ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "potion_punisher");
    public static final ResourceLocation HIGHLY_TOXIC_BLADE = ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "highly_toxic_blade");
    public static final ResourceLocation FLYING = ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "flying");
    public static final ResourceLocation INFLICTION_CORROSION = ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "infliction_corrosion");
    public static final ResourceLocation VIBRANT = ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "vibrant");
    public static final ResourceLocation INHIBIT_THERAPY = ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "inhibit_therapy");

    public static int getEnchantmentLevel(net.minecraft.world.item.ItemStack stack, ResourceLocation id) {
        var holder = stack.getEnchantments().keySet().stream()
                .filter(h -> h.is(id))
                .findFirst();
        if (holder.isPresent()) {
            return stack.getEnchantments().getLevel(holder.get());
        }
        return 0;
    }
}