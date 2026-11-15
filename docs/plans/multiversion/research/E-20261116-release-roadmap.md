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

## 26.x 全源码迁移执行准备（本节只读研究，尚未执行）

本节读取了本机缓存里的 Loom 1.10-SNAPSHOT（实际运行显示 1.10.5）、Loom 1.17.21、Stonecutter 0.9.7、Mercury、MixinRemapper 的字节码，并核对仓库已有 26.x 探针。**没有运行 Gradle，没有修改源码或构建配置。** 以下步骤供主代理执行时使用，不能把静态检查写成已完成迁移。

### 已核实的工具行为

| 项目 | Loom 1.10 | Loom 1.17.21 |
|---|---|---|
| 任务 | `MigrateMappingsTask` | 同名，继承 `AbstractMigrateMappingsTask` |
| 参数 | `--mappings`、`--input`、`--output` | 相同；另有 `--overrideInputsIHaveABackup` |
| 默认输入/输出 | 节点的 `src/main/java` / `remappedSrc` | 相同 |
| 官方映射参数 | `net.minecraft:mappings:1.21.11` | 相同 |
| 跨 MC 版本 | 拒绝，必须与任务所属项目 MC 完全同版 | 相同 |
| 连接路径 | 源 `named` → `intermediary` → 目标 `named` | 相同 |
| Java 改写器 | 仅 `MercuryRemapper` | 先 `MixinRemapper`，再 `MercuryRemapper` |
| 迁移异常 | 警告 `Could not remap fully!` 后继续，可能留下部分结果 | 清理本次输出并抛出 `Failed to migrate mappings` |
| 已存在输出 | 只执行 `Files.deleteIfExists`，非空目录会失败 | **递归删除输出目录**后重新生成 |
| 自动类路径 | `compileClasspath` + Minecraft named/intermediary JAR | 相同，**不自动加入 clientCompileClasspath** |

因此优先使用已升级的 **1.17.21**，绝不向仓库共享 `src` 或已修好的 26.x 源码执行覆盖迁移。输出只使用独立的 `.deps/migration-1.21.11-<时间>/output`。不要传 `--overrideInputsIHaveABackup`。

两个版本都启用 `Mercury.setGracefulClasspathChecks(true)`；这只让无法解析的符号被容忍，不保证已完全改名。即使任务成功，仍需检查文件清单、残留 Yarn 符号、mixin 字符串和实际编译。

### 为什么同版两种 Loom 插件值得先直接尝试

当前主代理因 1.21.10 的 Unpick 格式已将旧节点升级到 `net.fabricmc.fabric-loom-remap:1.17.21`。

本机字节码显示：

- `LoomRemapGradlePlugin` 为当前 `Project` 应用基础 `fabric-loom`。
- `LoomNoRemapGradlePlugin` 先检查当前 `Project` 是否已经应用基础插件，再应用同一基础插件。
- `LoomGradleExtensionImpl` 按**当前项目**是否安装 `net.fabricmc.fabric-loom` 决定 `disableObfuscation/dontRemap`；不是全局静态模式。
- 因此同一个构建中，各节点只应用一种入口插件、统一版本 **1.17.21**，可以先试验；原先 1.10 与 1.17 的实现版本冲突已不再必然存在。
- 同一项目不可先套旧 `build.gradle` 的 remap 插件再切 no-remap；插件应用顺序检查会明确拒绝。

Stonecutter 0.9.7 已实查 `NodeBuilder.buildscript(String)`，例如 `version('26.1.2').buildscript('build26.gradle')` 是实际存在的接口；但**换构建脚本并不会自动把 Yarn 共享源码转成官方命名**。26.x 必须有独立的完整官方命名源码根，并核验最终 `sourceSets` 不再挂载旧版共享 Java 文件。

### 步骤 1：冻结可编译的 1.21.11 输入

先完成 1.21.11 的全部功能适配和构建，然后记录 Git 提交与源文件清单。不能直接把仍以 1.20.4 为 active 的根 `src/` 送给映射迁移：里面的条件分支尚未展开为 1.21.11。

