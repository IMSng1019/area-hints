package areahint.config;

import areahint.AreashintClient;
import areahint.data.ConfigData;
import areahint.file.FileManager;
import areahint.render.VulkanModCompat;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 客户端配置类
 * 用于读写和管理客户端配置
 */
public class ClientConfig {
    // 当前配置
    private static ConfigData config = new ConfigData();
    // 配置是否已经加载
    private static boolean loaded = false;
    
    /**
     * 初始化配置
     */
    public static void init() {
        Path configFile = AreashintClient.getConfigFile();
        try {
            // 如果配置文件不存在，创建默认配置
            FileManager.createDefaultConfigFile(configFile);
        } catch (IOException e) {
            AreashintClient.LOGGER.error("创建配置文件失败: " + e.getMessage());
        }
        
        // 加载配置
        load();
    }
    
    /**
     * 加载配置
     */
    public static void load() {
        Path configFile = AreashintClient.getConfigFile();
        config = FileManager.readConfigData(configFile);
        loaded = true;
        AreashintClient.LOGGER.info("已加载配置: 频率={}, 渲染方式={}, 域名标题样式={}, 域名标题大小={}, 副字幕大小={}",
                config.getFrequency(), config.getHintRender(), config.getTitleStyle(), config.getTitleSize(), config.getSubtitleSize());
    }

    /**
     * 按当前运行环境修正提示文字渲染方式配置。
     * 规则：
     * - 已加载 VulkanMod -> 强制使用 Vulkan
     * - 未加载 VulkanMod 且当前为 Vulkan -> 改回 OpenGL
     * - 未加载 VulkanMod 且当前为 OpenGL/CPU -> 保持不变
     */
    public static void correctHintRenderForEnvironment() {
        if (!loaded) {
            return;
        }

        String currentMode = ConfigData.normalizeRenderMode(config.getHintRender());
        String targetMode = currentMode;

        if (VulkanModCompat.isLoaded()) {
            targetMode = "Vulkan";
        } else if ("Vulkan".equals(currentMode)) {
            targetMode = "OpenGL";
        }

        if (targetMode.equals(currentMode)) {
            return;
        }

        config.setHintRender(targetMode);
        save();
        AreashintClient.LOGGER.info("已按运行环境修正提示文字渲染方式: {} -> {}", currentMode, targetMode);
    }
    
    /**
     * 获取当前配置副本。
     * @return 当前配置的独立副本
     */
    public static ConfigData copy() {
        return config.copy();
    }

    /**
     * 一次性应用新的客户端配置。
     * @param newConfig 新配置
     */
    public static void apply(ConfigData newConfig) {
        if (newConfig == null) {
            return;
        }

        config = newConfig.copy();
        save();
    }

    /**
     * 保存配置
     */
    public static void save() {
        if (!loaded) {
            return;
        }
        
        Path configFile = AreashintClient.getConfigFile();
        boolean success = FileManager.writeConfigData(configFile, config);
        if (success) {
            AreashintClient.LOGGER.info("已保存配置");
        } else {
            AreashintClient.LOGGER.error("保存配置失败");
        }
    }
    
    /**
     * 获取检测频率
     * @return 检测频率
     */
    public static double getFrequency() {
        return config.getFrequency();
    }
    
    /**
     * 设置检测频率
     * @param frequency 检测频率
     */
    public static void setFrequency(double frequency) {
        config.setFrequency(frequency);
        save();
    }
    
    /**
     * 获取提示文字渲染方式
     * @return 提示文字渲染方式
     */
    public static String getHintRender() {
        return config.getHintRender();
    }
    
    /**
     * 设置提示文字渲染方式
     * @param hintRender 提示文字渲染方式
     */
    public static void setHintRender(String hintRender) {
        String normalizedRender = ConfigData.normalizeRenderMode(hintRender);
        config.setHintRender(normalizedRender);
        save();
    }
    
