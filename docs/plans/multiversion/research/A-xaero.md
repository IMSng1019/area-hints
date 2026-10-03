# A · Xaero 小地图 / Xaero 世界地图：10 个目标 MC 版本的 Fabric 坐标

- 调研员：`deps-xaero`（共享任务 `task-1`，写入范围仅本文件）
- 调研时间：2026-10-03
- 被查项目：Xaero 小地图 `xaeros-minimap`（Modrinth project_id `1bokaNcj`）、Xaero 世界地图 `xaeros-world-map`（project_id `NcUtCpym`）
- 用途（读自仓库现状，**未改动任何配置**）：`build.gradle` L140-L148 的 `declareThirdParty('xaero_minimap')` / `declareThirdParty('xaero_world_map')`，内部同时声明 `modCompileOnly "maven.modrinth:<slug>:<值>"` 与 `modLocalRuntime`；坐标由 `gradle.properties` 的 `deps.<mcVersion>.<name>` 提供
- 本文件所有坐标均按「Modrinth API 存在 → Modrinth Maven `maven-metadata.xml` 在列 → 直接 HEAD jar 返回 200」三层验证。**未运行任何 gradle 命令**。

---

## 0. 结论速览

1. **10/10 目标版本、两个 slug 全部有可用的 Fabric release 坐标。没有缺失版本，没有 beta/alpha，本报告不需要出现「该版本无可用坐标」。**
2. **Xaero 更换过 version_number 命名规则**，这是本次调研最需要注意的事实：
   - 旧格式 `<模组版本>_Fabric_<MC版本>`，例：`25.2.0_Fabric_1.20.4`（仓库当前基线）、`1.39.2_Fabric_1.20.4`
   - 新格式 `fabric-<MC版本>-<模组版本>`，例：`fabric-1.20.1-26.5.0`、`fabric-1.20.1-1.46.0`
   - 旧格式在 2025-12-31 发布的 `25.3.2` / `1.40.2` 之后**停用**；1.20.6 之后再无旧格式；26.1 / 26.2 **只有新格式**。
   - 1.20.6 是唯一「新格式不再覆盖」的版本：小地图停在 `25.2.10_Fabric_1.20.6`，世界地图停在 `1.39.12_Fabric_1.20.6`。
3. `docs/plans/multiversion/02-阶段1-版本矩阵与依赖表.md` L63 的表述「Xaero 的坐标格式是 `<mod版本>_Fabric_<MC版本>`，MC 版本段必须与目标版本一致」**需要修正**（该文件我不改，仅在此记录）：
   - 该格式只适用于 1.20.x–1.21.11 的旧命名坐标；
   - 而且 **MC 版本段并不总等于目标版本**：1.20.1 目标用 `25.3.2_Fabric_1.20`（段是 `1.20`），1.21.1 目标用 `25.3.2_Fabric_1.21`（段是 `1.21`）；
   - 判定「坐标是否适配目标 MC」的**唯一可靠依据是 Modrinth 的 `game_versions` 字段**。名字段不可靠，`fabric.mod.json` 里的 `minecraft` 范围更不可靠（见第 4.3 节）。
4. 发现并排除/定位了 3 个「编译能过、运行时加载失败」类风险点，见第 4 节（其中 4.1 是本项目必须处理的硬风险）。

---

## 1. 主表 A：Xaero 小地图（`maven.modrinth:xaeros-minimap:<值>`）

推荐规则（我定的，见第 5 节不确定项 2）：**取该目标 MC 版本上发布日期最新的 release**。10 行全部为 `release`。

