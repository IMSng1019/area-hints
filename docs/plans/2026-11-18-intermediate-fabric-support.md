# Fabric 中间版本适配实施计划

**Goal:** 为 1.20.2、1.21.2、1.21.3、1.21.9 增加完整构建目标，保留域名功能并回归已有十二个版本。

**Architecture:** 延续 Stonecutter 共享源码和版本专属实现。1.20.2 结合 1.20.1 文本与 1.20.4 平台实现；1.21.2/1.21.3 以 1.21.4 为基线并修正 UI 条件边界；1.21.9 复用 1.21.10 主体，为缺失的 Fabric 世界渲染事件提供版本专属 Mixin 桥接。所有新包精确声明 Minecraft 版本。

**Tech Stack:** Fabric、Java 17/21/25、Gradle、Stonecutter、JUnit 5。

用户已授权按上一轮核查结果实施，直接在 main 修改并提交；不创建 worktree。Gradle 只由主代理串行运行，子代理按版本目录分工。

## 任务与验收

1. 核实四个目标的 Yarn、Fabric API、Mod Menu、Xaero 坐标，更新 `settings.gradle` 与 `gradle.properties`，复制相邻目标的版本实现作为编译基线。
2. 先运行目标编译，记录实际接口失败；针对报告修正版本实现与共享源码条件。API 适配以真实目标编译为验证，业务逻辑变动增加行为回归测试。
3. 1.20.2：验证文本序列化、书本、界面、登录握手、网络和地图联动。
4. 1.21.2/1.21.3：验证列表滚动、屏幕与渲染接口；相同实现保持一致，分别构建两个版本。
5. 1.21.9：核对原版世界渲染生命周期和目标签名，提取与绘制保持同帧、矩阵及深度语义，避免引用不存在的 Fabric 事件。
6. 逐目标 `build`，随后 Java 25 全量 `build --no-parallel`。检查测试 XML、产物入口、Mixin 配置与目标、Java 字节码、语言资源、精确版本声明和协议一致性。
7. 交叉审查实现，更新中英文 README 与本记录，提交源码及文档。构建输出、缓存和运行数据不提交；清楚记录尚未执行的游戏内画面与联机验收。

## 执行记录

- 起始提交：`cd2d50b`；工作区干净；源码版本 `4.8.5`。
- 已确认断点：1.20.2 的屏幕/文本接口；1.21.2/1.21.3 的列表滚动继承结构；1.21.9 Fabric API 0.134.1 缺少 `WorldRenderEvents`。
- 当前状态：四个新增目标及全部十六个版本构建通过，适配与独立审查完成。

## 实际适配

- 1.20.2：首编译复现成书序列化与列表构造器两处错误；使用 `Text.Serializer.toJson`，并将五参数列表构造器的起始版本修正为 1.20.3。屏幕背景四参数接口仍从 1.20.2 开始。
- 1.21.2/1.21.3：首编译复现列表滚动与换行文本接口差异；`getScrollY/setScrollY` 和带阴影参数的 `drawWrappedText` 均从 1.21.4 起使用。已有十二节点仍选择原分支。
- 1.21.9：首编译复现缺失 `WorldRenderEvents` 与 Xaero 提示框类型差异。新增版本专属 `WorldRenderer1219Mixin`，在世界边界状态提取后、主帧图最后一次即时批次提交前调用桥接；保持同帧矩阵、纯字节快照、深度附件及资源释放语义。成品仅此节点注册新 Mixin。
- Xaero：1.20.2 使用 25.2.0/1.39.2；1.21.3 使用 25.2.0/1.39.2；1.21.9 使用 25.2.15/1.39.17，提示框按真实 API 使用 `CursorBox`。
- 1.21.2 的两种 Xaero 官方 Fabric 筛选均返回空列表；实际 1.21.3 构件声明 `minecraft: >1.21.2`，不能借用。该节点采用无第三方类型引用的工厂，保留核心域名和独立编辑器，不声明地图依赖或虚假支持。
- 四个新增成品均精确声明自己的 Minecraft 版本；未修改旧依赖参数、现代源码分支、域名协议或任何语言资源。

## 验证结果

- 目标构建：`:1.20.2:build :1.21.2:build :1.21.3:build :1.21.9:build --continue --no-parallel --no-daemon` 成功，68 个任务，四节点各 202 项测试。
- 全量命令：`.\gradlew.bat --gradle-user-home .gradle-home '-Dorg.gradle.java.home=J:/mc/jdk25' build --offline --no-parallel --no-daemon --console=plain`。十六节点成功，260 个任务，耗时 1 分 35 秒；3270 项测试，失败、错误、跳过均为零。
- 沙箱内下载依赖和访问 Loom 临时 JAR 曾被系统拒绝；经工具批准后在沙箱外完成下载与最终离线构建，没有跳过测试或修改机器权限。
- `python tools/test_xaero_initialization.py`：16/16 节点、151 项场景通过；无依赖节点先观察红测，再验证完全不提供 Loader/Xaero 类的工厂初始化。
- `python tools/test_xaero_right_click.py`：9 个适用节点通过，包括新增 1.20.2 和 1.21.3。
- `python tools/check_xaero_compat.py`：24 组真实依赖组合通过；1.21.2 成品无 Xaero 硬引用。无依赖扫描另外用 12 个真实编译样本验证父类、接口、字段、参数、返回值、数组、类字面量、类型转换及空包/缺包处理。
- 成品检查：全部入口和配置中的 Mixin 类存在，Java 字节码符合 17/21/25，11 份语言文件逐字节与根资源一致；1.21.9 不引用不存在的 Fabric 世界渲染事件，新 Mixin 类及配置只进入此包。
- 独立审查对照真实 1.21.9 原版字节码与 1.21.10 Fabric 实现，确认提取/绘制注入点、回调签名、状态清理和 GPU 附件语义。实现和构建配置均通过交叉审查。
- 本轮没有启动游戏进行画面、地图交互或真实联机验收；自动测试与静态产物检查不能替代这些验收。

原始日志：`.gradle/compat-middle-initial-online.log`、`.gradle/compat-middle-targets-online.log`、`.gradle/compat-middle-all-build-verified.log`。本机产物核验脚本和结果：`.deps/verify-middle-artifacts.py`、`.deps/compat-middle-artifacts.json`；这些生成证据不提交。
