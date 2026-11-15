package areahint.i18n;

import areahint.AreashintClient;
import areahint.config.ClientConfig;
import areahint.file.FileManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * 国际化管理器
 * 负责加载和管理翻译文件
 */
public class I18nManager {
    private static final String LANG_FOLDER = "lang";
    private static final String DEFAULT_LANGUAGE = "zh_cn";
    private static final String FALLBACK_LANGUAGE = "en_us";

    // 当前语言的翻译映射
    private static Map<String, String> translations = new HashMap<>();
    // 英语回退翻译，避免非核心语言文件缺少新key时直接显示key本身
    private static Map<String, String> fallbackTranslations = new HashMap<>();
    // 当前语言
    private static String currentLanguage = DEFAULT_LANGUAGE;

    /**
     * 初始化i18n系统
     */
    public static void init() {
        LanguageFileSynchronizer.syncBundledLanguageFiles();
        loadLanguage(ClientConfig.getLanguage());
    }

    /**
     * 获取语言文件夹路径
     */
    public static Path getLangFolder() {
        return FileManager.getConfigFolder().resolve(LANG_FOLDER);
    }

    /**
     * 解析请求语言，若不存在则回退到英语
     * @param requestedLanguage 请求的语言代码
     * @return 实际应加载的语言代码
     */
    public static String resolveLanguageOrFallback(String requestedLanguage) {
        String resolvedLanguage = requestedLanguage;
        if (resolvedLanguage == null || resolvedLanguage.isEmpty()) {
            resolvedLanguage = ClientConfig.getLanguage();
        }
        if (resolvedLanguage == null || resolvedLanguage.isEmpty()) {
            resolvedLanguage = DEFAULT_LANGUAGE;
        }

        Path requestedFile = getLangFolder().resolve(resolvedLanguage + ".json");
        if (Files.exists(requestedFile)) {
            return resolvedLanguage;
        }

        AreashintClient.LOGGER.warn("语言文件不存在: " + resolvedLanguage + ".json，回退到 " + FALLBACK_LANGUAGE);
        Path fallbackFile = getLangFolder().resolve(FALLBACK_LANGUAGE + ".json");
        if (Files.exists(fallbackFile)) {
            return FALLBACK_LANGUAGE;
        }

        AreashintClient.LOGGER.warn("回退语言文件也不存在: " + FALLBACK_LANGUAGE + ".json，将使用空翻译");
        return FALLBACK_LANGUAGE;
    }

    /**
     * 加载指定语言
     * @param language 语言代码（如 zh_cn, en_us）
     * @return 实际加载生效的语言代码
     */
    public static String loadLanguage(String language) {
        fallbackTranslations = loadTranslations(resolveLanguageFile(FALLBACK_LANGUAGE));
        String resolvedLanguage = resolveLanguageOrFallback(language);
        Path langFile = getLangFolder().resolve(resolvedLanguage + ".json");

        if (Files.notExists(langFile)) {
            translations = new HashMap<>();
            currentLanguage = resolvedLanguage;
            return resolvedLanguage;
        }

        try {
            String json = Files.readString(langFile, StandardCharsets.UTF_8);
            translations = parseTranslations(json);
            currentLanguage = resolvedLanguage;
            AreashintClient.LOGGER.info("已加载语言: " + resolvedLanguage + " (" + translations.size() + " 条翻译)");
        } catch (Exception e) {
            AreashintClient.LOGGER.error("加载语言文件失败: " + e.getMessage());
            translations = new HashMap<>();
            currentLanguage = resolvedLanguage;
        }

        return resolvedLanguage;
    }

    private static Path resolveLanguageFile(String language) {
        return getLangFolder().resolve(language + ".json");
    }

    private static Map<String, String> loadTranslations(Path langFile) {
        if (Files.notExists(langFile)) {
            return new HashMap<>();
        }
        try {
            String json = Files.readString(langFile, StandardCharsets.UTF_8);
            return parseTranslations(json);
        } catch (Exception e) {
            AreashintClient.LOGGER.error("读取语言文件失败: " + langFile + " - " + e.getMessage());
            return new HashMap<>();
        }
    }

    private static Map<String, String> parseTranslations(String json) {
        Map<String, String> parsedTranslations = new HashMap<>();
        JsonObject languageObject = JsonParser.parseString(json).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : languageObject.entrySet()) {
            JsonElement value = entry.getValue();
            if (value != null && !value.isJsonNull()) {
                parsedTranslations.put(entry.getKey(), value.getAsString());
            }
        }
        return parsedTranslations;
    }

    /**
     * 获取翻译文本
     * @param key 翻译键
     * @return 翻译后的文本，如果不存在则返回key本身
     */
    public static boolean hasKey(String key) {
        return translations.containsKey(key) || fallbackTranslations.containsKey(key);
    }

    public static String translate(String key) {
        String value = translations.get(key);
        if (value != null) {
            return value;
        }
        return fallbackTranslations.getOrDefault(key, key);
    }

    /**
     * 获取翻译文本（带参数替换）
     * @param key 翻译键
     * @param args 替换参数（兼容 {0}, {1}, ... 和 %s/%d 等格式）
     */
    public static String translate(String key, Object... args) {
        String text = translate(key);
        if (args == null || args.length == 0) {
            return text;
        }

        String indexedText = replaceIndexedPlaceholders(text, args);
        if (!indexedText.equals(text)) {
            return indexedText;
        }

        try {
            return String.format(Locale.ROOT, text, args);
        } catch (IllegalArgumentException e) {
            return indexedText;
        }
    }

    private static String replaceIndexedPlaceholders(String text, Object... args) {
        String formattedText = text;
        for (int i = 0; i < args.length; i++) {
            formattedText = formattedText.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return formattedText;
    }

    /**
     * 获取所有可用的语言列表
     * @return 语言代码列表（不含.json后缀）
     */
    public static List<String> getAvailableLanguages() {
        List<String> languages = new ArrayList<>();
        Path langDir = getLangFolder();

        if (Files.notExists(langDir)) {
            return languages;
        }

        try (Stream<Path> files = Files.list(langDir)) {
            files.filter(p -> p.toString().endsWith(".json"))
                 .forEach(p -> {
                     String fileName = p.getFileName().toString();
                     languages.add(fileName.substring(0, fileName.length() - 5));
                 });
        } catch (IOException e) {
            AreashintClient.LOGGER.error("扫描语言文件夹失败: " + e.getMessage());
        }

        return languages;
    }

    /**
     * 获取语言文件中定义的语言显示名称
     * @param langCode 语言代码
     * @return 显示名称，如果没有定义则返回语言代码
     */
    public static String getLanguageDisplayName(String langCode) {
        Path langFile = getLangFolder().resolve(langCode + ".json");
        if (Files.notExists(langFile)) {
            return langCode;
        }

        try {
            String json = Files.readString(langFile, StandardCharsets.UTF_8);
            Map<String, String> data = parseTranslations(json);
            if (data != null && data.containsKey("language.name")) {
                return data.get("language.name");
            }
        } catch (Exception e) {
            // 忽略，返回语言代码
        }
        return langCode;
    }

    /**
     * 获取当前语言代码
     */
    public static String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * 获取默认语言代码
     */
    public static String getDefaultLanguage() {
        return DEFAULT_LANGUAGE;
    }
}
