package com.yixi_xun.more_potion_effects.api;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@EventBusSubscriber(modid = MorePotionEffectsMod.MOD_ID)
public class PotionBrewingSystem {
    private static final Path CONFIG_DIR = Paths.get("config", "more_potion_effects");
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(EffectConfig.class, new EffectConfig.Deserializer())
            .create();
    private static final List<ItemStack> GENERATED_POTION_STACKS = new ArrayList<>();
    // 当前生效的配方列表。注册到游戏的是读取该列表的"活"包装配方（见 LiveBrewingRecipe），
    // 因此热重载时只需替换列表内容，服务端与客户端的酿造逻辑即时生效。
    private static final List<IngredientBrewingRecipe> LIVE_RECIPES = new CopyOnWriteArrayList<>();

    public static List<ItemStack> getCustomsPotionStacks() {
        synchronized (GENERATED_POTION_STACKS) {
            List<ItemStack> ALL_CUSTOM_POTION = new ArrayList<>();
            GENERATED_POTION_STACKS.forEach(stack -> {
                ALL_CUSTOM_POTION.add(stack);
                ALL_CUSTOM_POTION.add(gunpowderConversion(stack));
                ALL_CUSTOM_POTION.add(dragonBreathConversion(stack));
                ALL_CUSTOM_POTION.add(getPotionArrow(stack));
            });
            return new ArrayList<>(ALL_CUSTOM_POTION);
        }
    }