| 目标 MC | 坐标值（可直接填入） | version_type | 该 release 声明的 game_versions | 发布日期 | version id | 证据 URL（Modrinth Maven jar，已实际请求，HTTP 200） |
|---|---|---|---|---|---|---|
| 1.20.1 | `fabric-1.20.1-26.5.0` | release | 1.20, 1.20.1 | 2026-09-10 | `eLoJ9IXg` | https://api.modrinth.com/maven/maven/modrinth/xaeros-minimap/fabric-1.20.1-26.5.0/xaeros-minimap-fabric-1.20.1-26.5.0.jar |
| 1.20.4 | `fabric-1.20.4-26.5.0` | release | 1.20.4 | 2026-09-10 | `A7F7BIix` | https://api.modrinth.com/maven/maven/modrinth/xaeros-minimap/fabric-1.20.4-26.5.0/xaeros-minimap-fabric-1.20.4-26.5.0.jar |
| 1.20.6 | `25.2.10_Fabric_1.20.6` | release | 1.20.6 | 2025-07-17 | `OQrZzH2e` | https://api.modrinth.com/maven/maven/modrinth/xaeros-minimap/25.2.10_Fabric_1.20.6/xaeros-minimap-25.2.10_Fabric_1.20.6.jar |
| 1.21.1 | `fabric-1.21.1-26.5.0` | release | 1.21.1 | 2026-09-10 | `ZD6hK4GV` | https://api.modrinth.com/maven/maven/modrinth/xaeros-minimap/fabric-1.21.1-26.5.0/xaeros-minimap-fabric-1.21.1-26.5.0.jar |
| 1.21.4 | `fabric-1.21.4-26.5.0` | release | 1.21.4 | 2026-09-10 | `XsKw9RY8` | https://api.modrinth.com/maven/maven/modrinth/xaeros-minimap/fabric-1.21.4-26.5.0/xaeros-minimap-fabric-1.21.4-26.5.0.jar |
| 1.21.5 | `fabric-1.21.5-26.5.0` | release | 1.21.5 | 2026-09-10 | `UiyvcqIl` | https://api.modrinth.com/maven/maven/modrinth/xaeros-minimap/fabric-1.21.5-26.5.0/xaeros-minimap-fabric-1.21.5-26.5.0.jar |
| 1.21.8 | `fabric-1.21.8-26.5.0` | release | 1.21.8 | 2026-09-10 | `bSWbd7fk` | https://api.modrinth.com/maven/maven/modrinth/xaeros-minimap/fabric-1.21.8-26.5.0/xaeros-minimap-fabric-1.21.8-26.5.0.jar |
| 1.21.11 | `fabric-1.21.11-26.5.0` | release | 1.21.11 | 2026-09-10 | `VNYP3B0c` | https://api.modrinth.com/maven/maven/modrinth/xaeros-minimap/fabric-1.21.11-26.5.0/xaeros-minimap-fabric-1.21.11-26.5.0.jar |
| 26.1 | `fabric-26.1.2-26.5.0` | release | 26.1, 26.1.1, 26.1.2 | 2026-09-10 | `kWf58HtH` | https://api.modrinth.com/maven/maven/modrinth/xaeros-minimap/fabric-26.1.2-26.5.0/xaeros-minimap-fabric-26.1.2-26.5.0.jar |
| 26.2 | `fabric-26.2-26.5.1` | release | 26.2 | 2026-09-15 | `VlMbRW2O` | https://api.modrinth.com/maven/maven/modrinth/xaeros-minimap/fabric-26.2-26.5.1/xaeros-minimap-fabric-26.2-26.5.1.jar |

补充：`maven.modrinth:xaeros-minimap:<值>` 在 Modrinth Maven 的 `maven-metadata.xml` 共列出 **1235** 个版本，上表 10 个值全部在列（逐个 `-contains` 校验为 `True`）。

## 2. 主表 B：Xaero 世界地图（`maven.modrinth:xaeros-world-map:<值>`）

