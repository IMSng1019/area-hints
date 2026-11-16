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

## 实现与验证记录

2026-11-16：边界笔、地形读取、草稿记忆、限制线预览和受约束减点均已实现。原减点算法新增禁区检查，普通工具继续沿用原入口；边界笔的绘制完成、主动简化和保存均使用受约束入口。

- 完整构建命令：`.\gradlew.bat '-Porg.gradle.java.installations.paths=J:/mc/jdk25' test build --no-parallel --no-daemon --console=plain`，12 个活跃版本全部成功。Java 25 路径仅为本机验证参数，未写入发布构建配置；一次性进程用于避开此前受限守护进程的系统临时文件权限。
- XML 结果核对：全部活跃版本累计 1406 项测试，失败和错误均为 0；每版本包含 33 项新边界相关测试（23 项笔触几何、5 项保存与减点、5 项缓存）。
- `python tools/check_xaero_compat.py`：20 组当前及更新依赖组合，0 失败；所有12版本新增桥接均编译、打包成功。
- `python tools/check_vizedit_translations.py`：全部11种语言键集、占位符和源码引用一致。`git diff --check` 通过。
- 用实际编译代码复核混合阶梯海岸与局部受限缺角：126 个原顶点，绘制预览和保存均为 9 个顶点，与禁区交集为空。仍遵守一位小数、1 格总误差和既有边保护。
- 独立审查及合成探针覆盖圆岛、圆湖、弯河、狭窄及远处已有面积选岸、300 格跨笔偏移恢复、未知地形延迟补齐、负坐标精度、撤销副本和最终限制线预览，未留下已知阻塞问题。

尚未进行游戏内 Xaero 远处源数据加载、窗口交互和联机验收。合成弯岸性能观测中默认4格笔宽约1.3毫秒中位数，256格最大笔宽可出现约114毫秒单次计算峰值；这是本机几何探针结果，不代表游戏帧率。
