# D - 官方依赖矩阵（Areas Hint 多版本适配）

> **调研员**：deps-official（共享任务 task-4）
> **调研日期**：2026-10-03（Asia/Shanghai）
> **证据标准**：表中每条坐标都由**真实 HTTP 请求**核对过（Modrinth API / maven-metadata.xml / Fabric meta / Mojang piston-meta），并额外对 Maven 目录做过 HTTP 200/404 探针。没有一条来自记忆或相近版本推断。
> **约束**：本文件是本人唯一写入的文件；全程**未运行任何 gradle/gradlew**；未修改仓库其它文件。
> **重要口径**：坐标为**原文**，请直接复制；「最新」= 2026-10-03 当天 maven-metadata.xml 的取值，上游仍在发版，未来会变。

---

## 一、结论速览（先看这 5 条）

1. **10 个目标 MC 版本全部真实存在，且全部是正式版（type=release）**，不是快照。其中 26.1 另有补丁版 26.1.1、26.1.2；26.2 无补丁版。
2. **Yarn 映射只到 1.21.11 为止**。26.1 / 26.2 **没有 Yarn，也没有 intermediary**（不是"没查到"，是**确定不存在**：Maven 目录 HTTP 404，元数据尾部停在 1.21.11）。
3. 原因是 **Minecraft 从 26.1 起不再混淆**（官方称之为 removing obfuscation），Fabric 官方决定从该版本起不再维护第三方映射（Yarn 2016–2025 终结，公告日期 2025-10-31）。
4. **Java 工具链不是 17/21 两档，而是 17/21/25 三档**：1.20.1、1.20.4 → **17**；1.20.6 ~ 1.21.11 → **21**；**26.1、26.2 → 25**（不是 21）。26.x 的 Fabric API 自身就声明 `java >=25`。
5. **Fabric Loader 0.19.5 对全部 10 个版本都可用**（Fabric meta 为每个版本都提供 0.19.5 的安装档；其字节码基线是 Java 7/8 + Multi-Release 17，因此 1.20.1 的 Java 17 目标也能跑）。

---

## 二、主表（每个 MC 版本一行）

> 「API 声明」列是从 `fabric-api-<版本>.jar` 里的 `fabric.mod.json` 实际读出的 `depends`，这正是本项目的痛点所在（跨版本沿用会「编译能过、运行时加载失败」），请以它为准做版本条件约束。

