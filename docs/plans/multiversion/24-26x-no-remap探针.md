# 24 · 26.x no-remap Loom 受控探针

> 执行：deps-official（task-19）｜日期：2026-10-03｜探针工程：`J:\mc\26x-probe`（**仓库外**，不进 git）
> 探针源码与原始日志：`J:\mc\26x-probe`、`J:\mc\26x-probe\logs\01..06-*.log`（原始输出**全文**在那里，本文件只摘关键行）
> 纪律：本次只写了本文件一个仓库文件；**没有跑仓库的 gradle**；没有 git add/commit/push；没有改 build.gradle / settings.gradle / gradle.properties。

---

## 零、结论

**可行。** 不重映射的 Loom 能在 **Gradle 9.5.1（仓库现役版本，不用升）** 上给 Minecraft **26.1.2** 编出 jar，全程不需要任何 mappings。

**但有一个硬约束（实测得来的，不是猜的）**：

| 组合 | 结果 |
|---|---|
| Loom **1.18.2** + Gradle 9.5.1 + JVM 21 | ✗ 构建脚本类路径解析失败：Loom 要求 **JVM 25** |
| Loom **1.18.2** + Gradle 9.5.1 + JVM 25 | ✗ 仍然失败：Loom 声明 `org.gradle.plugin.api-version = 9.7.0`，而消费者是 **9.5.1** |
| Loom **1.17.21** + Gradle 9.5.1 + JVM 21 | ✓ **成功**（插件加载、MC 解析、Java 25 编译、出 jar 全通） |

→ **仓库要保留 Gradle 9.5.1，26.x 节点就必须用 Loom ≤ 1.17.21。**
→ 若坚持用 Loom 1.18.2，则必须同时把 Wrapper 升到 **9.7.0** 且把 Gradle 守护 JVM 换成 **25**（两条都改，只改一条仍失败，上面两行就是证据）。

**已实测产物**：`J:\mc\26x-probe\build\libs\probe-26x-0.0.1.jar`（1274 字节），内含 `probe/Probe.class`、`probe/McRef.class`，`Probe.class` 字节码 **major=69（Java 25）**。

**三个前置条件**：
1. 26.x 节点用插件 id `net.fabricmc.fabric-loom`，版本 **1.17.21**；
2. **不写 mappings**，直接 `minecraft "com.mojang:minecraft:26.1.2"`（Loom 自动解析成 `net.minecraft:minecraft-merged-deobf:26.1.2`）；
3. 必须让 Gradle **能找到 JDK 25**：本机 `~/.gradle/jdks` 里的 eclipse_adoptium-25 **没有被自动探测到**（实测报错），要显式给路径。

---

## 一、探针工程（仓库外）

```
J:\mc\26x-probe\
├── gradlew / gradlew.bat / gradle/wrapper/**      ← 从仓库原样复制（Gradle 9.5.1）
├── settings.gradle                                ← pluginManagement: Fabric maven + mavenCentral + gradlePluginPortal
├── gradle.properties                              ← 只有一行：org.gradle.java.installations.paths=J:/mc/jdk25
├── build.gradle                                   ← 见下
├── src/main/java/probe/Probe.java                 ← 空类
├── src/main/java/probe/McRef.java                 ← 只引用官方名 MC 类型的类
└── logs/01..06-*.log                              ← 每次运行的原始输出全文
```

最终 `build.gradle`（就是跑出 BUILD SUCCESSFUL 的那一份）：

```groovy
// 26.x no-remap Loom 受控探针：只验证「插件能否解析 + MC 26.1.2 能否解析 + 能否出 jar」
plugins {
    id 'net.fabricmc.fabric-loom' version '1.17.21'
}

version = '0.0.1'
group = 'probe'

base {
    archivesName = 'probe-26x'
}

repositories {
    maven {
        name = 'Fabric'
        url = 'https://maven.fabricmc.net/'
    }
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // no-remap 模式下只声明 Minecraft 本体，不声明任何 mappings
    minecraft "com.mojang:minecraft:26.1.2"
}
```

`gradle.properties`：

```properties
# 显式指定本机 JDK 25（ASCII 路径，避开 gradle.properties 的 ISO-8859-1 编码问题）
org.gradle.java.installations.paths=J:/mc/jdk25
```

