# B. ModMenu 与 VulkanMod 跨版本坐标查证

> 调研人：`deps-modmenu`（共享任务 `task-2`）　查证时间：2026-10-03（Asia/Shanghai）
> 目标版本（10 个）：1.20.1、1.20.4、1.20.6、1.21.1、1.21.4、1.21.5、1.21.8、1.21.11、26.1、26.2
> 查证手段：Modrinth API v2、Maven `maven-metadata.xml`、HTTP HEAD 探测、**下载 jar 解包 `fabric.mod.json`**；全部为真实 HTTP 请求，未执行任何 gradle / gradlew 命令（避开 Lead 的构建文件锁）。
> 项目标识：ModMenu → 项目 ID `mOgUt4GM`（slug `modmenu`）；VulkanMod → 项目 ID `JYQhtZtO`（slug `vulkanmod`）。

---

## 0. 结论速览

| 目标 MC | ModMenu（`maven.modrinth:modmenu:`） | 类型 | VulkanMod 有 Fabric 版？ | VulkanMod 建议坐标（`maven.modrinth:vulkanmod:`） |
|---|---|---|---|---|
| 1.20.1 | **7.2.2** | release | 有 | `ZhvilXaM`（= 0.5.2，beta） |
| 1.20.4 | **9.2.0**（与当前基线一致） | release | 有 | `hZjQjfpD`（= 0.4.7，beta） |
| 1.20.6 | **10.0.0** | release | 有 | `bPsylH8I`（= 0.4.2，alpha） |
| 1.21.1 | **11.0.5** | release | 有 | `2eqroDKD`（= 0.6.7+1.21.1，release） |
| 1.21.4 | **13.0.4** | release | 有 | `RAtVo03C`（= 0.5.6，beta） |
| 1.21.5 | **14.0.2** | release | 有 | `7pYvUnJk`（= 0.5.6，beta，**必须用 ID**） |
| 1.21.8 | **15.0.2** | release | **无**（VulkanMod 项目根本没有 1.21.6/1.21.7/1.21.8） | 该版本无可用坐标 |
| 1.21.11 | **17.0.1** | release | 有 | `hWKRTXbQ`（= 0.6.8+1.21.11，release） |
| 26.1 | **18.0.2** | release | 有 | `CEmnv55N`（= 0.6.8+26.1.2，release） |
| 26.2 | **20.0.3** | release | **无**（VulkanMod 项目最高支持 26.1.2） | 该版本无可用坐标 |

**三句话结论**：① ModMenu 10 个版本全部有精确坐标，全部是 release，本仓库继续用 `maven.modrinth:modmenu:<版本号>`（版本号在 202 个 ModMenu 版本中无任何重复，不存在歧义）；② 1.20.1 **必须**降到 **7.2.2**（9.2.0 的 `fabric.mod.json` 写死 `minecraft >=1.20.3`，1.20.1 会被加载器拒绝）；③ VulkanMod 的 `version_number` **跨 MC 版本大量重复**（0.5.6/0.5.5/0.5.2/0.4.7/0.4.2 都重复），`maven.modrinth:vulkanmod:<版本号>` 实测会解析到错的 MC 版本，**必须用 Modrinth 版本 ID 当坐标**；且 1.21.8 与 26.2 根本没有 VulkanMod 构建。

---

## 1. ModMenu

### 1.1 表 B-1：Modrinth 坐标（本仓库应当用这一套）

坐标写法：`modCompileOnly "maven.modrinth:modmenu:<下表的版本号>"`（对应 `gradle.properties` 的 `deps.<MC>.modmenu`）。
"声明的 game_versions"= Modrinth 版本元数据；"jar 内 depends"= 下载该版本 jar 后解包 `fabric.mod.json` 读到的真实谓词。
证据 URL 统一为 `https://api.modrinth.com/v2/version/<版本 ID>`（返回 `version_number`/`version_type`/`game_versions`/`loaders`/`files[].hashes`）。