| 目标 MC | 坐标值（可直接填入） | version_type | 该 release 声明的 game_versions | 发布日期 | version id | 证据 URL（Modrinth Maven jar，已实际请求，HTTP 200） |
|---|---|---|---|---|---|---|
| 1.20.1 | `fabric-1.20.1-1.46.0` | release | 1.20, 1.20.1 | 2026-09-10 | `X84cZdC3` | https://api.modrinth.com/maven/maven/modrinth/xaeros-world-map/fabric-1.20.1-1.46.0/xaeros-world-map-fabric-1.20.1-1.46.0.jar |
| 1.20.4 | `fabric-1.20.4-1.46.0` | release | 1.20.3, 1.20.4 | 2026-09-10 | `S9GHYEYj` | https://api.modrinth.com/maven/maven/modrinth/xaeros-world-map/fabric-1.20.4-1.46.0/xaeros-world-map-fabric-1.20.4-1.46.0.jar |
| 1.20.6 | `1.39.12_Fabric_1.20.6` | release | 1.20.6 | 2025-07-17 | `EmIpPLsW` | https://api.modrinth.com/maven/maven/modrinth/xaeros-world-map/1.39.12_Fabric_1.20.6/xaeros-world-map-1.39.12_Fabric_1.20.6.jar |
| 1.21.1 | `fabric-1.21.1-1.46.0` | release | 1.21, 1.21.1 | 2026-09-10 | `txl9KURC` | https://api.modrinth.com/maven/maven/modrinth/xaeros-world-map/fabric-1.21.1-1.46.0/xaeros-world-map-fabric-1.21.1-1.46.0.jar |
| 1.21.4 | `fabric-1.21.4-1.46.0` | release | 1.21.4 | 2026-09-10 | `IoGJS3fE` | https://api.modrinth.com/maven/maven/modrinth/xaeros-world-map/fabric-1.21.4-1.46.0/xaeros-world-map-fabric-1.21.4-1.46.0.jar |
| 1.21.5 | `fabric-1.21.5-1.46.0` | release | 1.21.5 | 2026-09-10 | `kJhXAZVZ` | https://api.modrinth.com/maven/maven/modrinth/xaeros-world-map/fabric-1.21.5-1.46.0/xaeros-world-map-fabric-1.21.5-1.46.0.jar |
| 1.21.8 | `fabric-1.21.8-1.46.0` | release | 1.21.8 | 2026-09-10 | `q54JRi33` | https://api.modrinth.com/maven/maven/modrinth/xaeros-world-map/fabric-1.21.8-1.46.0/xaeros-world-map-fabric-1.21.8-1.46.0.jar |
| 1.21.11 | `fabric-1.21.11-1.46.0` | release | 1.21.11 | 2026-09-10 | `Jfe3ZJIB` | https://api.modrinth.com/maven/maven/modrinth/xaeros-world-map/fabric-1.21.11-1.46.0/xaeros-world-map-fabric-1.21.11-1.46.0.jar |
| 26.1 | `fabric-26.1.2-1.46.0` | release | 26.1, 26.1.1, 26.1.2 | 2026-09-10 | `IYPINJuw` | https://api.modrinth.com/maven/maven/modrinth/xaeros-world-map/fabric-26.1.2-1.46.0/xaeros-world-map-fabric-26.1.2-1.46.0.jar |
| 26.2 | `fabric-26.2-1.46.1` | release | 26.2 | 2026-09-15 | `q0Wvp46X` | https://api.modrinth.com/maven/maven/modrinth/xaeros-world-map/fabric-26.2-1.46.1/xaeros-world-map-fabric-26.2-1.46.1.jar |

补充：`maven.modrinth:xaeros-world-map` 的 `maven-metadata.xml` 共列出 **1130** 个版本，上表 10 个值全部在列。

### 2.1 基线坐标复核（仓库现有值，**未改动**）

| 位置 | 键 | 现有值 | 复核结果 |
|---|---|---|---|
| `gradle.properties` L22 / L42 | `xaero_minimap` | `25.2.0_Fabric_1.20.4` | Maven 在列；jar HEAD 200（1738684 字节）；class major 61（Java 17）；`fabric.mod.json` minecraft 范围 `>1.20.2` |
| `gradle.properties` L23 / L43 | `xaero_world_map` | `1.39.2_Fabric_1.20.4` | Maven 在列；jar HEAD 200（953090 字节）；class major 61（Java 17）；`fabric.mod.json` minecraft 范围 `>1.20.2` |

→ 基线值本身是**有效且仍可取**的，不需要为 1.20.4 换坐标。

---

## 3. 备选表：旧命名世代（`_Fabric_` 格式）坐标

用途：如果 Lead 希望各目标的 Xaero API 与基线（`25.2.0_Fabric_1.20.4` / `1.39.2_Fabric_1.20.4`）**漂移最小**，应改用本表 —— 它与基线只差 1~3 个小版本，且保持 `02-阶段1` 文档里写的命名格式。全部为 `release`，全部 Maven 在列，全部 jar HEAD 200。

