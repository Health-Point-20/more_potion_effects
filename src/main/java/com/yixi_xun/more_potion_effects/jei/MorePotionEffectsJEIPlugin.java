package com.yixi_xun.more_potion_effects.jei;

import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import com.yixi_xun.more_potion_effects.api.PotionBrewingSystem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class MorePotionEffectsJEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "main");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<IJeiBrewingRecipe> brewingRecipes = new ArrayList<>();

        // 注册IngredientBrewingRecipe
        for (PotionBrewingSystem.IngredientBrewingRecipe recipe : PotionBrewingSystem.getRegisteredIngredientRecipes()) {
            List<ItemStack> inputs = new ArrayList<>();

            // 获取所有可能的输入药水
            for (Item potionItem : new Item[]{Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION}) {
                // 首先通过NBT查找
                ItemStack input = PotionBrewingSystem.getCustomsPotionStacks().stream()
                        .filter(stack -> stack.getItem() == potionItem &&
                                stack.has(DataComponents.CUSTOM_DATA) &&
                                stack.get(DataComponents.CUSTOM_DATA).contains("BasePotion") &&
                                stack.get(DataComponents.CUSTOM_DATA).copyTag().getString("BasePotion").equals(recipe.basePotionId()))
                        .findFirst()
                        .orElse(ItemStack.EMPTY);

                // 再查找原版药水
                if (input.isEmpty()) {
                    if (recipe.basePotion().equals(Potions.AWKWARD)) {
                        ItemStack awkwardPotion = new ItemStack(potionItem);
                        awkwardPotion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.AWKWARD));
                        inputs.add(awkwardPotion);
                    } else if (recipe.basePotion().equals(Potions.WATER)) {
                        ItemStack waterPotion = new ItemStack(potionItem);
                        waterPotion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
                        inputs.add(waterPotion);
                    } else if (recipe.basePotion().equals(Potions.MUNDANE)) {
                        ItemStack mundanePotion = new ItemStack(potionItem);
                        mundanePotion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.MUNDANE));
                        inputs.add(mundanePotion);
                    } else if (recipe.basePotion().equals(Potions.THICK)) {
                        ItemStack thickPotion = new ItemStack(potionItem);
                        thickPotion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.THICK));
                        inputs.add(thickPotion);
                    }
                } else {
                    inputs.add(input);
                }
            }

            if (!inputs.isEmpty() && recipe.ingredient().getItems().length > 0) {
                ItemStack output = recipe.outputTemplate().copy();
                ItemStack ingredient = recipe.ingredient().getItems()[0]; // 获取催化剂
                ResourceLocation uid = ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "brewing_" + brewingRecipes.size());

                IJeiBrewingRecipe brewingRecipe = registration.getVanillaRecipeFactory()
                        .createBrewingRecipe(List.of(ingredient), inputs, output, uid);
                brewingRecipes.add(brewingRecipe);
            }
        }



        // 注册GunpowderConversionRecipe
        List<ItemStack> potionInputs = PotionBrewingSystem.getCustomsPotionStacks().stream()
                .toList();

        if (!potionInputs.isEmpty()) {
            for (ItemStack input : potionInputs) {
                ItemStack output = PotionBrewingSystem.gunpowderConversion(input);
                ResourceLocation uid = ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "brewing_" + brewingRecipes.size());

                IJeiBrewingRecipe gunpowderRecipe = registration.getVanillaRecipeFactory()
                        .createBrewingRecipe(
                                List.of(new ItemStack(Items.GUNPOWDER)),
                                List.of(input),
                                output,
                                uid
                        );
                brewingRecipes.add(gunpowderRecipe);
            }
        }

        // 注册DragonBreathConversionRecipe
        List<ItemStack> splashInputs = PotionBrewingSystem.getCustomsPotionStacks().stream().toList();

        if (!splashInputs.isEmpty()) {
            for (ItemStack input : splashInputs) {
                ItemStack output = PotionBrewingSystem.dragonBreathConversion(input);
                ResourceLocation uid = ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "brewing_" + brewingRecipes.size());

                IJeiBrewingRecipe dragonBreathRecipe = registration.getVanillaRecipeFactory()
                        .createBrewingRecipe(
                                List.of(new ItemStack(Items.DRAGON_BREATH)),
                                List.of(input),
                                 output,
                                 uid
                        );
                brewingRecipes.add(dragonBreathRecipe);
            }
        }

        registration.addRecipes(RecipeTypes.BREWING, brewingRecipes);
    }
}