```powershell
.\gradlew.bat :1.21.11:build --console=plain --no-parallel
```

输入应由四个目录合并，按相对包路径保存，发生同路径重复立即报错：

1. `versions/1.21.11/build/generated/stonecutter/main/java`：已预处理的共享服务端源码。
2. `versions/1.21.11/src/main/java`：网络等版本实现。
3. `versions/1.21.11/build/generated/stonecutter/client/java`：已预处理的共享客户端源码。
4. `versions/1.21.11/src/client/java`：渲染、Xaero、mixin 等版本实现。

已从 1.21.8 实际目录确认：`build/generated/stonecutter/client/java` **不包含**版本目录里的 `XaeroCompatFactory` / `AreaWorldMapRenderer`，只迁移 generated 会漏掉功能。上述 `build` 目录仅作为读取来源，不手工改写。

可在 PowerShell 执行以下暂存步骤；输出目录使用本次新时间戳，不删除历史目录：

```powershell
$ErrorActionPreference = 'Stop'
$repoRoot = (Get-Location).Path
$migrationRoot = Join-Path $repoRoot ('.deps/migration-1.21.11-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$migrationInput = Join-Path $migrationRoot 'input'
$manifest = @{}
$roots = @(
    @{ env='main'; path='versions/1.21.11/build/generated/stonecutter/main/java' },
    @{ env='main'; path='versions/1.21.11/src/main/java' },
    @{ env='client'; path='versions/1.21.11/build/generated/stonecutter/client/java' },
    @{ env='client'; path='versions/1.21.11/src/client/java' }
)
foreach ($root in $roots) {
    $sourceRoot = (Resolve-Path -LiteralPath $root.path).Path
    foreach ($source in Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Filter '*.java') {
        $relative = $source.FullName.Substring($sourceRoot.Length).TrimStart([char[]]'\/')
        if ($manifest.ContainsKey($relative)) { throw "重复源码：$relative" }
        $manifest[$relative] = @{ environment=$root.env; source=$source.FullName }
        $destination = Join-Path $migrationInput $relative
        New-Item -ItemType Directory -Force ([IO.Path]::GetDirectoryName($destination)) | Out-Null
        Copy-Item -LiteralPath $source.FullName -Destination $destination
    }
}
[IO.File]::WriteAllText((Join-Path $migrationRoot 'manifest.json'),
    ($manifest | ConvertTo-Json -Depth 4), [Text.UTF8Encoding]::new($false))
$migrationRoot
```

所有 main/client Java 一次放进同一输入根，Mercury 才能看见本模组跨环境引用；用 manifest 在迁移后恢复 main/client 分离。resources 与测试另行保留，不能因这个 Java 暂存步骤遗漏它们。

### 步骤 2：在 1.21.11 项目执行官方命名迁移

常规任务调用为：

```powershell
.\gradlew.bat :1.21.11:migrateMappings --mappings 'net.minecraft:mappings:1.21.11' --input 'J:/mc/mods/.deps/migration-1.21.11-本次时间/input' --output 'J:/mc/mods/.deps/migration-1.21.11-本次时间/output' --console=plain --no-parallel
```

但本模组必须补充客户端 classpath。建议为**这一次迁移**创建临时 Gradle init script，而不是修改正常构建依赖关系。下面脚本通过已核对的受保护 getter 取得 Loom 迁移选项，在任务输入快照前补齐类路径；使用 `projectsEvaluated` 是为保证 Loom 配置已完成。**这段脚本尚未实跑，执行时若 Loom 锁定选项导致失败，应使用下面的独立 Mercury 备选，不能删掉客户端源码来绕开。**