| 目标 MC | 小地图坐标 | 世界地图坐标 | 声明 game_versions（两者） | 发布日期 | 备注 |
|---|---|---|---|---|---|
| 1.20.1 | `25.3.2_Fabric_1.20` | `1.40.2_Fabric_1.20` | 1.20, 1.20.1 | 2025-12-31 | MC 段是 `1.20` 而非 `1.20.1` |
| 1.20.4 | `25.3.2_Fabric_1.20.4` | `1.40.2_Fabric_1.20.4` | 1.20.4 | 2025-12-31 | 与基线同 MC 段 |
| 1.20.6 | `25.2.10_Fabric_1.20.6` | `1.39.12_Fabric_1.20.6` | 1.20.6 | 2025-07-17 | 与主表 B 相同（1.20.6 无更新坐标） |
| 1.21.1 | `25.3.2_Fabric_1.21` | `1.40.2_Fabric_1.21` | 1.21, 1.21.1 | 2025-12-31 | MC 段是 `1.21` 而非 `1.21.1` |
| 1.21.4 | `25.3.2_Fabric_1.21.4` | `1.40.2_Fabric_1.21.4` | 1.21.4 | 2025-12-31 |  |
| 1.21.5 | `25.3.2_Fabric_1.21.5` | `1.40.2_Fabric_1.21.5` | 1.21.5 | 2025-12-31 |  |
| 1.21.8 | `25.3.2_Fabric_1.21.8` | `1.40.2_Fabric_1.21.8` | 1.21.8 | 2025-12-31 |  |
| 1.21.11 | `25.3.2_Fabric_1.21.11` | `1.40.2_Fabric_1.21.11` | 1.21.11 | 2025-12-31 |  |
| 26.1 | **无** | **无** | — | — | 旧命名从未覆盖 26.x |
| 26.2 | **无** | **无** | — | — | 旧命名从未覆盖 26.x |

版本 id（备选坐标，`https://api.modrinth.com/v2/version/<id>` 全部返回 OK）：小地图 1.20.1 `jcQ3zpWh` / 1.20.4 `DYeyYMjV` / 1.20.6 `OQrZzH2e` / 1.21.1 `T78xsQuu` / 1.21.4 `ca1kpdhu` / 1.21.5 `QeuKUG15` / 1.21.8 `dj4C4U07` / 1.21.11 `62EynHPJ`；世界地图 1.20.1 `yckp3FUK` / 1.20.4 `qW3vBizb` / 1.20.6 `EmIpPLsW` / 1.21.1 `yz0BNgrU` / 1.21.4 `6dgKx4o1` / 1.21.5 `ExNL5EaA` / 1.21.8 `b52t6kbY` / 1.21.11 `3DZ5aNVp`。

### 3.1 两套值的取舍（我的观察，决定权在 Lead）

- 主表（最新 release）优点：是作者当前维护的版本；缺点：与基线跨了多个大版本，`src/client/java/areahint/xaero/` 下的 `XaeroCompat` / `XaeroMinimapBridge` / `XaeroWorldMapBridge` 是否还能编译**我没有验证**（禁止跑 gradle）。
- 备选表（`_Fabric_` 世代）优点：与基线只差 1~3 个小版本，API 漂移最小，且 1.20.1–1.21.11 全部覆盖；缺点：26.1 / 26.2 用不上，且它是 2025-12-31 的版本，之后不再更新。
- 若兼容层是**跨目标共享同一份源码**：注意 1.20.6 两个 slug 都**只能**用 25.2.10 / 1.39.12 这条线，而 1.20.4 有一条新得多的线 —— 共享源码实际可用到的 API 交集由 1.20.6 决定。

---

## 4. 关键发现（跨版本风险点）

### 4.1 【硬风险】26.1 / 26.2 的坐标是 Java 25 字节码（class major 69）

我对每个坐标 jar 抽样读取了第一个 `.class` 条目的字节码主版本号：

| 目标 MC | 小地图坐标 | class major | 等价 Java | 世界地图坐标 | class major | 等价 Java |
|---|---|---|---|---|---|---|
| 1.20.1 | `fabric-1.20.1-26.5.0` | 61 | Java 17 | `fabric-1.20.1-1.46.0` | 61 | Java 17 |
| 1.20.4 | `fabric-1.20.4-26.5.0` | 61 | Java 17 | `fabric-1.20.4-1.46.0` | 61 | Java 17 |
| 1.20.6 | `25.2.10_Fabric_1.20.6` | 65 | Java 21 | `1.39.12_Fabric_1.20.6` | 65 | Java 21 |
| 1.21.1 | `fabric-1.21.1-26.5.0` | 65 | Java 21 | `fabric-1.21.1-1.46.0` | 65 | Java 21 |
| 1.21.4 | `fabric-1.21.4-26.5.0` | 65 | Java 21 | `fabric-1.21.4-1.46.0` | 65 | Java 21 |
| 1.21.5 | `fabric-1.21.5-26.5.0` | 65 | Java 21 | `fabric-1.21.5-1.46.0` | 65 | Java 21 |
| 1.21.8 | `fabric-1.21.8-26.5.0` | 65 | Java 21 | `fabric-1.21.8-1.46.0` | 65 | Java 21 |
| 1.21.11 | `fabric-1.21.11-26.5.0` | 65 | Java 21 | `fabric-1.21.11-1.46.0` | 65 | Java 21 |
| 26.1 | `fabric-26.1.2-26.5.0` | 69 | Java 25 | `fabric-26.1.2-1.46.0` | 69 | Java 25 |
| 26.2 | `fabric-26.2-26.5.1` | 69 | Java 25 | `fabric-26.2-1.46.1` | 69 | Java 25 |

