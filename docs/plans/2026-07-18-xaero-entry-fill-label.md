# Xaero Entry Fill and Label Scale Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 让两张 Xaero 地图在玩家进入域名后挖空当前域名及其祖先交集，并把联合域名标签放大为当前视觉尺寸的 4 倍。

**Architecture:** 在现有不可变域名快照之上新增共享填充解析器；主功能开启时消费 `AreaChangeTracker` 的权威当前域名，关闭时复用 `AreaDetector` 的同步层级命中，并缓存每个域名经过祖先差集处理后的世界坐标三角网格。小地图和世界地图只负责把共享网格转换到各自坐标系；名称仍沿用现有 `displayName()` 数据流。

**Tech Stack:** Java 17、Fabric 1.20.4、Xaero's Minimap 25.2.0、Xaero's World Map 1.39.2、Minecraft BufferBuilder

---

### Task 1: 新增共享进入状态与填充差集解析器

**Files:**
- Create: `src/client/java/areahint/xaero/AreaOverlayFillResolver.java`
- Modify: `src/client/java/areahint/detection/AreaDetector.java`
- Test: 不新增测试文件；仓库明确由用户自行测试

**Step 1: 按域名层级解析当前域名**

主功能开启时读取 `AreaChangeTracker` 的当前域名；主功能关闭时调用 `AreaDetector.findAreaForXaeroOverlay` 复用原有高度、点包含、文件顺序和 `base-name` 层级选择；查看其他维度时返回空进入状态。

**Step 2: 建立填充缓存**

以快照修订号、维度和当前域名名称为键，缓存每个域名最终需要提交的不可变三角形列表。

**Step 3: 计算祖先差集**

当前域名返回空网格；沿 `base-name` 找到的祖先逐三角形减去当前域名三角网格；其他域名直接转换原始网格。

### Task 2: 接入小地图填充并放大名称

**Files:**
- Modify: `src/client/java/areahint/xaero/minimap/AreaMinimapContext.java`
- Modify: `src/client/java/areahint/xaero/minimap/AreaMinimapRenderer.java`
- Modify: `src/client/java/areahint/xaero/OverlayRenderHelper.java`

**Step 1: 复用共享当前域名**

在 `preRender` 中把当前客户端状态交给填充解析器，并让现有最深层名称直接使用共享解析结果，避免填充与名称选中不同域名。

**Step 2: 绘制差集网格**

把共享世界坐标三角形转换到小地图坐标后，继续使用现有圆形或矩形视口裁剪。

**Step 3: 按 4 倍实际尺寸约束名称**

按放大后的宽高计算截断、圆形半径和矩形边界，再围绕标签中心执行 4 倍矩阵缩放。

### Task 3: 接入世界地图填充并放大名称

**Files:**
- Modify: `src/client/java/areahint/xaero/worldmap/AreaWorldMapRenderContext.java`
- Modify: `src/client/java/areahint/xaero/worldmap/AreaWorldMapProvider.java`
- Modify: `src/client/java/areahint/xaero/worldmap/AreaWorldMapRenderer.java`

**Step 1: 在 Provider 建立本帧填充计划**

取得当前查看维度快照后，以实际玩家维度和主检测状态更新共享解析器，并保存填充计划。

**Step 2: 提交差集三角形**

世界地图渲染器读取填充计划中的世界坐标三角形，沿用当前坐标缩放、边界和悬停逻辑。

**Step 3: 放大世界地图名称**

在现有逆地图缩放基础上乘以共享的 `4.0` 名称缩放系数，保持当前显示阈值不变。

### Task 4: 编译与差异验证

**Files:**
- Verify: `src/client/java/areahint/xaero/`
- Verify: `src/client/java/areahint/detection/AreaDetector.java`
- Verify: `docs/plans/2026-07-18-xaero-entry-fill-label-design.md`
- Verify: `docs/plans/2026-07-18-xaero-entry-fill-label.md`

**Step 1: 编译客户端代码**

Run: `.\gradlew.bat compileClientJava`

Expected: `BUILD SUCCESSFUL`，无 Xaero API、记录类型或矩阵调用错误。

**Step 2: 执行完整构建**

Run: `.\gradlew.bat build`

Expected: `BUILD SUCCESSFUL`。

**Step 3: 检查差异质量与范围**

Run: `git diff --check`

Expected: 无空白错误；`git status --short` 仅包含两份计划文档和 Xaero 联动源文件，不包含测试、生成输出或提交。