> `J:\mc\jdk25` 是我为本探针建的**目录 junction**，指向 `%USERPROFILE%\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2`（Temurin 25.0.4+7）。建 junction 的唯一原因是原路径含中文字符，写进 `gradle.properties` 会被按 ISO-8859-1 解析。

---

## 二、实测过程（原始命令 + 原始输出）

### 步骤 a-1：插件 id 能不能解析 —— 能，但 Loom 1.18.2 挡在 JVM 上

命令：
```powershell
Set-Location 'J:\mc\26x-probe'
.\gradlew.bat build --console=plain --stacktrace
# 环境：JAVA_HOME=C:\Program Files\Java\jdk-21，Gradle 9.5.1
```

原始输出（`logs/01-build-loom-1.18.2-mc26.1.2.log`，54 KB 全文）：
```
FAILURE: Build failed with an exception.

* What went wrong:
A problem occurred configuring root project 'probe-26x'.
> Could not resolve all artifacts for configuration 'classpath'.
   > Could not resolve net.fabricmc:fabric-loom:1.18.2.
     Required by:
         buildscript of root project 'probe-26x' > net.fabricmc.fabric-loom:net.fabricmc.fabric-loom.gradle.plugin:1.18.2
      > Dependency requires at least JVM runtime version 25. This build uses a Java 21 JVM.

* Try:
> Run this build using a Java 25 or newer JVM.
BUILD FAILED in 6s
```

**判读**：插件 id `net.fabricmc.fabric-loom` **解析成功了**（能从 maven.fabricmc.net 找到 marker，并指向 `net.fabricmc:fabric-loom:1.18.2`）——失败点已经推进到"这个插件本身要什么运行时"。

### 步骤 a-2：把守护 JVM 换成 25 —— 换了一个失败点

命令：
```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2"
Set-Location 'J:\mc\26x-probe'
.\gradlew.bat build --console=plain --stacktrace
```

原始输出（`logs/02-build-jvm25.log`，58 KB 全文）：
```
* What went wrong:
A problem occurred configuring root project 'probe-26x'.
> Could not resolve all artifacts for configuration 'classpath'.
   > Could not resolve net.fabricmc:fabric-loom:1.18.2.
     Required by:
         buildscript of root project 'probe-26x' > net.fabricmc.fabric-loom:net.fabricmc.fabric-loom.gradle.plugin:1.18.2
      > No matching variant of net.fabricmc:fabric-loom:1.18.2 was found. The consumer was configured to find a library for use during runtime, compatible with Java 25, packaged as a jar, and its dependencies declared externally, as well as attribute 'org.gradle.plugin.api-version' with value '9.5.1' but:
          - Variant 'runtimeElements' declares a library for use during runtime, packaged as a jar, and its dependencies declared externally:
              - Incompatible because this component declares a component, as well as attribute 'org.gradle.plugin.api-version' with value '9.7.0' and the consumer needed a component, as well as attribute 'org.gradle.plugin.api-version' with value '9.5.1'
BUILD FAILED in 6s
```

**判读**：**Loom 1.18.2 要求 Gradle 9.7.0**。仓库 Wrapper 是 9.5.1，所以 1.18.x 这条线必须连 Gradle 一起升。

### 步骤 a-3：降 Loom 到 1.17.21 —— 插件过了，卡在工具链

命令：把 `build.gradle` 里插件版本改成 `1.17.21`，其余不动，用回默认 JVM 21。
```powershell
Set-Location 'J:\mc\26x-probe'
.\gradlew.bat build --console=plain
```

原始输出（`logs/03-build-loom-1.17.21.log`，全文 1137 字节）：
```
> Configure project :
Fabric Loom: 1.17.21

FAILURE: Build failed with an exception.

* What went wrong:
Could not determine the dependencies of task ':compileJava'.
> Could not resolve all dependencies for configuration ':compileClasspath'.
   > Failed to calculate the value of task ':compileJava' property 'javaCompiler'.
      > Cannot find a Java installation on your machine (Windows 11 10.0 amd64) matching: {languageVersion=25, vendor=any vendor, implementation=vendor-specific, nativeImageCapable=false}. Toolchain download repositories have not been configured.
BUILD FAILED in 28s
```

