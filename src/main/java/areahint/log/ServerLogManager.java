package areahint.log;

import areahint.Areashint;
import areahint.file.FileManager;
import areahint.i18n.ServerI18nManager;

import java.nio.file.Path;

/**
 * 服务端日志管理器
 * 负责管理服务端日志的写入和清理
 */
public class ServerLogManager {
    // 可能被主线程与网络线程同时访问，保持可见性
    private static volatile AsyncLogManager logManager;
    private static final String LOG_FOLDER_NAME = "server_log";
    private static final int RETENTION_DAYS = 7; // 服务端日志保留7天

    /**
     * 初始化服务端日志管理器
     */
    public static void init() {
        // 已初始化时直接复用，避免重复创建写入通道
        if (logManager != null) {
            return;
        }

        try {
            Path configFolder = FileManager.getConfigFolder();
            Path logFolder = configFolder.resolve(LOG_FOLDER_NAME);

            logManager = new AsyncLogManager(logFolder, "server", RETENTION_DAYS);
            Areashint.LOGGER.info("服务端日志管理器初始化完成");
        } catch (Exception e) {
            Areashint.LOGGER.error("初始化服务端日志管理器失败", e);
        }
    }

    /**
     * 记录玩家进入域名
     * @param playerName 玩家名称
     * @param areaLevel 域名等级
     * @param surfaceName 联合域名名称
     * @param areaName 域名名称
     * @param dimensionalName 维度域名名称
     */
    public static void logPlayerEnterArea(String playerName, int areaLevel, String surfaceName, String areaName, String dimensionalName) {
        // 先取局部引用，避免关闭时出现空指针
        AsyncLogManager manager = logManager;
        if (manager == null) {
            return;
        }

        String levelText = getLevelText(areaLevel);
        String dim = dimensionalName != null ? dimensionalName : "";
        String message;

        if (surfaceName != null && !surfaceName.isEmpty()) {
            message = ServerI18nManager.translate("server.log.enter.surface", dim, playerName, levelText, surfaceName, areaName);
        } else {
            message = ServerI18nManager.translate("server.log.enter", dim, playerName, levelText, areaName);
        }

        manager.log(message);
        Areashint.LOGGER.info(message);
    }

    /**
     * 记录玩家离开域名
     * @param playerName 玩家名称
     * @param areaLevel 域名等级
     * @param surfaceName 联合域名名称
     * @param areaName 域名名称
     * @param dimensionalName 维度域名名称
     */
    public static void logPlayerLeaveArea(String playerName, int areaLevel, String surfaceName, String areaName, String dimensionalName) {
        // 先取局部引用，避免关闭时出现空指针
        AsyncLogManager manager = logManager;
        if (manager == null) {
            return;
        }

        String levelText = getLevelText(areaLevel);
        String dim = dimensionalName != null ? dimensionalName : "";
        String message;

        if (surfaceName != null && !surfaceName.isEmpty()) {
            message = ServerI18nManager.translate("server.log.leave.surface", dim, playerName, levelText, surfaceName, areaName);
        } else {
            message = ServerI18nManager.translate("server.log.leave", dim, playerName, levelText, areaName);
        }

        manager.log(message);
        Areashint.LOGGER.info(message);
    }

    /**
     * 获取域名等级文本
     * @param level 域名等级
     * @return 等级文本
     */
    private static String getLevelText(int level) {
        if (level >= 1 && level <= 3) {
            return ServerI18nManager.translate("server.log.level." + level);
        }
        return ServerI18nManager.translate("server.log.level.other", level);
    }

    /**
     * 记录一般日志信息
     * @param message 日志消息
     */
    public static void log(String message) {
        AsyncLogManager manager = logManager;
        if (manager != null) {
            manager.log(message);
        }
    }

    /**
     * 关闭服务端日志管理器
     */
    public static void shutdown() {
        if (logManager != null) {
            logManager.shutdown();
            logManager = null;
        }
    }
}