| 目标 MC | 版本号 | version_type | 声明的 game_versions | jar 内 `depends.minecraft` | jar 内 `depends.fabricloader` | 版本 ID | 证据 URL |
|---|---|---|---|---|---|---|---|
| 1.20.1 | **7.2.2** | release | `1.20.1` | `>=1.19.4-` | `>=0.12.13` | `lEkperf6` | https://api.modrinth.com/v2/version/lEkperf6 |
| 1.20.4 | **9.2.0** | release | `1.20.4` | `>=1.20.3` | `>=0.12.23` | `FawTQg29` | https://api.modrinth.com/v2/version/FawTQg29 |
| 1.20.6 | **10.0.0** | release | `1.20.5`, `1.20.6` | `>=1.20.5-` | `>=0.12.23` | `HLa8flfl` | https://api.modrinth.com/v2/version/HLa8flfl |
| 1.21.1 | **11.0.5** | release | `1.21`, `1.21.1` | `>=1.21-beta.2` | `>=0.15.10` | `6lgOkclV` | https://api.modrinth.com/v2/version/6lgOkclV |
| 1.21.4 | **13.0.4** | release | `1.21.4` | `>=1.21.4-rc.3` | `>=0.16.9` | `qEKKsTqd` | https://api.modrinth.com/v2/version/qEKKsTqd |
| 1.21.5 | **14.0.2** | release | `1.21.5` | `>=1.21.5 <1.21.6` | `>=0.16.10` | `2ZvBR7XI` | https://api.modrinth.com/v2/version/2ZvBR7XI |
| 1.21.8 | **15.0.2** | release | `1.21.6`, `1.21.7`, `1.21.8` | `~1.21.6` | `>=0.16.10` | `ku5NivOP` | https://api.modrinth.com/v2/version/ku5NivOP |
| 1.21.11 | **17.0.1** | release | `1.21.11` | `>=1.21.11 <26` | `>=0.17.2` | `cLkYaVuL` | https://api.modrinth.com/v2/version/cLkYaVuL |
| 26.1 | **18.0.2** | release | `26.1`, `26.1.1`, `26.1.2` | `>1.26-`（写法怪异，见「不确定项」） | `>=0.17.2` | `ZJ6YTrMY` | https://api.modrinth.com/v2/version/ZJ6YTrMY |
| 26.2 | **20.0.3** | release | `26.2` | `>=26.2 <26.3-alpha.3` | `>=0.19.2` | `iRo7AZXf` | https://api.modrinth.com/v2/version/iRo7AZXf |

补充说明：

- 上表每个版本号都已实测可下载：`https://api.modrinth.com/maven/maven/modrinth/modmenu/<版本号>/modmenu-<版本号>.jar` 全部 HTTP 200，且 10 个 jar 均成功解出 `fabric.mod.json`（`id=modmenu`，`version` 与版本号一致）。
- 每个版本也都有 Modrinth CDN 直链，例如 7.2.2 → `https://cdn.modrinth.com/data/mOgUt4GM/versions/lEkperf6/modmenu-7.2.2.jar`。
- ModMenu 全部 202 个版本中 **没有任何重复的 `version_number`**（实测分组统计结果：NO DUPLICATE），所以 `maven.modrinth:modmenu:<版本号>` 是无歧义坐标。
- 上表中 1.21.1 的 11.0.5 虽然是 2026-09-24 才发布（ModMenu 会回头给小版本补发 release），但 Modrinth 标注的 `game_versions` 就是 `1.21`/`1.21.1`，可直接用。
- ModMenu 另有 8.0.1（1.20.2）、12.0.1（1.21.2/1.21.3）、16.0.1（1.21.9/1.21.10）等版本，但对应的 MC 版本不在本次 10 个目标内，故不入表。

### 1.2 表 B-2：Terraformers Maven 坐标（`com.terraformersmc:modmenu`）

仓库：`https://maven.terraformersmc.com/releases`；坐标：`compileOnly "com.terraformersmc:modmenu:<版本号>"`。
元数据入口（真实请求，HTTP 200）：https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/maven-metadata.xml
该 metadata 的 `<latest>`/`<release>` 都是 `22.0.0-alpha.1`，`<versions>` 列表是「追加式、非排序」，但包含下表全部版本。

