package com.yixi_xun.more_potion_effects.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import com.yixi_xun.more_potion_effects.api.PotionBrewingSystem;
import com.yixi_xun.more_potion_effects.network.MPEReloadPayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * {@code /mpe reload} —— 在游戏内热重载酿造配方。
 * 服务端重新读取本地 config 使酿造逻辑即时生效，并通知所有在线客户端重载各自本地配方。
 */
@EventBusSubscriber(modid = MorePotionEffectsMod.MOD_ID)
public class MPECommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("mpe")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(MPECommand::reload)));
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        // 服务端重载：即时更新酿造逻辑（服务端 PotionBrewing 通过活包装配方读取新列表）
        int count = PotionBrewingSystem.reloadRecipes();
        // 通知所有在线客户端重载各自的本地配方与创造标签页缓存
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, new MPEReloadPayload());
        }
        source.sendSuccess(() -> Component.literal("§a[MPE] 已热重载酿造配方，共 " + count + " 条"), true);
        return count;
    }
}