    /**
     * 获取域名标题样式
     * @return 域名标题样式
     */
    public static String getTitleStyle() {
        return config.getTitleStyle();
    }
    
    /**
     * 设置域名标题样式
     * @param titleStyle 域名标题样式
     */
    public static void setTitleStyle(String titleStyle) {
        String normalizedStyle = ConfigData.normalizeStyleMode(titleStyle);
        config.setTitleStyle(normalizedStyle);
        save();
    }

    /**
     * 获取模组启用状态
     * @return 模组是否启用
     */
    public static boolean isEnabled() {
        return config.isEnabled();
    }
    
    /**
     * 设置模组启用状态
     * @param enabled 模组启用状态
     */
    public static void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
        save(); // 立即保存配置
    }

    /**
     * 获取记录按键代码
     * @return 记录按键代码（GLFW键码）
     */
    public static int getRecordKey() {
        return config.getRecordKey();
    }

    /**
     * 设置记录按键代码
     * @param recordKey 记录按键代码（GLFW键码）
     */
    public static void setRecordKey(int recordKey) {
        config.setRecordKey(recordKey);
        save(); // 立即保存配置
    }

    /**
     * 获取域名标题大小
     * @return 域名标题大小
     */
    public static String getTitleSize() {
        return config.getTitleSize();
    }

    /**
     * 设置域名标题大小
     * @param titleSize 域名标题大小
     */
    public static void setTitleSize(String titleSize) {
        String normalizedSize = ConfigData.normalizeSize(titleSize);
        config.setTitleSize(normalizedSize);
        save();
    }

    /**
     * 获取副字幕大小
     * @return 副字幕大小，auto 表示跟随主标题并小一级
     */
    public static String getSubtitleSize() {
        return config.getSubtitleSize();
    }

    /**
     * 设置副字幕大小
     * @param subtitleSize 副字幕大小
     */
    public static void setSubtitleSize(String subtitleSize) {
        String normalizedSize = ConfigData.normalizeSubtitleSize(subtitleSize);
        config.setSubtitleSize(normalizedSize);
        save();
    }

    /**
     * 获取语言设置
     * @return 语言代码
     */
    public static String getLanguage() {
        String lang = config.getLanguage();
        return lang != null ? lang : "zh_cn";
    }

    /**
     * 设置语言
     * @param language 语言代码
     */
    public static void setLanguage(String language) {
        config.setLanguage(language);
        save();
    }

    /**
     * 获取语言锁定状态
     * @return 语言是否已锁定
     */
    public static boolean isLanguageLocked() {
        return config.isLanguageLocked();
    }

    /**
     * 设置语言锁定状态
     * @param locked 是否锁定语言
     */
    public static void setLanguageLocked(boolean locked) {
        config.setLanguageLocked(locked);
        save();
    }

    /**
     * 获取边界可视化开关状态
     * @return 边界可视化是否启用
     */
    public static boolean isBoundVizEnabled() {
        return config.isBoundVizEnabled();
    }

    /**
     * 设置边界可视化开关状态
     * @param enabled 边界可视化开关状态
     */
    public static void setBoundVizEnabled(boolean enabled) {
        config.setBoundVizEnabled(enabled);
        save();
    }

    public static boolean isXaeroMinimapOverlayEnabled() {
        return config.isXaeroMinimapOverlayEnabled();
    }

    public static void setXaeroMinimapOverlayEnabled(boolean enabled) {
        config.setXaeroMinimapOverlayEnabled(enabled);
        save();
    }

    public static boolean isXaeroWorldMapOverlayEnabled() {
        return config.isXaeroWorldMapOverlayEnabled();
    }

    public static void setXaeroWorldMapOverlayEnabled(boolean enabled) {
        config.setXaeroWorldMapOverlayEnabled(enabled);
        save();
    }

    public static String getTeleportFormat() {
        return config.getTeleportFormat();
    }

    public static void setTeleportFormat(String teleportFormat) {
        config.setTeleportFormat(teleportFormat);
        save();
    }
}
