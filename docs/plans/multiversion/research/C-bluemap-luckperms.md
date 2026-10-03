# C · BlueMap API 与 LuckPerms API：10 个目标 MC 版本坐标 + 「是否需按 MC 版本区分」结论

- 调研员：`deps-bluemap`（共享任务 `task-3`，写入范围仅本文件）
- 调研时间：2026-10-03（Asia/Shanghai）
- 被查依赖（读自仓库现状，**未改动任何配置**）：`build.gradle` L136 `compileOnly "net.luckperms:api:${v}"`、L137 `compileOnly "de.bluecolored.bluemap:BlueMapAPI:${v}"`；值来自 `gradle.properties` 的 `deps.<mcVersion>.luckperms`（L45 现为 5.4）与 `deps.<mcVersion>.bluemap`（L44 现为 2.7.2）
- **未运行任何 gradle / gradlew 命令**；全部证据来自真实 HTTP 请求（`pwsh` + `Invoke-WebRequest`/`Invoke-RestMethod`，带 User-Agent 头）与下载后的 jar 检查
- 取证方式说明：本会话的 `web_fetch` 工具把这些域名判定为「resolves to a non-public IP address」而拒绝请求（本机 DNS 将它们解析到 198.18.2.x 本地代理网段），因此改用 `pwsh` 直连，所有状态码均为真实返回

---

## 0. 结论速览

1. **核心问题的直接回答：这两个依赖都「不需要按 MC 版本区分」。** 它们是平台/服务端插件 API，jar 内不含任何 MC 版本声明（详见 2.5、3.3）。计划文档 `02-阶段1-版本矩阵与依赖表.md` Step 4 中「它们同样不能跨版本沿用」的表述**需要修正**（该文件我不改，仅在此记录）。
2. **但 BlueMap 有一个必须处理的硬事实（本报告最重要的发现）：坐标改过名，且新坐标按目标 Java 字节码版本分层。**
   - 旧坐标 `de.bluecolored.bluemap:BlueMapAPI` 已**停更**：release 仓库只有 `2.7.1`、`2.7.2`，`lastUpdated=20240603205644`；`2.7.2` 仍可取（jar HTTP 200）。
   - 官方现用新坐标 **`de.bluecolored:bluemap-api`**（groupId/artifactId 都变了）：`2.7.3` … `2.8.1`，`latest=2.8.1`，`lastUpdated=20260925115028`。
   - Gradle module metadata 的 `org.gradle.jvm.version`：旧坐标 2.7.2 = **16**；新坐标 2.7.3–2.7.8 = **21**；2.8.0 / 2.8.0-6 / 2.8.1 = **25**。→ 在 `release = 17` 的 1.20.1 / 1.20.4 节点上，新坐标**在依赖解析阶段就会失败**（需要 Java 21+），只能用旧坐标 2.7.2。
   - 因此 BlueMap 的分档依据是「目标 Java 字节码版本」，而它恰好与 MC 版本分档重合（17 / 21 / 25），**不是**因为 API 里有 MC 版本相关内容。
3. **LuckPerms 完全无陷阱**：官方文档明确「API artifact is published to the **Maven Central** repository」，`net.luckperms:api` 在 Central 上有 `5.0 / 5.1 / 5.2 / 5.3 / 5.4 / 5.5` 共 6 个版本，全部 jar HTTP 200；最新 `5.5`（2025-05-25）。10 个目标版本可共用同一坐标（推荐 5.5，现有 5.4 也仍然可取）。
4. **旧 `repo.luckperms.net` 不可作为证据支撑**：本机 TLS 握手失败、明文 HTTP 空响应、两个独立第三方代理均返回 HTTP 522；而 Maven Central 与官方文档已足够，故 LuckPerms 仓库地址应写 `mavenCentral()`。
5. **JUnit `org.junit:junit-bom:5.12.2` 在 Maven Central 可取得**（metadata 在列 + POM HTTP 200），无需按 MC 版本区分。

---

## 1. 主表：10 个目标 MC 版本的可填坐标

「该版本要求的 Java」一列取自 Mojang 官方版本清单 `piston-meta`（https://piston-meta.mojang.com/mc/game/version_manifest_v2.json ，逐版本再取该版本 JSON 的 `javaVersion.majorVersion`），全部 `type=release`。

