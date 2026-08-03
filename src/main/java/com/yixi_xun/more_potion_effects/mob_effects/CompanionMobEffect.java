package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.api.EffectUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.yixi_xun.more_potion_effects.MPEConfig.COMPANION_EFFECTS_LIST;

public class CompanionMobEffect extends MobEffect {

    public static final Map<Holder<Attribute>, AttributeConfig> ATTRIBUTE_CONFIGS = new HashMap<>();

    public CompanionMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -13159);
    }

    @Override
    public void removeAttributeModifiers(@NotNull AttributeMap attributeMap) {
        super.removeAttributeModifiers(attributeMap);
        removeCompanionAttributes(attributeMap);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @FunctionalInterface
    public interface ValueCalculator {
        double calculate(LivingEntity entity, int deathCount, int amplifier);
    }

    public record AttributeConfig(ResourceLocation id, String name, ValueCalculator valueCalculator) { }

    static {
        ATTRIBUTE_CONFIGS.put(Attributes.MAX_HEALTH, new AttributeConfig(ResourceLocation.fromNamespaceAndPath("more_potion_effects", "companion.health"), "Companion health bonus",
                (entity, deathCount, amplifier) -> entity.getMaxHealth() * 0.1 * (deathCount + amplifier + 1)));
        ATTRIBUTE_CONFIGS.put(Attributes.ARMOR, new AttributeConfig(ResourceLocation.fromNamespaceAndPath("more_potion_effects", "companion.armor"), "Companion armor bonus",
                (entity, deathCount, amplifier) -> deathCount + amplifier + 2));
        ATTRIBUTE_CONFIGS.put(Attributes.ARMOR_TOUGHNESS, new AttributeConfig(ResourceLocation.fromNamespaceAndPath("more_potion_effects", "companion.armor_toughness"), "Companion armor toughness bonus",
                (entity, deathCount, amplifier) -> 0.5 * (deathCount + amplifier + 2)));
        ATTRIBUTE_CONFIGS.put(Attributes.ATTACK_DAMAGE, new AttributeConfig(ResourceLocation.fromNamespaceAndPath("more_potion_effects", "companion.attack"), "Companion attack bonus",
                (entity, deathCount, amplifier) -> {
                    AttributeInstance attackDamage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
                    return attackDamage != null ? attackDamage.getBaseValue() * 0.1 * (deathCount + amplifier + 1) : 0;
                }));
        ATTRIBUTE_CONFIGS.put(Attributes.MOVEMENT_SPEED, new AttributeConfig(ResourceLocation.fromNamespaceAndPath("more_potion_effects", "companion.speed"), "Companion speed bonus",
                (entity, deathCount, amplifier) -> {
                    AttributeInstance moveSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
                    return moveSpeed != null ? moveSpeed.getBaseValue() * 0.05 * (deathCount + amplifier + 1) : 0;
                }));
    }

    public static void removeCompanionAttributes(LivingEntity entity) {
        ATTRIBUTE_CONFIGS.forEach((attribute, config) -> {
            AttributeInstance attrInstance = entity.getAttribute(attribute);
            if (attrInstance != null) {
                attrInstance.removeModifier(config.id());
            }
        });
    }

    public static void removeCompanionAttributes(AttributeMap attributeMap) {
        ATTRIBUTE_CONFIGS.forEach((attribute, config) -> {
            AttributeInstance attrInstance = attributeMap.getInstance(attribute);
            if (attrInstance != null) {
                attrInstance.removeModifier(config.id());
            }
        });
    }

    public static void addCompanionEffect(Player player, int amplifier, int deathCount) {
        List<Holder.Reference<MobEffect>> effects = COMPANION_EFFECTS_LIST.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .map(BuiltInRegistries.MOB_EFFECT::getHolder)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        Holder<MobEffect> effect;
        if (effects.isEmpty()) {
            effect = EffectUtils.getRandomGoodEffect();
        } else {
            effect = getRandomEffect(effects);
        }

        if (effect != null) {
            player.addEffect(new MobEffectInstance(effect, (amplifier + deathCount) * 1200, amplifier));
        }
    }

    private static <T extends Holder<MobEffect>> Holder<MobEffect> getRandomEffect(List<T> effects) {
        if (effects.isEmpty()) return null;
        return effects.get((int) (Math.random() * effects.size()));
    }
}