（61 = Java 17，65 = Java 21，69 = Java 25；基线 `25.2.0_Fabric_1.20.4` 与 `1.39.2_Fabric_1.20.4` 实测也都是 61。）

**含义**：`modCompileOnly` / `modLocalRuntime` 会把 jar 直接交给编译器与运行期。若 26.1 / 26.2 节点的 Gradle Java 工具链不是 25，这两个依赖会在**编译期**直接失败（`class file has wrong version 69.0`），而不是运行时。1.20.1 / 1.20.4 是 Java 17、1.20.6–1.21.11 是 Java 21，与 `02-阶段1` 文档第 20-27 行的世代表一致。

**我没有验证的部分**：当前 `gradle.properties` 只声明到 1.20.6，`deps.26.1.java` / `deps.26.2.java` 尚不存在，我也没有跑 gradle 去实测，所以 26.x 的工具链要求需要 Lead 自行对齐（见第 5 节不确定项 1）。

### 4.2 【已排除】`xaerolib` 不需要额外坐标

新命名坐标（`25.3.2` / `1.40.2` 及以后）的 `fabric.mod.json` 都声明了额外依赖 `"xaerolib": ">=1.0.42"`（更晚的写 `>=1.0`），而：

- Modrinth Maven 的 POM **不声明任何 dependencies**（`xaeros-minimap-fabric-1.20.1-26.5.0.pom` 原文里只有 groupId/artifactId/version/name/description）；
- `maven.modrinth:xaerolib` 在 Modrinth Maven 上**只有 forge 构件**（共 15 个：`forge-1.12*`、`forge-1.16.5*`，最新 `forge-1.16.5-1.7.3`），没有 fabric 构件，`fabric-1.21.1-1.7.1` 这类坐标为 404；

看起来会缺依赖，但**实际不会**：新命名 jar 把 xaerolib 作为**嵌套 jar 内嵌**了。实测 `xaeros-minimap-fabric-1.20.1-26.5.0.jar` 的 `fabric.mod.json` 写有 `jars: {"file": "META-INF/jars/xaerolib-fabric-1.20.1-1.7.3.jar"}`，展开该嵌套 jar 得到 `id=xaerolib, version=1.7.3`；`xaeros-world-map-fabric-1.20.1-1.46.0.jar` 同样内嵌 `xaerolib-fabric-1.20.1-1.7.3.jar`。Fabric 的嵌套 jar 会满足该 `depends`。

→ **不需要为 xaerolib 添加任何坐标或 `deps.<mc>.xaerolib` 键**。顺带解释了仓库 `.gradle/loom-cache/.../unspecified/xaerolib-fabric-1.21.1-1.7.1/` 这个缓存的来源：它是 Loom 为 remap 而解包出来的嵌套 jar，不是外部依赖（仓库里 grep 不到 `xaerolib` 是正常的）。对照：基线 `1.39.2_Fabric_1.20.4` 没有 `jars` 字段、没有嵌套 jar（老版本 xaerolib 是打包进主 jar 的）。

### 4.3 `game_versions` 才是适配判据，`fabric.mod.json` 的 `minecraft` 范围普遍偏宽

实测到的 `fabric.mod.json` → `depends.minecraft` 与 Modrinth `game_versions` 的差异：