| 目标 MC | 该 MC 要求的 Java（实测） | BlueMap 编译期坐标（推荐，可直接填） | LuckPerms 编译期坐标（可直接填） | 证据 URL（均实际请求过） |
|---|---|---|---|---|
| 1.20.1 | 17 | `de.bluecolored.bluemap:BlueMapAPI:2.7.2`（jvm 16，新坐标不可用：需 21） | `net.luckperms:api:5.5` | BlueMap: https://repo.bluecolored.de/releases/de/bluecolored/bluemap/BlueMapAPI/2.7.2/BlueMapAPI-2.7.2.jar ｜ LP: https://repo1.maven.org/maven2/net/luckperms/api/5.5/api-5.5.jar |
| 1.20.4 | 17 | `de.bluecolored.bluemap:BlueMapAPI:2.7.2`（同上） | `net.luckperms:api:5.5` | 同上 |
| 1.20.6 | 21 | `de.bluecolored:bluemap-api:2.7.8`（jvm 21） | `net.luckperms:api:5.5` | BlueMap: https://repo.bluecolored.de/releases/de/bluecolored/bluemap-api/2.7.8/bluemap-api-2.7.8.jar |
| 1.21.1 | 21 | `de.bluecolored:bluemap-api:2.7.8` | `net.luckperms:api:5.5` | 同上 |
| 1.21.4 | 21 | `de.bluecolored:bluemap-api:2.7.8` | `net.luckperms:api:5.5` | 同上 |
| 1.21.5 | 21 | `de.bluecolored:bluemap-api:2.7.8` | `net.luckperms:api:5.5` | 同上 |
| 1.21.8 | 21 | `de.bluecolored:bluemap-api:2.7.8` | `net.luckperms:api:5.5` | 同上 |
| 1.21.11 | 21 | `de.bluecolored:bluemap-api:2.7.8` | `net.luckperms:api:5.5` | 同上 |
| 26.1 | 25 | `de.bluecolored:bluemap-api:2.8.1`（jvm 25；2.7.x 需 21，也兼容，但 2.8.1 才是当前官方版） | `net.luckperms:api:5.5` | BlueMap: https://repo.bluecolored.de/releases/de/bluecolored/bluemap-api/2.8.1/bluemap-api-2.8.1.jar |
| 26.2 | 25 | `de.bluecolored:bluemap-api:2.8.1` | `net.luckperms:api:5.5` | 同上 |

**没有任何一行是「该版本无可用坐标」——10/10 版本两个依赖都有真实可取的坐标。**

补充：如果希望**10 个节点统一成一个坐标**、不做任何分档，可以全部使用旧坐标 `de.bluecolored.bluemap:BlueMapAPI:2.7.2`：它 `org.gradle.jvm.version=16`，可被 17/21/25 的 consumer 消费（producer ≤ consumer 即可），且它的公开成员是新版 2.8.1 的子集（见 2.7），仍能解析。代价是停留在 2024-06-03 的旧坐标、拿不到 2.8.x 新增 API（`PlayerDisplayNameProvider`）。**取舍由 Lead 决定，我不改任何构建文件。**

---

## 2. BlueMap 证据链（`de.bluecolored.bluemap:BlueMapAPI` → `de.bluecolored:bluemap-api`）

### 2.1 旧坐标 release 仓库的 maven-metadata.xml（原文）

请求：`https://repo.bluecolored.de/releases/de/bluecolored/bluemap/BlueMapAPI/maven-metadata.xml` → **HTTP 200**

```xml
<metadata>
  <groupId>de.bluecolored.bluemap</groupId>
  <artifactId>BlueMapAPI</artifactId>
  <versioning>
    <latest>2.7.2</latest>
    <release>2.7.2</release>
    <versions>
      <version>2.7.1</version>
      <version>2.7.2</version>
    </versions>
    <lastUpdated>20240603205644</lastUpdated>
  </versioning>
</metadata>
```

- **结论 1：`2.7.2` 仍可取（latest = release = 2.7.2），但该坐标下不存在更高版本。**
- 旁证：snapshots 仓库 `https://repo.bluecolored.de/snapshots/de/bluecolored/bluemap/BlueMapAPI/maven-metadata.xml` → 200，只有 `2.7.0-5 / 2.7.1-1 / 2.7.1-4`，`lastUpdated=20240527202316`，同样停更。

