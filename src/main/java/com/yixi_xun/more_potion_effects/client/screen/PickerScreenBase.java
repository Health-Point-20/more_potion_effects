package com.yixi_xun.more_potion_effects.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * 注册表条目选择器基类：搜索框 + 命名空间过滤 + 可滚动列表 + 点击回调。
 *
 * @param <T> 条目类型
 */
public abstract class PickerScreenBase<T> extends Screen {

    protected enum Filter {
        ALL("全部"), MPE("本模组"), VANILLA("原版"), OTHER("其它");

        final String label;

        Filter(String label) {
            this.label = label;
        }
    }

    public static final String MPE_NS = "more_potion_effects";

    protected final Screen returnTo;
    protected final Consumer<T> onPick;
    protected final List<T> all = new ArrayList<>();

    protected EditBox searchBox;
    protected Filter filter = Filter.ALL;
    protected String query = "";
    protected int scroll = 0;

    protected PickerScreenBase(String title, Screen returnTo, Consumer<T> onPick) {
        super(Component.literal(title));
        this.returnTo = returnTo;
        this.onPick = onPick;
    }

    /** 条目的显示文本（主行）。 */
    protected abstract String displayText(T entry);

    /** 条目的次行文本（资源ID），可为空字符串。 */
    protected abstract String displayId(T entry);

    /** 条目所属命名空间（用于过滤）。 */
    protected abstract String namespace(T entry);

    /** 条目搜索匹配文本。 */
    protected abstract String searchableText(T entry);

    /** 可选：绘制条目左侧图标（如物品）。返回图标占用的宽度，0 表示无图标。 */
    protected int renderIcon(GuiGraphics g, T entry, int x, int y) {
        return 0;
    }

    protected boolean matchesFilter(T entry) {
        String ns = namespace(entry);
        return switch (filter) {
            case MPE -> ns.equals(MPE_NS);
            case VANILLA -> ns.equals("minecraft");
            case OTHER -> !ns.equals(MPE_NS) && !ns.equals("minecraft");
            default -> true;
        };
    }

    protected List<T> visible() {
        List<T> out = new ArrayList<>();
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (T entry : all) {
            if (!matchesFilter(entry)) {
                continue;
            }
            if (!q.isEmpty() && !searchableText(entry).toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            out.add(entry);
        }
        return out;
    }

    /* ---- 几何 ---- */
    protected int gw() {
        return Math.min(this.width - 24, 380);
    }

    protected int gh() {
        return Math.min(this.height - 24, 300);
    }

    protected int gx() {
        return (this.width - gw()) / 2;
    }

    protected int gy() {
        return (this.height - gh()) / 2;
    }

    protected int listTop() {
        return gy() + 52;
    }

    protected int listBottom() {
        return gy() + gh() - 26;
    }

    protected int rowH() {
        return 24;
    }

    protected int rowsVisible() {
        return Math.max(1, (listBottom() - listTop()) / rowH());
    }

    @Override
    protected void init() {
        super.init();
        int gx = gx();
        int gy = gy();
        int gw = gw();

        this.searchBox = new EditBox(this.font, gx + 8, gy + 8, gw - 96, 16, Component.literal("搜索"));
        this.searchBox.setMaxLength(80);
        this.searchBox.setHint(Component.literal("输入关键字过滤…"));
        this.searchBox.setResponder(s -> {
            this.query = s;
            this.scroll = 0;
        });
        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);
        this.addRenderableWidget(Button.builder(Component.literal("取消"), b -> back())
                .bounds(gx + gw - 52, gy + 8, 44, 16)
                .build());

        int fx = gx + 8;
        for (Filter f : Filter.values()) {
            int w = this.font.width(f.label) + 16;
            final Filter ff = f;
            this.addRenderableWidget(Button.builder(Component.literal(f.label), b -> {
                this.filter = ff;
                this.scroll = 0;
            }).bounds(fx, gy + 30, w, 16).build());
            fx += w + 4;
        }
    }

    protected void back() {
        if (this.returnTo != null) {
            this.minecraft.setScreen(this.returnTo);
        } else {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        ScreenLayout.drawPanel(g, gx(), gy(), gw(), gh());
        g.fill(gx() + 5, listTop(), gx() + gw() - 5, listBottom(), 0x90000000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        List<T> vis = visible();
        int rows = rowsVisible();
        int maxScroll = Math.max(0, vis.size() - rows);
        if (this.scroll > maxScroll) {
            this.scroll = maxScroll;
        }
        int lx = gx() + 5;
        int lr = gx() + gw() - 5;
        for (int i = 0; i < rows; i++) {
            int idx = scroll + i;
            if (idx >= vis.size()) {
                break;
            }
            T entry = vis.get(idx);
            int ry = listTop() + i * rowH();
            boolean hovered = mx >= lx && mx <= lr && my >= ry && my < ry + rowH();
            if (hovered) {
                g.fill(lx, ry, lr, ry + rowH(), 0xFF2C5C99);
            }
            int textX = lx + 4 + renderIcon(g, entry, lx + 3, ry + (rowH() - 16) / 2) + 3;
            String name = displayText(entry);
            g.drawString(this.font, truncate(name, (lr - textX) - 4), textX, ry + 3,
                    namespace(entry).equals(MPE_NS) ? 0xFF8EF78E : 0xFFFFFFFF, false);
            String id = displayId(entry);
            if (!id.isEmpty()) {
                g.drawString(this.font, truncate(id, (lr - textX) - 4), textX, ry + 13, 0xFF9A9A9A, false);
            }
        }
        if (vis.isEmpty()) {
            g.drawString(this.font, "（无匹配条目）", lx + 4, listTop() + 6, 0xFFA0A0A0, false);
        }
        g.drawString(this.font, "共 " + vis.size() + " / " + all.size() + " 项（绿色=本模组）",
                lx, listBottom() + 8, 0xFFA0A0A0, false);
        if (scroll > 0) {
            g.drawString(this.font, "↑", gx() + gw() - 14, listTop() + 2, 0xFFC0C0C0, false);
        }
        if (scroll + rows < vis.size()) {
            g.drawString(this.font, "↓", gx() + gw() - 14, listBottom() - 12, 0xFFC0C0C0, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        List<T> vis = visible();
        int max = Math.max(0, vis.size() - rowsVisible());
        if (mx >= gx() && mx <= gx() + gw() && my >= listTop() && my <= listBottom() && max > 0) {
            this.scroll = Math.max(0, Math.min(max, this.scroll - (int) dy));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            List<T> vis = visible();
            if (mx >= gx() + 5 && mx <= gx() + gw() - 5 && my >= listTop() && my < listBottom()) {
                int idx = scroll + (int) ((my - listTop()) / rowH());
                if (idx >= 0 && idx < vis.size()) {
                    this.onPick.accept(vis.get(idx));
                    back();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            back();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected String truncate(String s, int maxPx) {
        if (maxPx <= 0 || this.font.width(s) <= maxPx) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            String next = sb + "" + s.charAt(i);
            if (this.font.width(next) > maxPx - 3) {
                break;
            }
            sb.append(s.charAt(i));
        }
        return sb + "...";
    }

    /** 供编辑器复用的通用面板绘制。 */
    static final class ScreenLayout {
        static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x, y, x + w, y + h, 0xE6101016);
            g.fill(x + w, y, x + w + 1, y + h, 0xFF8A8A8A);
            g.fill(x, y, x + 1, y + h, 0xFF8A8A8A);
            g.fill(x, y, x + w, y + 1, 0xFF8A8A8A);
            g.fill(x, y + h - 1, x + w, y + h, 0xFF8A8A8A);
        }
    }
}
