package com.yixi_xun.more_potion_effects.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.function.Consumer;

/**
 * 酿造材料选择器：浏览全部已注册物品（带图标），支持搜索与命名空间过滤。
 */
public final class ItemPickerScreen extends PickerScreenBase<Item> {

    public ItemPickerScreen(Screen returnTo, Consumer<String> onPick) {
        // 回调返回注册表资源ID（Item.toString() 形如 "minecraft:xxx[item]"，不能直接写入配置）
        super("选择酿造材料", returnTo,
                item -> onPick.accept(BuiltInRegistries.ITEM.getKey(item).toString()));
        BuiltInRegistries.ITEM.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().toString()))
                .forEach(e -> all.add(e.getValue()));
    }

    private String idOf(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    @Override
    protected String displayText(Item entry) {
        String s = entry.getDescription().getString();
        return s.isEmpty() ? idOf(entry) : s;
    }

    @Override
    protected String displayId(Item entry) {
        return idOf(entry);
    }

    @Override
    protected String namespace(Item entry) {
        return BuiltInRegistries.ITEM.getKey(entry).getNamespace();
    }

    @Override
    protected String searchableText(Item entry) {
        return idOf(entry) + " " + displayText(entry);
    }

    @Override
    protected int renderIcon(GuiGraphics g, Item entry, int x, int y) {
        try {
            ItemStack stack = new ItemStack(entry);
            if (!stack.isEmpty()) {
                g.renderItem(stack, x, y);
                return 16;
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    public static void open(Screen returnTo, Consumer<String> onPick) {
        Minecraft.getInstance().setScreen(new ItemPickerScreen(returnTo, onPick));
    }
}
