package com.yixi_xun.more_potion_effects.client;

import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import com.yixi_xun.more_potion_effects.client.screen.RecipeEditorScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * 配方编辑器入口（纯客户端）：默认按键 K，或客户端命令 {@code /mpe_gui}。
 * 使用独立命令名而非 {@code /mpe gui}，避免与游戏端命令 {@code /mpe} 的根节点合并冲突。
 */
@EventBusSubscriber(modid = MorePotionEffectsMod.MOD_ID, value = Dist.CLIENT)
public final class RecipeEditorClientEvents {

    public static final KeyMapping OPEN_EDITOR_KEY = new KeyMapping(
            "key." + MorePotionEffectsMod.MOD_ID + ".open_recipe_editor",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories." + MorePotionEffectsMod.MOD_ID);

    private RecipeEditorClientEvents() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_EDITOR_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_EDITOR_KEY.consumeClick()) {
            openEditor();
        }
    }

    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mpe_gui").executes(ctx -> {
            openEditor();
            return 1;
        }));
    }

    private static void openEditor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (mc.screen instanceof RecipeEditorScreen) {
            return;
        }
        // 注册表（药水/效果/物品）需在游戏连接就绪后访问
        mc.setScreen(new RecipeEditorScreen());
    }
}
