package com.yixi_xun.more_potion_effects.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potion;

import java.util.Comparator;
import java.util.function.Consumer;

/**
 * 药水类型选择器：浏览全部已注册 Potion（原版与数据包/模组注册的药水）。
 */
public final class PotionPickerScreen extends PickerScreenBase<Potion> {

    public PotionPickerScreen(Screen returnTo, Consumer<String> onPick) {
        // 回调返回注册表资源ID（Potion.toString() 不是资源ID）
        super("选择基础药水", returnTo,
                potion -> onPick.accept(BuiltInRegistries.POTION.getKey(potion).toString()));
        BuiltInRegistries.POTION.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().toString()))
                .forEach(e -> all.add(e.getValue()));
    }

    private String idOf(Potion potion) {
        return BuiltInRegistries.POTION.getKey(potion).toString();
    }

    @Override
    protected String displayText(Potion entry) {
        // 1.21 中 Potion 显示名通过翻译键 potion.effect.<注册名 path> 解析（原版约定不含命名空间）
        ResourceLocation rl = BuiltInRegistries.POTION.getKey(entry);
        String key = "potion.effect." + rl.getPath();
        String s = net.minecraft.network.chat.Component.translatable(key).getString();
        if (s.equals(key)) {
            s = rl.getPath();
        }
        return s;
    }

    @Override
    protected String displayId(Potion entry) {
        return idOf(entry);
    }

    @Override
    protected String namespace(Potion entry) {
        return BuiltInRegistries.POTION.getKey(entry).getNamespace();
    }

    @Override
    protected String searchableText(Potion entry) {
        return idOf(entry) + " " + displayText(entry);
    }

    public static void open(Screen returnTo, Consumer<String> onPick) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new PotionPickerScreen(returnTo, onPick));
    }
}
