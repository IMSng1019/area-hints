package areahint.xaero;

import areahint.AreashintClient;

/** 1.21.2 没有对应的 Xaero Fabric 发行文件，仅隔离地图联动，核心域名与独立编辑器照常初始化。 */
public final class XaeroCompatFactory {
    private XaeroCompatFactory() {
    }

    public static XaeroBridgeHost create() {
        // 工厂不引用地图类，避免未安装可选依赖时因类型解析失败阻断整个客户端。
        return () -> AreashintClient.LOGGER.info("Minecraft 1.21.2 暂无对应的 Xaero Fabric 发行文件，已跳过地图联动。");
    }
}