| 目标 MC | 版本号 | jar 证据（HEAD，均 HTTP 200） | pom 证据（HEAD，均 HTTP 200） |
|---|---|---|---|
| 1.20.1 | 7.2.2 | https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/7.2.2/modmenu-7.2.2.jar | …/7.2.2/modmenu-7.2.2.pom |
| 1.20.4 | 9.2.0 | https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/9.2.0/modmenu-9.2.0.jar | …/9.2.0/modmenu-9.2.0.pom |
| 1.20.6 | 10.0.0 | https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/10.0.0/modmenu-10.0.0.jar | …/10.0.0/modmenu-10.0.0.pom |
| 1.21.1 | 11.0.5 | https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/11.0.5/modmenu-11.0.5.jar | …/11.0.5/modmenu-11.0.5.pom |
| 1.21.4 | 13.0.4 | https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/13.0.4/modmenu-13.0.4.jar | …/13.0.4/modmenu-13.0.4.pom |
| 1.21.5 | 14.0.2 | https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/14.0.2/modmenu-14.0.2.jar | …/14.0.2/modmenu-14.0.2.pom |
| 1.21.8 | 15.0.2 | https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/15.0.2/modmenu-15.0.2.jar | …/15.0.2/modmenu-15.0.2.pom |
| 1.21.11 | 17.0.1 | https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/17.0.1/modmenu-17.0.1.jar | …/17.0.1/modmenu-17.0.1.pom |
| 26.1 | 18.0.2 | https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/18.0.2/modmenu-18.0.2.jar | …/18.0.2/modmenu-18.0.2.pom |
| 26.2 | 20.0.3 | https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/20.0.3/modmenu-20.0.3.jar | …/20.0.3/modmenu-20.0.3.pom |

不止 HEAD：实际把 Terraformers 的 7.2.2 与 20.0.3 两个 jar 下载并解包校验过，分别为 721366 字节 / 616919 字节，`fabric.mod.json` 里 `id=modmenu`、`version=7.2.2`/`20.0.3`、`minecraft` 谓词与 Modrinth 侧一致 —— 说明这个仓库不是 soft-404 页面，是真 jar。

### 1.3 两套坐标该选哪一套

**建议继续用 `maven.modrinth:modmenu:<版本号>`（即现状）**，理由（均有实测支撑）：

1. 本仓库 `build.gradle` 已经声明了 Modrinth 仓库并做了 `content { includeGroup 'maven.modrinth' }` 隔离，且现有 `declareThirdParty('modmenu', …)` 就是 `maven.modrinth` 写法，换仓库属于无收益改动。
2. Modrinth 侧版本号与 MC 版本一一对应、无重复（202 个版本 0 重复），且 `version_type` 明确；10 个目标版本全部命中。
3. Terraformers Maven 的元数据把 `<release>` 标成 `22.0.0-alpha.1`（alpha 冒充 release），任何用 `latest.release` / `+` 动态版本的写法都可能拿到 alpha；本仓库虽然写死版本号不受影响，但没有必要引入这个坑。
4. Terraformers Maven 的 `<versions>` 顺序是追加式乱序（例如 `20.0.1` 排在 `18.0.1` 前面、`22.0.0-alpha.1` 在末尾），人工挑选容易看错。

**Terraformers Maven 作为备选/镜像是可用的**：10 个目标版本的 jar 与 pom 全部 HTTP 200，且抽样解包校验通过。若哪天 Modrinth Maven 不可用，可整体切到 `com.terraformersmc:modmenu`，版本号与表 B-1 完全一致（不需要改版本号，只改 group/artifact 与仓库）。

---

## 2. 1.20.1 专项：为什么必须是 7.2.2

