
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package com.yixi_xun.more_potion_effects.init;

import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import com.yixi_xun.more_potion_effects.mob_effects.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MorePotionEffectsModMobEffects {
	public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, MorePotionEffectsMod.MOD_ID);
	public static final DeferredHolder<MobEffect, MobEffect> IMMUNE = REGISTRY.register("immune", ImmuneMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> STRONG_HEART = REGISTRY.register("strong_heart", StrongHeartMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> EFFECT_SIPHON = REGISTRY.register("effect_siphon", EffectSiphonMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> LOCK = REGISTRY.register("lock", LockMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> RANDOM_EFFECT = REGISTRY.register("random_effect", RandomEffectMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> RANDOM_NEGATIVE_EFFECT = REGISTRY.register("random_negative_effect", RandomNegativeEffectMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> RANDOM_POSITIVE_EFFECT = REGISTRY.register("random_positive_effect", RandomPositiveEffectMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> REUSE = REGISTRY.register("reuse", ReuseMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> IMPRISON = REGISTRY.register("imprison", ImprisonMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> IMMORTAL = REGISTRY.register("immortal", ImmortalMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> CLIMBING = REGISTRY.register("climbing", ClimbingMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> ATTACK_AOE = REGISTRY.register("attack_aoe", AttackAoeMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> ADAPTATION = REGISTRY.register("adaptation", AdaptationMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> TRUE_DAMAGE = REGISTRY.register("true_damage", TrueDamageMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> STATIC_LIFE = REGISTRY.register("static_life", StaticLifeMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> AGGRO = REGISTRY.register("aggro", AggroMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> DECAY = REGISTRY.register("decay", DecayMobEffect::new);
	}
