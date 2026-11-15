# 2026-11-16 版本路线与公开依赖复核

本记录只说明上游公开版本与迁移断点，不代表这些版本已通过本模组的编译或游戏测试。

## 发布终点和路线

Mojang `version_manifest_v2.json` 实时返回 `latest.release=26.3`、`latest.snapshot=26.4-snapshot-8`；Fabric Meta 同样将 26.3 列为最新稳定游戏版本。

建议按每个发布代际的最后修订推进：**1.21.8 → 1.21.10 → 1.21.11 → 26.1.2 → 26.2 → 26.3**。如果要覆盖被补丁替代的正式版，还需要分别补充 1.21.6、1.21.7、1.21.9、26.1、26.1.1 构建与验证，不能仅扩大元数据版本范围。

| Minecraft | Java | Yarn（26.x 无需映射） | Fabric API |
|---|---:|---|---|
| 1.21.8 | 21 | `1.21.8+build.1` | `0.136.1+1.21.8` |
| 1.21.10 | 21 | `1.21.10+build.3` | `0.138.4+1.21.10` |
| 1.21.11 | 21 | `1.21.11+build.6` | `0.141.6+1.21.11` |
| 26.1.2 | 25 | 无 | `0.155.3+26.1.2` |
| 26.2 | 25 | 无 | `0.161.0+26.2` |
| 26.3 | 25 | 无 | `0.166.2+26.3` |

Java 要求来自每个版本的 Mojang JSON；26.x 的 JSON 均没有 `client_mappings` / `server_mappings`。Yarn 来自 Fabric Meta；Fabric API 来自官方 Maven 元数据。

Fabric Loader 最新稳定版为 **0.20.1**。现有 **0.19.5** 满足本次在内存解压核验的 Fabric API 硬依赖：26.1.2、26.2 要求 `>=0.18.4`，26.3 要求 `>=0.19.3`；三者都要求 Java 25。

## 工具链

- ≤1.21.11 使用重映射 Loom（`fabric-loom` 或 `net.fabricmc.fabric-loom-remap`）。
- ≥26.1 使用 `net.fabricmc.fabric-loom`，没有 `mappings`；将 `modImplementation/modCompileOnly/modLocalRuntime` 换成普通 Java 配置，将 `remapJar` 换成 `jar`。
- 官方 26.3 公告建议 Loom 1.17 和 Gradle 9.6.0；仓库已有的 26.x 探针实测 Loom **1.17.21** + Gradle **9.5.1** 能编译 26.1.2 简单官方命名代码。此组合能否编译完整 26.3 仍需验证。
- 实时读取 Loom `.module`：1.17.21 要求 Gradle **9.5.0** / 插件运行 JVM **21**；1.18.2 要求 Gradle **9.7.0** / 插件运行 JVM **25**。Java 25 编译工具链与 Gradle 启动 JVM 是两件事。
- 现有 `24-26x-no-remap探针.md` 记录了本机 JDK 25 的发现问题与已验证路径；不应将机器绝对路径写入发布配置。

## 可选联动依赖

以下版本号来自各项目自己的 Modrinth 发布列表；26.x 表中列出当日最新正式版，不说明现有适配源码可直接兼容。

| Minecraft | Mod Menu | Xaero Minimap | Xaero World Map |
|---|---|---|---|
| 1.21.8 | `15.0.2` | 保留 `25.3.2_Fabric_1.21.8` | 保留 `1.40.2_Fabric_1.21.8` |
| 1.21.10 | `16.0.1` | `25.3.2_Fabric_1.21.10` 可用 | `1.40.2_Fabric_1.21.10` 可用 |
| 1.21.11 | `17.0.1` | 保留 `25.3.2_Fabric_1.21.11` | 保留 `1.40.2_Fabric_1.21.11` |
| 26.1.2 | `18.0.2` | `fabric-26.1.2-26.6.2` | `fabric-26.1.2-1.47.2` |
| 26.2 | `20.0.3` | `fabric-26.2-26.6.2` | `fabric-26.2-1.47.2` |
| 26.3 | `21.0.1` | `fabric-26.3-26.6.2` | `fabric-26.3-1.47.2` |

Mod Menu 21.0.1 的项目元数据还声明 **Text Placeholder API**（Modrinth `placeholder-api` / `eXts2L7r`）必需，安排开发运行环境时应检查该依赖。

BlueMap 与 LuckPerms 属于平台无关 API，可沿用仓库已有按 Java 分层的配置；既有调研证明 `de.bluecolored:bluemap-api:2.7.8` 为 Java 21，而 `2.8.1` 为 Java 25。无需为迁移夹带无关 API 升级。

## 重点断点

- **1.21.8**：GUI/HUD 转入提取和延迟绘制流程；二维矩阵与 RenderPipeline/RenderLayer 配合改变；HUD 注册使用 `HudElementRegistry`。自绘与 Xaero 联动需核验顺序、矩阵和裁剪。
- **1.21.10**：世界绘制使用 `OrderedRenderCommandQueue`，输入事件改为 `KeyInput` 等上下文对象，按键分类使用结构化类型。
- **1.21.11**：最后一代 Yarn；应在这一节点用官方 `migrateMappings` 或 Ravel 迁移机制校准 Mojang 类与成员名，并人工复核 mixin。
- **26.1.2**：无混淆、Java 25、Loom 无重映射，Fabric API 同步改用官方术语；旧 `HudRenderCallback` 被移除。
- **26.2**：支持 OpenGL/Vulkan 后端，绘制不得依赖原始 OpenGL；GUI/HUD 拆分，`Minecraft.setScreen` 移到 `Minecraft.gui.setScreen`。
- **26.3**：GLFW 换成 SDL，按键应使用 `InputConstants`，不能硬编码 GLFW 鼠标常量；自定义文本输入焦点需同步 `TextInputManager`。

## 可复查来源

- [Mojang 版本清单](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json)
- [Fabric 游戏版本](https://meta.fabricmc.net/v2/versions/game)、[Loader](https://meta.fabricmc.net/v2/versions/loader)
- [Fabric API Maven 元数据](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml)
- [Loom 1.17.21 模块元数据](https://maven.fabricmc.net/net/fabricmc/fabric-loom/1.17.21/fabric-loom-1.17.21.module)
- [1.21.6–1.21.8 迁移](https://fabricmc.net/2025/06/15/1216.html)、[1.21.9–1.21.10 迁移](https://fabricmc.net/2025/09/23/1219.html)、[1.21.11 迁移](https://fabricmc.net/2025/12/05/12111.html)
- [26.1 迁移](https://fabricmc.net/2026/03/14/261.html)、[26.2 迁移](https://fabricmc.net/2026/06/15/262.html)、[26.3 迁移](https://fabricmc.net/2026/09/15/263.html)
- [映射迁移文档](https://docs.fabricmc.net/develop/porting/mappings/)
- [Mod Menu API](https://api.modrinth.com/v2/project/modmenu/version)、[Xaero Minimap API](https://api.modrinth.com/v2/project/xaeros-minimap/version)、[Xaero World Map API](https://api.modrinth.com/v2/project/xaeros-world-map/version)