- **9.2.0 不能用（实测 jar 内 `fabric.mod.json`）**：`{"depends":{"minecraft":">=1.20.3", "fabricloader":">=0.12.23", …}}` —— 谓词把 1.20.1 排除在外，Fabric Loader 在 1.20.1 上会直接拒绝加载。这正是 Lead 说的「编译能过、运行时加载失败」。
- **7.2.2 可用（实测 jar 内 `fabric.mod.json`）**：`{"depends":{"minecraft":">=1.19.4-", "fabricloader":">=0.12.13", …}}`，且 Modrinth 该版本声明 `game_versions=["1.20.1"]`、`version_type=release`、`loaders=["fabric","quilt"]`。
- 1.20.1 分支的候选（Modrinth 精确查询 `game_versions=["1.20.1"]&loaders=["fabric"]`）：`7.2.2`(release)、`7.2.1`、`7.2.0`、`7.1.0`、`7.0.1`(game_versions=`1.20`,`1.20.1-rc1`,`1.20.1`)。**取最新的 7.2.2**。
- **不要用 8.0.1 顶替**：8.0.1 的 Modrinth 声明是 `game_versions=["1.20.2"]`（8.0.0 还带 `23w40a`），属于 1.20.2 分支。
- 该结论与计划文档 `docs/plans/multiversion/02-阶段1-版本矩阵与依赖表.md` 里已写的 `deps.1.20.1.modmenu=7.2.2` 一致（本次为它补上了 jar 级证据）。

---

## 3. VulkanMod

VulkanMod 在 Modrinth 上是纯客户端 Fabric 模组（`loaders=["fabric","quilt"]`，`environment=client`），项目 game_versions 全量只有：
`1.18.2, 1.19.2, 1.19.3, 1.19.4, 1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.5, 1.20.6, 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2`
→ **没有 1.21.6 / 1.21.7 / 1.21.8，也没有 26.2**。

### 3.1 表 B-3：VulkanMod 各目标版本可用性

"jar 内 depends"= 下载对应 jar 解包 `fabric.mod.json` 实测值。证据 URL 为 `https://api.modrinth.com/v2/version/<版本 ID>`。

| 目标 MC | 有 Fabric 版？ | 版本号 | version_type | 声明的 game_versions | jar 内 `depends.minecraft` | jar 内 `depends.fabricloader` | 版本 ID | 证据 URL |
|---|---|---|---|---|---|---|---|---|
| 1.20.1 | 有 | 0.5.2 | beta | `1.20`, `1.20.1` | `1.20 1.20.1` | `>=0.14.14` | `ZhvilXaM` | https://api.modrinth.com/v2/version/ZhvilXaM |
| 1.20.4 | 有 | 0.4.7 | beta | `1.20.3`, `1.20.4` | `1.20.3 1.20.4` | `>=0.14.14` | `hZjQjfpD` | https://api.modrinth.com/v2/version/hZjQjfpD |
| 1.20.6 | 有 | 0.4.2 | alpha | `1.20.5`, `1.20.6` | `>=1.20.5` | `>=0.14.14` | `bPsylH8I` | https://api.modrinth.com/v2/version/bPsylH8I |
| 1.21.1 | 有 | 0.6.7+1.21.1 | release | `1.21`, `1.21.1` | `1.21 1.21.1` | `>=0.18.0` | `2eqroDKD` | https://api.modrinth.com/v2/version/2eqroDKD |
| 1.21.4 | 有 | 0.5.6 | beta | `1.21.4` | `=1.21.4` | `>=0.14.14` | `RAtVo03C` | https://api.modrinth.com/v2/version/RAtVo03C |
| 1.21.5 | 有 | 0.5.6 | beta | `1.21.5` | `>=1.21.5` | `>=0.14.14` | `7pYvUnJk` | https://api.modrinth.com/v2/version/7pYvUnJk |
| 1.21.8 | **该版本无可用坐标** | — | — | — | — | — | — | 见 3.2 的证据 |
| 1.21.11 | 有 | 0.6.8+1.21.11 | release | `1.21.11` | `1.21.11` | `>=0.14.14` | `hWKRTXbQ` | https://api.modrinth.com/v2/version/hWKRTXbQ |
| 26.1 | 有 | 0.6.8+26.1.2 | release | `26.1`, `26.1.1`, `26.1.2` | `>=26.1` | `>=0.18.6` | `CEmnv55N` | https://api.modrinth.com/v2/version/CEmnv55N |
| 26.2 | **该版本无可用坐标** | — | — | — | — | — | — | 见 3.2 的证据 |

坐标写法（直接可粘贴）：

