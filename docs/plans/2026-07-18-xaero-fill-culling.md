# Xaero Multi-Area Fill Culling Fix Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 让 Xaero 大地图和小地图显示所有按现有规则应填充的域名，而不再受域名顶点绕序影响。

**Architecture:** 只修改两张地图共用的 `OverlayRenderHelper`。覆盖层开始时保存 Xaero 的剔除状态，几何提交前关闭背面剔除，覆盖层结束时精确恢复原状态；填充解析、颜色、边界和交互数据流保持不变。

**Tech Stack:** Java 17、Fabric 1.20.4、Minecraft RenderSystem/OpenGL、Xaero's Minimap 25.2.0、Xaero's World Map 1.39.2

---

### Task 1: 修复共享几何的背面剔除状态

**Files:**
- Modify: `src/client/java/areahint/xaero/OverlayRenderHelper.java`

**Step 1: 确认失败基线**

对照 `PolygonGeometry.triangulate`、`AreaOverlayFillResolver` 与 Xaero 世界地图反编译调用，确认三角形保留任意绕序、Xaero 在元素渲染阶段开启剔除，而共享工具没有关闭或恢复剔除。

Expected: 域名填充是否可见取决于最终三角形绕序；边界线与颜色不受影响。

**Step 2: 扩展捕获状态**

在 `RenderState` 中加入 `cullEnabled`，并使用 `GL11.glIsEnabled(GL11.GL_CULL_FACE)` 捕获进入覆盖层前的真实状态。

**Step 3: 关闭覆盖层几何剔除**

在 `prepareGeometryState` 中调用 `RenderSystem.disableCull()`，保证每次文字或其他渲染切换状态后，三角形和边线提交都使用一致的双面几何状态。

**Step 4: 精确恢复剔除状态**

在 `RenderState.restore` 中根据 `cullEnabled` 调用 `RenderSystem.enableCull()` 或 `RenderSystem.disableCull()`，保持现有嵌套状态栈语义。

### Task 2: 验证修复

**Files:**
- Verify: `src/client/java/areahint/xaero/OverlayRenderHelper.java`
- Verify: `docs/plans/2026-07-18-xaero-fill-culling-design.md`
- Verify: `docs/plans/2026-07-18-xaero-fill-culling.md`

**Step 1: 检查差异完整性**

Run: `git diff --check`

Expected: 无空白错误。

**Step 2: 运行完整构建**

Run: `.\gradlew.bat build`

Expected: `BUILD SUCCESSFUL`，无 Minecraft RenderSystem、OpenGL 状态或 Xaero API 编译错误。

**Step 3: 检查工作区范围**

Run: `git status --short`

Expected: 只包含两份计划文档和 `OverlayRenderHelper.java`，不包含测试、翻译、运行数据、生成输出或提交。
