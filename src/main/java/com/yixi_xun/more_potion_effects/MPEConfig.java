package com.yixi_xun.more_potion_effects;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class MPEConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<Double> BASE_STEAL_CHANCE;
    public static final ModConfigSpec.ConfigValue<Double> DURATION_RATIO;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> RANDOM_EFFECT_EXCLUSION;
    public static final ModConfigSpec.ConfigValue<Integer> EXTENSION_METHOD;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BAN_LIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FORCE_EFFECTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_LIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> IMMUNE_EFFECTS;

    static {
        BUILDER.push("Effects");
        BASE_STEAL_CHANCE = BUILDER.define("Base Steal Chance", 0.2);
        DURATION_RATIO = BUILDER.define("Duration Ratio", 0.5);
        RANDOM_EFFECT_EXCLUSION = BUILDER.comment("不会被随机效果抽取到的效果。").defineList("Random Effect Exclusion", List.of(), () -> "", entry -> true);
        EXTENSION_METHOD = BUILDER.comment("0：在得到药水效果时增加时长（增加30+等级*30秒时长）；1：若该buff等级≥其它状态效果数量，则令其它状态效果的持续时间不会降低，直至该buff消失。  若该buff等级＜其它状态效果数量，则令其它状态效果轮流消耗持续时间").define("Extension_method",  0);
        BUILDER.pop();

        BUILDER.push("Special");
        BAN_LIST = BUILDER.comment("禁用的药水效果").defineList("Ban List", List.of(), () -> "", entry -> true);
        FORCE_EFFECTS = BUILDER.comment("能被强行添加到生物的效果。").defineList("Force effects", List.of(), () -> "", entry -> true);
        ENTITY_LIST = BUILDER.comment("强行使列表中的生物能被添加药水效果。").defineList("Entity List", List.of(), () -> "", entry -> true);
        BUILDER.pop();

        BUILDER.push("Immune");
        IMMUNE_EFFECTS = BUILDER.comment("免疫所免疫的效果").defineList("Immune Effects", List.of(""), () -> "", entry -> true);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
