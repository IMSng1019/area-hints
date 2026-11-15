package areahint.i18n;

import areahint.Areashint;
import areahint.file.FileManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 语言文件同步器
 * 负责在模组启动时把内置翻译同步到本地 areas-hint/lang 目录。
 */
public final class LanguageFileSynchronizer {
    private static final String LANG_FOLDER = "lang";
    private static final String RESOURCE_LANG_DIR = "assets/areas-hint/lang";
    private static final List<String> FALLBACK_LANGUAGES = List.of("zh_cn", "en_us");

    private LanguageFileSynchronizer() {
    }

    /**
     * 同步所有内置语言文件，本地额外自定义语言文件不会被删除或修改。
     */
    public static void syncBundledLanguageFiles() {
        Path langDir = FileManager.getConfigFolder().resolve(LANG_FOLDER);
        if (!ensureLangFolder(langDir)) {
            return;
        }

        SyncSummary summary = new SyncSummary();
        List<Path> bundledLanguageFiles = findBundledLanguageFiles();
        if (bundledLanguageFiles.isEmpty()) {
            syncFallbackLanguages(langDir, summary);
        } else {
            syncBundledLanguagePaths(langDir, bundledLanguageFiles, summary);
        }

        if (summary.copied > 0 || summary.updated > 0) {
            Areashint.LOGGER.info("语言文件同步完成: 新建 {} 个，更新 {} 个，未变更 {} 个",
                    summary.copied, summary.updated, summary.unchanged);
        }
    }

    private static boolean ensureLangFolder(Path langDir) {
        try {
            if (Files.notExists(langDir)) {
                Files.createDirectories(langDir);
                Areashint.LOGGER.info("已创建语言文件夹: {}", langDir);
            }
            return true;
        } catch (IOException e) {
            Areashint.LOGGER.error("创建语言文件夹失败: {}", e.getMessage());
            return false;
        }
    }

    private static List<Path> findBundledLanguageFiles() {
        List<Path> bundledLanguageFiles = new ArrayList<>();
        var modContainer = FabricLoader.getInstance().getModContainer(Areashint.MOD_ID);
        if (modContainer.isEmpty()) {
            Areashint.LOGGER.warn("未找到模组容器，无法扫描内置语言文件");
            return bundledLanguageFiles;
        }

        var langPath = modContainer.get().findPath(RESOURCE_LANG_DIR);
        if (langPath.isEmpty()) {
            Areashint.LOGGER.warn("未找到内置语言目录: {}", RESOURCE_LANG_DIR);
            return bundledLanguageFiles;
        }

        try (Stream<Path> files = Files.list(langPath.get())) {
            files.filter(LanguageFileSynchronizer::isJsonLanguageFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(bundledLanguageFiles::add);
        } catch (IOException e) {
            Areashint.LOGGER.error("扫描内置语言文件失败: {}", e.getMessage());
        }
        return bundledLanguageFiles;
    }

    private static boolean isJsonLanguageFile(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json");
    }

    private static void syncBundledLanguagePaths(Path langDir, List<Path> bundledLanguageFiles, SyncSummary summary) {
        for (Path bundledLanguageFile : bundledLanguageFiles) {
            String fileName = bundledLanguageFile.getFileName().toString();
            try {
                byte[] bundledContent = Files.readAllBytes(bundledLanguageFile);
                recordResult(syncLanguageContent(langDir.resolve(fileName), bundledContent), summary);
            } catch (Exception e) {
                Areashint.LOGGER.error("同步语言文件失败: {} - {}", fileName, e.getMessage());
            }
        }
    }

    private static void syncFallbackLanguages(Path langDir, SyncSummary summary) {
        for (String language : FALLBACK_LANGUAGES) {
            String fileName = language + ".json";
            String resourcePath = RESOURCE_LANG_DIR + "/" + fileName;
            try (InputStream inputStream = LanguageFileSynchronizer.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    Areashint.LOGGER.warn("内置语言文件不存在: {}", resourcePath);
                    continue;
                }
                recordResult(syncLanguageContent(langDir.resolve(fileName), inputStream.readAllBytes()), summary);
            } catch (Exception e) {
                Areashint.LOGGER.error("同步备用语言文件失败: {} - {}", fileName, e.getMessage());
            }
        }
    }

    private static SyncResult syncLanguageContent(Path targetFile, byte[] bundledContent) throws IOException {
        if (Files.exists(targetFile) && Files.isDirectory(targetFile)) {
            throw new IOException("目标路径是文件夹，无法写入语言文件");
        }
        if (Files.notExists(targetFile)) {
            Files.write(targetFile, bundledContent);
            return SyncResult.COPIED;
        }

        byte[] localContent = Files.readAllBytes(targetFile);
        if (Arrays.equals(localContent, bundledContent)) {
            return SyncResult.UNCHANGED;
        }

        Files.write(targetFile, bundledContent);
        return SyncResult.UPDATED;
    }

    private static void recordResult(SyncResult result, SyncSummary summary) {
        switch (result) {
            case COPIED -> summary.copied++;
            case UPDATED -> summary.updated++;
            case UNCHANGED -> summary.unchanged++;
        }
    }

    private enum SyncResult {
        COPIED,
        UPDATED,
        UNCHANGED
    }

    private static final class SyncSummary {
        private int copied;
        private int updated;
        private int unchanged;
    }
}