```groovy
// 1.20.1 / 1.20.4 / 1.20.6 / 1.21.1 / 1.21.4 / 1.21.5 / 1.21.11 / 26.1：用 Modrinth 版本 ID，不要用版本号
modCompileOnly "maven.modrinth:vulkanmod:ZhvilXaM"   // 1.20.1  0.5.2
modCompileOnly "maven.modrinth:vulkanmod:hZjQjfpD"   // 1.20.4  0.4.7
modCompileOnly "maven.modrinth:vulkanmod:bPsylH8I"   // 1.20.6  0.4.2
modCompileOnly "maven.modrinth:vulkanmod:2eqroDKD"   // 1.21.1  0.6.7+1.21.1
modCompileOnly "maven.modrinth:vulkanmod:RAtVo03C"   // 1.21.4  0.5.6
modCompileOnly "maven.modrinth:vulkanmod:7pYvUnJk"   // 1.21.5  0.5.6（版本号 0.5.6 会被解析成 1.21.4 的 jar！）
modCompileOnly "maven.modrinth:vulkanmod:hWKRTXbQ"   // 1.21.11 0.6.8+1.21.11
modCompileOnly "maven.modrinth:vulkanmod:CEmnv55N"   // 26.1    0.6.8+26.1.2
// 1.21.8 与 26.2：查无坐标，按 D2 约定「不写这个键」，让 declareThirdParty 打印跳过日志
```

### 3.2 为什么 1.21.8 / 26.2 判定为「无可用坐标」

1. 项目级 `game_versions`（`GET https://api.modrinth.com/v2/project/vulkanmod`）不含 `1.21.6`/`1.21.7`/`1.21.8`，也不含 `26.2`（最高到 `26.1.2`）。
2. 逐版本核对（`GET https://api.modrinth.com/v2/project/vulkanmod/version`，共 125 个版本）后，**没有任何一个版本**的 `game_versions` 里出现过 `1.21.6`/`1.21.7`/`1.21.8`/`26.2`。
3. 带 facets 的精确查询也返回空：`GET https://api.modrinth.com/v2/project/vulkanmod/version?game_versions=%5B%221.21.8%22%5D&loaders=%5B%22fabric%22%5D` → `[]`；把 `1.21.8` 换成 `26.2` 同样为 `[]`。
4. 相关但不等于替代：Modrinth 搜索能找到 `vulkanmod-add-libs`（"VulkanMod Additional Libraries"，支持到 1.21.11）与 `valkaw`（"Rebranch of Sliwed Optimize … VulkanMod for Vulkan API"，声明支持到 26.2）。**它们是另外的项目，不是 VulkanMod 本体，不能用来顶替 VulkanMod 坐标**。

### 3.3 最大的坑：`version_number` 跨 MC 版本重复 → 必须用版本 ID

Modrinth 允许同一个 `version_number` 在不同 MC 版本上各发一次，VulkanMod 大量这么做。实测重复情况：

| 版本号 | 覆盖的 MC 版本（版本 ID） |
|---|---|
| 0.4.2 | 1.20/1.20.1（`jVYN5DAq`）、1.20.3/1.20.4（`2UbtVTVW`）、1.20.5/1.20.6（`bPsylH8I`） |
| 0.4.7 | 1.20/1.20.1（`15yBKrl7`）、1.20.3/1.20.4（`hZjQjfpD`）、1.21/1.21.1（`US6s2g38`） |
| 0.5.2 | 1.20/1.20.1（`ZhvilXaM`）、1.21/1.21.1（`6MYI9w5k`）、1.21.2/1.21.3（`q7e6hiCI`）、1.21.4（`I3uJz86I`） |
| 0.5.5 | 1.21/1.21.1（`pY3DXoVe`）、1.21.4（`hnKdQD6g`）、1.21.5（`V8ZoM1w6`） |
| 0.5.6 | 1.21.4（`RAtVo03C`）、1.21.5（`7pYvUnJk`） |
| 0.6.3 / 0.6.4 / 0.6.5 / 0.6.6 | 各出现两次：1.21.11 与 26.1.x 各一份 |