| 坐标 | fabric.mod.json 的 minecraft | Modrinth game_versions | 差异 |
|---|---|---|---|
| 小地图 `25.2.0_Fabric_1.20.4`（基线） | `>1.20.2` | `1.20.4` | 范围宽到会覆盖 1.20.6 |
| 世界地图 `1.39.2_Fabric_1.20.4`（基线） | `>1.20.2` | `1.20.4` | 同上 |
| 小地图 `25.2.10_Fabric_1.20.6` | `>1.20.4` | `1.20.6` | 范围宽到会覆盖 1.21.x |
| 小地图 `25.3.2_Fabric_1.21` | `>1.20.6` | `1.21, 1.21.1` | 范围宽 |
| 小地图 `25.3.2_Fabric_1.21.8` | `>1.21.7 <1.22` | `1.21.8` | 范围含 1.21.9–1.21.11 |
| 小地图 `fabric-1.21.11-26.5.0` | `>1.21.10 <26.1.0` | `1.21.11` | 范围宽 |

**含义**：Fabric Loader 用 `fabric.mod.json` 做依赖校验，这些偏宽的范围会让「MC 版本不对的 Xaero」照样加载成功 —— 正是本项目踩过的「编译能过、运行时加载失败」那条坑的镜像（这里更隐蔽：连运行时都不会报错，只是行为不对）。因此填 `deps.<mc>.xaero_*` 时必须按 **Modrinth `game_versions`** 判定，既不能凭名字段，也不能凭 `fabric.mod.json`。

### 4.4 命名规则切换的确切边界

| 项目 | 最后一个旧命名 release | 之后 | 首个新命名 release |
|---|---|---|---|
| xaeros-minimap | `25.3.2_Fabric_*`（2025-12-31） | → | `fabric-<mc>-26.1.0`（1.20.1 的实测日期 2026-06-11） |
| xaeros-world-map | `1.40.2_Fabric_*`（2025-12-31） | → | `fabric-<mc>-1.41.0`（1.20.1 的实测日期 2026-06-11） |

边界之后，**1.20.6 再没有拿到过任何新版本**（两个 slug 都是），所以 1.20.6 的坐标被永久固定在 2025-07-17 的 `25.2.10` / `1.39.12`。

---

## 5. 不确定项

1. **26.1 / 26.2 的 Java 工具链未验证。** 这两个目标的坐标是 class major 69（Java 25）。当前 `gradle.properties` 里没有 `deps.26.1.java` / `deps.26.2.java`，我也被禁止跑 gradle，因此**无法确认这两个节点的工具链是否 ≥ 25**。如果实际配的是 21，`modCompileOnly` 会直接编译失败。这是本报告唯一需要 Lead 立刻对齐的硬约束。
2. **「推荐坐标」的选择规则是我自定的**（= 该 MC 上发布日期最新的 release），不是 Lead 指定的。我**没有验证**主表坐标与仓库现有 `src/client/java/areahint/xaero/XaeroCompat.java`、`minimap/XaeroMinimapBridge.java`、`worldmap/XaeroWorldMapBridge.java` 的 API 兼容性 —— 既没跑 gradle，也没反编译比对方法签名。**不能保证换成主表坐标后兼容层仍能编译。** 若要最小化风险，请优先用第 3 节备选表（与基线只差 1~3 个小版本）。
3. **game_versions 是作者声明，我没有做真实运行验证**（未启动游戏、未跑 runClient）。「坐标真实存在且可取」已用 HTTP 证明；「装上后确实能跑」未证明。
4. **26.1 目标与坐标的 MC 段不完全同号。** 可用坐标的 `game_versions` 是 `[26.1, 26.1.1, 26.1.2]`，而坐标名里的 `26.1.2` 是 **MC 版本段**（不是模组版本段，模组版本是 26.5.0 / 1.46.0），容易误读。我未验证 26.1.0 与 26.1.2 之间的运行时差异。
5. **字节码版本是抽样结论。** 我只读了每个 jar 里排序第一个 `.class` 条目。理论上 multi-release jar 可能出现差异，我未逐类核验。
6. **备选表的 14 个坐标我做了 HEAD + 字节码 + `fabric.mod.json` 校验，但没有逐个下载解包做完整依赖树比对**（主表的 22 个下载解包过）。
7. **未验证 Xaero 与各节点 Fabric API / Loader 的版本约束是否相容。** 所有坐标都要求 `fabric-api >= 0.43.1`（很宽松，应无问题）与 `fabricloader >= 0.13.0`；但这属于另一条依赖线（`deps.<mc>.fabric` / `deps.<mc>.loader`），不在本任务范围。
8. **Modrinth 项目页标题/元数据可能滞后。** 例如 `maven.modrinth:xaerolib` 对应的 Modrinth 项目 `ZaQIxZKn` 在 API 里 `loaders` 只写 `forge`，但 Maven 上确实只有 forge 构件，与第 4.2 节结论一致；此项不影响主表。
9. **我没能确定 `.gradle/loom-cache` 里 `xaerolib-fabric-1.21.1-1.7.1` 是何时、由哪次构建产生的**（4.2 节给的是机制解释，不是构建日志证据）。我没有为此改动或新增任何构建脚本。

