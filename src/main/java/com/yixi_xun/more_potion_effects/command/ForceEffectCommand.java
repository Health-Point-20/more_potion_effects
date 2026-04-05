package com.yixi_xun.more_potion_effects.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import com.yixi_xun.more_potion_effects.api.EffectUtils;

import javax.annotation.Nullable;
import java.util.Collection;

@EventBusSubscriber
public class ForceEffectCommand {

    private static final SuggestionProvider<CommandSourceStack> EFFECT_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(
                    BuiltInRegistries.MOB_EFFECT.keySet(),
                    builder
            );

    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        var simplyCommand = Commands.literal("fe")
                .requires(s -> s.hasPermission(2));
                
        var command = Commands.literal("force_effect")
                .requires(s -> s.hasPermission(2));

        // 添加效果分支
        var addEffect = Commands.literal("add")
                .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("effect", ResourceLocationArgument.id())
                                .suggests(EFFECT_SUGGESTIONS)
                                .executes(ctx -> addEffect(
                                        ctx.getSource(),
                                        EntityArgument.getEntities(ctx, "targets"),
                                        getEffect(ctx),
                                        600, 0, false,
                                        ctx.getSource().getEntity()
                                ))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                        .executes(ctx -> addEffect(
                                                ctx.getSource(),
                                                EntityArgument.getEntities(ctx, "targets"),
                                                getEffect(ctx),
                                                IntegerArgumentType.getInteger(ctx, "seconds") * 20,
                                                0, false,
                                                ctx.getSource().getEntity()
                                        ))
                                        .then(Commands.argument("amplifier", IntegerArgumentType.integer(0))
                                                .executes(ctx -> addEffect(
                                                        ctx.getSource(),
                                                        EntityArgument.getEntities(ctx, "targets"),
                                                        getEffect(ctx),
                                                        IntegerArgumentType.getInteger(ctx, "seconds") * 20,
                                                        IntegerArgumentType.getInteger(ctx, "amplifier"),
                                                        false,
                                                        ctx.getSource().getEntity()
                                                ))
                                                .then(Commands.argument("hideParticles", BoolArgumentType.bool())
                                                        .executes(ctx -> addEffect(
                                                                ctx.getSource(),
                                                                EntityArgument.getEntities(ctx, "targets"),
                                                                getEffect(ctx),
                                                                IntegerArgumentType.getInteger(ctx, "seconds") * 20,
                                                                IntegerArgumentType.getInteger(ctx, "amplifier"),
                                                                BoolArgumentType.getBool(ctx, "hideParticles"),
                                                                ctx.getSource().getEntity()
                                                        ))
                                                )
                                        )
                                )
                        )
                );

        // 移除效果分支
        var removeEffect = Commands.literal("remove")
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(ctx -> removeAllEffects(
                                ctx.getSource(),
                                EntityArgument.getEntities(ctx, "targets")
                        ))
                        .then(Commands.argument("effect", ResourceLocationArgument.id())
                                .suggests(EFFECT_SUGGESTIONS)
                                .executes(ctx -> removeEffect(
                                        ctx.getSource(),
                                        EntityArgument.getEntities(ctx, "targets"),
                                        getEffect(ctx)
                                ))
                        ));

        // 更新效果分支
        var updateEffect = Commands.literal("update")
                .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("effect", ResourceLocationArgument.id())
                                .suggests(EFFECT_SUGGESTIONS)
                                .executes(ctx -> updateEffect(
                                        ctx.getSource(),
                                        EntityArgument.getEntities(ctx, "targets"),
                                        getEffect(ctx),
                                        600, 0, false
                                ))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                        .executes(ctx -> updateEffect(
                                                ctx.getSource(),
                                                EntityArgument.getEntities(ctx, "targets"),
                                                getEffect(ctx),
                                                IntegerArgumentType.getInteger(ctx, "seconds") * 20,
                                                0, false
                                        ))
                                        .then(Commands.argument("amplifier", IntegerArgumentType.integer(0))
                                                .executes(ctx -> updateEffect(
                                                        ctx.getSource(),
                                                        EntityArgument.getEntities(ctx, "targets"),
                                                        getEffect(ctx),
                                                        IntegerArgumentType.getInteger(ctx, "seconds") * 20,
                                                        IntegerArgumentType.getInteger(ctx, "amplifier"),
                                                        false
                                                ))
                                                .then(Commands.argument("hideParticles", BoolArgumentType.bool())
                                                        .executes(ctx -> updateEffect(
                                                                ctx.getSource(),
                                                                EntityArgument.getEntities(ctx, "targets"),
                                                                getEffect(ctx),
                                                                IntegerArgumentType.getInteger(ctx, "seconds") * 20,
                                                                IntegerArgumentType.getInteger(ctx, "amplifier"),
                                                                BoolArgumentType.getBool(ctx, "hideParticles")
                                                        ))
                                                )
                                        )
                                )
                        )
                );

        simplyCommand.then(addEffect);
        simplyCommand.then(removeEffect);
        simplyCommand.then(updateEffect);
        command.then(addEffect);
        command.then(removeEffect);
        command.then(updateEffect);
        dispatcher.register(simplyCommand);
        dispatcher.register(command);
    }

    private static Holder<MobEffect> getEffect(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "effect");

        return BuiltInRegistries.MOB_EFFECT.getHolder(id).orElseThrow();
    }

    private static int addEffect(CommandSourceStack source,
                             Collection<? extends Entity> targets,
                             Holder<MobEffect> effect,
                             int duration,
                             int amplifier,
                             boolean hideParticles,
                             @Nullable Entity sourceEntity) {
        int count = processTargets(targets, living -> {
            MobEffectInstance instance = new MobEffectInstance(
                    effect,
                    duration,
                    amplifier,
                    false,
                    !hideParticles,
                    true
            );
            EffectUtils.forceAddEffect(living, instance, sourceEntity);
        });

        return handleResult(source, targets, count, effect, amplifier, "添加");
    }

    private static int updateEffect(CommandSourceStack source,
                               Collection<? extends Entity> targets,
                               Holder<MobEffect> effect,
                               int duration,
                               int amplifier,
                               boolean hideParticles) {
        int count = processTargets(targets, living -> {
            MobEffectInstance instance = new MobEffectInstance(
                    effect,
                    duration,
                    amplifier,
                    false,
                    !hideParticles,
                    true
            );
            EffectUtils.forceUpdateEffect(living, effect, instance, source.getEntity());
        });

        return handleResult(source, targets, count, effect, amplifier, "更新");
    }

    private static int removeEffect(CommandSourceStack source,
                               Collection<? extends Entity> targets,
                               Holder<MobEffect> effect) {
        int count = processTargets(targets, living -> EffectUtils.forceRemoveEffect(living, effect));

        return handleResult(source, targets, count, effect, 0, "移除");
    }

    private static int removeAllEffects(CommandSourceStack source,
                                   Collection<? extends Entity> targets) {
        int count = processTargets(targets, living -> {
            for (Holder<MobEffect> effect : living.getActiveEffectsMap().keySet()) {
                EffectUtils.forceRemoveEffect(living, effect);
            }
        });

        if (count == 0) {
            source.sendFailure(Component.literal("没有有效的生物实体"));
            return 0;
        }

        if (count < 8) {
            String entitiesList = targets.stream()
                    .filter(entity -> entity instanceof LivingEntity)
                    .map(entity -> entity.getDisplayName().getString())
                    .collect(java.util.stream.Collectors.joining(", "));
            source.sendSuccess(() -> Component.literal("强制移除所有效果从 %s".formatted(entitiesList)), true);
        } else {
            source.sendSuccess(() -> Component.literal("强制移除所有效果从 %d 个实体".formatted(count)), true);
        }

        return count;
    }

    @FunctionalInterface
    private interface EntityProcessor {
        void process(LivingEntity entity);
    }

    private static int processTargets(Collection<? extends Entity> targets, EntityProcessor processor) {
        int count = 0;
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity living) {
                processor.process(living);
                count++;
            }
        }
        return count;
    }

    private static int handleResult(CommandSourceStack source,
                               Collection<? extends Entity> targets,
                               int count,
                               Holder<MobEffect> effect,
                               int amplifier,
                               String action) {
        if (count == 0) {
            source.sendFailure(Component.literal("§c没有有效的生物实体"));
            return 0;
        }

        String effectName = effect.value().getDisplayName().getString();

        if (count < 8) {
            String entitiesList = targets.stream()
                    .filter(entity -> entity instanceof LivingEntity)
                    .map(entity -> entity.getDisplayName().getString())
                    .collect(java.util.stream.Collectors.joining(", "));
            source.sendSuccess(() -> Component.literal("%s %s 效果给 %s".formatted(
                    "强制".equals(action) ? "强制" + action : action,
                    effectName + (amplifier > 0 ? amplifier : ""),
                    entitiesList)), true);
        } else {
            source.sendSuccess(() -> Component.literal("%s %s 效果给 %d 个实体".formatted(
                    "强制".equals(action) ? "强制" + action : action,
                    effectName + (amplifier > 0 ? amplifier : ""),
                    count)), true);
        }

        return count;
    }
}