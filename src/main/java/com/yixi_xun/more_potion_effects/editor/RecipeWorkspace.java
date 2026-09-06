package com.yixi_xun.more_potion_effects.editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 配方编辑器工作区：读写 {@code config/more_potion_effects/*.json}
 * （与 {@code PotionBrewingSystem} 的加载目录一致）。
 * <p>
 * 以「文件」为粒度：
 *  - 载入：合并目录内全部 *.json，每条配方记住来源文件；
 *  - 保存：按来源文件分组回写；新建/导入配方归入默认文件 gui_recipes.json；
 *          本会话管理过但被删空的文件写空数组，防止"删了又复活"；
 *  - 导入/导出：跨文件搬运配方（导入按内容去重）。
 */
public final class RecipeWorkspace {

    public static final String MPE_CONFIG_SUBDIR = "more_potion_effects";
    public static final String DEFAULT_FILE_NAME = "gui_recipes.json";

    private final Path baseDir;
    private final Path defaultFile;
    private final List<RecipeDoc.RecipeEntry> recipes = new ArrayList<>();
    private final List<String> loadProblems = new ArrayList<>();
    /** 本会话管理过的文件（loadAll 成功解析的 + saveAll 写过的）。 */
    private final Set<Path> managedFiles = new LinkedHashSet<>();

    public RecipeWorkspace() {
        // 与 PotionBrewingSystem 的 Paths.get("config","more_potion_effects") 一致
        this.baseDir = Paths.get("config", MPE_CONFIG_SUBDIR);
        this.defaultFile = this.baseDir.resolve(DEFAULT_FILE_NAME);
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public List<RecipeDoc.RecipeEntry> getRecipes() {
        return recipes;
    }

    public List<String> getLoadProblems() {
        return loadProblems;
    }

    public int problemCount() {
        int c = 0;
        for (RecipeDoc.RecipeEntry e : recipes) {
            if (RecipeDoc.hasProblems(e)) {
                c++;
            }
        }
        return c;
    }

    /** 重新载入目录下全部 JSON（丢弃未保存的编辑）。 */
    public void loadAll() {
        recipes.clear();
        loadProblems.clear();
        managedFiles.clear();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException io) {
            loadProblems.add("无法创建目录 " + baseDir + " : " + io.getMessage());
            return;
        }
        List<Path> files;
        try (Stream<Path> paths = Files.list(baseDir)) {
            files = paths.filter(p -> p.toString().endsWith(RecipeDoc.FILE_SUFFIX)).sorted().toList();
        } catch (IOException io) {
            loadProblems.add("读取目录失败 " + baseDir + " : " + io.getMessage());
            return;
        }
        for (Path file : files) {
            try {
                String content = Files.readString(file);
                List<RecipeDoc.RecipeEntry> parsed = RecipeDoc.parse(content);
                managedFiles.add(file.toAbsolutePath().normalize());
                for (RecipeDoc.RecipeEntry e : parsed) {
                    e.sourceFile = file;
                    recipes.add(e);
                }
            } catch (Exception ex) {
                loadProblems.add(file.getFileName() + " 解析失败：" + ex.getMessage());
            }
        }
    }

    /**
     * 保存全部配方（按来源文件分组回写）。调用前应保证所有配方通过校验。
     *
     * @return 实际写入的文件清单
     */
    public List<Path> saveAll() throws IOException {
        Map<Path, List<RecipeDoc.RecipeEntry>> byFile = new LinkedHashMap<>();
        Path baseAbs = baseDir.toAbsolutePath().normalize();
        for (RecipeDoc.RecipeEntry e : recipes) {
            Path target = (e.sourceFile == null ? defaultFile : e.sourceFile).toAbsolutePath().normalize();
            if (!target.startsWith(baseAbs)) {
                target = defaultFile.toAbsolutePath().normalize();
            }
            byFile.computeIfAbsent(target, k -> new ArrayList<>()).add(e);
        }
        Files.createDirectories(baseDir);
        List<Path> written = new ArrayList<>();
        for (Map.Entry<Path, List<RecipeDoc.RecipeEntry>> me : byFile.entrySet()) {
            List<RecipeDoc.RecipeEntry> list = me.getValue();
            if (list.isEmpty()) {
                continue;
            }
            Path file = me.getKey();
            Files.writeString(file, RecipeDoc.toFileText(list));
            managedFiles.add(file);
            written.add(file);
            for (RecipeDoc.RecipeEntry e : list) {
                e.sourceFile = file;
            }
        }
        // 管理过但当前已无配方的文件：写空数组，避免下次启动"复活"
        for (Path f : managedFiles) {
            if (!byFile.containsKey(f) && Files.exists(f)) {
                Files.writeString(f, RecipeDoc.toFileText(List.of()));
                written.add(f);
            }
        }
        return written;
    }

    /** 从外部 JSON 导入配方（按规范化内容去重），新配方归属默认文件。返回新增数量。 */
    public int importFrom(Path file) throws IOException {
        String content = Files.readString(file);
        List<RecipeDoc.RecipeEntry> parsed = RecipeDoc.parse(content);
        Set<String> existing = new HashSet<>();
        for (RecipeDoc.RecipeEntry e : recipes) {
            existing.add(RecipeDoc.canonical(e));
        }
        int added = 0;
        for (RecipeDoc.RecipeEntry e : parsed) {
            if (existing.add(RecipeDoc.canonical(e))) {
                e.sourceFile = null;
                recipes.add(e);
                added++;
            }
        }
        return added;
    }

    /** 把当前全部配方导出为单个 JSON 快照文件。返回导出数量。 */
    public int exportTo(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, RecipeDoc.toFileText(recipes));
        return recipes.size();
    }

    /**
     * 路径解析：绝对路径直接用；含目录分隔符的相对路径相对游戏根目录；
     * 纯文件名视为 config/more_potion_effects/ 下的文件。
     */
    public Path resolveInputPath(String raw) {
        String t = raw == null ? "" : raw.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException("路径不能为空");
        }
        Path p = Path.of(t);
        if (p.isAbsolute()) {
            return p;
        }
        if (t.contains("/") || t.contains("\\")) {
            return Path.of("").toAbsolutePath().resolve(p).normalize();
        }
        return baseDir.resolve(t).normalize();
    }

    /* ------------------------------------------------------------------ */
    /* 分类（= 一个 JSON 文件）                                            */
    /* ------------------------------------------------------------------ */

    /** 一个配方分类，对应 {@code config/more_potion_effects/} 下的一个 JSON 文件。 */
    public static final class Category {
        public final Path file;
        public final String displayName;
        public final boolean isDefault;

        Category(Path file, String displayName, boolean isDefault) {
            this.file = file;
            this.displayName = displayName;
            this.isDefault = isDefault;
        }
    }

    private Path normalize(Path p) {
        return p.toAbsolutePath().normalize();
    }

    private Path catFile(String fileStem) {
        String stem = fileStem == null ? "" : fileStem.trim();
        if (stem.isEmpty() || !stem.matches("[A-Za-z0-9_\\-]+")) {
            throw new IllegalArgumentException("分类文件名只能包含字母、数字、下划线或连字符");
        }
        return baseDir.resolve(stem + RecipeDoc.FILE_SUFFIX).toAbsolutePath().normalize();
    }

    private static String stem(Path file) {
        String n = file.getFileName().toString();
        return n.endsWith(RecipeDoc.FILE_SUFFIX) ? n.substring(0, n.length() - RecipeDoc.FILE_SUFFIX.length()) : n;
    }

    /** 该配方所属分类（缺 sourceFile 或 sourceFile 落在默认文件 → 默认分类）。 */
    public Category categoryOf(RecipeDoc.RecipeEntry e) {
        Path def = normalize(defaultFile);
        Path f = e.sourceFile == null ? null : normalize(e.sourceFile);
        if (f == null || f.equals(def)) {
            return new Category(def, DEFAULT_FILE_NAME, true);
        }
        return new Category(f, stem(f), false);
    }

    /** 目录下全部分类（含默认分类；空文件也算），按文件名排序。 */
    public List<Category> getCategories() {
        List<Category> cats = new ArrayList<>();
        Path def = normalize(defaultFile);
        try {
            if (Files.exists(baseDir)) {
                try (Stream<Path> paths = Files.list(baseDir)) {
                    paths.filter(p -> p.toString().endsWith(RecipeDoc.FILE_SUFFIX))
                            .map(this::normalize)
                            .sorted(Comparator.comparing(RecipeWorkspace::stem))
                            .forEach(p -> cats.add(new Category(p, stem(p), p.equals(def))));
                }
            }
        } catch (IOException io) {
            loadProblems.add("读取分类失败 " + baseDir + " : " + io.getMessage());
        }
        if (cats.stream().noneMatch(c -> c.file.equals(def))) {
            cats.addFirst(new Category(def, DEFAULT_FILE_NAME, true));
        }
        return cats;
    }

    /** 指定分类下的配方，保持当前列表顺序（数组顺序 = 文件内顺序 = 创造栏顺序）。 */
    public List<RecipeDoc.RecipeEntry> getRecipesForCategory(Category c) {
        List<RecipeDoc.RecipeEntry> out = new ArrayList<>();
        Path cf = normalize(c.file);
        for (RecipeDoc.RecipeEntry e : recipes) {
            Path ef = e.sourceFile == null ? normalize(defaultFile) : normalize(e.sourceFile);
            if (ef.equals(cf)) {
                out.add(e);
            }
        }
        return out;
    }

    /** 新建一个空分类（写一个空 recipes 文件）。返回新分类。 */
    public Category createCategory(String fileStem) throws IOException {
        Path file = catFile(fileStem);
        if (Files.exists(file)) {
            throw new IOException("分类 " + fileStem + " 已存在");
        }
        Files.createDirectories(baseDir);
        Files.writeString(file, RecipeDoc.toFileText(List.of()));
        managedFiles.add(normalize(file));
        boolean def = file.equals(normalize(defaultFile));
        return new Category(file, fileStem, def);
    }

    /** 重命名分类（物理重命名文件，并更新其配方归属）。默认分类不可重命名。 */
    public void renameCategory(Category c, String newStem) throws IOException {
        if (c.isDefault) {
            throw new IOException("默认分类不可重命名");
        }
        if (newStem == null || newStem.trim().isEmpty()) {
            throw new IOException("文件名不能为空");
        }
        Path target = catFile(newStem);
        Path source = normalize(c.file);
        if (target.equals(source)) {
            return;
        }
        if (Files.exists(target)) {
            throw new IOException("分类 " + newStem + " 已存在");
        }
        Files.createDirectories(baseDir);
        if (Files.exists(source)) {
            Files.move(source, target);
        }
        managedFiles.remove(source);
        managedFiles.add(target);
        for (RecipeDoc.RecipeEntry e : recipes) {
            if (e.sourceFile != null && normalize(e.sourceFile).equals(source)) {
                e.sourceFile = target;
            }
        }
    }

    /** 删除分类（物理删除文件，其中配方移回默认分类）。默认分类不可删除。 */
    public void deleteCategory(Category c) throws IOException {
        if (c.isDefault) {
            throw new IOException("默认分类不可删除");
        }
        Path file = normalize(c.file);
        for (RecipeDoc.RecipeEntry e : recipes) {
            if (e.sourceFile != null && normalize(e.sourceFile).equals(file)) {
                e.sourceFile = null;
            }
        }
        if (Files.exists(file)) {
            Files.delete(file);
        }
        managedFiles.remove(file);
    }

    /** 把配方移动到目标分类：改归属文件，并把条目在 master 列表中就近安放到目标分类块内。 */
    public void moveRecipeToCategory(RecipeDoc.RecipeEntry e, Category target) {
        Category cur = categoryOf(e);
        if (cur.file.equals(normalize(target.file))) {
            return;
        }
        // 先按新归属重写 sourceFile
        e.sourceFile = target.isDefault ? null : normalize(target.file);
        // 从 master 移除后，插到目标分类最后一个成员之后（保持分类块连续）
        recipes.remove(e);
        int insert = recipes.size();
        for (int i = 0; i < recipes.size(); i++) {
            if (categoryOf(recipes.get(i)).file.equals(normalize(target.file))) {
                insert = i + 1;
            }
        }
        recipes.add(insert, e);
    }

    /** 在分类内上移配方（只与同分类相邻条目交换，保持分类块连续）。 */
    public void moveRecipeUp(RecipeDoc.RecipeEntry e) {
        int idx = recipes.indexOf(e);
        if (idx <= 0) {
            return;
        }
        if (categoryOf(recipes.get(idx - 1)).file.equals(categoryOf(e).file)) {
            recipes.remove(idx);
            recipes.add(idx - 1, e);
        }
    }

    /** 在分类内下移配方（只与同分类相邻条目交换，保持分类块连续）。 */
    public void moveRecipeDown(RecipeDoc.RecipeEntry e) {
        int idx = recipes.indexOf(e);
        if (idx < 0 || idx >= recipes.size() - 1) {
            return;
        }
        if (categoryOf(recipes.get(idx + 1)).file.equals(categoryOf(e).file)) {
            recipes.remove(idx);
            recipes.add(idx + 1, e);
        }
    }
}
