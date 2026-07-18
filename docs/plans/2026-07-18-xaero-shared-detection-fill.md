# Xaero Shared Detection Fill Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 让两张 Xaero 地图复用模组已经完成的域名判定，仅在判定或域名数据变化时更新动态填充，并移除小地图上的所有域名文字。

**Architecture:** `AreaChangeTracker` 发布带维度和版本的不可变判定状态；共享的 `AreaOverlayFillResolver` 以该状态和域名快照修订号缓存每个维度的填充计划。主功能关闭时才按配置频率调用现有备用检测；小地图与世界地图只消费共享计划。

**Tech Stack:** Java 17、Fabric 1.20.4、Xaero's Minimap 25.2.0、Xaero's World Map 1.39.2、Minecraft BufferBuilder

---

### Task 1: 发布可版本化的模组判定状态

**Files:**
- Modify: `src/client/java/areahint/log/AreaChangeTracker.java`
- Test: 不新增测试文件；仓库明确由用户自行测试

**Step 1: 定义不可变判定快照**

在 `AreaChangeTracker` 中加入包含实际域名名称、维度标识和状态版本的记录，并通过 `volatile` 字段统一发布。

**Step 2: 统一状态发布入口**

在同步检测、异步结果消费和重置路径完成原有消息处理后发布状态；仅当域名名称或维度变化时增加版本。

**Step 3: 保持原接口兼容**

保留 `getCurrentAreaData()`，新增只读判定快照获取方法，避免影响标题、描述和日志功能。

### Task 2: 让两张地图共享填充解析与备用判定

**Files:**
- Modify: `src/client/java/areahint/xaero/AreaOverlayFillResolver.java`
- Modify: `src/client/java/areahint/xaero/minimap/AreaMinimapContext.java`
- Modify: `src/client/java/areahint/xaero/worldmap/AreaWorldMapRenderContext.java`

**Step 1: 建立共享解析器**

提供单例解析器，并让小地图与世界地图上下文引用同一实例。

**Step 2: 使用模组判定快照**

主功能开启时校验判定维度与玩家、地图维度一致，再按实际域名名称从当前 `OverlaySnapshot` 取得活动域名。

**Step 3: 按维度缓存不可变填充计划**

缓存键包含域名快照修订号、判定状态版本、判定维度和活动域名名称；仅在键变化时执行现有祖先差集算法。

**Step 4: 限流备用判定**

主功能关闭时复用 `AreaDetector.findAreaForXaeroOverlay`，按域名快照、玩家维度、玩家坐标和配置检测间隔缓存一次共享结果。

### Task 3: 移除小地图域名文字

**Files:**
- Modify: `src/client/java/areahint/xaero/minimap/AreaMinimapContext.java`
- Modify: `src/client/java/areahint/xaero/minimap/AreaMinimapRenderer.java`

**Step 1: 删除文字状态**

移除小地图上下文中只用于文字显示的最深层域名字段及其赋值。

**Step 2: 删除文字渲染**

删除 `renderDeepestName` 调用、方法和不再需要的文字相关导入；保留所有填充、裁剪和边界逻辑。

### Task 4: 编译与差异验证

**Files:**
- Verify: `src/client/java/areahint/log/AreaChangeTracker.java`
- Verify: `src/client/java/areahint/xaero/`
- Verify: `docs/plans/2026-07-18-xaero-shared-detection-fill-design.md`
- Verify: `docs/plans/2026-07-18-xaero-shared-detection-fill.md`

**Step 1: 编译客户端代码**

Run: `.\gradlew.bat compileClientJava`

Expected: `BUILD SUCCESSFUL`，无记录类型、Xaero API 或共享解析器错误。

**Step 2: 执行完整构建**

Run: `.\gradlew.bat build`

Expected: `BUILD SUCCESSFUL`。

**Step 3: 检查差异质量和范围**

Run: `git diff --check` 和 `git status --short`

Expected: 无空白错误；仅包含计划文档和预期 Java 源文件，不包含测试、翻译或生成输出。

**Step 4: 不提交变更**

按照仓库要求保留 main 工作区修改，不创建 commit 或 worktree。
