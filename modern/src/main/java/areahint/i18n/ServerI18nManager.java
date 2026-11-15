package areahint.i18n;

import areahint.Areashint;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端国际化管理器
 * 支持多语言加载和按玩家语言翻译
 */
public class ServerI18nManager {
    private static final String DEFAULT_LANGUAGE = "zh_cn";
    private static final String FALLBACK_LANGUAGE = "en_us";

    // 语言代码 -> 翻译映射
    private static final Map<String, Map<String, String>> allTranslations = new HashMap<>();
    // 玩家UUID -> 语言代码
    private static final Map<UUID, String> playerLanguages = new ConcurrentHashMap<>();
    // 服务端日志用的当前语言
    private static Map<String, String> translations = new HashMap<>();
    private static String currentLanguage = DEFAULT_LANGUAGE;

    public static void init() {
        LanguageFileSynchronizer.syncBundledLanguageFiles();
        loadLanguageFile(DEFAULT_LANGUAGE);
        loadLanguageFile(FALLBACK_LANGUAGE);
        translations = allTranslations.getOrDefault(DEFAULT_LANGUAGE, new HashMap<>());
        currentLanguage = DEFAULT_LANGUAGE;
    }

    private static void loadLanguageFile(String language) {
        String path = "assets/areas-hint/lang/" + language + ".json";
        try (InputStream is = ServerI18nManager.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                Areashint.LOGGER.warn("Server lang file not found: " + path);
                return;
            }
            Map<String, String> loaded = parseTranslations(new InputStreamReader(is, StandardCharsets.UTF_8));
            allTranslations.put(language, loaded);
            Areashint.LOGGER.info("Server loaded language: " + language + " (" + loaded.size() + " entries)");
        } catch (Exception e) {
            Areashint.LOGGER.error("Failed to load server lang: " + e.getMessage());
        }
    }

    private static Map<String, String> parseTranslations(InputStreamReader reader) {
        Map<String, String> parsedTranslations = new HashMap<>();
        JsonObject languageObject = JsonParser.parseReader(reader).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : languageObject.entrySet()) {
            JsonElement value = entry.getValue();
            if (value != null && !value.isJsonNull()) {
                parsedTranslations.put(entry.getKey(), value.getAsString());
            }
        }
        return parsedTranslations;
    }

    public static void setPlayerLanguage(UUID playerUuid, String language) {
        playerLanguages.put(playerUuid, language);
        // 确保该语言已加载
        if (!allTranslations.containsKey(language)) {
            loadLanguageFile(language);
        }
    }

    public static void removePlayer(UUID playerUuid) {
        playerLanguages.remove(playerUuid);
    }

    /** 按玩家语言翻译 */
    public static String translateForPlayer(UUID playerUuid, String key) {
        String lang = playerLanguages.getOrDefault(playerUuid, DEFAULT_LANGUAGE);
        Map<String, String> langMap = allTranslations.getOrDefault(lang, translations);
        String value = langMap.get(key);
        if (value != null) {
            return value;
        }
        return getFallbackTranslations().getOrDefault(key, key);
    }

    /** 按玩家语言翻译（带参数） */
    public static String translateForPlayer(UUID playerUuid, String key, Object... args) {
        String text = translateForPlayer(playerUuid, key);
        for (int i = 0; i < args.length; i++) {
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return text;
    }

    /** 服务端日志用（默认语言） */
    public static String translate(String key) {
        String value = translations.get(key);
        if (value != null) {
            return value;
        }
        return getFallbackTranslations().getOrDefault(key, key);
    }

    public static String translate(String key, Object... args) {
        String text = translations.getOrDefault(key, key);
        for (int i = 0; i < args.length; i++) {
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return text;
    }

    public static void loadLanguage(String language) {
        if (language == null || language.isEmpty()) language = DEFAULT_LANGUAGE;
        if (!allTranslations.containsKey(language)) loadLanguageFile(language);
        translations = allTranslations.getOrDefault(language, new HashMap<>());
        currentLanguage = language;
    }

    private static Map<String, String> getFallbackTranslations() {
        if (!allTranslations.containsKey(FALLBACK_LANGUAGE)) {
            loadLanguageFile(FALLBACK_LANGUAGE);
        }
        return allTranslations.getOrDefault(FALLBACK_LANGUAGE, new HashMap<>());
    }

    public static String getCurrentLanguage() {
        return currentLanguage;
    }
}
