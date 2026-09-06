package com.yixi_xun.more_potion_effects.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 自定义酿造配方（JSON）的编辑用数据模型与序列化。
 * <p>
 * 目标格式与 {@code com.yixi_xun.more_potion_effects.api.PotionBrewingSystem} 的加载器逐字段对齐：
 * <pre>
 * { "recipes": [ {
 *     "base_potion":  "minecraft:awkward",
 *     "ingredient":   "minecraft:nether_wart",
 *     "effects": [ { "effect_id":"...", "duration":12000, "amplifier":0,
 *                    "ambient":false, "visible":true, "show_icon":true } ],
 *     "custom_name":  "§e示例药水",
 *     "custom_color": "FFD700",
 *     "custom_base":  "custom_xxx"
 * } ] }
 * </pre>
 */
public final class RecipeDoc {

    /** 每条配方在编辑器中最多提供的效果槽位数。 */
    public static final int EFFECT_SLOT_COUNT = 6;

    public static final String FILE_SUFFIX = ".json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private RecipeDoc() {
    }

    /** 一条可变配方（供 GUI 直接编辑）。sourceFile 不属于 JSON，仅记录保存时写回哪个文件。 */
    public static final class RecipeEntry {
        public String basePotion = "minecraft:awkward";
        public String ingredient = "";
        public String customName = "";
        public String customColor = "";
        public String customBase = "";
        public final List<EffectEntry> effects = new ArrayList<>();
        /** 归属文件：加载时指向来源文件；新建/导入的配方为 null → 保存到默认文件。 */
        public Path sourceFile;

        public RecipeEntry() {
        }

        public RecipeEntry copy() {
            RecipeEntry e = new RecipeEntry();
            e.basePotion = this.basePotion;
            e.ingredient = this.ingredient;
            e.customName = this.customName;
            e.customColor = this.customColor;
            e.customBase = this.customBase;
            for (EffectEntry fx : this.effects) {
                e.effects.add(fx.copy());
            }
            e.sourceFile = this.sourceFile;
            return e;
        }
    }

    /** 单个效果位。durationText/amplifierText 存字符串，便于文本框直接编辑、保存前统一校验。 */
    public static final class EffectEntry {
        public String effectId = "";
        public String durationText = "6000";
        public String amplifierText = "0";
        public boolean ambient;
        public boolean visible = true;
        public boolean showIcon = true;

        public EffectEntry() {
        }

        public EffectEntry copy() {
            EffectEntry e = new EffectEntry();
            e.effectId = this.effectId;
            e.durationText = this.durationText;
            e.amplifierText = this.amplifierText;
            e.ambient = this.ambient;
            e.visible = this.visible;
            e.showIcon = this.showIcon;
            return e;
        }

        public Optional<Integer> duration() {
            return parseNonNegInt(this.durationText);
        }

        public Optional<Integer> amplifier() {
            try {
                return Optional.of(Integer.parseInt(this.amplifierText.trim()));
            } catch (NumberFormatException nfe) {
                return Optional.empty();
            }
        }
    }

    private static Optional<Integer> parseNonNegInt(String s) {
        if (s == null) {
            return Optional.empty();
        }
        try {
            int v = Integer.parseInt(s.trim());
            return v >= 0 ? Optional.of(v) : Optional.empty();
        } catch (NumberFormatException nfe) {
            return Optional.empty();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  读取                                                               */
    /* ------------------------------------------------------------------ */

    /** 从 JSON 文本解析出全部配方。缺少 "recipes" 数组时抛异常（与加载器行为一致：整文件被跳过）。 */
    public static List<RecipeEntry> parse(String content) {
        List<RecipeEntry> out = new ArrayList<>();
        JsonElement rootEl = JsonParser.parseString(content);
        if (!rootEl.isJsonObject()) {
            throw new IllegalArgumentException("顶层不是 JSON 对象，缺少 recipes 数组");
        }
        JsonObject root = rootEl.getAsJsonObject();
        JsonElement recipesEl = root.get("recipes");
        if (recipesEl == null || !recipesEl.isJsonArray()) {
            throw new IllegalArgumentException("缺少 recipes 数组");
        }
        for (JsonElement element : recipesEl.getAsJsonArray()) {
            if (element.isJsonObject()) {
                out.add(parseRecipe(element.getAsJsonObject()));
            }
        }
        return out;
    }

    private static RecipeEntry parseRecipe(JsonObject obj) {
        RecipeEntry e = new RecipeEntry();
        e.basePotion = strOr(obj, "base_potion", e.basePotion);
        e.ingredient = strOr(obj, "ingredient", e.ingredient);
        e.customName = strOr(obj, "custom_name", e.customName);
        e.customColor = strOr(obj, "custom_color", e.customColor);
        e.customBase = strOr(obj, "custom_base", e.customBase);
        if (obj.has("effects") && obj.get("effects").isJsonArray()) {
            for (JsonElement fxEl : obj.getAsJsonArray("effects")) {
                if (fxEl.isJsonObject()) {
                    e.effects.add(parseEffect(fxEl.getAsJsonObject()));
                }
            }
        }
        return e;
    }

    private static EffectEntry parseEffect(JsonObject obj) {
        EffectEntry fx = new EffectEntry();
        fx.effectId = strOr(obj, "effect_id", fx.effectId);
        fx.durationText = intOrStr(obj, "duration", fx.durationText);
        fx.amplifierText = intOrStr(obj, "amplifier", fx.amplifierText);
        fx.ambient = boolOr(obj, "ambient", fx.ambient);
        fx.visible = boolOr(obj, "visible", fx.visible);
        fx.showIcon = boolOr(obj, "show_icon", fx.showIcon);
        return fx;
    }

    private static String strOr(JsonObject o, String key, String def) {
        if (o.has(key) && !o.get(key).isJsonNull()) {
            return o.get(key).getAsString();
        }
        return def;
    }

    private static String intOrStr(JsonObject o, String key, String def) {
        if (o.has(key) && !o.get(key).isJsonNull()) {
            try {
                return String.valueOf(o.get(key).getAsInt());
            } catch (NumberFormatException | UnsupportedOperationException ex) {
                return o.get(key).getAsString();
            }
        }
        return def;
    }

    private static boolean boolOr(JsonObject o, String key, boolean def) {
        if (o.has(key) && !o.get(key).isJsonNull() && o.get(key).isJsonPrimitive() && o.get(key).getAsJsonPrimitive().isBoolean()) {
            return o.get(key).getAsBoolean();
        }
        return def;
    }

    /* ------------------------------------------------------------------ */
    /*  写出                                                               */
    /* ------------------------------------------------------------------ */

    /** 把配方列表序列化为标准 {recipes:[...]} 文档文本（pretty）。 */
    public static String toFileText(List<RecipeEntry> recipes) {
        JsonArray arr = new JsonArray();
        for (RecipeEntry e : recipes) {
            arr.add(toRecipeObject(e));
        }
        JsonObject root = new JsonObject();
        root.add("recipes", arr);
        return GSON.toJson(root);
    }

    private static JsonObject toRecipeObject(RecipeEntry e) {
        JsonObject o = new JsonObject();
        o.addProperty("base_potion", e.basePotion == null ? "" : e.basePotion.trim());
        o.addProperty("ingredient", e.ingredient == null ? "" : e.ingredient.trim());
        if (!e.effects.isEmpty()) {
            JsonArray fxArr = new JsonArray();
            for (EffectEntry fx : e.effects) {
                JsonObject fo = new JsonObject();
                fo.addProperty("effect_id", fx.effectId == null ? "" : fx.effectId.trim());
                fo.addProperty("duration", fx.duration().orElse(0));
                fo.addProperty("amplifier", fx.amplifier().orElse(0));
                if (fx.ambient) {
                    fo.addProperty("ambient", true);
                }
                if (!fx.visible) {
                    fo.addProperty("visible", false);
                }
                if (!fx.showIcon) {
                    fo.addProperty("show_icon", false);
                }
                fxArr.add(fo);
            }
            o.add("effects", fxArr);
        }
        if (e.customName != null && !e.customName.trim().isEmpty()) {
            o.addProperty("custom_name", e.customName.trim());
        }
        if (e.customColor != null && !e.customColor.trim().isEmpty()) {
            o.addProperty("custom_color", e.customColor.trim());
        }
        if (e.customBase != null && !e.customBase.trim().isEmpty()) {
            o.addProperty("custom_base", e.customBase.trim());
        }
        return o;
    }

    /** 配方“内容等价”的规范化字符串，用于导入去重。 */
    public static String canonical(RecipeEntry e) {
        return GSON.toJson(toRecipeObject(e));
    }

    /* ------------------------------------------------------------------ */
    /*  校验（与 PotionBrewingSystem.BrewingRecipe.isValid() 语义对齐）     */
    /* ------------------------------------------------------------------ */

    /** 校验一条配方，返回全部问题描述（空列表 = 通过）。 */
    public static List<String> validate(RecipeEntry e) {
        List<String> errs = new ArrayList<>();
        String base = e.basePotion == null ? "" : e.basePotion.trim();
        String ing = e.ingredient == null ? "" : e.ingredient.trim();

        if (base.isEmpty()) {
            errs.add("基础药水(base_potion)不能为空");
        } else {
            ResourceLocation id = ResourceLocation.tryParse(base);
            if (id == null) {
                errs.add("基础药水「" + base + "」不是合法的资源ID");
            } else if (!BuiltInRegistries.POTION.containsKey(id)) {
                String cb = e.customBase == null ? "" : e.customBase.trim();
                if (cb.isEmpty()) {
                    errs.add("「" + base + "」不是已注册药水；自定义基底配方必须填写 custom_base");
                }
            }
        }

        if (ing.isEmpty()) {
            errs.add("酿造材料(ingredient)不能为空");
        } else {
            ResourceLocation id = ResourceLocation.tryParse(ing);
            if (id == null) {
                errs.add("酿造材料「" + ing + "」不是合法的资源ID");
            } else if (!BuiltInRegistries.ITEM.containsKey(id)) {
                errs.add("酿造材料「" + ing + "」不是已注册物品");
            }
        }

        String color = e.customColor == null ? "" : e.customColor.trim();
        if (!color.isEmpty() && !color.matches("[0-9a-fA-F]{1,8}")) {
            errs.add("颜色应为1~8位十六进制(如 FFD700)，当前「" + color + "」");
        }

        int i = 0;
        for (EffectEntry fx : e.effects) {
            i++;
            String fid = fx.effectId == null ? "" : fx.effectId.trim();
            if (fid.isEmpty()) {
                errs.add("第" + i + "个效果未选择效果ID");
            } else {
                ResourceLocation rid = ResourceLocation.tryParse(fid);
                if (rid == null) {
                    errs.add("第" + i + "个效果ID「" + fid + "」不合法");
                } else if (!BuiltInRegistries.MOB_EFFECT.containsKey(rid)) {
                    errs.add("效果「" + fid + "」未注册");
                }
            }
            if (fx.duration().isEmpty()) {
                errs.add("第" + i + "个效果时长需为 >=0 的整数(tick)");
            }
            Optional<Integer> amp = fx.amplifier();
            if (amp.isEmpty()) {
                errs.add("第" + i + "个效果强度需为整数");
            } else if (amp.get() < -1) {
                errs.add("第" + i + "个效果强度不能小于-1");
            }
        }
        return errs;
    }

    public static boolean hasProblems(RecipeEntry e) {
        return !validate(e).isEmpty();
    }

    public static Optional<Integer> parseHexColor(String s) {
        if (s == null) {
            return Optional.empty();
        }
        String t = s.trim();
        if (t.isEmpty() || !t.matches("[0-9a-fA-F]{1,8}")) {
            return Optional.empty();
        }
        try {
            long v = Long.parseLong(t, 16);
            return Optional.of((int) (v & 0xFFFFFFFFL));
        } catch (NumberFormatException nfe) {
            return Optional.empty();
        }
    }
}
