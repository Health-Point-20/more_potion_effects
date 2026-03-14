package com.yixi_xun.more_potion_effects;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class MPEConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<Double> BASE_STEAL_CHANCE;
    public static final ModConfigSpec.ConfigValue<Double> DURATION_RATIO;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> RANDOM_EFFECT_EXCLUSION;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BAN_LIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FORCE_EFFECTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_LIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> LEVEL_1_LIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> LEVEL_2_LIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> LEVEL_3_LIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> LEVEL_4_LIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> LEVEL_JUDGMENT_EFFECTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> IMMUNE_EFFECTS;

    static {
        BUILDER.push("Effects");
        BASE_STEAL_CHANCE = BUILDER.define("Base Steal Chance", 0.2);
        DURATION_RATIO = BUILDER.define("Duration Ratio", 0.5);
        RANDOM_EFFECT_EXCLUSION = BUILDER.comment("不会被随机效果抽取到的效果。").defineList("Random Effect Exclusion", List.of(), () -> "", entry -> true);
        BUILDER.pop();

        BUILDER.push("Special");
        BAN_LIST = BUILDER.comment("禁用的药水效果").defineList("Ban List", List.of(), () -> "", entry -> true);
        FORCE_EFFECTS = BUILDER.comment("能被强行添加到生物的效果。").defineList("Force effects", List.of(), () -> "", entry -> true);
        ENTITY_LIST = BUILDER.comment("强行使列表中的生物能被添加药水效果。").defineList("Entity List", List.of(), () -> "", entry -> true);
        BUILDER.pop();

        BUILDER.push("Immune");
        LEVEL_1_LIST = BUILDER.comment("免疫I免疫的效果种类").defineList("Level 1 List", List.of("more_potion_effects:virus", "minecraft:unluck", "minecraft:hunger", "minecraft:slowness", "minecraft:blindness", "minecraft:poison", "minecraft:nausea"),
                () -> "", entry -> true);
        LEVEL_2_LIST = BUILDER.comment("免疫II免疫的效果种类").defineList("Level 2 List", List.of("minecraft:wither", "minecraft:mining_fatigue", "minecraft:weakness", "minecraft:darkness", "more_potion_effects:highly_toxic",
                "more_potion_effects:injury_accumulation", "more_potion_effects:fear", "more_potion_effects:combustion", "more_potion_effects:spatial_anchor", "more_potion_effects:slot_lock"), () -> "", entry -> true);
        LEVEL_3_LIST = BUILDER.comment("免疫III免疫的效果种类").defineList("Level 3 List", List.of("more_potion_effects:corrosion", "more_potion_effects:wane", "more_potion_effects:armor_broken", "more_potion_effects:broken_magic_shield",
                "more_potion_effects:magic_inhibition", "more_potion_effects:weakening_recovery", "more_potion_effects:bleeding", "more_potion_effects:aggro", "more_potion_effects:deflagration"), () -> "", entry -> true);
        LEVEL_4_LIST = BUILDER.comment("免疫IV免疫的效果种类").defineList("Level 4 List", List.of("more_potion_effects:dispel", "more_potion_effects:injury_outburst", "more_potion_effects:potion_antagonism", "more_potion_effects:death"), () -> "", entry -> true);
        LEVEL_JUDGMENT_EFFECTS = BUILDER.comment("需比免疫的等级低才会被免疫的效果").defineList("Level Judgment Effects", List.of(
                "more_potion_effects:virus, minecraft:unluck, minecraft:hunger, minecraft:slowness, minecraft:blindness, minecraft:poison, minecraft:nausea",
                "minecraft:wither, minecraft:mining_fatigue, minecraft:weakness, minecraft:darkness, more_potion_effects:highly_toxic, more_potion_effects:injury_accumulation, more_potion_effects:fear, more_potion_effects:combustion, more_potion_effects:spatial_anchor, more_potion_effects:slot_lock",
                "more_potion_effects:corrosion, more_potion_effects:wane, more_potion_effects:armor_broken, more_potion_effects:broken_magic_shield, more_potion_effects:magic_inhibition, more_potion_effects:weakening_recovery, more_potion_effects:bleeding, more_potion_effects:aggro, more_potion_effects:deflagration",
                "more_potion_effects:dispel, more_potion_effects:injury_outburst, more_potion_effects:potion_antagonism, more_potion_effects:death"
        ), () -> "", entry -> true);
        IMMUNE_EFFECTS = BUILDER.comment("免疫所免疫的效果").defineList("Immune Effects", List.of(""), () -> "", entry -> true);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