```groovy
// 保存为本次 .deps/migration-.../with-client-classpath.init.gradle。
gradle.projectsEvaluated {
    def p = gradle.rootProject.findProject(':1.21.11')
    if (p == null) throw new GradleException('必须从包含 1.21.11 节点的仓库构建执行')
    def task = p.tasks.named('migrateMappings').get()
    // 在读取内部选项之前提供映射值，避免命令行任务选项尚未应用时访问空 Property。
    task.mappings.set('net.minecraft:mappings:1.21.11')
    def type = task.class
    while (type != null && type.name != 'net.fabricmc.loom.task.MigrateMappingsTask') type = type.superclass
    if (type == null) throw new GradleException('Loom 迁移任务类型不符合 1.17.21 契约')
    def getter = type.getDeclaredMethod('getMigrationServiceOptions')
    getter.setAccessible(true)
    def sourceOptions = getter.invoke(task).get()
    def mappingOptions = sourceOptions.mappings.get()
    mappingOptions.classpath.from(p.sourceSets.client.compileClasspath)
    mappingOptions.classpath.from(p.sourceSets.main.output, p.sourceSets.client.output)
}
```

把 `-I <脚本绝对路径>` 加到上一条 Gradle 命令。不要给正常 `compileClasspath` 添加 `clientCompileClasspath`：客户端本来依赖 main，反向扩展会制造依赖环。

迁移完成后至少比对 manifest 与输出 `.java` 的相对路径集合；数量少了也算失败。随后按 `environment` 将全部输出恢复到拟定的官方源码根，如 `modern/src/main/java` / `modern/src/client/java`。复制完整语言资源、图标、两套 mixin JSON、许可证、原有测试；只改必要的模组元数据与 mixin 目标。旧根 `src/` 和所有旧节点保持独立。

### 独立 Mercury 备选（避免 Gradle 内部选项限制）

本地已缓存可用组件：

- `net.fabricmc:mercury:0.4.3` / `0.4.2`（包含重定位后的 Eclipse JDT）。
- `net.fabricmc:mercurymixin:0.2.2`。
- `net.fabricmc:lorenz-tiny:4.0.2`、`org.cadixdev:lorenz:0.5.7`。
- `net.fabricmc:mapping-io:0.8.0` / `0.7.1`，以及所需 Bombe 等传递依赖。

这些组件都有实际 class，不需要用正则猜 Java 成员名。可在独立 Java 小工具中复现 **Loom 1.17.21 的同一算法**：

```java
MemoryMappingTree yarnTree = new MemoryMappingTree();
MappingReader.read(yarnMappingsPath, yarnTree);
MemoryMappingTree mojangTree = new MemoryMappingTree();
MappingReader.read(mojangMappingsPath, mojangTree);
MappingSet mappings = new TinyMappingsJoiner(
    yarnTree, "named", mojangTree, "named", "intermediary").read();
Mercury mercury = new Mercury();
mercury.setSourceCompatibility("21");
mercury.setEncoding(StandardCharsets.UTF_8);
mercury.setGracefulClasspathChecks(true);
mercury.getClassPath().addAll(fullNamedClasspath);
mercury.getProcessors().add(MixinRemapper.create(mappings));
mercury.getProcessors().add(MercuryRemapper.create(mappings));
mercury.rewrite(inputPath, outputPath);
```

`fullNamedClasspath` 必须使用 **1.21.11 Yarn 环境**的 main/client compileClasspath、该节点编译输出、所有第三方 remapped JAR、Minecraft named/intermediary JAR；不能混入 26.x 的官方 API。先通过只输出 classpath 路径的 Gradle 任务取得准确列表，别把整个 Gradle 缓存所有版本统统塞进解析器。

目标官方 Tiny 已在本机存在：`.gradle-home/caches/fabric-loom/1.21.11/loom.mappings.1_21_11.layered+hash.2198-v2/mappings.tiny`，头为 `tiny 2 0 official intermediary named`，`named` 中已核对为 `com/mojang/math/Axis` 等官方名字。源 Tiny 应用**完成构建的 1.21.11 Yarn 节点本次缓存路径**，不能用别的 MC 版本拼接。

`tiny-remapper` 用于字节码重映射，不能代替 Java AST 源码迁移；映射迁移也不会修复 1.21.11→26.1.2 的真实 API 删除与行为改变。