**实测危害**：`https://api.modrinth.com/maven/maven/modrinth/vulkanmod/0.5.6/vulkanmod-0.5.6.jar` 返回的是 **1.21.4 的那份**（Content-Length 19810358，等于 `VulkanMod_1.21.4-0.5.6.jar`；下载解包后 `depends.minecraft = "=1.21.4"`）。也就是说，若 1.21.5 节点写 `maven.modrinth:vulkanmod:0.5.6`，会拿到 1.21.4 的 jar，**必然重现「编译能过、运行时加载失败」**。

**规避方式（已验证可行）**：Modrinth Maven 的 `maven-metadata.xml` 同时把「版本 ID」也列为可解析的版本（例如 `0.5.5` 与 `hnKdQD6g`、`V8ZoM1w6` 并列出现）。实测 `https://api.modrinth.com/maven/maven/modrinth/vulkanmod/7pYvUnJk/vulkanmod-7pYvUnJk.jar` → HTTP 200，19869924 字节，解包后 `depends.minecraft = ">=1.21.5"`，正是 1.21.5 那一份；其 `.pom` 也返回 HTTP 200。**所以 VulkanMod 一律用版本 ID 当坐标。**

（ModMenu 不存在这个问题：202 个版本 0 重复。所以 ModMenu 用版本号、VulkanMod 用版本 ID。）

### 3.4 本仓库现在需不需要 VulkanMod 编译期依赖？——**不需要**

- `src/client/java/areahint/render/VulkanModCompat.java` 的类注释写明「不直接依赖 VulkanMod API 类型，避免未安装时触发类加载错误」，实现是 `Class.forName("net.vulkanmod.Initializer")` 与 `Class.forName("net.vulkanmod.render.chunk.build.frapi.VulkanModRenderer")` 反射，**源码里没有任何 `net.vulkanmod` 的 import**（全仓库 grep：只有上面两处字符串常量 + `fabric.mod.json` 的 `suggests`）。
- `src/main/resources/fabric.mod.json` 把 `"vulkanmod": "*"` 放在 `suggests` 段（不是 `depends`/`recommends`），本就是可选。
- 结论：**当前不声明 VulkanMod 依赖是正确设计**，加了 `modCompileOnly` 反而会往编译类路径塞一个约 19–23 MB 的整包（含 quilt 支持与内嵌库）。只有将来把反射改成直接引用 VulkanMod 类型时，才需要按表 B-3 用版本 ID 声明；且注意 VulkanMod 是 `environment=client` 的纯客户端模组，必须落在 client 源集。

---

## 4. 证据清单（本次真实发起的 HTTP 请求）

**Modrinth API**

- `https://api.modrinth.com/v2/project/modmenu` → 项目 `mOgUt4GM`，`game_versions` 含全部 10 个目标版本（含 `26.1`/`26.2`），`environment=client_only`。
- `https://api.modrinth.com/v2/project/vulkanmod` → 项目 `JYQhtZtO`，`loaders=fabric,quilt`；`game_versions` 不含 1.21.6/1.21.7/1.21.8/26.2。
- 逐版本精确查询（10 个版本 × 2 个项目）：
  `https://api.modrinth.com/v2/project/modmenu/version?game_versions=%5B%22<MC>%22%5D&loaders=%5B%22fabric%22%5D`
  `https://api.modrinth.com/v2/project/vulkanmod/version?game_versions=%5B%22<MC>%22%5D&loaders=%5B%22fabric%22%5D`
- 全量版本列表：`https://api.modrinth.com/v2/project/modmenu/version`（202 个）、`https://api.modrinth.com/v2/project/vulkanmod/version`（125 个），用于重复版本号统计。
- 单版本详情（表内全部版本 ID）：`https://api.modrinth.com/v2/version/<ID>`。

**Maven**

