package com.yixi_xun.more_potion_effects;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class MPEConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // Effect configs
    public static final ModConfigSpec.ConfigValue<Double> BASE_STEAL_CHANCE;
    public static final ModConfigSpec.ConfigValue<Double> DURATION_RATIO;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> RANDOM_EFFECT_EXCLUSION;
    public static final ModConfigSpec.ConfigValue<Integer> EXTENSION_METHOD;
    public static final ModConfigSpec.ConfigValue<String> SHATTERED_HEART_REDUCED_HEALTH;
    public static final ModConfigSpec.ConfigValue<String> INJURY_LINK_RADIUS;
    public static final ModConfigSpec.ConfigValue<Integer> SIDE_EFFECT_LIMIT;
    public static final ModConfigSpec.ConfigValue<String> NUMBER_OF_SIDE_EFFECTS;
    public static final ModConfigSpec.ConfigValue<String> UNYIELDING_CHANCE;
    public static final ModConfigSpec.ConfigValue<String> GLUTTONY_SPEED_MULTIPLIER;
    public static final ModConfigSpec.ConfigValue<String> FEAST_FOOD_ENHANCED;

    // Damage modifier configs (from 1.20.1)
    public static final ModConfigSpec.ConfigValue<String> MELEE_DOMAIN_DISTANCE;
    public static final ModConfigSpec.ConfigValue<Double> EVASION_PROBABILITY;
    public static final ModConfigSpec.ConfigValue<Double> ARMOR_BROKEN_VALUE;
    public static final ModConfigSpec.ConfigValue<Double> ARMOR_TOUGHNESS_BROKEN;
    public static final ModConfigSpec.ConfigValue<Double> HUGE_FORCE_DAMAGE;
    public static final ModConfigSpec.ConfigValue<Double> WANE_REDUCE_DAMAGE;
    public static final ModConfigSpec.ConfigValue<Double> FRAGILE_DAMAGE;
    public static final ModConfigSpec.ConfigValue<Double> LEECHING_HEALTH;
    public static final ModConfigSpec.ConfigValue<Double> ACCURATE_DAMAGE;
    public static final ModConfigSpec.ConfigValue<Double> MISALIGNMENT_REDUCE_DAMAGE;
    public static final ModConfigSpec.ConfigValue<Double> WEAKENING_RECOVERY_AMOUNT;
    public static final ModConfigSpec.ConfigValue<Double> SLAUGHTER_DAMAGE;
    public static final ModConfigSpec.ConfigValue<Double> INJURY_ACCUMULATION_DAMAGE;
    public static final ModConfigSpec.ConfigValue<Double> MAGIC_FOCUS_DAMAGE;
    public static final ModConfigSpec.ConfigValue<Double> MAGIC_SHIELD_REDUCE_DAMAGE;
    public static final ModConfigSpec.ConfigValue<Double> MAGIC_INHIBITION_REDUCE_DAMAGE;
    public static final ModConfigSpec.ConfigValue<Double> BROKEN_MAGIC_SHIELD_DAMAGE;
    public static final ModConfigSpec.ConfigValue<Double> STRONG_HEART_RECOVERY;
    public static final ModConfigSpec.ConfigValue<Double> CURSE_COUNT;
    public static final ModConfigSpec.ConfigValue<Double> HEALTH_CONVERSION_RATIO;
    public static final ModConfigSpec.ConfigValue<String> KINETIC_CALCULATION_FORMULA;

    // Virus effect config
    public static final ModConfigSpec.ConfigValue<Double> VIRUS_BASE_RADIUS;
    public static final ModConfigSpec.ConfigValue<Double> VIRUS_INFECTION_SPEED;
    public static final ModConfigSpec.ConfigValue<Double> VIRUS_THRESHOLD_FACTOR;
    public static final ModConfigSpec.ConfigValue<Double> VIRUS_DAMAGE_INTERVAL;
    public static final ModConfigSpec.ConfigValue<Double> VIRUS_DAMAGE_FACTOR;
    public static final ModConfigSpec.ConfigValue<Double> VIRUS_INFECTED_TIME_CAP_FACTOR;

    // Death effect config
    public static final ModConfigSpec.ConfigValue<Boolean> SUPER_DEATH_MODE;

    // Rank effect config
    public static final ModConfigSpec.ConfigValue<Boolean> RANK_EFFECTS_ENABLED;

    // Corrosion effect config
    public static final ModConfigSpec.ConfigValue<Boolean> SUPER_CORROSION;

    // Enchantment configs
    public static final ModConfigSpec.ConfigValue<Boolean> ENCHANT_MAIN_TARGET_ONLY;
    public static final ModConfigSpec.ConfigValue<Boolean> SOURCE_OF_BLESSING;
    public static final ModConfigSpec.ConfigValue<Boolean> SOURCE_OF_CURSES;
    public static final ModConfigSpec.ConfigValue<String> SOURCE_OF_BLESSING_PROBABILITY;
    public static final ModConfigSpec.ConfigValue<String> SOURCE_OF_CURSES_PROBABILITY;
    public static final ModConfigSpec.ConfigValue<String> ELIMINATION_EFFECT_PROBABILITY;
    public static final ModConfigSpec.ConfigValue<String> SUNDER_ARMOR_PROBABILITY;
    public static final ModConfigSpec.ConfigValue<String> ADMINISTER_POISON_PROBABILITY;
    public static final ModConfigSpec.ConfigValue<String> INFLICTION_CORROSION_PROBABILITY;
    public static final ModConfigSpec.ConfigValue<String> INHIBIT_THERAPY_PROBABILITY;

    // Special configs
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BAN_LIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FORCE_EFFECTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_LIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> IMMUNE_EFFECTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> UPGRADE_EXCLUSION;
    public static final ModConfigSpec.ConfigValue<Boolean> NEGATIVE_POTION_ANTAGONISM;
    public static final ModConfigSpec.ConfigValue<String> POTION_ANTAGONISM_REDUCE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> NON_REMOVABLE_EFFECTS;


    static {
        BUILDER.push("Enchantment");
        ENCHANT_MAIN_TARGET_ONLY = BUILDER.define("Enchant Main Target Only", true);
        SOURCE_OF_BLESSING = BUILDER.comment("是否启用赐福之源附魔。").define("Source of Blessing", true);
        SOURCE_OF_CURSES = BUILDER.comment("是否启用诅咒之源附魔。").define("Source of Curses", true);
        SOURCE_OF_BLESSING_PROBABILITY = BUILDER.comment("祝福之源触发概率（可用变量：EnchantLevel）").define("Source of Blessing Probability", "EnchantLevel * 0.2");
        SOURCE_OF_CURSES_PROBABILITY = BUILDER.comment("诅咒之源触发概率（可用变量：EnchantLevel）").define("Source of Curses Probability", "EnchantLevel * 0.2");
        ELIMINATION_EFFECT_PROBABILITY = BUILDER.comment("破法附魔消除效果的概率。").define("Elimination Effect Probability", "EnchantLevel * 0.3");
        SUNDER_ARMOR_PROBABILITY = BUILDER.comment("破甲附魔触发概率。").define("Sunder Armor Probability", "EnchantLevel * 0.1");
        ADMINISTER_POISON_PROBABILITY = BUILDER.comment("剧毒之刃施加剧毒效果的几率。").define("Administer Poison Probability", "EnchantLevel * 0.15");
        INFLICTION_CORROSION_PROBABILITY = BUILDER.comment("腐败施加施加腐蚀效果的几率。").define("Infliction Corrosion Probability", "EnchantLevel * 0.1");
        INHIBIT_THERAPY_PROBABILITY = BUILDER.comment("抑疗施加弱效恢复效果的几率").define("Inhibit Therapy Probability", "EnchantLevel * 0.08");
        BUILDER.pop();

        BUILDER.push("Effects");
        BASE_STEAL_CHANCE = BUILDER.define("Base Steal Chance", 0.2);
        DURATION_RATIO = BUILDER.define("Duration Ratio", 0.5);
        RANDOM_EFFECT_EXCLUSION = BUILDER.comment("不会被随机效果抽取到的效果。").defineList("Random Effect Exclusion", List.of(), () -> "", entry -> true);
        EXTENSION_METHOD = BUILDER.comment("0：得到效果时增加时长；1：轮流消耗持续时间").define("Extension_method", 0);
        SHATTERED_HEART_REDUCED_HEALTH = BUILDER.comment("碎心效果所减少的最大生命值（可用变量：maxHealth、effectLevel）。").define("Shattered Heart Reduced Health", "maxHealth * 0.1 * effectLevel");
        INJURY_LINK_RADIUS = BUILDER.comment("生命链接半径（可用变量：effectLevel）。").define("Injury Link Radius", "3 + effectLevel");
        SIDE_EFFECT_LIMIT = BUILDER.comment("当玩家有多少个正面效果时会获得副作用效果。（设置0以禁用）").define("Side Effect Limit", 0);
        NUMBER_OF_SIDE_EFFECTS = BUILDER.comment("有副作用效果时每次获得新正面效果产生几个负面效果。(可用变量：SideEffectLevel、NewEffectLevel)").define("Number of Side Effects", "SideEffectLevel");
        UNYIELDING_CHANCE = BUILDER.comment("不屈触发的概率。（可用的变量：effectLevel）").define("Unyielding Chance", "1 - 0.8^effectLevel");
        GLUTTONY_SPEED_MULTIPLIER = BUILDER.comment("暴食效果的进食加速倍率。").define("Gluttony Speed Multiplier", "1.0 + 0.5 * effectLevel");
        FEAST_FOOD_ENHANCED = BUILDER.comment("盛宴效果增强后食物提供的饱食度/饱和度增幅。（可用变量effectLevel）").define("Feast Food Enhanced", "1 + 0.5 * effectLevel");

        // Damage modifier configs
        MELEE_DOMAIN_DISTANCE = BUILDER.comment("近战领域无敌范围（可用变量：damage，effectLevel）").define("Melee Domain Distance", "0.5 * effectLevel");
        EVASION_PROBABILITY = BUILDER.comment("每级闪避提供多少闪避概率。(1%~100%)").define("Evasion Probability", 4.0);
        ARMOR_BROKEN_VALUE = BUILDER.comment("每级护甲碎裂减少多少护甲值。").define("Armor Broken", 4.0);
        ARMOR_TOUGHNESS_BROKEN = BUILDER.comment("每级护甲碎裂减少多少护甲韧性。").define("Armor Toughness Broken", 1.0);
        HUGE_FORCE_DAMAGE = BUILDER.comment("每级巨力增加百分之多少的伤害。").define("Huge Force Damage", 0.25);
        WANE_REDUCE_DAMAGE = BUILDER.comment("伤害变为：(1-x)^溃力等级-溃力等级。").define("Wane Reduce damage", 0.2);
        FRAGILE_DAMAGE = BUILDER.comment("每级脆弱增加多少被造成的伤害。").define("Fragile Damage", 0.25);
        LEECHING_HEALTH = BUILDER.comment("每级吸血增加百分之多少的吸血量。").define("Leeching Health", 0.1);
        ACCURATE_DAMAGE = BUILDER.comment("每级精准增加的伤害。").define("Accurate Damage", 0.25);
        MISALIGNMENT_REDUCE_DAMAGE = BUILDER.comment("每级失准减少的伤害。").define("Misalignment Reduce Damage", 0.2);
        WEAKENING_RECOVERY_AMOUNT = BUILDER.comment("每级弱效回复减少的回复量。").define("Weakening Recovery Amount", 0.2);
        SLAUGHTER_DAMAGE = BUILDER.comment("使伤害变为：伤害*(实体的损失生命值百分比^x)*效果等级").define("Slaughter Damage", 0.35);
        INJURY_ACCUMULATION_DAMAGE = BUILDER.comment("受到伤害时额外受到(损失的生命值*x)*效果等级的伤害").define("Injury Accumulation", 0.08);
        MAGIC_FOCUS_DAMAGE = BUILDER.comment("魔力聚焦增加造成的魔法伤害百分比。").define("Magic Focus Damage", 0.25);
        MAGIC_SHIELD_REDUCE_DAMAGE = BUILDER.comment("魔法护盾减少的受到的魔法伤害的百分比。").define("Magic Shield Reduce Damage", 0.2);
        MAGIC_INHIBITION_REDUCE_DAMAGE = BUILDER.comment("魔力抑制减少造成的魔法伤害的百分比。").define("Magic Inhibition Reduce Damage", 0.3);
        BROKEN_MAGIC_SHIELD_DAMAGE = BUILDER.comment("魔法破防增加的受到伤害的百分比").define("Broken Magic Shield Damage", 0.3);
        STRONG_HEART_RECOVERY = BUILDER.comment("每级强心提升恢复的生命值。").define("Strong Heart Recovery", 0.25);
        CURSE_COUNT = BUILDER.comment("每级诅咒效果施加几种对应的负面效果。(Range：1-4)").define("Curse Count", 2.0);
        HEALTH_CONVERSION_RATIO = BUILDER.comment("每级生命转化的转化率").define("Health Conversion Ratio", 0.2);
        KINETIC_CALCULATION_FORMULA = BUILDER.comment("动能效果的增伤公式（可用变量:effectLevel、damage、speed）").define("Kinetic Calculation Formula", "damage * speed * effectLevel * 0.25f + effectLevel * speed * 2f");

        BUILDER.push("Virus");
        VIRUS_BASE_RADIUS = BUILDER.comment("病毒效果基础传染半径。").define("Virus Base Radius", 2.0);
        VIRUS_INFECTION_SPEED = BUILDER.comment("病毒感染进度累积基础速度（每tick）。").define("Virus Infection Speed", 1.0);
        VIRUS_THRESHOLD_FACTOR = BUILDER.comment("感染阈值系数（阈值 = 目标生命值 * 此系数）。").define("Virus Threshold Factor", 10.0);
        VIRUS_DAMAGE_INTERVAL = BUILDER.comment("伤害累积时间（ticks）。").define("Virus Damage Interval", 40.0);
        VIRUS_DAMAGE_FACTOR = BUILDER.comment("伤害系数（伤害 = 累积感染伤害 + 最大生命值 * 此系数 * (等级+1)）。").define("Virus Damage Factor", 0.01);
        VIRUS_INFECTED_TIME_CAP_FACTOR = BUILDER.comment("感染时间上限系数（上限 = 最大生命值 * 此系数）。").define("Virus Infected Time Cap Factor", 3.0);
        BUILDER.pop();

        BUILDER.pop();

        BUILDER.push("Death");
        SUPER_DEATH_MODE = BUILDER.comment("是否启用超级死亡模式（高等级死亡效果会移除实体）。").define("Super Death Mode", false);
        BUILDER.pop();

        BUILDER.push("Rank");
        RANK_EFFECTS_ENABLED = BUILDER.comment("是否为非玩家实体添加随机效果池效果。").define("Rank Effects Enabled", true);
        BUILDER.pop();

        BUILDER.push("Corrosion");
        SUPER_CORROSION = BUILDER.comment("是否启用超级腐蚀模式（可以完全摧毁装备）。").define("Super Corrosion", false);
        BUILDER.pop();

        BUILDER.push("Special");
        BAN_LIST = BUILDER.comment("禁用的药水效果").defineList("Ban List", List.of(), () -> "", entry -> true);
        FORCE_EFFECTS = BUILDER.comment("能被强行添加到生物的效果。").defineList("Force effects", List.of(), () -> "", entry -> true);
        ENTITY_LIST = BUILDER.comment("强行使列表中的生物能被添加药水效果。").defineList("Entity List", List.of(), () -> "", entry -> true);
        UPGRADE_EXCLUSION = BUILDER.comment("升级效果不会升级的效果列表。").defineList("Upgrade Exclusion", List.of(), () -> "", entry -> true);
        NEGATIVE_POTION_ANTAGONISM = BUILDER.comment("药水拮抗是否对负面效果生效。").define("Negative Potion Antagonism", false);
        POTION_ANTAGONISM_REDUCE = BUILDER.comment("药水拮抗的持续时间减少公式（可用变量：duration、effectLevel）。").define("Potion Antagonism Reduce", "duration * 0.5 ^ effectLevel");
        NON_REMOVABLE_EFFECTS = BUILDER.comment("不会被移除的药水效果，除了持续时间结束或/force_effect").defineList("NON-Removable Effects", List.of(), entry -> true);
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