### 2.2 新坐标的 maven-metadata.xml（原文）

请求：`https://repo.bluecolored.de/releases/de/bluecolored/bluemap-api/maven-metadata.xml` → **HTTP 200**

```xml
<metadata>
  <groupId>de.bluecolored</groupId>
  <artifactId>bluemap-api</artifactId>
  <versioning>
    <latest>2.8.1</latest>
    <release>2.8.1</release>
    <versions>
      <version>2.7.3</version><version>2.7.4</version><version>2.7.5</version>
      <version>2.7.6</version><version>2.7.7</version><version>2.7.8</version>
      <version>2.8.0</version><version>2.8.0-6</version><version>2.8.1</version>
    </versions>
    <lastUpdated>20260925115028</lastUpdated>
  </versioning>
</metadata>
```

- **结论 2：更高版本确实存在，但换到了新坐标 `de.bluecolored:bluemap-api`；截止调研时最新为 `2.8.1`（2026-09-25）。**
- 官方维基原文（https://raw.githubusercontent.com/wiki/BlueMap-Minecraft/BlueMapAPI/Home.md ，HTTP 200）：
  > With Gradle or Maven you can use our Maven Repository (https://repo.bluecolored.de/#/releases/de/bluecolored/bluemap-api) … `compileOnly 'de.bluecolored:bluemap-api:2.7.7'` … Make sure your dependency is `compileOnly` or `<scope>provided</scope>`!
- GitHub 侧一致：BlueMapAPI 仓库 tag 从 `v2.2.0` 到 `v2.8.1`，release `v2.8.1` 发布于 2026-09-25（https://api.github.com/repos/BlueMap-Minecraft/BlueMapAPI/releases ）。
- **该坐标不在 Maven Central**：`https://repo1.maven.org/maven2/de/bluecolored/bluemap-api/maven-metadata.xml` → HTTP 404。必须保留 `maven { url 'https://repo.bluecolored.de/releases' }`（仓库 `build.gradle` L84 已有）。

### 2.3 jar 存在性（HTTP HEAD / 实际下载）

| 坐标 | 请求 URL | 结果 |
|---|---|---|
| `BlueMapAPI:2.7.2`（旧） | https://repo.bluecolored.de/releases/de/bluecolored/bluemap/BlueMapAPI/2.7.2/BlueMapAPI-2.7.2.jar | HEAD 200；实际下载 **67740 字节**，SHA-256 `3e664f4f11d6599c783c470ad64f7c487a4cb0627d15f501d5e4f74c994b9372` |
| `BlueMapAPI:2.7.1`（旧） | https://repo.bluecolored.de/releases/de/bluecolored/bluemap/BlueMapAPI/2.7.1/BlueMapAPI-2.7.1.jar | HEAD 200 |
| `BlueMapAPI:2.7.0`（旧） | https://repo.bluecolored.de/releases/de/bluecolored/bluemap/BlueMapAPI/2.7.0/BlueMapAPI-2.7.0.jar | **HTTP 404**（metadata 里也没有，一致） |
| `bluemap-api:2.7.3` | https://repo.bluecolored.de/releases/de/bluecolored/bluemap-api/2.7.3/bluemap-api-2.7.3.jar | 200，下载 68064 字节，SHA-256 `e1ec950b077a58d1dfd1687056cc086746374f9437a596824d3872a22d8c2dc3` |
| `bluemap-api:2.7.8` | https://repo.bluecolored.de/releases/de/bluecolored/bluemap-api/2.7.8/bluemap-api-2.7.8.jar | 200，下载 68334 字节，SHA-256 `0c3c47cc819af283d8bb16b5b7e42440e6fac8a467a7d08509178f3582cca8d9` |
| `bluemap-api:2.8.1` | https://repo.bluecolored.de/releases/de/bluecolored/bluemap-api/2.8.1/bluemap-api-2.8.1.jar | 200，下载 68816 字节，SHA-256 `c5345d909a505a1338f2a3d9843d93fbe1a2c6442567f53dad60269b977a168b` |
| 2.7.3 / 2.7.8 / 2.8.0 / 2.8.1 的旧坐标探测 | https://repo.bluecolored.de/releases/de/bluecolored/bluemap/BlueMapAPI/<v>/BlueMapAPI-<v>.jar | 全部 **404**（证实旧坐标已冻结在 2.7.2） |

### 2.4 `org.gradle.jvm.version`（Gradle module metadata，决定能否被某节点消费）

逐个请求 `…/<v>/<artifact>-<v>.module` 后读取 `variants[apiElements].attributes['org.gradle.jvm.version']`：

| 坐标 | 版本 | jvm.version |
|---|---|---|
| `de.bluecolored.bluemap:BlueMapAPI` | 2.7.2 | **16** |
| `de.bluecolored:bluemap-api` | 2.7.3 / 2.7.4 / 2.7.5 / 2.7.6 / 2.7.7 / 2.7.8 | **21** |
| `de.bluecolored:bluemap-api` | 2.8.0 / 2.8.0-6 / 2.8.1 | **25** |

配合第 1 表实测的 MC Java 要求：1.20.1 / 1.20.4 = Java 17 ⇒ 只能吃 2.7.2（jvm 16）；1.20.6–1.21.11 = Java 21 ⇒ 可吃 2.7.3–2.7.8；26.1 / 26.2 = Java 25 ⇒ 可吃 2.8.x。

### 2.5 jar 内不含 MC / 平台相关声明（「不需要按 MC 版本区分」的直接证据）

对 `BlueMapAPI-2.7.2.jar`（60 个条目）逐项检查：

- 顶层目录只有 `de/` 与 `META-INF/`；**没有 `fabric.mod.json`、`plugin.yml`、`paper-plugin.yml`、`mods.toml`、`mcmod.info`**。
- 全 jar 字节扫描（Latin-1 全文正则计数）：`org/bukkit`=0、`net/minecraft`=0、`net/fabricmc`=0、`io/papermc`=0、`org/spongepowered`=0、`net/neoforged`=0、`forge`=0。
- 全部 48 个类都在 `de/bluecolored/bluemap/api/**` 下（`BlueMapAPI`、`BlueMapMap`、`BlueMapWorld`、`markers/*`、`math/*`、`plugin/*`、`gson/*` 等）。
- POM（https://repo.bluecolored.de/releases/de/bluecolored/bluemap/BlueMapAPI/2.7.2/BlueMapAPI-2.7.2.pom ，200）只有两个依赖：`com.flowpowered:flow-math:1.0.3`、`com.google.code.gson:gson:2.8.0`；2.8.1 的 POM 同样只有 `flow-math:1.0.3` + `gson:2.8.9`。没有任何 Minecraft/加载器依赖。
- jar 内 `de/bluecolored/bluemap/api/version.json` 内容为 `{"version": "2.7.2", "git-hash": "ec977113495dacd6f2e24239015f4b94b305fc52"}` —— 这个版本号是 **API 自己的版本**，与 MC 版本无关。

**上游把该坐标定义为「平台无关的纯 Java API」**，这也是官方维基要求 `compileOnly` 的原因：运行时的 API 实现由服务端的 BlueMap 插件/模组提供。

### 2.6 「同一个 jar 覆盖多个 MC 版本」的上游事实

- BlueMap 的 GitHub release 每个版本只出 per-platform 的一个 jar（`bluemap-5.28-fabric.jar`、`-paper.jar`、`-forge.jar`、`-neoforge.jar`、`-spigot.jar`、`-sponge.jar`、`-cli.jar`），来源 https://api.github.com/repos/BlueMap-Minecraft/BlueMap/releases （v5.28 发布于 2026-09-25）。
- Modrinth 项目 `bluemap`（project_id `swbUV1cr`）的 `game_versions` 覆盖 1.13 → 26.3；LuckPerms 同理（见 3.4）。请求：https://api.modrinth.com/v2/project/bluemap
- 实测 `bluemap-5.28-paper.jar`（6285353 字节，https://github.com/BlueMap-Minecraft/BlueMap/releases/download/v5.28/bluemap-5.28-paper.jar ）内嵌的 `de/bluecolored/bluemap/api/*.class` 共 49 个，**与 `bluemap-api:2.8.1` 的 49 个类集合完全一致（多 0 个、少 0 个）**；其 `plugin.yml` 为 `version: "5.28"`、`api-version: "26.1.1"`。

### 2.7 版本间二进制兼容性（javap 公开成员比对）

用本机 JDK 21 的 `javap`（仅读取 class，未跑 gradle）对同名类逐个 dump 公开成员并 diff：

- `BlueMapAPI:2.7.2`（旧）vs `bluemap-api:2.7.3`（新）：**公开成员完全一致（onlyA=0、onlyB=0）** ⇒ 2.7.3 相对 2.7.2 是纯「换坐标 + 重新构建」。
- `BlueMapAPI:2.7.2`（旧）vs `bluemap-api:2.8.1`（新）：48 个共有类中仅 3 行差异，且**没有删除任何方法**：
  - `BlueMapAPI.onEnable/registerInstance/unregisterInstance` 去掉了 `synchronized` 修饰符（方法描述符不变，二进制兼容）；
  - 新增 `getPlayerDisplayNameProvider()` / `setPlayerDisplayNameProvider(...)` 两个抽象方法（对应 2.8.0 的 PR #9「Add API to allow addons to customize display names」）。
  - 结论：**用 2.7.2 编译出来的调用代码，在 BlueMap 5.28（内嵌 API 2.8.1）上不会因为 API 缺失而 `NoSuchMethodError`**；反向（用 2.8.1 编译、运行时是 2.7.x 的老 BlueMap）才会因新方法缺失而出问题。

### 2.8 与本仓库 `build.gradle` 结构的联动（提示，不修改文件）

`build.gradle` L137 把 group:artifact 写死为 `de.bluecolored.bluemap:BlueMapAPI`，只让 `${v}` 变化：
```groovy
declareThirdParty('bluemap', 'BlueMap') { v -> compileOnly "de.bluecolored.bluemap:BlueMapAPI:${v}" }
```
若采用第 1 表的「按 Java 分档」方案，1.20.6+ 需要换成 `de.bluecolored:bluemap-api`，**这个闭包必须能按版本切换 group:artifact（或增加一个 `deps.<v>.bluemap_coord` 之类的完整坐标键）**，否则新坐标无法生效。若采用「全版本统一 2.7.2」，现有写法无需改动。

---

## 3. LuckPerms 证据链（`net.luckperms:api`）

### 3.1 官方仓库地址 = Maven Central（官方文档原文）

官方维基页（源文件 https://raw.githubusercontent.com/LuckPerms/wiki/master/pages/Developer-API.md ，HTTP 200，页面 https://luckperms.net/wiki/Developer-API ）原文：

> The API artifact is published to the [Maven Central](http://central.sonatype.org/) repository.
> ```gradle
> repositories { mavenCentral() }
> dependencies { compileOnly 'net.luckperms:api:5.5' }
> ```
> The current API release is `5.5`.
> The API uses Semantic Versioning … You can rest assured knowing your integration will not break between versions, providing the major version remains the same.

上游构建文件（https://raw.githubusercontent.com/LuckPerms/LuckPerms/master/api/build.gradle ，200）也印证：`group = 'net.luckperms'`、`project.version = '5.5'`、`options.release = 8`、发布目标是 `https://oss.sonatype.org/service/local/staging/deploy/maven2/`（Sonatype → Maven Central）。仓库根 README 的徽章同样指向 `repo1.maven.org/maven2/net/luckperms/api` 与 javadoc.io。

### 3.2 全部可用版本（Central metadata 原文 + jar 实检）

请求：`https://repo1.maven.org/maven2/net/luckperms/api/maven-metadata.xml` → **HTTP 200**

```xml
<metadata>
  <groupId>net.luckperms</groupId>
  <artifactId>api</artifactId>
  <versioning>
    <latest>5.5</latest>
    <release>5.5</release>
    <versions>
      <version>5.0</version><version>5.1</version><version>5.2</version>
      <version>5.3</version><version>5.4</version><version>5.5</version>
    </versions>
    <lastUpdated>20250525080913</lastUpdated>
  </versioning>
</metadata>
```

逐个 jar 实测（HTTP HEAD，均 200）：

| 版本 | jar URL | Content-Length | Last-Modified |
|---|---|---|---|
| 5.0 | https://repo1.maven.org/maven2/net/luckperms/api/5.0/api-5.0.jar | 118863 | 2019-11-23 |
| 5.1 | https://repo1.maven.org/maven2/net/luckperms/api/5.1/api-5.1.jar | 122383 | 2020-05-10 |
| 5.2 | https://repo1.maven.org/maven2/net/luckperms/api/5.2/api-5.2.jar | 130108 | 2020-10-14 |
| 5.3 | https://repo1.maven.org/maven2/net/luckperms/api/5.3/api-5.3.jar | 139704 | 2021-03-04 |
| **5.4（仓库现用）** | https://repo1.maven.org/maven2/net/luckperms/api/5.4/api-5.4.jar | 142302 | 2022-02-09，SHA-256 `086e3971ea63c0b5ad567881b2dfd955acbbffa24fe85ceac8abf54b114ce986` |
| **5.5（最新）** | https://repo1.maven.org/maven2/net/luckperms/api/5.5/api-5.5.jar | 149331 | 2025-05-25，SHA-256 `cd910f936adc4dee7705fb51cc29401fde4f29aba6ee73a6475a3a0515f6294f` |

- **`5.4` 确实存在且仍可取（HTTP 200，2022-02-09 发布）；最新版本是 `5.5`（2025-05-25）。**
- POM 侧：`5.4` 与 `5.5` 的 POM（`…/net/luckperms/api/5.5/api-5.5.pom`，200）**都不含任何 `<dependencies>`**；Gradle module metadata 的 `apiElements` / `runtimeElements` 两个变体 `dependencies` 均为空 ⇒ **零传递依赖**。
- `org.gradle.jvm.version = 8`（5.4 与 5.5 均是）⇒ 可被 Java 17 / 21 / 25 的任意节点消费。
- 5.5 的 MANIFEST 多一行 `Automatic-Module-Name: net.luckperms.api`（5.4 没有），对编译期依赖无影响。

### 3.3 是否需要按 MC 版本区分：证据与结论

**结论：不需要。** 证据：

1. **jar 内没有任何 MC / 平台绑定**：`api-5.4.jar`（231 条目）与 `api-5.5.jar`（241 条目）顶层只有 `net/`、`META-INF/`、`LICENSE.txt`；字节扫描 `org/bukkit`=0、`net/minecraft`=0、`net/fabricmc`=0、`io/papermc`=0、`org/spongepowered`=0、`net/neoforged`=0、`forge`=0；没有 `plugin.yml` / `fabric.mod.json` / `mods.toml`。
2. **它的版本号是 LuckPerms 自己的语义化版本**（官方文档：major 不变即保证不破坏兼容），不是 MC 版本号；一个 API 版本横跨所有受支持的 MC 版本。
3. **上游插件本身就是一个构建覆盖一大段 MC 版本**：Modrinth `luckperms`（project_id `Vebnzrzj`）最新 `v5.5.85-fabric` 的 `game_versions` 为 `26.1, 26.1.1, 26.1.2, 26.2, 26.3`（单个 jar），项目级 `game_versions` 覆盖 1.8.8 → 26.3。请求：https://api.modrinth.com/v2/project/luckperms/version?limit=4 、https://api.modrinth.com/v2/project/luckperms
4. 运行时实现由服务端的 LuckPerms 插件/模组提供（Fabric 上是 `LuckPerms-Fabric-5.5.85.jar`），编译期只需要 API 接口。

因此 10 个目标版本可以**共用同一个坐标**；推荐 `net.luckperms:api:5.5`（最新，且语义化版本保证与 5.4 兼容）；出于最小改动也可以保留 `5.4`，两者都真实可取。

### 3.4 关于 `repo.luckperms.net`（为什么不用它）

- 本机 `Invoke-WebRequest https://repo.luckperms.net/net/luckperms/api/maven-metadata.xml`（含 `-SkipCertificateCheck`）→ `The SSL connection could not be established`；
- `curl.exe https://repo.luckperms.net/…` → `schannel: failed to receive handshake, SSL/TLS connection failed`（`http_code=000`）；
- 明文 `http://repo.luckperms.net/…` → `curl: (52) Empty reply from server`；
- 两个独立第三方代理（`api.codetabs.com/v1/proxy`、`api.allorigins.win/raw`）请求同一 URL → 均 **HTTP 522**。

**无法证实 `repo.luckperms.net` 当前可用；但这不是问题**——官方文档已明确 API 发布在 Maven Central，Central 上 5.0–5.5 全部实测可取。本报告不把 `repo.luckperms.net` 列为可用仓库。

---

## 4. JUnit 5（顺带确认，一行结论）

`org.junit:junit-bom:5.12.2` 在 Maven Central 可取：`https://repo1.maven.org/maven2/org/junit/junit-bom/maven-metadata.xml`（200，`5.12.2` 在 `<versions>` 列表内，该文件当前 `latest=6.1.3`），且 `https://repo1.maven.org/maven2/org/junit/junit-bom/5.12.2/junit-bom-5.12.2.pom` → HTTP 200（Last-Modified 2025-04-11）。测试依赖与 MC 版本无关，10 个目标共用即可（`build.gradle` L151-L153 现有写法无需改动）。

---

## 5. 不确定项

1. **`repo.luckperms.net` 的真实状态无法判定**：本机 TLS 握手失败 + 明文空响应 + 两个第三方代理 522。可能已下线，也可能是本机网络/代理环境问题。由于官方文档已指定 Maven Central，且 Central 实测可用，此不确定项不影响任何坐标结论。
2. **`org.gradle.jvm.version` 的分档结论未经 Gradle 实跑验证**（规则禁止跑 gradle）。它来自 Gradle module metadata 的真实字段，但「Java 17 节点解析 jvm 21 依赖会失败」这一点是 Gradle 的既定行为推断，未在本仓库实测。若 Lead 要采用新坐标，请在实际构建里确认一次。
3. **BlueMap 各历史插件版本内嵌的 API 版本未逐一核验**：只实测了最新 `5.28-paper.jar`（内嵌类集合 == `bluemap-api:2.8.1`）。2.7.x 时期各 BlueMap 版本内嵌哪个 API 小版本，未逐个下载核对。
4. **方法级兼容性只比对了两个端点**：`2.7.2 → 2.8.1`、`2.7.2 → 2.7.3`（javap 公开成员）。`2.7.4/2.7.5/2.7.6/2.7.7/2.8.0` 未逐一比对（下载 2.7.8 的那次 javap 比对因目录未展开而失败，未重跑）。
5. **`web_fetch` 工具在本机对这些域名不可用**（判定为非公网 IP），因此所有证据来自 `pwsh` 直连 HTTP。上述 URL 均可复现，复现命令见第 6 节。
6. 26.1 / 26.2 的 Java 25 结论来自 Mojang 官方 `piston-meta`（`javaVersion.component = java-runtime-epsilon`）；实际 Fabric 工具链在 2026-10 是否已完全切到 JDK 25，属于另一议题，不是本报告结论。

---

## 6. 复现命令（PowerShell，未跑 gradle）

```powershell
$h = @{ 'User-Agent' = 'AreasHint-DepResearch/1.0' }
# BlueMap 旧坐标 / 新坐标版本清单
(Invoke-WebRequest 'https://repo.bluecolored.de/releases/de/bluecolored/bluemap/BlueMapAPI/maven-metadata.xml' -Headers $h).Content
(Invoke-WebRequest 'https://repo.bluecolored.de/releases/de/bluecolored/bluemap-api/maven-metadata.xml' -Headers $h).Content
# LuckPerms 版本清单
(Invoke-WebRequest 'https://repo1.maven.org/maven2/net/luckperms/api/maven-metadata.xml' -Headers $h).Content
# Gradle jvm 目标（决定能否被 Java 17/21/25 节点消费）
(Invoke-RestMethod 'https://repo.bluecolored.de/releases/de/bluecolored/bluemap-api/2.8.1/bluemap-api-2.8.1.module' -Headers $h).variants.attributes.'org.gradle.jvm.version'
(Invoke-RestMethod 'https://repo1.maven.org/maven2/net/luckperms/api/5.5/api-5.5.module' -Headers $h).variants.attributes.'org.gradle.jvm.version'
# 各 MC 版本要求的 Java
$man = Invoke-RestMethod 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json' -Headers $h
($man.versions | Where-Object id -eq '26.2' | ForEach-Object { (Invoke-RestMethod $_.url -Headers $h).javaVersion })
```