### 步骤 3：装配全功能 no-remap 构建

首先尝试当前同版 Loom 的单构建方案：26.x 节点单独脚本使用 `net.fabricmc.fabric-loom:1.17.21`，`minecraft 'com.mojang:minecraft:26.1.2'`，没有 `mappings`。明确指定独立官方源码根，并检查 `compileJava.source` / `compileClientJava.source` 文件清单，确认没有旧 Yarn 共享文件混入。

以下要从现有完整脚本逐项保留：main/client split、入口点、全部网络实现、命令与权限、HUD/边界绘制、配置与编辑器、Xaero/BlueMap/LuckPerms 可选联动、资源展开、许可证、JUnit、sources JAR。改 `implementation` / `compileOnly` / 运行期配置与 `jar`，**不是删掉版本敏感源码来拿到一个小 JAR**。

26.x 的 Modrinth 版本号已从 `25.3.2_Fabric_...` 变为 `fabric-26.1.2-26.6.2` 形式，现有 `split('_Fabric_')[0]` 的 suggests 提取逻辑不能沿用。门面严格支持的版本号应读实际 JAR 的 `fabric.mod.json`，与编译坐标同步更新。

若同构建遇到可复现的 Loom/Stonecutter 类加载或源根耦合问题，切换**独立 included build**，结构例如：

```text
modern/
  settings.gradle       # 独立 pluginManagement / 仓库 / 节点列表
  build.gradle          # no-remap Loom 1.17.21 完整构建脚本
  gradle.properties     # 明确导入或自有参数，不假定继承外层属性
  src/main/java/        # 全量官方命名共享代码
  src/client/java/
  src/main/resources/   # 也可明确指向外层共享资源；Java 源根不得混用
  src/client/resources/
  versions/26.1.2/      # 真实版本差异实现
  versions/26.2/
  versions/26.3/
```

根 settings 可声明：

```groovy
includeBuild('modern') { name = 'areas-hint-modern' }
```

根控制器提供显式任务依赖：

```groovy
tasks.register('buildModern') {
    dependsOn gradle.includedBuild('areas-hint-modern').task(':build')
}
```

先 `gradlew -p modern <目标>:build` 单独确认，再 `gradlew buildModern` 验证组合入口。included build 隔离的是配置、插件、仓库与属性，不是另起任意版本的 Gradle：复合调用仍使用外层运行环境，不能靠 included build 内的 Wrapper 偷换 Gradle/JVM。依据：[Gradle Composite Builds](https://docs.gradle.org/current/userguide/composite_builds.html)。

### 步骤 4：迁移验收与后续节点

先在 **1.21.11 官方映射**环境编译迁移输出，可将「改名不完整」与「26.x API 已变」分离定位；这是临时验证节点，不冒充新增目标支持。随后切至 **26.1.2 / Java 25** 逐项修复真实断点，再顺序推进 26.2、26.3，每个节点单独构建、检查 JAR、提交与更新交接记录。

重点检查如下：

- Mixin 的字符串方法名、descriptor、`@At(target=...)`、反射读取的 Minecraft 成员；MixinRemapper 有帮助但不能替代运行验证。
- Fabric 26.x 的 API 改名和 HUD 注册替换；字面量网络频道、JSON 字段、翻译 key 不应随 Java 命名迁移改变。
- Java 25 工具链发现；不可把 `J:/mc/jdk25` 固定写入发布配置。先用现有环境提供的 JDK，必要时显式传 Gradle 工具链路径。
- 26.2 GUI/HUD 拆分与图形后端；26.3 SDL/InputConstants 与文本焦点同步。
- 全功能类清单与入口点、完整语言文件集合、mixin 配置、可选联动的运行环境及源码数量；只看到 `BUILD SUCCESSFUL` 不足以证明迁移完整。

目前未验证：同构建 remap/no-remap 的真实配置、补齐迁移 classpath 的 init script、完整 1.21.11 官方映射编译、真实 26.x 模组运行。本节只提供已核实工具契约与可执行步骤，不将这些待办记为完成。