    public static List<IngredientBrewingRecipe> getRegisteredIngredientRecipes() {
        return new ArrayList<>(LIVE_RECIPES);
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> loadRecipes(true));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addRecipe(new GunpowderConversionRecipe());
        event.getBuilder().addRecipe(new DragonBreathConversionRecipe());
        // 只注册一个"活"包装配方：它在每次酿造查询时读取 LIVE_RECIPES，
        // 因此热重载只需替换 LIVE_RECIPES 内容，无需重建 PotionBrewing。
        event.getBuilder().addRecipe(new LiveBrewingRecipe());
        MorePotionEffectsMod.LOGGER.info("Registered {} custom brewing recipes.", LIVE_RECIPES.size());
    }

    /**
     * 热重载：重新从本地 config 目录读取酿造配方并即时生效。
     * 服务端与客户端各自调用（读取各自游戏实例下的 config）。
     *
     * @return 重载后生效的配方数量
     */
    public static int reloadRecipes() {
        loadRecipes(false);
        return LIVE_RECIPES.size();
    }

    private static void loadRecipes(boolean createExamplesIfEmpty) {
        List<ItemStack> newStacks = new ArrayList<>();
        List<IngredientBrewingRecipe> newRecipes = new ArrayList<>();
        try {
            Files.createDirectories(CONFIG_DIR);
            try (var paths = Files.list(CONFIG_DIR)) {
                List<Path> jsonFiles = paths.filter(path -> path.toString().endsWith(".json")).toList();
                jsonFiles.forEach(path -> loadRecipeFile(path, newStacks, newRecipes));
                if (jsonFiles.isEmpty() && createExamplesIfEmpty) {
                    createExampleConfigs();
                }
            }
        } catch (IOException e) {
            MorePotionEffectsMod.LOGGER.error("Failed to load brewing recipes", e);
        }
        // 原子替换：包装配方读取的是 LIVE_RECIPES / GENERATED_POTION_STACKS 的内容，
        // 这里整体替换后，正在被游戏引用的包装配方会立即看到新数据。
        synchronized (GENERATED_POTION_STACKS) {
            GENERATED_POTION_STACKS.clear();
            GENERATED_POTION_STACKS.addAll(newStacks);
        }
        LIVE_RECIPES.clear();
        LIVE_RECIPES.addAll(newRecipes);
        MorePotionEffectsMod.LOGGER.info("Loaded {} custom brewing recipes.", LIVE_RECIPES.size());
    }

    private static void createExampleConfigs() throws IOException {
        Path exampleFile = CONFIG_DIR.resolve("example_recipes.json");
        if (!Files.exists(exampleFile)) {
            JsonObject root = new JsonObject();
            JsonArray recipes = new JsonArray();

            JsonObject multiEffect = new JsonObject();
            multiEffect.addProperty("base_potion", "minecraft:awkward");
            multiEffect.addProperty("ingredient", "minecraft:totem_of_undying");
            JsonArray effects = new JsonArray();
            JsonObject effect = new JsonObject();
            effect.addProperty("effect_id", "more_potion_effects:immortal");
            effect.addProperty("duration", 12000);
            effect.addProperty("amplifier", 0);
            effects.add(effect);
            multiEffect.add("effects", effects);
            multiEffect.addProperty("custom_name", "§e不死药水");
            multiEffect.addProperty("custom_color", "FFD700");
            multiEffect.addProperty("custom_base", "custom_immortal");
            recipes.add(multiEffect);

            JsonObject Potion = new JsonObject();
            Potion.addProperty("base_potion", "custom_immortal");
            Potion.addProperty("ingredient", "minecraft:totem_of_undying");
            JsonArray effects2 = new JsonArray();
            JsonObject effect1 = new JsonObject();
            effect1.addProperty("effect_id", "more_potion_effects:immortal");
            effect1.addProperty("duration", 12000);
            effect1.addProperty("amplifier", 1);
            effects2.add(effect1);
            JsonObject effect2 = new JsonObject();
            effect2.addProperty("effect_id", "minecraft:regeneration");
            effect2.addProperty("duration", 200);
            effect2.addProperty("amplifier", 2);
            effects2.add(effect2);
            Potion.add("effects", effects2);
            Potion.addProperty("custom_name", "§e不死药水II");
            Potion.addProperty("custom_color", "FFD700");
            Potion.addProperty("custom_base", "custom_immortal2");
            recipes.add(Potion);

            root.add("recipes", recipes);
            Files.writeString(exampleFile, GSON.toJson(root));
        }
    }

    private static void loadRecipeFile(Path file, List<ItemStack> stacksOut, List<IngredientBrewingRecipe> recipesOut) {
        try {
            String content = Files.readString(file);
            JsonElement rootElement = JsonParser.parseString(content);
            if (!rootElement.isJsonObject()) {
                MorePotionEffectsMod.LOGGER.error("Invalid JSON structure in file: {}", file);
                return;
            }
            JsonObject root = rootElement.getAsJsonObject();
            if (!root.has("recipes") || !root.get("recipes").isJsonArray()) {
                MorePotionEffectsMod.LOGGER.error("Missing 'recipes' array in file: {}", file);
                return;
            }
            JsonArray recipes = root.getAsJsonArray("recipes");
            for (JsonElement element : recipes) {
                if (!element.isJsonObject()) {
                    MorePotionEffectsMod.LOGGER.warn("Skipping invalid recipe element in {}", file);
                    continue;
                }
                try {
                    BrewingRecipe recipe = GSON.fromJson(element, BrewingRecipe.class);
                    if (recipe.isValid()) {
                        registerRecipe(recipe, stacksOut, recipesOut);
                        MorePotionEffectsMod.LOGGER.info("Successfully registered recipe: {}", recipe);
                    } else {
                        MorePotionEffectsMod.LOGGER.warn("Skipping invalid recipe in {}: {}", file, element);
                    }
                } catch (JsonParseException e) {
                    MorePotionEffectsMod.LOGGER.error("Error parsing recipe in {}: {}", file, element, e);
                }
            }
        } catch (Exception e) {
            MorePotionEffectsMod.LOGGER.error("Error reading recipe file: {}", file, e);
        }
    }

    private static void registerRecipe(BrewingRecipe recipe, List<ItemStack> stacksOut, List<IngredientBrewingRecipe> recipesOut) {
        List<MobEffectInstance> effectInstances = new ArrayList<>();
        if (recipe.effects != null) {
            for (EffectConfig effectConfig : recipe.effects) {
            // 使用 BuiltInRegistries 获取 MobEffect 的 Holder
            Optional<Holder.Reference<MobEffect>> effectHolderOpt = BuiltInRegistries.MOB_EFFECT.getHolder(effectConfig.getEffectId());
            if (effectHolderOpt.isEmpty()) {
                MorePotionEffectsMod.LOGGER.warn("Skipping unknown effect: {}", effectConfig.getEffectId());
                continue;
            }
            Holder.Reference<MobEffect> effectHolder = effectHolderOpt.get();
            effectInstances.add(new MobEffectInstance(
                    effectHolder,
                    effectConfig.duration,
                    effectConfig.amplifier,
                    effectConfig.ambient,
                    effectConfig.visible,
                    effectConfig.showIcon
            ));
            }
        }

        ItemStack outputStack = new ItemStack(Items.POTION);

        // 处理自定义颜色
        Optional<Integer> customColorOpt = Optional.empty();
        if (recipe.getCustomColor().isPresent()) {
            String colorStr = recipe.getCustomColor().get();
            try {
                long colorLong = Long.parseLong(colorStr, 16);
                int colorValue = (int) (colorLong & 0xFFFFFFFFL);
                customColorOpt = Optional.of(colorValue);
            } catch (NumberFormatException e) {
                MorePotionEffectsMod.LOGGER.warn("Invalid color format in recipe: {}, using default", colorStr);
            }
        }

        PotionContents potionContents = new PotionContents(
                Optional.empty(),
                customColorOpt,      // 自定义颜色
                effectInstances      // 自定义效果列表
        );
        outputStack.set(DataComponents.POTION_CONTENTS, potionContents);

        // 处理自定义基底
        recipe.getCustomBase().ifPresent(base -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("BasePotion", base);
            outputStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        });

        // 处理自定义名称
        recipe.getCustomName().ifPresent(name ->
                outputStack.set(DataComponents.CUSTOM_NAME, Component.literal(name))
        );

        // 存储成品药水
        stacksOut.add(outputStack.copy());

        // 注册配方
        Potion basePotion = recipe.getBasePotion();
        Item ingredientItem = recipe.getIngredientItem();
        if (ingredientItem == null) {
            MorePotionEffectsMod.LOGGER.error("Failed to register recipe: ingredient item is null");
            return;
        }

        // 获取用于配方匹配的Potion（原版药水配方需要有效的Potion，自定义基底配方使用null）
        Potion recipePotion = recipe.isCustomBaseRecipe() ? null : basePotion;
        if (recipe.isCustomBaseRecipe() && basePotion != null) {
            MorePotionEffectsMod.LOGGER.warn("Custom base recipe '{}' has a valid vanilla potion '{}', ignoring vanilla potion", recipe.base_potion, basePotion);
        }

        IngredientBrewingRecipe brewingRecipe = new IngredientBrewingRecipe(
                recipePotion,
                recipe.getBasePotionId(),
                Ingredient.of(ingredientItem),
                outputStack
        );

        // 存储配方实例
        recipesOut.add(brewingRecipe);
    }

    // 酿造配方数据类
    public static class BrewingRecipe {
        @SerializedName("base_potion")
        public String base_potion;
        @SerializedName("ingredient")
        public String ingredient;
        @SerializedName("effects")
        public List<EffectConfig> effects;
        @SerializedName("custom_name")
        public String custom_name; // 可选字段
        @SerializedName("custom_color")
        public String custom_color; // 可选字段
        @SerializedName("custom_base")
        public String custom_base; // 可选字段

        public Optional<String> getCustomBase() {
            return Optional.ofNullable(custom_base);
        }

        public Optional<String> getCustomName() {
            return Optional.ofNullable(custom_name);
        }

        public Optional<String> getCustomColor() {
            return Optional.ofNullable(custom_color);
        }

        // 检查是否是自定义基底配方
        public boolean isCustomBaseRecipe() {
            return base_potion != null && getBasePotion() == null;
        }

        // 获取 Potion
        public Potion getBasePotion() {
            return base_potion != null ?
                    BuiltInRegistries.POTION.get(ResourceLocation.parse(base_potion)) :
                    null;
        }

        public String getBasePotionId() {
            return base_potion;
        }

        // 获取 Item
        public Item getIngredientItem() {
            return ingredient != null ?
                    BuiltInRegistries.ITEM.get(ResourceLocation.parse(ingredient)) :
                    null;
        }

        public boolean isValid() {
            if (base_potion == null || ingredient == null || getIngredientItem() == null) {
                return false;
            }
            // 原版药水配方需要有效的base_potion
            if (!isCustomBaseRecipe() && getBasePotion() == null) {
                return false;
            }
            // 自定义基底配方需要有效的custom_base
            return !isCustomBaseRecipe() || (custom_base != null && !custom_base.isEmpty());
        }

        @Override
        public String toString() {
            return "BrewingRecipe{" +
                    "base_potion='" + base_potion + '\'' +
                    ", ingredient='" + ingredient + '\'' +
                    ", effects=" + effects +
                    '}';
        }
    }

    // 新增：火药转换配方（普通 -> 喷溅）
    public static class GunpowderConversionRecipe implements IBrewingRecipe {
        @Override
        public boolean isInput(ItemStack input) {
            // 使用新的 DataComponent 系统检查
            if (input.getItem() != Items.POTION) return false;
            PotionContents contents = input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            // 检查是否有自定义效果或自定义基底标记
            boolean hasCustomEffects = contents != PotionContents.EMPTY && !contents.customEffects().isEmpty();
            boolean hasCustomBase = input.get(DataComponents.CUSTOM_DATA) != null;
            return hasCustomEffects || hasCustomBase;
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.getItem() == Items.GUNPOWDER;
        }

        @Override
        public @NotNull ItemStack getOutput(@NotNull ItemStack input, @NotNull ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            return gunpowderConversion(input);
        }
    }

    public static ItemStack gunpowderConversion(ItemStack potion) {
        ItemStack result = new ItemStack(Items.SPLASH_POTION);
        // 复制 PotionContents
        PotionContents contentsToCopy = potion.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (contentsToCopy != PotionContents.EMPTY) {
            result.set(DataComponents.POTION_CONTENTS, contentsToCopy);
        }
        // 复制 CustomName
        result.set(DataComponents.CUSTOM_NAME, potion.get(DataComponents.CUSTOM_NAME)); // 明确类型
        // 复制自定义基底 NBT
        result.set(DataComponents.CUSTOM_DATA, potion.get(DataComponents.CUSTOM_DATA));
        return result;
    }

    // 新增：龙息转换配方（喷溅 -> 滞留）
    public static class DragonBreathConversionRecipe implements IBrewingRecipe {
        @Override
        public boolean isInput(ItemStack input) {
            if (input.getItem() != Items.SPLASH_POTION) return false;
            PotionContents contents = input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            // 检查是否有自定义效果或自定义基底标记
            boolean hasCustomEffects = contents != PotionContents.EMPTY && !contents.customEffects().isEmpty();
            boolean hasCustomBase = input.get(DataComponents.CUSTOM_DATA) != null;
            return hasCustomEffects || hasCustomBase;
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.getItem() == Items.DRAGON_BREATH;
        }

        @Override
        public @NotNull ItemStack getOutput(@NotNull ItemStack input, @NotNull ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            return dragonBreathConversion(input);
        }
    }

    public static ItemStack dragonBreathConversion(ItemStack potion) {
        ItemStack result = new ItemStack(Items.LINGERING_POTION);
        // 复制 PotionContents
        PotionContents contentsToCopy = potion.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (contentsToCopy != PotionContents.EMPTY) {
            result.set(DataComponents.POTION_CONTENTS, contentsToCopy);
        }
        // 复制 CustomName
        result.set(DataComponents.CUSTOM_NAME, potion.get(DataComponents.CUSTOM_NAME));
        // 复制自定义基底 NBT (CUSTOM_DATA)
        result.set(DataComponents.CUSTOM_DATA, potion.get(DataComponents.CUSTOM_DATA));
        return result;
    }

    public static ItemStack getPotionArrow(ItemStack potion) {
        ItemStack result = new ItemStack(Items.TIPPED_ARROW);
        // 复制 PotionContents
        PotionContents contentsToCopy = potion.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (contentsToCopy != PotionContents.EMPTY) {
            result.set(DataComponents.POTION_CONTENTS, contentsToCopy);
        }
        // 复制并修改 CustomName
        String newName = potion.getOrDefault(DataComponents.CUSTOM_NAME, Component.empty()).getString().replace("药水", "之箭");
        result.set(DataComponents.CUSTOM_NAME, Component.literal(newName).setStyle(potion.getOrDefault(DataComponents.CUSTOM_NAME, Component.empty()).getStyle()));

        // 复制自定义基底 NBT (CUSTOM_DATA)
        result.set(DataComponents.CUSTOM_DATA, potion.get(DataComponents.CUSTOM_DATA));
        return result;
    }

    // 效果配置
    public static class EffectConfig {
        private String effect_id;
        public int duration;
        public int amplifier;
        public boolean ambient = false;
        public boolean visible = true;
        public boolean showIcon = true;

        // 提供解析为 ResourceLocation 的方法
        public ResourceLocation getEffectId() {
            return ResourceLocation.parse(effect_id);
        }

        // 自定义反序列化器
        public static class Deserializer implements JsonDeserializer<EffectConfig> {
            @Override
            public EffectConfig deserialize(JsonElement json, Type type, JsonDeserializationContext context)
                    throws JsonParseException {
                JsonObject obj = json.getAsJsonObject();
                EffectConfig config = new EffectConfig();
                // 必需字段
                if (obj.has("effect_id") && !obj.get("effect_id").isJsonNull())
                    config.effect_id = obj.get("effect_id").getAsString();
                if (obj.has("duration") && !obj.get("duration").isJsonNull())
                    config.duration = obj.get("duration").getAsInt();
                if (obj.has("amplifier") && !obj.get("amplifier").isJsonNull())
                    config.amplifier = obj.get("amplifier").getAsInt();
                // 可选字段
                if (obj.has("ambient")) config.ambient = obj.get("ambient").getAsBoolean();
                if (obj.has("visible")) config.visible = obj.get("visible").getAsBoolean();
                if (obj.has("show_icon")) config.showIcon = obj.get("show_icon").getAsBoolean();
                return config;
            }
        }
    }

    // 酿造配方实现
    public record IngredientBrewingRecipe(Potion basePotion, String basePotionId, Ingredient ingredient, ItemStack outputTemplate) implements IBrewingRecipe {
        public IngredientBrewingRecipe(Potion basePotion, String basePotionId, Ingredient ingredient, ItemStack outputTemplate) {
            this.basePotion = basePotion;
            this.basePotionId = basePotionId;
            this.ingredient = ingredient;
            this.outputTemplate = outputTemplate.copy();
        }

        @Override
        public boolean isInput(ItemStack stack) {
            // 检查是否是可酿造的药水类型
            if (stack.getItem() != Items.POTION &&
                    stack.getItem() != Items.SPLASH_POTION &&
                    stack.getItem() != Items.LINGERING_POTION) {
                return false;
            }

            // 检查自定义基底
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag.contains("BasePotion")) {
                    String inputBase = tag.getString("BasePotion");
                    return inputBase.equals(basePotionId);
                }
            }

            // 检查原版药水类型
            PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (contents != PotionContents.EMPTY) {
                return contents.potion().map(Holder::value).orElse(null) == basePotion;
            }

            return false;
        }

        @Override
        public boolean isIngredient(@NotNull ItemStack stack) {
            return ingredient.test(stack);
        }

        @Override
        public @NotNull ItemStack getOutput(@NotNull ItemStack input, @NotNull ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            // 确定输出物品类型（与输入相同）
            Item outputItem = input.getItem();
            // 创建新药水并复制效果和NBT
            ItemStack result = new ItemStack(outputItem);
            // 复制 PotionContents
            PotionContents contentsToCopy = this.outputTemplate.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (contentsToCopy != PotionContents.EMPTY) {
                result.set(DataComponents.POTION_CONTENTS, contentsToCopy);
            }
            // 复制 CustomName
            result.set(DataComponents.CUSTOM_NAME, this.outputTemplate.get(DataComponents.CUSTOM_NAME)); // 明确类型
            // 复制 CUSTOM_DATA
            result.set(DataComponents.CUSTOM_DATA, this.outputTemplate.get(DataComponents.CUSTOM_DATA));

            return result;
        }
    }

    /**
     * 单一"活"包装配方：注册到 {@link net.minecraft.world.item.alchemy.PotionBrewing} 后，
     * 每次查询都实时读取 {@link #LIVE_RECIPES}。热重载只需替换该列表内容即可即时生效，
     * 无需重建不可变的 PotionBrewing 实例，也无需反射或访问转换。
     */
    public static class LiveBrewingRecipe implements IBrewingRecipe {
        @Override
        public boolean isInput(@NotNull ItemStack input) {
            for (IngredientBrewingRecipe recipe : LIVE_RECIPES) {
                if (recipe.isInput(input)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isIngredient(@NotNull ItemStack ingredient) {
            for (IngredientBrewingRecipe recipe : LIVE_RECIPES) {
                if (recipe.isIngredient(ingredient)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public @NotNull ItemStack getOutput(@NotNull ItemStack input, @NotNull ItemStack ingredient) {
            for (IngredientBrewingRecipe recipe : LIVE_RECIPES) {
                ItemStack output = recipe.getOutput(input, ingredient);
                if (!output.isEmpty()) {
                    return output;
                }
            }
            return ItemStack.EMPTY;
        }
    }

    /**
     * 失效客户端创造模式标签页缓存（{@code CreativeModeTabs.CACHED_PARAMETERS}）。
     * 创造标签页内容为纯客户端构建，重载药水后需让下次打开背包时重建。
     * 通过按类型反射查找静态字段，规避混淆名，仅应在客户端调用。
     */
    public static void invalidateCreativeTabCache() {
        try {
            for (Field field : CreativeModeTabs.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        && field.getType().getSimpleName().equals("ItemDisplayParameters")) {
                    field.setAccessible(true);
                    field.set(null, null);
                    return;
                }
            }
        } catch (Throwable t) {
            MorePotionEffectsMod.LOGGER.debug("Failed to invalidate creative tab cache", t);
        }
    }
}