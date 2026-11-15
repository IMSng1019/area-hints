package areahint.easyadd;

/**
 * EasyAdd按键处理器
 * 提供按键相关的辅助方法
 * X键现在由UnifiedKeyHandler统一处理
 */
public class EasyAddKeyHandler {
    
    /**
     * 注册方法（现在为空，X键由UnifiedKeyHandler处理）
     */
    public static void register() {
        // X键现在由UnifiedKeyHandler统一处理
        // 这里保留方法以保持向后兼容
    }
    
    /**
     * 获取当前按键的显示名称
     */
    public static String getRecordKeyDisplayName() {
        // 使用统一处理器的按键显示名称
        return areahint.keyhandler.UnifiedKeyHandler.getRecordKeyDisplayName();
    }
} 