**判读**：两件事同时被证明——
1. **`Fabric Loom: 1.17.21` 在 Gradle 9.5.1 + JVM 21 上成功加载**（插件这一关过了）；
2. **本机 `~/.gradle/jdks` 里的 JDK 25 不会被自动探测到**（这是本次探针最有价值的坑之一），必须显式指路。

### 步骤 b/c/d：显式给 JDK 25，一次全通

命令：
```powershell
# gradle.properties 增加一行：org.gradle.java.installations.paths=J:/mc/jdk25
Set-Location 'J:\mc\26x-probe'
.\gradlew.bat build --console=plain
```

原始输出（`logs/04-build-toolchain25.log`，全文 647 字节）：
```
> Configure project :
Fabric Loom: 1.17.21

> Task :compileJava
> Task :processResources NO-SOURCE
> Task :classes
> Task :processIncludeJars
> Task :compileTestJava NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test NO-SOURCE
> Task :validateAccessWidener NO-SOURCE
> Task :check UP-TO-DATE

> Task :jar
Could not determine Mixin version for jar manifest

> Task :assemble
> Task :build

BUILD SUCCESSFUL in 44s
3 actionable tasks: 3 executed
```

**判读**：
- **b（MC 26.1.2 坐标）** ✓：`minecraft "com.mojang:minecraft:26.1.2"` 解析成功，配置阶段无报错；
- **c（Java 工具链）** ✓：`languageVersion = 25` 生效并找到 JDK；
- **d（出 jar）** ✓：注意产物走的是 **`:jar`**，**没有 `remapJar`**（no-remap 的直接证据）；
- `Could not determine Mixin version for jar manifest` 是**无害警告**（探针没有 mixin 依赖），不影响产物。

### 步骤 d 加固：官方类名可直接编译 + 产物字节码 + 类路径实据

追加一个只引用官方名类型的类：
```java
package probe;

/** 只引用官方名（未混淆）MC 类型的探针：证明 no-remap 下官方类名可直接编译 */
public final class McRef {
    private McRef() {
    }

    public static Class<?> vanillaType() {
        return net.minecraft.SharedConstants.class;
    }
}
```

命令与原始输出（`logs/05-build-mcref.log`）：
```
> Task :compileJava
> Task :jar
> Task :build
BUILD SUCCESSFUL in 1s
```

命令与原始输出（`logs/06-dependencies-compileClasspath.log`，关键行）：
```
+--- com.mojang:authlib:7.0.63
+--- com.mojang:blocklist:1.0.10
+--- com.mojang:brigadier:1.3.10
+--- com.mojang:datafixerupper:9.0.19
+--- com.mojang:jtracy:1.0.37
+--- com.mojang:logging:1.6.11
+--- com.mojang:patchy:2.2.10
+--- com.mojang:text2speech:1.18.11
+--- net.minecraft:minecraft-merged-deobf:26.1.2
BUILD SUCCESSFUL in 1s
```

产物核对：
```
build/libs/probe-26x-0.0.1.jar   1274 bytes
jar 内容：META-INF/ , META-INF/MANIFEST.MF , probe/ , probe/McRef.class , probe/Probe.class
probe/Probe.class 字节码 major = 69   （69 = Java 25，61 = Java 17）
```

**判读**：`net.minecraft.SharedConstants` 是**官方（未混淆）类名**，能直接编译通过 → 26.x **不需要任何映射**，调研结论被实测确认。MC 本体以 `net.minecraft:minecraft-merged-deobf:26.1.2` 进入编译类路径。

### 步骤 a 的补充证据：Loom 各版本的 Gradle/JVM 要求（HTTP 实测）

数据源：`https://maven.fabricmc.net/net/fabricmc/fabric-loom/<版本>/fabric-loom-<版本>.module`（Gradle Module Metadata）：

| Loom 版本 | `org.gradle.plugin.api-version` | `org.gradle.jvm.version` | 能否用在仓库 Gradle 9.5.1 |
|---|---|---|---|
| 1.18.2 | **9.7.0** | **25** | ✗（实测两次失败） |
| 1.18.1 | 9.7.0 | 25 | ✗（未实测，元数据同 1.18.2） |
| **1.17.21** | **9.5.0** | **21** | ✓ **实测成功** |
| 1.17.1 | 9.5.0 | 21 | ✓（仅元数据核对） |
| 1.16.3 | 9.4.0 | 21 | ✓（仅元数据核对） |
| 1.15.5 / 1.15.1 | 9.2.0 | 21 | ✓（仅元数据核对） |

