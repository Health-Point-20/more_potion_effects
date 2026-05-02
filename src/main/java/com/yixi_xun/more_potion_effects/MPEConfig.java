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
    public static final ModConfigSpec.ConfigValue<String> SHATTERED_HEART_REDUCED_HEALTH;
    public static final ModConfigSpec.ConfigValue<String> INJURY_LINK_RADIUS;
    public static final ModConfigSpec.ConfigValue<Integer> SIDE_EFFECT_LIMIT;
    public static final ModConfigSpec.ConfigValue<String> NUMBER_OF_SIDE_EFFECTS;
    public static final ModConfigSpec.ConfigValue<String> UNYIELDING_CHANCE;


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
        SHATTERED_HEART_REDUCED_HEALTH = BUILDER.comment("碎心效果所减少的最大生命值（可用变量：maxHealth、effectLevel）。").define("Shattered Heart Reduced Health", "maxHealth * 0.1 * effectLevel");
        INJURY_LINK_RADIUS = BUILDER.comment("生命链接半径（可用变量：effectLevel）。").define("Injury Link Radius", "3 + effectLevel");
        SIDE_EFFECT_LIMIT = BUILDER.comment("当玩家有多少个正面效果时会获得副作用效果。（设置0以禁用）").define("Side Effect Limit", 0);
        NUMBER_OF_SIDE_EFFECTS = BUILDER.comment("有副作用效果时每次获得新正面效果产生几个负面效果。(可用变量：SideEffectLevel、NewEffectLevel)").define("Number of Side Effects", "SideEffectLevel");
        UNYIELDING_CHANCE = BUILDER.comment("不屈触发的概率。（可用的变量：effectLevel）").define("Unyielding Chance", "1 - 0.8^effectLevel");

        BUILDER.pop();

        BUILDER.push("Special");
        BAN_LIST = BUILDER.comment("禁用的药水效果").defineList("Ban List", List.of(), () -> "", entry -> true);
        FORCE_EFFECTS = BUILDER.comment("能被强行添加到生物的效果。").defineList("Force effects", List.of(), () -> "", entry -> true);
        ENTITY_LIST = BUILDER.comment("强行使列表中的生物能被添加药水效果。").defineList("Entity List", List.of(), () -> "", entry -> true);
        BUILDER.pop();

        BUILDER.push("Immune");
        IMMUNE_EFFECTS = BUILDER.comment("免疫效果所能免疫的效果").defineList("Immune Effects", List.of("more_potion_effects:virus, minecraft:unluck, minecraft:hunger, minecraft:slowness, minecraft:blindness, minecraft:poison, minecraft:nausea",
                "minecraft:wither, minecraft:mining_fatigue, minecraft:weakness, minecraft:darkness, more_potion_effects:highly_toxic, more_potion_effects:injury_accumulation, more_potion_effects:fear, more_potion_effects:combustion, more_potion_effects:spatial_anchor, more_potion_effects:slot_lock",
                "more_potion_effects:corrosion, more_potion_effects:wane, more_potion_effects:armor_broken, more_potion_effects:broken_magic_shield, more_potion_effects:magic_inhibition, more_potion_effects:weakening_recovery, more_potion_effects:bleeding, more_potion_effects:aggro, more_potion_effects:deflagration",
                "more_potion_effects:dispel, more_potion_effects:injury_outburst, more_potion_effects:potion_antagonism, more_potion_effects:death"), () -> "", entry -> true);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