| # | MC 版本 | ① 存在性（类型/发布时间） | ② Yarn 映射坐标 | ③ Loader | ④ Fabric API 精确坐标 | ⑤ Java 工具链 | ⑥ 映射方案 / Loom 插件 | 证据 URL |
|---|---|---|---|---|---|---|---|---|
| 1 | **1.20.1** | release 2023-06-12 | `net.fabricmc:yarn:1.20.1+build.10`（classifier `:v2`） | 0.19.5 | `0.92.12+1.20.1`<br>声明 `minecraft >=1.20 <1.20.2-` / `fabricloader >=0.16.10` / `java >=17` | **17**（java-runtime-gamma） | Yarn + `net.fabricmc.fabric-loom-remap` | [manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json) · [yarn](https://meta.fabricmc.net/v2/versions/yarn/1.20.1) · [loader](https://meta.fabricmc.net/v2/versions/loader/1.20.1) · [api](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.92.12+1.20.1/) |
| 2 | **1.20.4** | release 2023-12-07 | `net.fabricmc:yarn:1.20.4+build.3` | 0.19.5 | `0.97.3+1.20.4`<br>声明 `minecraft >=1.20.3- <1.20.5-` / `fabricloader >=0.15.1` / `java >=17` | **17**（java-runtime-gamma） | Yarn + `net.fabricmc.fabric-loom-remap` | [manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json) · [yarn](https://meta.fabricmc.net/v2/versions/yarn/1.20.4) · [loader](https://meta.fabricmc.net/v2/versions/loader/1.20.4) · [api](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.97.3+1.20.4/) |
| 3 | **1.20.6** | release 2024-04-29 | `net.fabricmc:yarn:1.20.6+build.3` | 0.19.5 | `0.100.8+1.20.6`<br>声明 `minecraft >=1.20.5- <1.20.7-` / `fabricloader >=0.15.6` / `java >=21` | **21**（java-runtime-delta） | Yarn + `net.fabricmc.fabric-loom-remap` | [manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json) · [yarn](https://meta.fabricmc.net/v2/versions/yarn/1.20.6) · [loader](https://meta.fabricmc.net/v2/versions/loader/1.20.6) · [api](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.100.8+1.20.6/) |
| 4 | **1.21.1** | release 2024-08-08 | `net.fabricmc:yarn:1.21.1+build.3` | 0.19.5 | `0.116.17+1.21.1`<br>声明 `minecraft >=1.21- <1.21.2-` / `fabricloader >=0.15.11` / `java >=21` | **21**（java-runtime-delta） | Yarn + `net.fabricmc.fabric-loom-remap` | [manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json) · [yarn](https://meta.fabricmc.net/v2/versions/yarn/1.21.1) · [loader](https://meta.fabricmc.net/v2/versions/loader/1.21.1) · [api](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.116.17+1.21.1/) |
| 5 | **1.21.4** | release 2024-12-03 | `net.fabricmc:yarn:1.21.4+build.8` | 0.19.5 | `0.119.4+1.21.4`<br>声明 `minecraft >=1.21.4- <1.21.5-` / `fabricloader >=0.16.9` / `java >=21` | **21**（java-runtime-delta） | Yarn + `net.fabricmc.fabric-loom-remap` | [manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json) · [yarn](https://meta.fabricmc.net/v2/versions/yarn/1.21.4) · [loader](https://meta.fabricmc.net/v2/versions/loader/1.21.4) · [api](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.119.4+1.21.4/) |
| 6 | **1.21.5** | release 2025-03-25 | `net.fabricmc:yarn:1.21.5+build.1` | 0.19.5 | `0.128.2+1.21.5`<br>声明 `minecraft >=1.21.5- <1.21.6-` / `fabricloader >=0.16.10` / `java >=21` | **21**（java-runtime-delta） | Yarn + `net.fabricmc.fabric-loom-remap` | [manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json) · [yarn](https://meta.fabricmc.net/v2/versions/yarn/1.21.5) · [loader](https://meta.fabricmc.net/v2/versions/loader/1.21.5) · [api](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.128.2+1.21.5/) |
| 7 | **1.21.8** | release 2025-07-17 | `net.fabricmc:yarn:1.21.8+build.1` | 0.19.5 | `0.136.1+1.21.8`<br>声明 `minecraft >=1.21.8- <1.21.9-` / `fabricloader >=0.16.13` / `java >=21` | **21**（java-runtime-delta） | Yarn + `net.fabricmc.fabric-loom-remap` | [manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json) · [yarn](https://meta.fabricmc.net/v2/versions/yarn/1.21.8) · [loader](https://meta.fabricmc.net/v2/versions/loader/1.21.8) · [api](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.136.1+1.21.8/) |
| 8 | **1.21.11** | release 2025-12-09 | `net.fabricmc:yarn:1.21.11+build.6`（**最后一个有 Yarn 的正式版**） | 0.19.5 | `0.141.6+1.21.11`<br>声明 `minecraft >=1.21.11- <1.21.12-` / `fabricloader >=0.17.3` / `java >=21` | **21**（java-runtime-delta） | Yarn + `net.fabricmc.fabric-loom-remap`（官方文档推荐默认改用 Mojang 名称） | [manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json) · [yarn](https://meta.fabricmc.net/v2/versions/yarn/1.21.11) · [loader](https://meta.fabricmc.net/v2/versions/loader/1.21.11) · [api](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.141.6+1.21.11/) |
| 9 | **26.1** | release 2026-03-24（另有 **26.1.1** 04-01、**26.1.2** 04-09） | **该版本无 Yarn 坐标**（也不存在 intermediary） | 0.19.5 | `0.155.3+26.1.2`（最新，声明 `~26.1-`，覆盖 26.1/26.1.1/26.1.2）<br>备选：`0.145.1+26.1`（严格 +26.1 后缀的最后一版，同样声明 `~26.1-`）<br>两者均声明 `fabricloader >=0.18.4` / `java >=25` | **25**（java-runtime-epsilon）⚠ 非 21 | **无映射（该版本起游戏代码不再混淆）**；用 `net.fabricmc.fabric-loom`（no-remap） | [manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json) · [yarn(空)](https://meta.fabricmc.net/v2/versions/yarn/26.1) · [intermediary(404)](https://maven.fabricmc.net/net/fabricmc/intermediary/26.1/) · [api](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.155.3+26.1.2/) · [官方迁移文档](https://docs.fabricmc.net/develop/porting/mappings/) |
| 10 | **26.2** | release 2026-06-16（无补丁版） | **该版本无 Yarn 坐标**（也不存在 intermediary） | 0.19.5 | `0.161.0+26.2`<br>声明 `~26.2-` / `fabricloader >=0.18.4` / `java >=25` | **25**（java-runtime-epsilon）⚠ 非 21 | **无映射**；用 `net.fabricmc.fabric-loom`（no-remap） | [manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json) · [yarn(空)](https://meta.fabricmc.net/v2/versions/yarn/26.2) · [intermediary(404)](https://maven.fabricmc.net/net/fabricmc/intermediary/26.2/) · [api](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.161.0+26.2/) · [官方迁移文档](https://docs.fabricmc.net/develop/porting/mappings/) |

**可直接写进 build.gradle 的字符串（≤1.21.11）**

```groovy
// 以 1.20.4 为例（8 个 Yarn 版本同理，换成主表里的坐标）
minecraft "com.mojang:minecraft:1.20.4"
mappings  "net.fabricmc:yarn:1.20.4+build.3:v2"      // classifier 必须是 :v2
modImplementation "net.fabricmc:fabric-loader:0.19.5"
modImplementation "net.fabricmc.fabric-api:fabric-api:0.97.3+1.20.4"
```

---

## 三、逐项证据

### ① 版本是否真实存在（10/10 全部为正式版）

- 数据源：`https://piston-meta.mojang.com/mc/game/version_manifest_v2.json`（Manifest 共 917 个版本；当天 `latest.release = 26.3`，`latest.snapshot = 26.4-snapshot-2`）。
- 10 个目标全部命中，`type` 全部为 `release`（**没有一个是快照**）：

| 版本 | type | releaseTime（Manifest 原值） |
|---|---|---|
| 1.20.1 | release | 06/12/2023 21:25:51 |
| 1.20.4 | release | 12/07/2023 20:56:20 |
| 1.20.6 | release | 04/29/2024 20:40:45 |
| 1.21.1 | release | 08/08/2024 20:24:45 |
| 1.21.4 | release | 12/03/2024 18:12:57 |
| 1.21.5 | release | 03/25/2025 20:14:58 |
| 1.21.8 | release | 07/17/2025 20:04:02 |
| 1.21.11 | release | 12/09/2025 20:23:30 |
| 26.1 | release | 03/24/2026 20:11:04 |
| 26.2 | release | 06/16/2026 20:03:33 |

- 交叉验证：`https://meta.fabricmc.net/v2/versions/game`（Fabric 认可的游戏版本表，529 条）中 10 个版本的 `stable` 全为 `true`。
- **26.x 的补丁版**（Manifest 中 `26.*` 的全部 release）：`26.3`(09/15/2026)、`26.2`(06/16/2026)、`26.1.2`(04/09/2026)、`26.1.1`(04/01/2026)、`26.1`(03/24/2026)。→ 26.2 **没有** 26.2.x 补丁；26.1 有 26.1.1/26.1.2。

### ② Yarn 映射

- `https://maven.fabricmc.net/net/fabricmc/yarn/maven-metadata.xml`：共 **3412** 个版本，**尾部 8 个**为
  `1.21.11-rc2+build.1, 1.21.11-rc3+build.1, 1.21.11+build.1 … 1.21.11+build.6`
  → Yarn 在 **1.21.11+build.6** 之后**再也没有新版本**（26.x 一条都没有）。
- 逐版本端点 `https://meta.fabricmc.net/v2/versions/yarn/<MC>`：1.20.1 返回 10 条、1.20.4 3 条、1.20.6 3 条、1.21.1 3 条、1.21.4 8 条、1.21.5 1 条、1.21.8 1 条、1.21.11 6 条；**26.1 / 26.2 返回空数组**。
- Maven 目录探针（HEAD）：`yarn/1.20.1+build.10/` → **200**；`yarn/1.21.11+build.6/` → **200**；`yarn/26.1+build.1/` → **404**。
- 产物文件名实测（1.20.1+build.10 目录）：`yarn-1.20.1+build.10-v2.jar`、`…-mergedv2.jar`、`…-sources.jar`、`…-javadoc.jar`、`…-constants.jar` → 所以 Loom 里写 **`:v2`** classifier。

### ③ Fabric Loader

- `https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml`：共 253 个版本，`release = 0.19.5`。
- `https://meta.fabricmc.net/v2/versions/loader/<MC>`：**10 个版本全部返回 253 条，第 1 条都是 0.19.5**，说明 Fabric 官方为这 10 个版本都提供 loader 0.19.5。
- 安装档端点实测存在：`/v2/versions/loader/1.20.1/0.19.5/profile/json` → `fabric-loader-0.19.5-1.20.1`；`/v2/versions/loader/26.2/0.19.5/profile/json` → `fabric-loader-0.19.5-26.2`。
- **Java 兼容性自证**（我把 `fabric-loader-0.19.5.jar` 下载到内存并逐条读 class 头）：class 文件主版本分布 = **Java 7 × 177、Java 8 × 590、Java 17 × 1**（那 1 个在 `META-INF/versions/17/`，即 Multi-Release JAR）。
  → Loader 0.19.5 的基线字节码是 Java 7/8，**在 Java 17 上可以运行**，因此第 1、2 行（Java 17 目标）用它没问题。
- 结论：**0.19.5 可以作为 10 个版本统一的 loader 坐标**；若要与 26.1 发布时的官方口径对齐，最低要求是 `>=0.18.4`（26.x 的 Fabric API 声明的下限）。

### ④ Fabric API

- `https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml`：共 **1161** 个版本，`release = 0.161.2+26.4`（26.4 是快照线，不在本项目范围）。
- 主表里的坐标 == 该 MC 版本后缀下的**最新一版**；并与 Modrinth 独立交叉验证一致：
  `https://api.modrinth.com/v2/project/fabric-api/version?loaders=["fabric"]&game_versions=["<MC>"]`
  （Modrinth 返回值：1.20.1→0.92.12+1.20.1；1.20.4→0.97.3+1.20.4；1.20.6→0.100.8+1.20.6；1.21.1→0.116.17+1.21.1；1.21.4→0.119.4+1.21.4；1.21.5→0.128.2+1.21.5；1.21.8→0.136.1+1.21.8；1.21.11→0.141.6+1.21.11；26.1→0.155.3+26.1.2；26.2→0.161.0+26.2。）
- Maven 目录探针全部 **200**：`0.92.12+1.20.1/`、`0.141.6+1.21.11/`、`0.145.1+26.1/`、`0.155.3+26.1.2/`、`0.161.0+26.2/`。
- **26.1 该选哪个**：`0.145.1+26.1` 与 `0.155.3+26.1.2` 的 `fabric.mod.json` 都声明 `minecraft: "~26.1-"`（= ≥26.1 <26.2），所以**两者都能跑在 26.1/26.1.1/26.1.2 上**；要"最新"就取 `0.155.3+26.1.2`，要"后缀与目标版本字面一致"就取 `0.145.1+26.1`。
- 注意：现基线 `CLAUDE.md` 记录的 `0.97.2+1.20.4` 仍存在，但 1.20.4 线上已到 `0.97.3+1.20.4`。

### ⑤ Java 工具链（三档，不是两档）

- Mojang 官方逐版本 `javaVersion`（读取每个版本的 piston-meta JSON）：

| 版本 | javaVersion.component | majorVersion |
|---|---|---|
| 1.20.1 | java-runtime-gamma | **17** |
| 1.20.4 | java-runtime-gamma | **17** |
| 1.20.6 | java-runtime-delta | **21** |
| 1.21.1 | java-runtime-delta | **21** |
| 1.21.4 | java-runtime-delta | **21** |
| 1.21.5 | java-runtime-delta | **21** |
| 1.21.8 | java-runtime-delta | **21** |
| 1.21.11 | java-runtime-delta | **21** |
| 26.1 | java-runtime-epsilon | **25** |
| 26.2 | java-runtime-epsilon | **25** |

- Fabric 官方文档逐代口径（原文引用）：
  - `https://docs.fabricmc.net/1.20.4/develop/getting-started/setting-up-a-development-environment`：「To develop mods for Minecraft 1.20.4, you will need **JDK 17**.」
  - `https://docs.fabricmc.net/1.21.1/develop/getting-started/setting-up-a-development-environment`：「To develop mods for Minecraft 1.21.1, you will need **JDK 21**.」
  - `https://docs.fabricmc.net/1.21.11/develop/getting-started/setting-up`：「To develop mods for Minecraft 1.21.11, you will need **JDK 21**.」
  - `https://docs.fabricmc.net/develop/getting-started/setting-up`（当前 = 26.2 线）：「Install **JDK 25** / To develop mods for Minecraft 26.1, you will need **JDK 25**.」
  - `https://fabricmc.net/2026/03/14/261.html`（26.1 公告）：「Minecraft 26.1 requires **Java 25 minimum** for the Gradle JVM.」；同文要求 **Loom 1.15 + Gradle 9.4.0**，并提示 IntelliJ IDEA 2025.3+ 才能正确跑 mixin。
- 硬约束旁证：26.x 的 fabric-api `fabric.mod.json` 自带 `"java": ">=25"`；1.20.1/1.20.4 的是 `">=17"`；1.20.6~1.21.11 的是 `">=21"`。
- → **需要 3 套 JDK（17 / 21 / 25）**，26.x 的 Gradle JVM 必须是 25。

### ⑥ 26.x 没有 Yarn 时的替代方案（重点）

**结论：26.1 / 26.2 不需要任何映射，也不要用 Yarn 或 Mojang Mappings 的 `officialMojangMappings()` 思路去套——因为这两个版本的正式版游戏代码本身就没有被混淆。**

证据链（全部为官方一手来源）：

1. **26.1 是第一个不混淆的 Minecraft 正式版**：`https://fabricmc.net/2026/03/14/261.html` 原文「26.1 is the first version of Minecraft to not be obfuscated. Because of that, no mods from 1.21.11 or before will work without, at a minimum, recompilation.」
2. **Fabric 官方决定不再维护第三方映射**：同文「Yarn is no longer officially supported by Fabric」；前一篇公告 `https://fabricmc.net/2025/10/31/obfuscation.html`（2025-10-31）说明 Mojang 将移除混淆，及 Fabric 工具链的应对。
3. **官方迁移文档 `https://docs.fabricmc.net/develop/porting/mappings/`**（文档版本选择器显示 26.2）：「If you are planning on updating your mod to **26.1 or above**, you will need to migrate from **Yarn to Mojang Mappings**」；并解释「Minecraft 26.1 is **unobfuscated** and **includes parameter names**, so there is **no need for any obfuscation mappings**」。文档还给出两条迁移路径：Loom 的 `migrateMappings` 任务，或 IntelliJ 插件 **Ravel**（Fabric API 自己就是用 Ravel 迁移的，见其 PR #4690）。
4. **插件 id 换代（最容易踩的坑）**：26.1 公告原文「developers should switch from the old `net.fabricmc.fabric-loom-remap` or `fabric-loom` plugins to the new **`net.fabricmc.fabric-loom`** plugin, which does **not remap** Minecraft or mods. Any use of `modImplementation`, `modCompileOnly`, or similar should be switched out for the standard `implementation` or `compileOnly`, and any use of `remapJar` should be switched out for `jar`.」
   Maven 侧实测（两个 plugin marker 的版本区间**完全相同**，所以只能靠官方文档区分语义，别用版本号猜）：
   - `https://maven.fabricmc.net/net/fabricmc/fabric-loom/net.fabricmc.fabric-loom.gradle.plugin/maven-metadata.xml` → `release = 1.18.2`，共 130 个版本，从 `1.14.0-alpha.32` 起
   - `https://maven.fabricmc.net/net/fabricmc/fabric-loom-remap/net.fabricmc.fabric-loom-remap.gradle.plugin/maven-metadata.xml` → `release = 1.18.2`，共 130 个版本，从 `1.14.0-alpha.32` 起
   - （历史名 `fabric-loom-no-remap` 的 marker 停在 `1.14.0-alpha.31`，是拆分前的过渡产物，**不要用**：`https://maven.fabricmc.net/net/fabricmc/fabric-loom-no-remap/net.fabricmc.fabric-loom-no-remap.gradle.plugin/maven-metadata.xml`）
   - 历史插件 `fabric-loom`（旧 id，本项目现在用的那个）与 `net.fabricmc.fabric-loom-remap` 属 remap 线，只用于 ≤1.21.11 的节点。
5. **不需要 parchment**：26.1+ 已自带参数名（见上文档原文）；且 parchment 根本没有 26.1 的产物（`https://maven.parchmentmc.org/org/parchmentmc/data/parchment-26.1/maven-metadata.xml` → **404**）。parchment 只在"≤1.21.11 但改用 Mojang 映射"这条可选路线上有意义，例如 `parchment-1.21.11`(release 2025.12.20)、`parchment-1.21.8`(2025.09.14)、`parchment-1.20.1`(2023.09.03) 都存在。

**给 Stonecutter 的落地建议（供 Lead 决策，非我执行）**：26.1/26.2 两个节点用独立的 buildscript，插件用 `net.fabricmc.fabric-loom`（Loom 1.15+，当前最新 1.18.2），不要写 `mappings` 一行，依赖用 `implementation`/`compileOnly`，产物用 `jar`（不是 `remapJar`）；`fabric.mod.json` 里 `depends.minecraft` 用 `~26.1-` / `~26.2-` 与 API 口径对齐。

---

## 四、证据 URL 全清单（按用途分组）

**版本存在性**
- `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json`（917 版本；latest.release=26.3；10 个目标全为 release）
- `https://meta.fabricmc.net/v2/versions/game`（529 条；10 个目标 stable=true）
- 逐版本 JSON（javaVersion 来源）：`https://piston-meta.mojang.com/v1/packages/c0a00f47b3dae01d83e21be9a646c9232379d9ab/1.20.1.json`、`…/bff1278922bca138b7722385a81366f3077abffa/1.20.4.json`、`…/84c5b02d36a9be4560597687f9e2d19d69fada5d/1.20.6.json`、`…/22a1966494dfa4eeb5ee778c8e6ed5b774839582/1.21.1.json`、`…/b547a27fc4d490dc10d62e40a42ace065162b644/1.21.4.json`、`…/5b1e09e69f4f9c650ba11c36d8b27fd0153e4e82/1.21.5.json`、`…/403dee3925f64c7138b72a5302130829cb588784/1.21.8.json`、`…/4f6bd9388f12e9d7adc2ded64acba66212d60521/1.21.11.json`、`…/84d0ff7bd4428695691af5a178edae22c7c83d89/26.1.json`、`…/c7868781b30aaf24be0dac894c94a34e5d6df10d/26.2.json`

**Yarn / intermediary**
- `https://maven.fabricmc.net/net/fabricmc/yarn/maven-metadata.xml`（3412 条，尾部止于 `1.21.11+build.6`）
- `https://meta.fabricmc.net/v2/versions/yarn/1.20.1`（其余 7 个同理换版本号；26.1/26.2 返回空）
- `https://maven.fabricmc.net/net/fabricmc/yarn/1.20.1+build.10/` → 200；`https://maven.fabricmc.net/net/fabricmc/yarn/26.1+build.1/` → 404
- `https://maven.fabricmc.net/net/fabricmc/intermediary/maven-metadata.xml`（461 条，尾部止于 `1.21.11`）
- `https://maven.fabricmc.net/net/fabricmc/intermediary/26.1/` → 404；`https://maven.fabricmc.net/net/fabricmc/intermediary/26.2/` → 404
- `https://meta.fabricmc.net/v2/versions/intermediary/26.1`（返回 `net.fabricmc:intermediary:0.0.0` 占位，非真实坐标）

**Fabric Loader**
- `https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml`（253 条，release 0.19.5）
- `https://meta.fabricmc.net/v2/versions/loader`（253 条，最新 stable=0.19.5）
- `https://meta.fabricmc.net/v2/versions/loader/<MC>`（10 个版本各 253 条，首条均为 0.19.5）
- `https://meta.fabricmc.net/v2/versions/loader/1.20.1/0.19.5/profile/json`、`https://meta.fabricmc.net/v2/versions/loader/26.2/0.19.5/profile/json`
- `https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.19.5/` → 200；jar 内存解析得 class 主版本 51/52/61

**Fabric API**
- `https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml`（1161 条，release 0.161.2+26.4）
- 逐版本目录探针（均 200）：`https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.92.12+1.20.1/`、`…/0.141.6+1.21.11/`、`…/0.145.1+26.1/`、`…/0.155.3+26.1.2/`、`…/0.161.0+26.2/`
- Modrinth 交叉验证：`https://api.modrinth.com/v2/project/fabric-api/version?loaders=%5B%22fabric%22%5D&game_versions=%5B%221.20.1%22%5D`（其余版本换 game_versions）

**Java 工具链 / 映射迁移（官方文档）**
- `https://docs.fabricmc.net/1.20.4/develop/getting-started/setting-up-a-development-environment`（JDK 17）
- `https://docs.fabricmc.net/1.21.1/develop/getting-started/setting-up-a-development-environment`（JDK 21）
- `https://docs.fabricmc.net/1.21.11/develop/getting-started/setting-up`（JDK 21）
- `https://docs.fabricmc.net/develop/getting-started/setting-up`（JDK 25）
- `https://docs.fabricmc.net/develop/porting/mappings/`（Yarn→官方名的迁移；26.1 起无映射）
- `https://fabricmc.net/2026/03/14/261.html`（26.1 公告：不混淆、Loom 1.15、Gradle 9.4.0、Java 25、插件换代）
- `https://fabricmc.net/2025/10/31/obfuscation.html`（Yarn 终止与工具链变更）
- `https://www.minecraft.net/en-us/article/removing-obfuscation-in-java-edition`（Mojang 侧公告，文档引用）

**Loom**
- `https://maven.fabricmc.net/net/fabricmc/fabric-loom/net.fabricmc.fabric-loom.gradle.plugin/maven-metadata.xml`（1.14.0-alpha.32 → 1.18.2，130 个）
- `https://maven.fabricmc.net/net/fabricmc/fabric-loom-remap/net.fabricmc.fabric-loom-remap.gradle.plugin/maven-metadata.xml`（同上区间）
- `https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml`（release 1.18.2）
- `https://maven.fabricmc.net/net/fabricmc/fabric-loom-no-remap/net.fabricmc.fabric-loom-no-remap.gradle.plugin/maven-metadata.xml`（止于 1.14.0-alpha.31，勿用）

---

## 五、不确定项

1. **「26.1」到底指哪个补丁版？** 26.1 / 26.1.1 / 26.1.2 三个正式版都在 Manifest 里，Fabric API 对三者用同一个 `~26.1-` 约束。我按「26.1 系列」给坐标（`0.155.3+26.1.2`）。**需要 Lead 明确锁定到哪一个**，否则 §Task 3 的 `depends.minecraft` 与产物名会含糊。26.2 无补丁版，无此问题。
2. **Loom 两个插件 id 的语义差异无法从 Maven 元数据自证**：`net.fabricmc.fabric-loom` 与 `net.fabricmc.fabric-loom-remap` 的版本区间、release 号完全一致（1.14.0-alpha.32–1.18.2）。「前者不 remap、后者 remap」这一条**只由 26.1 官方公告的正文支撑**。建议在阶段 0/2 的探针里各跑一次实际解析来确认，别只信文档。
3. **我没有跑任何 gradle**（按要求避让文件锁），所以「这些坐标在 Stonecutter 各节点里能否真的解析成功」尚未验证——本表只保证坐标**真实存在且声明兼容**。
4. **「最新」有时效性**：本表取 2026-10-03 的快照。上游当天已在发 26.3/26.4 线的构建（fabric-api `0.161.2+26.4`），未来 1.21.x 线也可能再更新。
5. **Loader 组合只做了静态验证**：我确认了 Fabric meta 为 10 个版本都提供 0.19.5、profile 端点存在、jar 字节码基线为 Java 7/8(+17)，但**没有实机启动**任意一个 loader×MC 组合。
6. `https://meta.fabricmc.net/v2/versions/intermediary/26.1` 会返回一个 `net.fabricmc:intermediary:0.0.0` 的条目——那是占位/兜底值，**不是可用坐标**（对应 Maven 目录 404）。若谁看到 `0.0.0` 误当成"intermediary 还在"，会踩坑。
7. 26.1 的「Jakarta/渲染」类变化（如 26.2 起 OpenGL/Vulkan 后端可切换）只影响业务代码，不属于本矩阵，未展开。
8. 1.20.1 没有 Fabric 官方文档的**版本化** setting-up 页面（文档版本化只回溯到 1.20.4），其「Java 17」结论来自 Mojang `javaVersion` 与 1.20.4 文档的同代口径。

---

## 六、对既有计划文档的两处勘误（我只报告，不修改）

`docs/plans/multiversion/02-阶段1-版本矩阵与依赖表.md` 第 27 行的世代断点表写：

| 文档原文 | 实测结论 |
|---|---|
| 26.1 – 26.2 的 Java = **21+** | **错**。Mojang `javaVersion.majorVersion = 25`（java-runtime-epsilon），Fabric 文档与 26.1 公告均要求 **JDK 25**，Fabric API 自身声明 `java >=25`。按 21 配置会直接构建失败。 |
| 26.1 – 26.2 的映射 = **Mojang Mappings** | **不准确**。26.1 起游戏代码**不再混淆**，因此**没有任何映射**（不是"换成 Mojang Mappings"）；官方文档原文：「Minecraft 26.1 is unobfuscated … there is no need for any obfuscation mappings」。Loom 侧对应的是 **no-remap 插件** `net.fabricmc.fabric-loom`，而不是"用 Mojang 映射的 remap 插件"。 |

另：现基线 `CLAUDE.md` 记录的 Fabric API `0.97.2+1.20.4` 仍可用，但 1.20.4 线最新为 `0.97.3+1.20.4`。
