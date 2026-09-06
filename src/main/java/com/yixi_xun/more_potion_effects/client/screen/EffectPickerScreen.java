package com.yixi_xun.more_potion_effects.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

import java.util.Comparator;
import java.util.function.Consumer;

/**
 * 状态效果选择器：浏览全部已注册 MobEffect（含本模组效果），支持搜索与命名空间过滤。
 */
public final class EffectPickerScreen extends PickerScreenBase<MobEffect> {

    public EffectPickerScreen(Screen returnTo, Consumer<String> onPick) {
        // 回调返回注册表资源ID（MobEffect.toString() 不是资源ID，必须走注册表）
        super("选择状态效果", returnTo,
                effect -> onPick.accept(BuiltInRegistries.MOB_EFFECT.getKey(effect).toString()));
        BuiltInRegistries.MOB_EFFECT.entrySet().stream()
                .map(e -> e.getValue())
                .sorted(Comparator.comparing(e -> BuiltInRegistries.MOB_EFFECT.getKey(e).toString()))
                .forEach(all::add);
    }

    private String idOf(MobEffect effect) {
        return BuiltInRegistries.MOB_EFFECT.getKey(effect).toString();
    }

    @Override
    protected String displayText(MobEffect entry) {
        String s = entry.getDisplayName().getString();
        return s.isEmpty() ? idOf(entry) : s;
    }

    @Override
    protected String displayId(MobEffect entry) {
        return idOf(entry);
    }

    @Override
    protected String namespace(MobEffect entry) {
        return BuiltInRegistries.MOB_EFFECT.getKey(entry).getNamespace();
    }

    @Override
    protected String searchableText(MobEffect entry) {
        return idOf(entry) + " " + displayText(entry);
    }

    public static void open(Screen returnTo, Consumer<String> onPick) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new EffectPickerScreen(returnTo, onPick));
    }
}
