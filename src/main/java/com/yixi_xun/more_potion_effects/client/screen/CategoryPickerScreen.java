package com.yixi_xun.more_potion_effects.client.screen;

import com.yixi_xun.more_potion_effects.editor.RecipeWorkspace;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Consumer;

/**
 * 分类选择器：列出当前工作区全部配方分类，点选回传 {@link RecipeWorkspace.Category}。
 */
public final class CategoryPickerScreen extends PickerScreenBase<RecipeWorkspace.Category> {

    public static void open(Screen returnTo, RecipeWorkspace.Category current, Consumer<RecipeWorkspace.Category> onPick) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new CategoryPickerScreen(returnTo, current, onPick));
    }

    private CategoryPickerScreen(Screen returnTo, RecipeWorkspace.Category current, Consumer<RecipeWorkspace.Category> onPick) {
        super("选择目标分类", returnTo, onPick);
        RecipeWorkspace w = new RecipeWorkspace();
        w.loadAll();
        all.addAll(w.getCategories());
    }

    @Override
    protected String displayText(RecipeWorkspace.Category entry) {
        if (entry.isDefault) {
            return entry.displayName + "（默认）";
        }
        return entry.displayName;
    }

    @Override
    protected String displayId(RecipeWorkspace.Category entry) {
        return entry.file.getFileName().toString();
    }

    @Override
    protected String namespace(RecipeWorkspace.Category entry) {
        return "category";
    }

    @Override
    protected String searchableText(RecipeWorkspace.Category entry) {
        return entry.displayName + " " + entry.file.getFileName();
    }
}