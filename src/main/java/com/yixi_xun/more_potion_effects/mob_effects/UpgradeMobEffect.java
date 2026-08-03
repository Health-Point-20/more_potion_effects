package com.yixi_xun.more_potion_effects.mob_effects;

import com.yixi_xun.more_potion_effects.api.IEffectAccessor;
import com.yixi_xun.more_potion_effects.api.IMoreMobEffect;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static com.yixi_xun.more_potion_effects.MPEConfig.UPGRADE_EXCLUSION;
import static com.yixi_xun.more_potion_effects.MorePotionEffectsMod.queueServerWork;
import static com.yixi_xun.more_potion_effects.init.MorePotionEffectsModMobEffects.UPGRADE;

public class UpgradeMobEffect extends MobEffect implements IMoreMobEffect {

    public UpgradeMobEffect() {
        super(MobEffectCategory.BENEFICIAL, -3368449);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {return true;}

    @Override
    public void onEffectAdded(LivingEntity entity, MobEffectInstance instance) {
        int appliedLevel = instance.getAmplifier() + 1;
        Set<String> exclusionSet = new HashSet<>(UPGRADE_EXCLUSION.get());

        entity.getActiveEffects().stream()
                .filter(e -> !e.getEffect().is(UPGRADE))
                .filter(e -> e.getAmplifier() < appliedLevel)
                .filter(e -> {
                    String effectKey = e.getEffect().getRegisteredName();
                    return !exclusionSet.contains(effectKey);
                })
                .forEach(effect -> queueServerWork(0, () -> {
                    int newAmplifier = entity.getRandom().nextInt(effect.getAmplifier() + 1, appliedLevel + 1);
                    var newInstance = new MobEffectInstance(
                            effect.getEffect(),
                            effect.getDuration(),
                            newAmplifier,
                            effect.isAmbient(),
                            effect.isVisible(),
                            effect.showIcon());
                    effect.update(newInstance);
                    ((IEffectAccessor)entity).callOnEffectUpdated(newInstance, true, entity);
                    effect.onEffectStarted(entity);
                    if (entity instanceof ServerPlayer player) player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), newInstance, false));
                }));
        queueServerWork(0, () -> entity.removeEffect(UPGRADE));
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}