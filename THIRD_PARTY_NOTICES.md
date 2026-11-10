# 第三方代码说明

## Province

可视化域名编辑器的几何、方块吸附、魔棒找面和撤销历史参考并改编自用户提供的 Province 源码（`J:\mc\copy-ah\Province-main`）。原作者为 Shuuki_，版权为 `Copyright (c) 2026 Shuuki_`，授权协议为 MIT。

对应来源文件：

- `src/main/java/com/mcprovince/core/VectorShape.java`：基于 Java AWT `Area` 的矢量布尔运算、轮廓和面积。
- `src/main/java/com/mcprovince/core/BlockSnap.java`：方块中心扫描线、四连通笔画和按行合并矩形。
- `src/main/java/com/mcprovince/core/FenceFaces.java`：线段求交、平面图半边和最小闭合面。
- `src/main/java/com/mcprovince/edit/EditSession.java`、`EditTool.java`：六种选区工具及 Shift 约束行为。
- `src/main/java/com/mcprovince/edit/EditHistory.java`：最多 64 步撤销与重做。
- `src/main/java/com/mcprovince/client/ProvinceEditor.java`：编辑工具状态、笔宽及历史的组织方式。
- `src/main/java/com/mcprovince/client/ProvinceMapHook.java`、`client/render/ProvinceMapOverlay.java`、`client/render/XaeroMapProjection.java`：界面事件、地图输入消费和屏幕／世界坐标互逆变换。

本仓库对应实现位于 `src/main/java/areahint/vizedit/geometry/`、`src/client/java/areahint/vizedit/` 及各活跃版本的 `xaero/worldmap/VizEditXaeroBridge.java`。与参考实现的主要差异是：旧域名保持小数与斜边，仅新笔画吸附方块；取消父级裁剪和同级互斥；魔棒只使用实际闭合边界，不补缺口或额外外框；保存前验证单一无洞简单多边形；增加绘制资源上限和服务端共享验证。

完整原始许可证保存在 `src/main/resources/META-INF/Province-LICENSE`，并随模组 JAR 一同发布。
