package com.yixi_xun.more_potion_effects.client;

import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import com.yixi_xun.more_potion_effects.api.PotionBrewingSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.lang.reflect.Method;

/**
 * 创造模式标签页即时刷新工具（纯客户端）。
 *
 * 依赖 {@link PotionBrewingSystem#invalidateCreativeTabCache()} 先把缓存置空，
 * 再这里触发 {@code CreativeModeTabs.tryRebuildTabContents(...)} 重建全部标签页内容
 * （会重新执行本模组 tab 的 {@code getCustomsPotionStacks()}，从而立即看到新药水），
 * 若创造栏当前正打开，则同时刷新其当前页并重建搜索树，无需重进存档。
 */
@OnlyIn(Dist.CLIENT)
public final class CreativeTabRefresher {

    private CreativeTabRefresher() {
    }

    private static final String METHOD_TAGS = "tryRefreshInvalidatedTabs";

    public static void refreshNow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        LocalPlayer p = mc.player;
        if (p == null) {
            return;
        }
        try {
            // 缓存已由 invalidateCreativeTabCache() 置空，此处必然触发全量重建
            CreativeModeTabs.tryRebuildTabContents(
                    p.connection.enabledFeatures(),
                    p.canUseGameMasterBlocks(),
                    p.level().registryAccess());

            // 创造栏若正打开：反射调用其 private 方法刷新当前页网格与搜索树
            if (mc.screen instanceof CreativeModeInventoryScreen creative) {
                for (Method m : creative.getClass().getDeclaredMethods()) {
                    if (m.getName().equals(METHOD_TAGS) && m.getParameterCount() == 3) {
                        m.setAccessible(true);
                        m.invoke(creative,
                                p.connection.enabledFeatures(),
                                p.canUseGameMasterBlocks(),
                                p.level().registryAccess());
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            MorePotionEffectsMod.LOGGER.debug("refreshCreativeTabs", t);
        }
    }
}