- `https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/maven-metadata.xml` → HTTP 200，`<versions>` 含表 B-2 全部 10 个版本；`<latest>`=`<release>`=`22.0.0-alpha.1`。
- `https://api.modrinth.com/maven/maven/modrinth/vulkanmod/maven-metadata.xml` → HTTP 200，`<latest>`=`<release>`=`0.6.8+26.1.2`，并同时列出「版本号」与「版本 ID」两类坐标。
- ModMenu 10 个版本 × `api.modrinth.com/maven` 的 jar HEAD → 全部 HTTP 200（len 616919 ~ 1087256）。
- ModMenu 10 个版本 × `maven.terraformersmc.com` 的 jar HEAD 与 pom HEAD → 全部 HTTP 200；另下载 7.2.2、20.0.3 两个 jar 解包校验 `fabric.mod.json` 通过。
- VulkanMod 8 个候选 jar HEAD → 全部 HTTP 200；其中 8 个 jar（`ZhvilXaM`/`hZjQjfpD`/`bPsylH8I`/`2eqroDKD`/`RAtVo03C`/`7pYvUnJk`/`hWKRTXbQ`/`CEmnv55N`）实际下载解包读取 `fabric.mod.json`。
- ModMenu 10 个候选 jar 实际下载解包读取 `fabric.mod.json`（7.2.2 ~ 20.0.3）。

> 说明：`web_fetch` 对本次所有目标域名都返回「URL hostname resolves to a non-public IP address」，因此证据全部改用 `pwsh` 的 `Invoke-RestMethod` / `Invoke-WebRequest`（带 `User-Agent`）取得，属于任务书允许的兜底手段。

---

## 5. 不确定项

1. **ModMenu 18.0.2 的 `minecraft` 谓词写成 `>1.26-`**，与 20.0.3 的规整写法（`>=26.2 <26.3-alpha.3`）风格不一致，疑似上游笔误。Modrinth 侧声明的是 `26.1`/`26.1.1`/`26.1.2`（可信），但**「26.1 节点装了 18.0.2 后 Fabric Loader 是否接受」没有实机验证**。建议 26.1 目标出包后实机跑一次。
2. **26.1 / 26.2 这两个「年份版本号」本身能否被本仓库的 Yarn/Fabric Loader/Fabric API 支持，不在本报告范围内**（由 D 报告负责）。若官方依赖缺失，ModMenu 坐标再准也无法编译；表中已给出 26.1/26.2 的 ModMenu 与（26.1 的）VulkanMod 坐标备用。
3. **Modrinth Maven 对「重复版本号」的选取规则未文档化**。本报告只实测出 `0.5.6` 指向 1.21.4 那一份，没有穷举其余重复号（0.4.2/0.4.7/0.5.2/0.5.5/0.6.3~0.6.6）。规避手段是确定的（用版本 ID），但如果有人图省事写版本号，仍可能踩坑 —— 建议在 `gradle.properties` 里对 VulkanMod 统一写版本 ID 并加注释。
4. **VulkanMod 各版本的 `version_type` 偏保守**：1.20.1 只有 beta/alpha（0.5.2 = beta），1.20.4 = beta，1.20.6 = alpha，1.21.4/1.21.5 = beta；只有 1.21.1（0.6.7+1.21.1）、1.21.11、26.1 是 release。作为编译期依赖不受 version_type 影响，但若将来要 `modLocalRuntime` 联调，得接受在客户端跑 beta/alpha 版 VulkanMod。
5. **VulkanMod 的 loader 下限会随版本抬高**：0.6.7+1.21.1 要求 `fabricloader >=0.18.0`，0.6.8+26.1.2 要求 `>=0.18.6`（其余候选是 `>=0.14.14`）。虽然本仓库当前不声明该依赖，但若官方依赖矩阵给 1.21.1/26.1 选的 Loader 低于此值，将来启用 VulkanMod 编译期依赖时会撞上，需与 D 报告对齐。
6. **VulkanMod 未做编译验证**：本报告只证明坐标可解析、jar 可下载、`fabric.mod.json` 声明匹配；没有跑过 Loom 的 remap/编译（任务书禁止跑 gradle）。若真的启用为 `modCompileOnly`，需 Lead 在构建阶段确认 Loom 对该整包（19–23 MB、含内嵌 `META-INF/jars`）的处理。
7. **1.21.8 的 VulkanMod 缺口没有任何折中方案**：既无本体坐标，也不建议用 `vulkanmod-add-libs` / `valkaw` 顶替（不同项目、不同 API）。1.21.8 目标的 Vulkan 相关代码只能继续走反射 + 缺失即回退 OpenGL 的现有路径。
