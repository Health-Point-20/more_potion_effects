package com.yixi_xun.more_potion_effects.client.screen;

import com.yixi_xun.more_potion_effects.api.PotionBrewingSystem;
import com.yixi_xun.more_potion_effects.client.CreativeTabRefresher;
import com.yixi_xun.more_potion_effects.editor.RecipeDoc;
import com.yixi_xun.more_potion_effects.editor.RecipePreview;
import com.yixi_xun.more_potion_effects.editor.RecipeWorkspace;
import com.yixi_xun.more_potion_effects.network.MPEReloadPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 酿造配方编辑器主屏（纯客户端、无容器/槽位）。
 *
 * 布局：左侧「分类列表 + 配方列表（含排序/移动/增删）」+ 右侧「配方详情（含产物预览）和挪分类」；
 * 底部 保存并生效 / 重新载入 / 导入… / 导出… / 关闭。
 *  - 分类 = config/more_potion_effects/ 下的一个 JSON 文件；支持新建/改名（改文件）/删除（配方移回默认分类）；
 *  - 分类内配方可用 ▲▼ 排序、跨分类用「挪到…」移动；分类内数组顺序 = 创造栏展示顺序；
 *  - 保存后触发热重载并即时刷新创造模式标签页（见 {@link CreativeTabRefresher}）。
 */
public class RecipeEditorScreen extends Screen {

    private static final int ROW_H = 18;
    private static final int PANEL_M = 4;
    private static final int GAP = 5;

    private final RecipeWorkspace workspace;
    private RecipeWorkspace.Category selectedCategory;
    private int selected = -1;
    private int listScroll = 0;
    private int catScroll = 0;
    private int effScroll = 0;
    private boolean dirty = false;
    private String status;
    private int statusColor = 0xFF9FC5FF;

    public RecipeEditorScreen() {
        super(Component.literal("酿造配方编辑器"));
        this.workspace = new RecipeWorkspace();
        this.workspace.loadAll();
        List<RecipeWorkspace.Category> cats = this.workspace.getCategories();
        this.selectedCategory = cats.isEmpty() ? null : cats.get(0);
        int loaded = this.workspace.getRecipes().size();
        this.status = this.workspace.getLoadProblems().isEmpty()
                ? "已加载 " + loaded + " 条配方。改完点「保存并生效」。"
                : String.join("；", this.workspace.getLoadProblems());
        this.statusColor = this.workspace.getLoadProblems().isEmpty() ? 0xFF9FC5FF : 0xFFFFC000;
    }

    /* ------------------------------------------------------------------ */
    /* 几何                                                               */
    /* ------------------------------------------------------------------ */

    private final class Geom {
        int gw, gh, gx, gy;
        int barY;
        int lw;
        int detX, detW;
        int contentBottom;
        // 左列：分类区 + 配方区
        int catHeadY, catBtnY, catListTop, catListBottom, catListRows, catActionY;
        int recHeadY, recBtnY, recListTop, recListBottom, recListRows;
        // 右列：详情
        int detHeadY, dtCatY, metaTop, effHeadY, effTitleY, effCapY, effTop, effBottom, effRows;
    }

    private Geom geom() {
        Geom g = new Geom();
        g.gw = Math.max(480, Math.min(this.width - 2 * PANEL_M, 680));
        g.gh = Math.max(260, Math.min(this.height - 2 * PANEL_M - 4, 380));
        g.gx = (this.width - g.gw) / 2;
        g.gy = (this.height - g.gh) / 2;
        g.barY = g.gy + g.gh - 22;
        g.contentBottom = g.barY - 2;
        g.lw = Math.max(190, Math.min(230, g.gw / 4 + 45));
        g.detX = g.gx + g.lw + GAP;
        g.detW = g.gw - g.lw - GAP;

        int leftTop = g.gy + 2;
        g.catHeadY = leftTop;
        g.catBtnY = leftTop + 12;
        g.catListTop = g.catBtnY + 18;
        g.catListRows = 4;
        g.catListBottom = Math.min(g.catListTop + g.catListRows * ROW_H, g.contentBottom);
        g.catActionY = g.catListBottom + 3;
        g.recHeadY = g.catActionY + 18;
        g.recBtnY = g.recHeadY + 12;
        g.recListTop = g.recBtnY + 18;
        g.recListBottom = g.contentBottom - 2;
        g.recListRows = Math.max(1, (g.recListBottom - g.recListTop) / ROW_H);

        g.detHeadY = g.gy + 3;
        g.dtCatY = g.detHeadY + 11;
        g.metaTop = g.dtCatY + 18;
        g.effHeadY = g.metaTop + 82;
        g.effTitleY = g.effHeadY + 11;
        g.effCapY = g.effTitleY + 18;
        g.effTop = g.effCapY + 10;
        g.effBottom = g.barY - 12;
        g.effRows = Math.max(1, (g.effBottom - g.effTop) / ROW_H);
        return g;
    }

    private String status() {
        return this.status == null ? "" : this.status;
    }

