# Boundary Brush Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在全部支持版本的 editviz 中增加可以贴合域名和自然岸线、可按鼠标意图扩展偏移的边界笔。

**Architecture:** 纯 Java 几何内核负责新增笔画裁剪、选岸、偏移记忆；客户端会话负责草稿历史及保存约束，Xaero 桥接提供有界地表快照。旧版与现代版保持几何行为一致，平台差异留在客户端适配层。

**Tech Stack:** Java 17/25、Java2D Area、Fabric、Xaero 地图源数据、JUnit 5、Stonecutter。

---

1. 在两套 `src/main/java/areahint/vizedit/geometry/` 新增边界笔内核与地形快照契约；先写两套 `src/test/java/areahint/vizedit/geometry/BoundaryBrushTest.java`，验证外沿越界裁剪、中心越界扩展、硬边界、已有面积不缩小、选岸和偏移撤销副本。先运行定向测试确认缺失，再实现至通过。
2. 在各版本 Xaero 桥接注册地形读取能力；客户端地形供应器只读当前世界/维度的地表块数据、请求已有地图加载并复制有界快照，不强制生成世界区块、不根据地图颜色识别岸线。核实旧版和现代版 API，并编译全部适配。
3. 在两套 `VizEditSession.java`、`VizEditScreen.java`、`VizEditClientManager.java` 接入新工具、预览状态、按笔历史和保存保护；工具行改为按工具数量布局，避免新增按钮遮挡。保存保持一位小数并检查约束，失败留草稿。
4. 更新全部11种语言和 `COMMAND_USAGE.md`，新增自然边界状态提示及操作说明；运行 `python tools/check_vizedit_translations.py`。
5. 运行 `./gradlew.bat test build --no-parallel --console=plain`、兼容检查及 `git diff --check`，独立审查需求和代码质量，修复发现的问题后再次运行相关检查。
6. 在 main 提交本次明确文件，保留用户已有 AGENTS.md 和 docs/bilibili-promo/ 改动；记录测试证据与未执行的游戏内验收项目。

按用户要求直接在 main 工作，不建立 worktree；实现工作分配到不重叠文件并行完成，由主代理整合、验证和提交。
