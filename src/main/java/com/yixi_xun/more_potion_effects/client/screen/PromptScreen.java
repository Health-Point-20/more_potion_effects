package com.yixi_xun.more_potion_effects.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 简易文本输入屏：用于「导入路径」「导出路径」。
 * 确认时调用 action.run(text)，返回 null 表示成功并返回上级；返回非 null 作为错误信息停留本屏。
 */
public final class PromptScreen extends Screen {

    public interface TextAction {
        @Nullable
        String run(String text);
    }

    private static final int W = 400;
    private static final int H = 132;

    private final List<String> hintLines;
    private final String initial;
    private final TextAction action;
    private final Screen returnTo;

    private EditBox input;
    private String error = "";

    public PromptScreen(String title, List<String> hintLines, String initial, TextAction action, Screen returnTo) {
        super(Component.literal(title));
        this.hintLines = hintLines;
        this.initial = initial;
        this.action = action;
        this.returnTo = returnTo;
    }

    private int gx() {
        return (this.width - W) / 2;
    }

    private int gy() {
        return Math.max(10, (this.height - H) / 2);
    }

    @Override
    protected void init() {
        super.init();
        int gx = gx();
        int gy = gy();
        this.input = new EditBox(this.font, gx + 8, gy + 52, W - 16, 16, Component.literal("文件路径"));
        this.input.setMaxLength(300);
        this.input.setValue(this.initial == null ? "" : this.initial);
        this.addRenderableWidget(this.input);
        this.setInitialFocus(this.input);
        this.addRenderableWidget(Button.builder(Component.literal("确定"), b -> confirm())
                .bounds(gx + W - 154, gy + 106, 70, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("取消"), b -> back())
                .bounds(gx + W - 78, gy + 106, 70, 16).build());
    }

    private void confirm() {
        String err = this.action.run(this.input.getValue());
        if (err == null) {
            back();
        } else {
            this.error = err;
        }
    }

    private void back() {
        if (this.returnTo != null) {
            this.minecraft.setScreen(this.returnTo);
        } else {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        PickerScreenBase.ScreenLayout.drawPanel(g, gx(), gy(), W, H);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int gx = gx();
        int gy = gy();
        g.drawString(this.font, this.title.getString(), gx + 8, gy + 6, 0xFFFFFFFF, false);
        int y = gy + 20;
        for (String line : this.hintLines) {
            g.drawString(this.font, line, gx + 8, y, 0xFFB0B0B0, false);
            y += 10;
        }
        if (!this.error.isEmpty()) {
            g.drawString(this.font, "! " + this.error, gx + 8, gy + 88, 0xFFFF7777, false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            back();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
