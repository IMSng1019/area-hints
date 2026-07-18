# Xaero Overlay Rendering Fix Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复 Xaero 世界地图覆盖层的缩放与渲染状态，并让小地图统一填充域名且严格裁剪在地图外框内。

**Architecture:** 保持现有 Xaero 版本化兼容层和不可变域名快照不变，只在共享几何提交工具、世界地图渲染器和小地图渲染器中修正状态、矩阵与裁剪。联合域名名称及右键管理数据流不做调整。

**Tech Stack:** Java 17、Fabric 1.20.4、Xaero's Minimap 25.2.0、Xaero's World Map 1.39.2、JOML、Minecraft BufferBuilder

---

### Task 1: 恢复几何渲染状态并修正世界地图缩放

**Files:**
- Modify: `src/client/java/areahint/xaero/OverlayRenderHelper.java`
- Modify: `src/client/java/areahint/xaero/worldmap/AreaWorldMapRenderer.java`
- Test: 不新增测试文件；使用客户端编译和完整构建验证

**Step 1: 在每次几何提交前恢复覆盖层状态**

提取共享状态准备方法，并在开始覆盖层、绘制三角形、绘制线段时调用，确保文字渲染后仍使用 `getPositionColorProgram`。

**Step 2: 应用世界地图缩放矩阵**

在域名元素局部平移后按当前 `mapScale` 缩放 X/Z 平面，使相对顶点与 Xaero 计算的元素中心使用同一比例；名称的现有逆缩放抵消该矩阵，保持文字可读。

**Step 3: 编译客户端代码**

Run: `.\gradlew.bat compileClientJava`

Expected: `BUILD SUCCESSFUL`，无 Xaero API 或矩阵类型编译错误。

### Task 2: 统一小地图填充并补齐矩形裁剪

**Files:**
- Modify: `src/client/java/areahint/xaero/minimap/AreaMinimapRenderer.java`
- Test: 不新增测试文件；使用编译和用户游戏内验证

**Step 1: 填充所有可见域名**

按现有快照顺序先遍历 `visibleAreas` 绘制全部填充，再遍历一次绘制边界；保留 `deepestArea` 仅用于原有名称显示。

**Step 2: 裁剪方形小地图填充**

将每个三角形依次裁剪到 `[-halfViewW, halfViewW] × [-halfViewH, halfViewH]`，再将输出多边形重新拆为三角形。

**Step 3: 裁剪方形小地图边线**

使用 Liang-Barsky 参数裁剪把每条边限制在相同矩形内；圆形小地图继续使用现有圆交点算法。

**Step 4: 编译客户端代码**

Run: `.\gradlew.bat compileClientJava`

Expected: `BUILD SUCCESSFUL`。

### Task 3: 完整验证与变更检查

**Files:**
- Verify: `src/client/java/areahint/xaero/OverlayRenderHelper.java`
- Verify: `src/client/java/areahint/xaero/worldmap/AreaWorldMapRenderer.java`
- Verify: `src/client/java/areahint/xaero/minimap/AreaMinimapRenderer.java`

**Step 1: 执行完整构建**

Run: `.\gradlew.bat build`

Expected: `BUILD SUCCESSFUL`。

**Step 2: 检查差异质量**

Run: `git diff --check`

Expected: 无空白错误。

**Step 3: 检查修改范围**

Run: `git status --short`

Expected: 仅包含两份计划文档与三个 Xaero 渲染文件，不包含测试、翻译、生成产物或提交。
