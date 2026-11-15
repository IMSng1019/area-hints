package areahint.log;

import areahint.Areashint;
import areahint.AreashintClient;
import areahint.file.FileManager;

import java.nio.file.Path;

/**
 * 客户端日志管理器
 * 负责管理客户端日志的写入和清理
 */
public class ClientLogManager {
    // 可能被客户端线程与其他回调线程同时访问，保持可见性
    private static volatile AsyncLogManager logManager;
    private static final String LOG_FOLDER_NAME = "client_log";
    private static final int RETENTION_DAYS = 3; // 客户端日志保留3天

    // 当前世界进入次数（用于日志文件命名）
    private static int worldEntryCount = 0;

    /**
     * 初始化客户端日志管理器
     */
    public static void init() {
        // 已初始化时直接复用，避免重复创建写入通道
        if (logManager != null) {
            return;
        }

        try {
            Path configFolder = FileManager.getConfigFolder();
            Path logFolder = configFolder.resolve(LOG_FOLDER_NAME);

            logManager = new AsyncLogManager(logFolder, "client", RETENTION_DAYS);
            AreashintClient.LOGGER.info("客户端日志管理器初始化完成");
        } catch (Exception e) {
            AreashintClient.LOGGER.error("初始化客户端日志管理器失败", e);
        }
    }

    /**
     * 玩家进入世界时调用，增加进入次数
     */
    public static void onWorldEnter() {
        worldEntryCount++;
        // 先取局部引用，避免关闭时出现空指针
        AsyncLogManager manager = logManager;
        if (manager != null) {
            manager.log("玩家进入世界（第" + worldEntryCount + "次）");
        }
    }

    /**
     * 记录玩家进入域名
     * @param areaLevel 域名等级
     * @param surfaceName 联合域名名称
     * @param areaName 域名名称
     * @param dimensionalName 维度域名名称
     */
    public static void logEnterArea(int areaLevel, String surfaceName, String areaName, String dimensionalName) {
        // 先取局部引用，避免关闭时出现空指针
        AsyncLogManager manager = logManager;
        if (manager == null) {
            return;
        }

        String levelText = getLevelText(areaLevel);
        String message;

        if (surfaceName != null && !surfaceName.isEmpty()) {
            message = String.format("[%s]进入了[%s] [%s]的[%s]",
                dimensionalName != null ? dimensionalName : "", levelText, surfaceName, areaName);
        } else {
            message = String.format("[%s]进入了[%s] [%s]",
                dimensionalName != null ? dimensionalName : "", levelText, areaName);
        }

        manager.log(message);
    }

    /**
     * 记录玩家离开域名
     * @param areaLevel 域名等级
     * @param surfaceName 联合域名名称
     * @param areaName 域名名称
     * @param dimensionalName 维度域名名称
     */
    public static void logLeaveArea(int areaLevel, String surfaceName, String areaName, String dimensionalName) {
        // 先取局部引用，避免关闭时出现空指针
        AsyncLogManager manager = logManager;
        if (manager == null) {
            return;
        }

        String levelText = getLevelText(areaLevel);
        String message;

        if (surfaceName != null && !surfaceName.isEmpty()) {
            message = String.format("[%s]离开了[%s] [%s]的[%s]",
                dimensionalName != null ? dimensionalName : "", levelText, surfaceName, areaName);
        } else {
            message = String.format("[%s]离开了[%s] [%s]",
                dimensionalName != null ? dimensionalName : "", levelText, areaName);
        }

        manager.log(message);
    }

    /**
     * 获取域名等级文本
     * @param level 域名等级
     * @return 等级文本
     */
    private static String getLevelText(int level) {
        if (level == 1) {
            return "顶级域名";
        } else if (level == 2) {
            return "二级域名";
        } else if (level == 3) {
            return "三级域名";
        } else {
            return level + "级域名";
        }
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
     * 关闭客户端日志管理器
     */
    public static void shutdown() {
        if (logManager != null) {
            logManager.shutdown();
            logManager = null;
        }
        worldEntryCount = 0;
    }
}
