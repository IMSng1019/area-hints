package areahint.xaero;

/**
 * Xaero 联动装配接口：零 xaero import，实现放在各版本目录（不同 FQN）。
 * 共享侧只通过本接口发起装配，具体注册哪个 Xaero 事件由实现决定。
 */
public interface XaeroBridgeHost {
    /** 装配小地图与世界地图联动；依赖缺失或版本不符时只记日志并降级 */
    void initialize();
}