marker 侧证据：`net.fabricmc.fabric-loom.gradle.plugin` 的 `maven-metadata.xml` → `latest=1.18-SNAPSHOT`、**`release=1.18.2`**、共 130 个版本；marker POM 明确依赖 `net.fabricmc:fabric-loom:1.18.2`（即"插件 id → 实现构件"的映射）。
附带核对：remap 线的 `net.fabricmc.fabric-loom-remap` marker 同样是 `release=1.18.2`，两条线版本号同步发布——**所以只能靠官方文档区分语义，不能靠版本号**。

---

## 三、下一步：搬代码要改哪些构建脚本

1. **给 26.x 单独的 buildscript**（不要动 1.20.4 那套；`settings.gradle` 里已有"26.1/26.2 需要另建 buildscript"的注释）：
   ```groovy
   plugins {
       id 'net.fabricmc.fabric-loom' version '1.17.21'   // no-remap；1.18.x 会要 Gradle 9.7.0
   }
   dependencies {
       minecraft "com.mojang:minecraft:26.1.2"
       // 不写 mappings —— 26.1 起游戏不再混淆
   }
   ```
2. **三个 API 替换**（仅 26.x 节点）：`modImplementation` → `implementation`、`modCompileOnly` → `compileOnly`、`remapJar` → `jar`。
3. **Java 25 必须能被 Gradle 找到**，三选一：
   - `gradle.properties` 加 `org.gradle.java.installations.paths=<JDK25 路径>`（本探针用的就是这条，**路径里不要有中文**）；
   - 或在 `settings.gradle` 加 foojay-resolver 插件走自动下载；
   - 或把 26.x 的 Gradle 守护 JVM 直接设成 25（`org.gradle.java.home`）。
4. **Gradle 不动**：保持 Wrapper 9.5.1 是可行的；要上 Loom 1.18.2 才需要 → Wrapper 升 **9.7.0**（已确认 `gradle-9.7.0-bin.zip` 存在，HTTP 200）**且** 守护 JVM 升 25。
5. Stonecutter 侧：现在 `centralScript = 'build.gradle'` 对**所有**节点生效，26.x 与 ≤1.21.11 的插件 id、依赖配置名都不同，**不能**共用同一份脚本；具体怎么分派由 Lead 决定（本探针不涉及 Stonecutter）。

---

## 四、未验证 / 风险（别把这些当结论）

1. 探针只编译了**一个空类 + 一个只引用 MC 类型的类**。**没有**编译 Areas Hint 真实代码，所以"26.x 能否编过真实源码"仍未验证（越界引用规模见 `13-越界引用清单.md`）。
2. 探针**没有**声明 Fabric API（26.x 应为 `0.155.3+26.1.2`）与任何第三方坐标，也没有构造 `fabric.mod.json`；这些在 no-remap 下的解析与打包**未验证**。
3. 探针**没有** mixin、没有 mixin 配置，`Could not determine Mixin version for jar manifest` 只说明"缺 mixin 依赖"，真加 mixin 后是否需要额外配置**未验证**。
4. `org.gradle.java.installations.paths=J:/mc/jdk25` 里的 `J:\mc\jdk25` 是我为探针建的 junction，**属于本机环境耦合**；正式落地建议用 foojay-resolver 自动下载，或把守护 JVM 设为 25。
5. Loom 1.15.x / 1.16.x / 1.17.1 只做了元数据核对，**没有**逐个真实构建（按 Lead 的 30 分钟止损要求，确认 1.17.21 可行后就停下，没有连续试更多组合）。
6. 26.2 与 26.1.2 的差别（是否同样可用）**未单独实测**；本探针只覆盖 26.1.2。
7. 本次未运行任何仓库内的 gradle 命令；探针用的 Gradle 是复制到仓库外的 Wrapper（发行版仍是同一份 `gradle-9.5.1-bin`）。