    /* ------------------------------------------------------------------ */
    /* 生命周期                                                           */
    /* ------------------------------------------------------------------ */

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        buildWidgets();
    }

    private void rebuild() {
        clearWidgets();
        buildWidgets();
    }

    private void buildWidgets() {
        Geom g = geom();
        // 当前选中的分类（可能已被删，回退到默认）
        List<RecipeWorkspace.Category> cats = this.workspace.getCategories();
        if (this.selectedCategory == null || cats.stream().noneMatch(c -> c.file.equals(this.selectedCategory.file))) {
            this.selectedCategory = cats.isEmpty() ? null : cats.get(0);
        }
        List<RecipeDoc.RecipeEntry> catRecipes = this.selectedCategory == null
                ? List.of() : this.workspace.getRecipesForCategory(this.selectedCategory);
        if (this.selected >= catRecipes.size()) {
            this.selected = catRecipes.size() - 1;
        }
        int maxListScroll = Math.max(0, catRecipes.size() - g.recListRows);
        if (this.listScroll > maxListScroll) {
            this.listScroll = maxListScroll;
        }
        int maxCatScroll = Math.max(0, cats.size() - g.catListRows);
        if (this.catScroll > maxCatScroll) {
            this.catScroll = maxCatScroll;
        }

        // ---- 分类标题 + 新建分类 ----
        this.addRenderableWidget(Button.builder(Component.literal("+ 新建分类"), b -> createCategory())
                .bounds(g.gx + 2, g.catBtnY, g.lw - 4, 16).build());

        // ---- 分类列表 ----
        for (int i = 0; i < g.catListRows; i++) {
            int idx = this.catScroll + i;
            if (idx >= cats.size()) {
                break;
            }
            final int fi = idx;
            RecipeWorkspace.Category c = cats.get(idx);
            String label = truncate(c.displayName + (c.isDefault ? "(默认)" : "")
                    + " " + this.workspace.getRecipesForCategory(c).size(), g.lw - 8);
            Component msg = Component.literal(label);
            if (c.file.equals(this.selectedCategory.file)) {
                msg = Component.literal("▶ " + label).withStyle(ChatFormatting.YELLOW);
            }
            this.addRenderableWidget(Button.builder(msg, b -> {
                this.selectedCategory = cats.get(fi);
                this.selected = -1;
                this.listScroll = 0;
                rebuild();
            }).bounds(g.gx + 2, g.catListTop + i * ROW_H, g.lw - 4, ROW_H - 1).build());
        }
        if (cats.isEmpty()) {
            this.addRenderableWidget(Button.builder(Component.literal("无分类"), b -> {
            }).bounds(g.gx + 2, g.catListTop, g.lw - 4, ROW_H - 1).build());
        }

        // ---- 分类操作行：改名 / 删除 ----
        int halfW = (g.lw - 4 - 4) / 2;
        boolean selDefault = this.selectedCategory != null && this.selectedCategory.isDefault;
        boolean catEditable = this.selectedCategory != null && !this.selectedCategory.isDefault;
        Button rnBtn = Button.builder(Component.literal("改名"), b -> renameCategory())
                .bounds(g.gx + 2, g.catActionY, halfW, 16).build();
        rnBtn.active = catEditable;
        Button delBtn = Button.builder(Component.literal("删除"), b -> deleteCategory())
                .bounds(g.gx + 2 + halfW + 4, g.catActionY, halfW, 16).build();
        delBtn.active = catEditable;
        this.addRenderableWidget(rnBtn);
        this.addRenderableWidget(delBtn);

        // ---- 配方标题 + 配方操作 ----
        this.addRenderableWidget(Button.builder(Component.literal("+ 新建配方"), b -> addRecipe())
                .bounds(g.gx + 2, g.recBtnY, halfW, 16).build());
        Button dupBtn = Button.builder(Component.literal("复制"), b -> duplicateRecipe())
                .bounds(g.gx + 2 + halfW + 4, g.recBtnY, halfW, 16).build();
        dupBtn.active = this.selected >= 0 && this.selected < catRecipes.size();
        this.addRenderableWidget(dupBtn);

        // ---- 配方列表（每行：▲▼ + 名称 + 删除） ----
        for (int i = 0; i < g.recListRows; i++) {
            int idx = this.listScroll + i;
            if (idx >= catRecipes.size()) {
                break;
            }
            final RecipeDoc.RecipeEntry e = catRecipes.get(idx);
            int rowY = g.recListTop + i * ROW_H;
            int upX = g.gx + 2;
            int dnX = upX + 13;
            int delX = g.gx + g.lw - 2 - 16;
            int nameX = dnX + 13;
            int nameW = delX - nameX - 2;

            boolean canUp = idx > 0;
            boolean canDown = idx < catRecipes.size() - 1;
            Button upBtn = Button.builder(Component.literal("▲"), b -> {
                this.workspace.moveRecipeUp(e);
                this.dirty = true;
                rebuild();
            }).bounds(upX, rowY + 1, 12, 15).build();
            upBtn.active = canUp;
            this.addRenderableWidget(upBtn);
            Button dnBtn = Button.builder(Component.literal("▼"), b -> {
                this.workspace.moveRecipeDown(e);
                this.dirty = true;
                rebuild();
            }).bounds(dnX, rowY + 1, 12, 15).build();
            dnBtn.active = canDown;
            this.addRenderableWidget(dnBtn);

            boolean invalid = RecipeDoc.hasProblems(e);
            String label = truncate(rowLabel(idx, e, invalid, idx + 1), Math.max(20, nameW - 2));
            Component msg = invalid
                    ? Component.literal("! ").withStyle(ChatFormatting.RED).append(Component.literal(label))
                    : Component.literal(label);
            final int fidx = idx;
            Button sel = Button.builder(Component.empty(), b -> {
                this.selected = fidx;
                this.effScroll = 0;
                rebuild();
            }).bounds(nameX, rowY + 1, nameW, 15).build();
            if (idx == this.selected) {
                sel.setMessage(Component.literal("▶ ").append(msg).withStyle(ChatFormatting.YELLOW));
            } else {
                sel.setMessage(Component.literal(label).withStyle(ChatFormatting.WHITE));
            }
            this.addRenderableWidget(sel);

            this.addRenderableWidget(Button.builder(Component.literal("×"), b -> {
                this.workspace.getRecipes().remove(e);
                this.dirty = true;
                if (this.selected >= this.workspace.getRecipes().size()) {
                    this.selected = this.workspace.getRecipes().size() - 1;
                }
                rebuild();
            }).bounds(delX, rowY + 1, 16, 15).build());
        }
        if (catRecipes.isEmpty()) {
            this.addRenderableWidget(Button.builder(Component.literal("· 点击配方进入编辑 ·"), b -> {
            }).bounds(g.gx + 2, g.recListTop, g.lw - 4, ROW_H - 1).build());
        }

        buildDetail(g);
        buildBottomBar(g);
    }

    private String rowLabel(int idx, RecipeDoc.RecipeEntry e, boolean invalid, int number) {
        String name = e.customName == null || e.customName.trim().isEmpty()
                ? (e.basePotion == null || e.basePotion.trim().isEmpty() ? "未命名配方" : e.basePotion.trim())
                : e.customName.trim();
        int n = e.effects.size();
        String base = number + ". " + name;
        if (!invalid) {
            base += "(" + n + ")";
        }
        return base;
    }

    /* ------------------------------------------------------------------ */
    /* 详情面板                                                           */
    /* ------------------------------------------------------------------ */

    private void buildDetail(Geom g) {
        List<RecipeDoc.RecipeEntry> catRecipes = this.selectedCategory == null
                ? List.of() : this.workspace.getRecipesForCategory(this.selectedCategory);
        if (this.selected < 0 || this.selected >= catRecipes.size()) {
            // 仍提供「挪到…」所在行的占位交给渲染
            return;
        }
        RecipeDoc.RecipeEntry e = catRecipes.get(this.selected);
        int px = g.detX + 6;
        int contentW = g.detW - 12;
        int cw = (contentW - 6) / 2;
        int x1 = px;
        int x2 = px + cw + 6;
        int pickW = 30;

        // 挪分类按钮（放在标题行右侧）
        this.addRenderableWidget(Button.builder(Component.literal("挪到…"), b -> moveCategory(e))
                .bounds(g.detX + g.detW - 58, g.dtCatY, 50, 15).build());

        fieldWithPicker(g, "基础药水 (base_potion)", x1, g.metaTop, cw, pickW,
                v -> {
                    e.basePotion = v;
                    dirty = true;
                }, e.basePotion, () -> PotionPickerScreen.open(this, id -> {
                    e.basePotion = id;
                    dirty = true;
                    rebuild();
                }));
        fieldWithPicker(g, "酿造材料 (ingredient)", x2, g.metaTop, cw, pickW,
                v -> {
                    e.ingredient = v;
                    dirty = true;
                }, e.ingredient, () -> ItemPickerScreen.open(this, id -> {
                    e.ingredient = id;
                    dirty = true;
                    rebuild();
                }));

        field(g, "自定义名 (custom_name)", x1, g.metaTop + 25, cw, v -> {
            e.customName = v;
            dirty = true;
        }, e.customName);
        field(g, "自定义基底 (custom_base)", x2, g.metaTop + 25, cw, v -> {
            e.customBase = v;
            dirty = true;
        }, e.customBase);

        field(g, "颜色 (custom_color)", x1, g.metaTop + 50, cw - 20, v -> {
            e.customColor = v;
            dirty = true;
        }, e.customColor);

        // 效果区块
        int effCount = e.effects.size();
        if (effCount < RecipeDoc.EFFECT_SLOT_COUNT) {
            String head = "效果 " + effCount + " / " + RecipeDoc.EFFECT_SLOT_COUNT;
            int headW = this.font.width(head);
            this.addRenderableWidget(Button.builder(Component.literal("+添加效果"), b -> addEffect(e))
                    .bounds(g.detX + 8 + headW + 8, g.effTitleY - 1, 70, 16).build());
        }

        int nRows = e.effects.size();
        if (this.effScroll > Math.max(0, nRows - g.effRows)) {
            this.effScroll = Math.max(0, nRows - g.effRows);
        }
        for (int r = 0; r < g.effRows; r++) {
            int idx = this.effScroll + r;
            if (idx >= nRows) {
                break;
            }
            buildEffectRow(g, e, idx, r);
        }
    }

    private void fieldWithPicker(Geom g, String label, int x, int y, int w, int pickW,
                                 Consumer<String> setter, String value, Runnable onPick) {
        int boxW = w - pickW - 3;
        EditBox box = new EditBox(this.font, x, y + 10, boxW, 14, Component.literal(label));
        box.setMaxLength(255);
        box.setValue(value == null ? "" : value);
        box.setResponder(setter);
        this.addRenderableWidget(box);
        this.addRenderableWidget(Button.builder(Component.literal("选择"), b -> onPick.run())
                .bounds(x + boxW + 3, y + 9, pickW, 16).build());
    }

    private void field(Geom g, String label, int x, int y, int w, Consumer<String> setter, String value) {
        EditBox box = new EditBox(this.font, x, y + 10, w, 14, Component.literal(label));
        box.setMaxLength(255);
        box.setValue(value == null ? "" : value);
        box.setResponder(setter);
        this.addRenderableWidget(box);
    }

    private void addEffect(RecipeDoc.RecipeEntry entry) {
        if (entry.effects.size() >= RecipeDoc.EFFECT_SLOT_COUNT) {
            return;
        }
        entry.effects.add(new RecipeDoc.EffectEntry());
        this.dirty = true;
        Geom g = geom();
        this.effScroll = Math.max(0, entry.effects.size() - g.effRows);
        rebuild();
    }

    private void buildEffectRow(Geom g, RecipeDoc.RecipeEntry entry, int effectIdx, int row) {
        RecipeDoc.EffectEntry fx = entry.effects.get(effectIdx);
        int x0 = g.detX + 6;
        int xEnd = g.detX + g.detW - 6;
        int y = g.effTop + row * ROW_H;

        int delW = 18;
        int flagW = 14;
        int selW = 28;
        int ampW = 32;
        int durW = 44;
        int idxW = 14;

        int delX = xEnd - delW;
        int flagX = delX - (flagW * 3 + 4) - 3;
        int selX = flagX - selW - 2;
        int ampX = selX - ampW - 2;
        int durX = ampX - durW - 2;
        int idX = x0 + idxW;
        int idW = Math.max(30, durX - idX - 4);

        this.addRenderableWidget(Button.builder(Component.literal("×"), b -> {
            entry.effects.remove(effectIdx);
            dirty = true;
            this.effScroll = Math.min(this.effScroll, Math.max(0, entry.effects.size() - 1));
            rebuild();
        }).bounds(delX, y + 1, delW, 15).build());

        addFlagButton(fx, y, flagX, flagW, 0);
        addFlagButton(fx, y, flagX + flagW + 2, flagW, 1);
        addFlagButton(fx, y, flagX + (flagW + 2) * 2, flagW, 2);

        this.addRenderableWidget(Button.builder(Component.literal("选"), b ->
                EffectPickerScreen.open(this, id -> {
                    fx.effectId = id;
                    dirty = true;
                    rebuild();
                })).bounds(selX, y + 1, selW, 15).build());

        EditBox dur = new EditBox(this.font, durX, y + 1, durW, 14, Component.literal("时长tick"));
        dur.setMaxLength(10);
        dur.setValue(fx.durationText);
        dur.setResponder(s -> {
            fx.durationText = s;
            dirty = true;
        });
        this.addRenderableWidget(dur);

        EditBox amp = new EditBox(this.font, ampX, y + 1, ampW, 14, Component.literal("强度"));
        amp.setMaxLength(4);
        amp.setValue(fx.amplifierText);
        amp.setResponder(s -> {
            fx.amplifierText = s;
            dirty = true;
        });
        this.addRenderableWidget(amp);

        EditBox id = new EditBox(this.font, idX, y + 1, idW, 14, Component.literal("效果ID"));
        id.setMaxLength(120);
        id.setValue(fx.effectId == null ? "" : fx.effectId);
        id.setResponder(s -> {
            fx.effectId = s;
            dirty = true;
        });
        this.addRenderableWidget(id);
    }

    private void addFlagButton(RecipeDoc.EffectEntry fx, int y, int x, int w, int slot) {
        String label;
        boolean on;
        switch (slot) {
            case 0 -> {
                label = "A";
                on = fx.ambient;
            }
            case 1 -> {
                label = "V";
                on = fx.visible;
            }
            default -> {
                label = "I";
                on = fx.showIcon;
            }
        }
        final int fslot = slot;
        Button b = Button.builder(Component.literal(label), btn -> {
            switch (fslot) {
                case 0 -> fx.ambient = !fx.ambient;
                case 1 -> fx.visible = !fx.visible;
                default -> fx.showIcon = !fx.showIcon;
            }
            dirty = true;
            rebuild();
        }).bounds(x, y + 1, w, 15).build();
        b.setMessage(Component.literal(label).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        this.addRenderableWidget(b);
    }

    private void buildBottomBar(Geom g) {
        int y = g.barY;
        int h = 18;
        int x = g.gx + 2;
        int[] ws = {86, 80, 66, 66, 56};
        String[] labels = {"保存并生效", "重新载入", "导入…", "导出…", "关闭"};
        Runnable[] actions = {this::saveAndApply, this::reloadAll, this::openImport, this::openExport, this::requestClose};
        for (int i = 0; i < labels.length; i++) {
            final Runnable a = actions[i];
            this.addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> a.run())
                    .bounds(x, y, ws[i], h).build());
            x += ws[i] + 4;
        }
    }

    /* ------------------------------------------------------------------ */
    /* 动作                                                               */
    /* ------------------------------------------------------------------ */

    private List<RecipeDoc.RecipeEntry> currentRecipes() {
        return this.selectedCategory == null
                ? List.of() : this.workspace.getRecipesForCategory(this.selectedCategory);
    }

    private void addRecipe() {
        if (this.selectedCategory == null) {
            this.status = "请先选择或新建一个分类。";
            this.statusColor = 0xFFFFC000;
            return;
        }
        RecipeDoc.RecipeEntry e = new RecipeDoc.RecipeEntry();
        this.workspace.getRecipes().add(e);
        this.workspace.moveRecipeToCategory(e, this.selectedCategory);
        this.selected = this.workspace.getRecipesForCategory(this.selectedCategory).indexOf(e);
        this.listScroll = Math.max(0, this.selected - geom().recListRows + 1);
        this.effScroll = 0;
        this.dirty = true;
        this.status = "已新建配方到分类「" + this.selectedCategory.displayName + "」。点「保存并生效」写入。";
        this.statusColor = 0xFF9FC5FF;
        rebuild();
    }

    private void duplicateRecipe() {
        List<RecipeDoc.RecipeEntry> cat = currentRecipes();
        if (this.selected < 0 || this.selected >= cat.size()) {
            return;
        }
        RecipeDoc.RecipeEntry src = cat.get(this.selected);
        RecipeDoc.RecipeEntry copy = src.copy();
        this.workspace.getRecipes().add(copy);
        this.workspace.moveRecipeToCategory(copy, this.workspace.categoryOf(src));
        this.selected = this.workspace.getRecipesForCategory(this.workspace.categoryOf(copy)).indexOf(copy);
        this.dirty = true;
        this.status = "已复制配方。";
        this.statusColor = 0xFF9FC5FF;
        rebuild();
    }

    private void removeRecipe() {
        List<RecipeDoc.RecipeEntry> cat = currentRecipes();
        if (this.selected < 0 || this.selected >= cat.size()) {
            return;
        }
        this.workspace.getRecipes().remove(cat.get(this.selected));
        if (this.selected >= currentRecipes().size()) {
            this.selected = currentRecipes().size() - 1;
        }
        this.dirty = true;
        this.status = "已删除该配方。点「保存并生效」写入。";
        this.statusColor = 0xFF9FC5FF;
        rebuild();
    }

    private void moveCategory(RecipeDoc.RecipeEntry e) {
        CategoryPickerScreen.open(this, this.workspace.categoryOf(e), target -> {
            this.workspace.moveRecipeToCategory(e, target);
            this.dirty = true;
            if (this.selectedCategory != null && !this.selectedCategory.file.equals(target.file)) {
                this.selectedCategory = target;
            }
            this.selected = -1;
            this.status = "已把配方挪到分类「" + target.displayName + "」。";
            this.statusColor = 0xFF9FC5FF;
            rebuild();
        });
    }

    private void createCategory() {
        this.minecraft.setScreen(new PromptScreen("新建分类（文件名）", List.of(
                        "每个分类对应 config/" + RecipeWorkspace.MPE_CONFIG_SUBDIR + "/ 下一个 JSON 文件。",
                        "文件名只能包含字母、数字、下划线或连字符 (a-z,0-9,_,-)。"),
                "category1",
                text -> {
                    try {
                        RecipeWorkspace.Category c = this.workspace.createCategory(text.trim());
                        this.selectedCategory = c;
                        this.selected = -1;
                        this.listScroll = 0;
                        this.dirty = true;
                        this.status = "已新建分类「" + c.displayName + "」。";
                        this.statusColor = 0xFF9FC5FF;
                        return null;
                    } catch (Exception ex) {
                        return ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    }
                },
                this));
    }

    private void renameCategory() {
        if (this.selectedCategory == null || this.selectedCategory.isDefault) {
            return;
        }
        this.minecraft.setScreen(new PromptScreen("重命名分类（改文件名）", List.of(
                        "将文件 " + this.selectedCategory.file.getFileName() + " 改名为新文件名（不含 .json）。",
                        "该分类内的配方会随之归属。"),
                this.selectedCategory.displayName,
                text -> {
                    try {
                        this.workspace.renameCategory(this.selectedCategory, text.trim());
                        this.selectedCategory = this.workspace.getCategories().stream()
                                .filter(c -> c.displayName.equals(text.trim()))
                                .findFirst().orElse(this.selectedCategory);
                        this.dirty = true;
                        this.status = "分类已重命名。";
                        this.statusColor = 0xFF9FC5FF;
                        return null;
                    } catch (Exception ex) {
                        return ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    }
                },
                this));
    }

    private void deleteCategory() {
        if (this.selectedCategory == null || this.selectedCategory.isDefault) {
            return;
        }
        String name = this.selectedCategory.displayName;
        this.minecraft.setScreen(new ConfirmScreen(ok -> {
            if (ok) {
                try {
                    this.workspace.deleteCategory(this.selectedCategory);
                    this.selectedCategory = this.workspace.getCategories().stream().filter(c -> c.isDefault).findFirst().orElse(null);
                    this.selected = -1;
                    this.listScroll = 0;
                    this.dirty = true;
                    this.status = "已删除分类「" + name + "」，其配方移回默认分类。";
                    this.statusColor = 0xFF9FC5FF;
                } catch (Exception ex) {
                    this.status = "删除失败：" + ex.getMessage();
                    this.statusColor = 0xFFFF5555;
                }
                rebuild();
            } else {
                this.minecraft.setScreen(this);
            }
        }, Component.literal("删除分类"),
                Component.literal("确定删除分类「" + name + "」吗？其中的配方会移回默认分类。")));
    }

    private void saveAndApply() {
        int bad = 0;
        String firstErr = "";
        for (RecipeDoc.RecipeEntry e : this.workspace.getRecipes()) {
            List<String> errs = RecipeDoc.validate(e);
            if (!errs.isEmpty()) {
                bad++;
                if (firstErr.isEmpty()) {
                    firstErr = errs.get(0);
                }
            }
        }
        if (bad > 0) {
            this.status = bad + " 条配方校验失败，未保存。例：" + firstErr;
            this.statusColor = 0xFFFF5555;
            return;
        }
        try {
            List<Path> written = this.workspace.saveAll();
            this.dirty = false;
            this.status = "已保存 " + this.workspace.getRecipes().size() + " 条配方 → " + written.size() + " 个文件，并已生效。";
            this.statusColor = 0xFF7FFF7F;
            applyAfterSave();
        } catch (Exception ex) {
            this.status = "保存失败：" + ex.getMessage();
            this.statusColor = 0xFFFF5555;
        }
        rebuild();
    }

    private void applyAfterSave() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isSingleplayer()) {
            MinecraftServer server = mc.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> {
                    PotionBrewingSystem.reloadRecipes();
                    PotionBrewingSystem.invalidateCreativeTabCache();
                    CreativeTabRefresher.refreshNow();
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        PacketDistributor.sendToPlayer(player, new MPEReloadPayload());
                    }
                });
                return;
            }
        }
        // 联机（或取不到本地服务端）：只重载本地客户端
        PotionBrewingSystem.reloadRecipes();
        PotionBrewingSystem.invalidateCreativeTabCache();
        CreativeTabRefresher.refreshNow();
        this.status += "（联机时：服务端需同步服务器上的配置文件后执行 /mpe reload）";
    }

    private void reloadAll() {
        this.workspace.loadAll();
        List<RecipeWorkspace.Category> cats = this.workspace.getCategories();
        this.selectedCategory = cats.isEmpty() ? null : cats.get(0);
        this.selected = -1;
        this.listScroll = 0;
        this.catScroll = 0;
        this.effScroll = 0;
        this.dirty = false;
        this.status = "已从磁盘重新载入。";
        this.statusColor = 0xFF9FC5FF;
        rebuild();
    }

    private void openImport() {
        this.minecraft.setScreen(new PromptScreen("导入 JSON 配方", List.of(
                        "把文件里的 recipes 并入编辑区（内容重复的自动跳过），归属 " + RecipeWorkspace.DEFAULT_FILE_NAME + "。",
                        "路径：① 纯文件名 → config/more_potion_effects/ 下查找；",
                        "      ② 相对路径 → 相对游戏目录；③ 绝对路径。"),
                "example_recipes.json",
                text -> {
                    try {
                        Path p = this.workspace.resolveInputPath(text);
                        int n = this.workspace.importFrom(p);
                        this.status = "已导入 " + n + " 条（来自 " + p.getFileName() + "）。点「保存并生效」写入。";
                        this.statusColor = 0xFF9FC5FF;
                        this.dirty = true;
                        return null;
                    } catch (Exception ex) {
                        return ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    }
                },
                this));
    }

    private void openExport() {
        this.minecraft.setScreen(new PromptScreen("导出 JSON 配方", List.of(
                        "把当前全部配方写成一个独立 JSON 快照文件（不影响现有配置文件）。",
                        "路径规则同导入。"),
                "my_recipes.json",
                text -> {
                    try {
                        Path p = this.workspace.resolveInputPath(text);
                        int n = this.workspace.exportTo(p);
                        this.status = "已导出 " + n + " 条 → " + p.toAbsolutePath();
                        this.statusColor = 0xFF9FC5FF;
                        return null;
                    } catch (Exception ex) {
                        return ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    }
                },
                this));
    }

    private void requestClose() {
        if (this.dirty) {
            this.minecraft.setScreen(new ConfirmScreen(
                    ok -> this.minecraft.setScreen(ok ? null : this),
                    Component.literal("关闭编辑器"),
                    Component.literal("有未保存的修改。确定不保存就关闭吗？")));
        } else {
            this.minecraft.setScreen(null);
        }
    }

    /* ------------------------------------------------------------------ */
    /* 渲染                                                               */
    /* ------------------------------------------------------------------ */

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        Geom geo = geom();
        PickerScreenBase.ScreenLayout.drawPanel(g, geo.gx, geo.gy, geo.gw, geo.gh);
        int sepX = geo.gx + geo.lw;
        g.fill(sepX, geo.gy, sepX + 1, geo.contentBottom, 0xFF404040);
        g.fill(geo.gx, geo.catListTop, geo.gx + geo.lw, geo.catListBottom, 0x60000000);
        g.fill(geo.gx, geo.recListTop, geo.gx + geo.lw, geo.recListBottom, 0x60000000);
        g.fill(geo.detX, geo.effTop, geo.detX + geo.detW, geo.effBottom, 0x60000000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        Geom geo = geom();
        List<RecipeDoc.RecipeEntry> catRecipes = this.selectedCategory == null
                ? List.of() : this.workspace.getRecipesForCategory(this.selectedCategory);

        // 分类标题
        g.drawString(this.font, "分类 (" + this.workspace.getCategories().size() + ")", geo.gx + 4, geo.catHeadY, 0xFFFFFFFF, false);

        // 配方标题
        String curCat = this.selectedCategory == null ? "-" : this.selectedCategory.displayName;
        String recHead = "配方: " + truncate(curCat, geo.lw - 70) + " (" + catRecipes.size() + ")";
        g.drawString(this.font, recHead, geo.gx + 4, geo.recHeadY, 0xFFFFFFFF, false);

        int problems = this.workspace.problemCount();
        if (problems > 0 && this.selected >= 0 && this.selected < catRecipes.size()) {
            String p = problems + " 条有问题";
            g.drawString(this.font, p, geo.gx + geo.lw - this.font.width(p) - 4, geo.recHeadY, 0xFFFF8888, false);
        }

        if (this.selected >= 0 && this.selected < catRecipes.size()) {
            renderDetail(g, geo, catRecipes.get(this.selected), this.selectedCategory);
        } else {
            g.drawString(this.font, "← 选择或新建一条配方后编辑", geo.detX + 8, geo.gy + 40, 0xFFB0B0B0, false);
            if (this.workspace.getRecipes().isEmpty()) {
                g.drawString(this.font, "当前目录 config/" + RecipeWorkspace.MPE_CONFIG_SUBDIR + "/ 没有可编辑的配方。",
                        geo.detX + 8, geo.gy + 56, 0xFFB0B0B0, false);
                g.drawString(this.font, "点「+新建配方」创建，或点「导入…」从 JSON 读入。",
                        geo.detX + 8, geo.gy + 68, 0xFFB0B0B0, false);
            }
        }

        String st = status();
        int x0 = geo.detX + 4;
        int maxW = Math.max(40, geo.gx + geo.gw - 4 - x0);
        g.drawString(this.font, truncate(st, maxW), x0, geo.barY - 10, this.statusColor, false);

        String t = (this.dirty ? "* " : "") + "酿造配方编辑器";
        g.drawString(this.font, t, geo.gx + geo.gw - this.font.width(t) - 4, geo.catHeadY,
                this.dirty ? 0xFFFFE066 : 0xFFFFFFFF, false);
    }

    private void renderDetail(GuiGraphics g, Geom geo, RecipeDoc.RecipeEntry e, RecipeWorkspace.Category cat) {
        g.drawString(this.font, "配方详情 #" + (this.selected + 1), geo.detX + 4, geo.detHeadY + 2, 0xFFFFFFFF, false);
        RecipeWorkspace.Category cur = cat == null ? this.workspace.categoryOf(e) : cat;
        g.drawString(this.font, "分类: " + truncate(cur.displayName, geo.detW - 74), geo.detX + 4, geo.dtCatY + 3, 0xFFC8C8C8, false);

        int px = geo.detX + 6;
        int cw = (geo.detW - 18) / 2;
        int x1 = px;
        int x2 = px + cw + 6;
        int lb = 0xFFC8C8C8;
        g.drawString(this.font, "基础药水 (base_potion)", x1, geo.metaTop, lb, false);
        g.drawString(this.font, "酿造材料 (ingredient)", x2, geo.metaTop, lb, false);
        g.drawString(this.font, "自定义名 (custom_name)", x1, geo.metaTop + 25, lb, false);
        g.drawString(this.font, "自定义基底 (custom_base)", x2, geo.metaTop + 25, lb, false);
        g.drawString(this.font, "颜色 (hex)", x1, geo.metaTop + 50, lb, false);

        ItemStack ing = RecipePreview.ingredientStack(e.ingredient);
        if (!ing.isEmpty()) {
            g.renderItem(ing, x2 + cw - 16, geo.metaTop - 3);
        }

        Optional<Integer> color = RecipeDoc.parseHexColor(e.customColor);
        int swatchX = x1 + cw - 16;
        int swatchY = geo.metaTop + 50 + 10;
        g.fill(swatchX, swatchY, swatchX + 13, swatchY + 13, 0xFF000000 | color.orElse(0xFF333333));
        g.renderOutline(swatchX, swatchY, 13, 13, 0xFFFFFFFF);

        g.drawString(this.font, "产物预览", x2, geo.metaTop + 50, lb, false);
        ItemStack preview = RecipePreview.build(e);
        if (!preview.isEmpty()) {
            g.renderItem(preview, x2 + 2, geo.metaTop + 58);
        }

        List<String> errs = RecipeDoc.validate(e);
        if (errs.isEmpty()) {
            g.drawString(this.font, "OK 校验通过", geo.detX + 6, geo.effHeadY, 0xFF7FFF7F, false);
        } else {
            g.drawString(this.font, truncate("! " + errs.get(0), geo.detW - 12), geo.detX + 6, geo.effHeadY, 0xFFFF8888, false);
            if (errs.size() > 1) {
                g.drawString(this.font, "+" + (errs.size() - 1) + " 个问题…", geo.detX + 6, geo.effHeadY + 10, 0xFFFF8888, false);
            }
        }

        int n = e.effects.size();
        g.drawString(this.font, "效果 " + n + " / " + RecipeDoc.EFFECT_SLOT_COUNT, geo.detX + 6, geo.effTitleY + 2, 0xFFFFFFFF, false);

        int xEnd = geo.detX + geo.detW - 6;
        int delW = 18;
        int flagW = 14;
        int selW = 28;
        int ampW = 32;
        int durW = 44;
        int flagX = xEnd - delW - (flagW * 3 + 4) - 5;
        int colDur = flagX - selW - ampW - durW - 8;
        int colAmp = flagX - selW - ampW - 4;
        g.drawString(this.font, "效果ID", px + 14, geo.effCapY, 0xFFA0A0A0, false);
        g.drawString(this.font, "时长", colDur + 8, geo.effCapY, 0xFFA0A0A0, false);
        g.drawString(this.font, "强度", colAmp + 4, geo.effCapY, 0xFFA0A0A0, false);
        g.drawString(this.font, "A V I", flagX + 2, geo.effCapY, 0xFFA0A0A0, false);

        if (n == 0) {
            g.drawString(this.font, "（该配方不含自定义效果）", px + 14, geo.effTop + 4, 0xFFB0B0B0, false);
        }
        for (int r = 0; r < geo.effRows; r++) {
            int idx = this.effScroll + r;
            if (idx >= n) {
                break;
            }
            g.drawString(this.font, String.valueOf(idx + 1), px, geo.effTop + r * ROW_H + 4, 0xFFC0C0C0, false);
        }
        if (this.effScroll + geo.effRows < n) {
            g.drawString(this.font, "↓", geo.detX + geo.detW - 11, geo.effBottom - 12, 0xFFC0C0C0, false);
        }
        if (this.effScroll > 0) {
            g.drawString(this.font, "↑", geo.detX + geo.detW - 11, geo.effTop + 2, 0xFFC0C0C0, false);
        }
    }

    private String truncate(String s, int maxPx) {
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

    /* ------------------------------------------------------------------ */
    /* 输入                                                               */
    /* ------------------------------------------------------------------ */

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        Geom geo = geom();
        List<RecipeWorkspace.Category> cats = this.workspace.getCategories();
        // 分类列表滚动
        if (mx >= geo.gx && mx <= geo.gx + geo.lw && my >= geo.catListTop && my <= geo.catListBottom) {
            int max = Math.max(0, cats.size() - geo.catListRows);
            if (max > 0) {
                this.catScroll = Math.max(0, Math.min(max, this.catScroll - (int) dy));
                rebuild();
                return true;
            }
        }
        // 配方列表滚动
        List<RecipeDoc.RecipeEntry> catRecipes = this.selectedCategory == null
                ? List.of() : this.workspace.getRecipesForCategory(this.selectedCategory);
        if (mx >= geo.gx && mx <= geo.gx + geo.lw && my >= geo.recListTop && my <= geo.recListBottom) {
            int max = Math.max(0, catRecipes.size() - geo.recListRows);
            if (max > 0) {
                this.listScroll = Math.max(0, Math.min(max, this.listScroll - (int) dy));
                rebuild();
                return true;
            }
        }
        // 效果列表滚动
        if (this.selected >= 0 && this.selected < catRecipes.size()) {
            int n = catRecipes.get(this.selected).effects.size();
            int max = Math.max(0, n - geo.effRows);
            if (mx >= geo.detX && mx <= geo.detX + geo.detW && my >= geo.effTop && my <= geo.effBottom && max > 0) {
                this.effScroll = Math.max(0, Math.min(max, this.effScroll - (int) dy));
                rebuild();
                return true;
            }
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            requestClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}