---

## 6. 证据清单：实际请求过的关键 URL

所有请求均通过 `pwsh` 的 `Invoke-RestMethod` / `Invoke-WebRequest` 发出（带 `User-Agent: areas-hint-deps-research/1.0`）。未使用 `web_fetch`。

### 6.1 Modrinth API — 逐 (slug, MC) 过滤查询（20 条，全部 200）

模式：

```
https://api.modrinth.com/v2/project/<slug>/version?game_versions=%5B%22<MC>%22%5D&loaders=%5B%22fabric%22%5D
```

`<slug>` ∈ {`xaeros-minimap`, `xaeros-world-map`}，`<MC>` ∈ {`1.20.1`, `1.20.4`, `1.20.6`, `1.21.1`, `1.21.4`, `1.21.5`, `1.21.8`, `1.21.11`, `26.1`, `26.2`}，共 20 条，**全部返回 200 且结果非空**（fabric 命中数：小地图 44/32/13/22/21/21/17/14/6/10，世界地图 38/28/7/20/21/21/16/16/7/11，顺序同上表 MC 顺序）。

### 6.2 Modrinth API — 逐版本详情（20 条，全部 OK）

`https://api.modrinth.com/v2/version/<id>`，`<id>` 见第 1、2 节主表；返回的 `version_number` / `version_type` / `game_versions` 与主表逐字一致。

### 6.3 Modrinth Maven — `maven-metadata.xml`（2 条，全部 200）

- `https://api.modrinth.com/maven/maven/modrinth/xaeros-minimap/maven-metadata.xml`（1235 个版本，`latest`/`release` = `forge-26.3-26.5.3`）
- `https://api.modrinth.com/maven/maven/modrinth/xaeros-world-map/maven-metadata.xml`（1130 个版本，`latest`/`release` = `forge-26.3-1.46.4`）

### 6.4 Modrinth Maven — jar 直接 HEAD（36 条，全部 HTTP 200 且返回真实 `Content-Length`）

模式：`https://api.modrinth.com/maven/maven/modrinth/<slug>/<值>/<slug>-<值>.jar`

覆盖：主表 20 个 + 基线 2 个 + 备选表 14 个 = 36 条。

### 6.5 Modrinth 版本页（抽样 2 条，HTTP 200）

- `https://modrinth.com/mod/xaeros-minimap/version/eLoJ9IXg`
- `https://modrinth.com/mod/xaeros-world-map/version/X84cZdC3`

### 6.6 补充证据（第 4 节结论所用）

- jar 内容：上述 36 条中，22 个主表坐标 + 基线 2 个下载后解包读取 `fabric.mod.json` 与首个 `.class`；备选 14 个同样处理。
- POM 原文：`https://api.modrinth.com/maven/maven/modrinth/xaeros-minimap/fabric-1.20.1-26.5.0/xaeros-minimap-fabric-1.20.1-26.5.0.pom`、`.../fabric-1.21.1-26.4.2/xaeros-minimap-fabric-1.21.1-26.4.2.pom`、`.../25.3.2_Fabric_1.20.4/...pom`、`.../25.2.0_Fabric_1.20.4/...pom`、`.../xaeros-world-map/fabric-1.20.1-1.46.0/...pom`
- xaerolib 排查：`https://api.modrinth.com/maven/maven/modrinth/xaerolib/maven-metadata.xml`（仅 15 个 forge 版本）；`https://api.modrinth.com/v2/project/ZaQIxZKn/version?game_versions=%5B%22<MC>%22%5D&loaders=%5B%22fabric%22%5D`（10 个 MC 全部 `n=0`）；`https://api.modrinth.com/v2/search?query=xaerolib&limit=5`；`https://api.modrinth.com/maven/maven/modrinth/xaerolib/fabric-1.21.1-1.7.1/xaerolib-fabric-1.21.1-1.7.1.jar`（404）

### 6.7 未能取到的数据

- 无。10 个目标版本的两个 slug 都查到了坐标，没有落入「查不到」分支。
