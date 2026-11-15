package areahint.easyadd;

/**
 * EasyAdd配置管理器
 * 管理EasyAdd功能的相关配置
 */
public class EasyAddConfig {
    
    // 默认记录键位
    private static String recordKey = "X";
    
    /**
     * 获取记录键位
     */
    public static String getRecordKey() {
        // 优先使用按键绑定的显示名称
        return EasyAddKeyHandler.getRecordKeyDisplayName();
    }
    
    /**
     * 设置记录键位
     */
    public static void setRecordKey(String key) {
        recordKey = key;
    }
    
    /**
     * 重置为默认配置
     */
    public static void resetToDefault() {
        recordKey = "T";
    }
} 