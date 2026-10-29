<a id="top"></a>

<p align="center">
  <a href="./README.md">English</a> · <strong>简体中文</strong>
</p>

<p align="center">
  <img src="src/main/resources/assets/areas-hint/icon.png" alt="Areas Hint Icon" width="128" height="128">
</p>

<h1 align="center">Areas Hint 域名模组</h1>

<p align="center">
  <img alt="Minecraft 构建目标" src="https://img.shields.io/badge/Minecraft-1.20.1%20%7C%201.20.4%20%7C%201.20.6%20%7C%201.21.1%20%7C%201.21.4-3C8527?style=flat-square">
  <img alt="Fabric" src="https://img.shields.io/badge/Loader-Fabric-DBD0B4?style=flat-square">
  <img alt="Java" src="https://img.shields.io/badge/Java-17%20%2F%2021-ED8B00?style=flat-square&logo=openjdk&logoColor=white">
  <img alt="源码版本" src="https://img.shields.io/badge/Mod-4.6.0-8A2BE2?style=flat-square">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-blue?style=flat-square">
</p>

<p align="center">
  为你的 Minecraft 世界增添沉浸式区域名称提示。
</p>

<p align="center">
  当玩家进入你定义的区域时，屏幕上方会优雅显示该区域的名称，让建筑、地图、RPG 场景与多层级世界观拥有更强的代入感与仪式感。
</p>

<p align="center">
  <a href="https://modrinth.com/mod/area-hints">Modrinth 下载</a> ·
  <a href="https://github.com/IMSng1019/area-hints/issues">问题反馈</a> ·
  <a href="./LICENSE">MIT 许可证</a>
</p>

<p align="center">
  <a href="#quickstart">快速开始</a> ·
  <a href="#commands">命令系统</a> ·
  <a href="#data-format">数据格式</a> ·
  <a href="#development">构建与开发</a> ·
  <a href="#docs">相关文档</a>
</p>

---

## 亮点速览

| 类型 | 内容 |
|---|---|
| 区域定义 | 支持多边形区域、二级顶点 AABB 预筛选、多层级域名体系 |
| 显示能力 | 支持区域名、联合域名、维度域名、副字幕与描述查询 |
| 编辑方式 | 支持 `/areahint easyadd`、扩展/收缩/分割、签名、描述等交互式流程 |
| 服务端管理 | 支持提交审批、打回后重新申请、域名城市化、客户端模组校验与域名游戏模式 |
| 地图与音效 | 支持 Xaero 小地图 / 世界地图覆盖层、BlueMap 标记及域名切换音效 |
| 性能与架构 | 采用客户端检测 + 服务端同步，并结合高度过滤与射线法优化 |

---

<a id="overview"></a>

## 概述

Areas Hint 是一个适用于 **Minecraft Fabric** 的区域提示模组，当前源码版本为 **4.6.0**。各 Minecraft 构建目标和依赖见下方版本表。
它允许玩家使用多边形定义区域（项目内通常称为“域名”），并在进入区域时显示自定义名称。

模组支持：

- 多边形区域定义
- 多层级域名体系
- 高度过滤
- 维度域名显示
- 交互式区域创建与编辑
- 多种提示文字渲染与域名标题样式设置
- 域名副字幕、描述、签名与安全传送
- 多世界 / 多服务器独立数据管理
- 权限系统与地图系统兼容扩展
- 域名修改审批、临时预览、审批回复与打回后重新申请
- 域名城市化、强制客户端模组校验与按域名切换游戏模式
- 可选的域名切换音效、音高和音量设置

在设计上，模组采用**客户端检测 + 服务端同步**的思路：
客户端负责提示所需的区域检测与显示，服务端负责数据存储、权限处理、审批与同步。启用域名游戏模式时，服务端会独立使用玩家坐标和正式域名数据判断游戏模式。

---

## 特点

- **玩家自定义**：自由命名区域，设定范围、颜色、层级与高度。
- **性能友好**：结合高度预筛选、AABB 预检查与射线法检测，减少无效计算。
- **自动区域提示**：进入区域时自动显示名称，适合 RPG、建筑展示与剧情地图。
- **多级区域支持**：区域可嵌套，例如“王国 → 主城 → 王座厅”。
- **维度名称支持**：离开所有普通区域时显示当前维度名称。
- **交互式创建**：支持 `/areahint easyadd` 等命令进行交互式编辑。
- **可编辑性强**：支持扩展、收缩、分割、重命名、重新着色、修改高度、描述与副字幕。
- **权限可控**：支持基础权限节点、签名者操作规则和 LuckPerms 兼容。
- **地图与传送**：支持 Xaero 小地图 / 世界地图覆盖层、BlueMap 标记，并提供域名中心 / 随机安全传送。
- **显示方式丰富**：支持 CPU / OpenGL / Vulkan（实验性）以及多种域名标题样式与大小设置。
- **兼容性扩展**：可与 LuckPerms、BlueMap 等系统协同使用。

---

## 为什么选择它？

### 对玩家友好
普通玩家也可以通过交互式流程参与区域创建与修改，而不必依赖手写 JSON。

### 对服主友好
适合服务器主城、村落、副本、野区、建筑区、剧情区域、活动区域等管理场景。

### 对地图作者友好
可通过域名层级、维度域名与联合域名构建更完整的世界观提示系统。

### 对性能友好
模组在区域检测上采用分层优化思路，先快速排除，再进行精确判断。

---

## 适合场景

- **RPG 服务器**：为主城、王国、村庄、副本、野区、地下城等区域添加名称提示。
- **冒险地图**：在关键剧情点、机关房间、隐藏区域、章节地点中展示自定义名称。
- **建筑服务器**：为大型建筑、展馆、社区区域、作者作品集提供空间标签。
- **单人生存世界**：记录家园、矿区、农场、港口、遗迹、维度据点等重要地点。

---

## 目录

- [概述](#overview)
- [特点](#特点)
- [为什么选择它？](#为什么选择它)
- [适合场景](#适合场景)
- [适用版本与依赖](#适用版本与依赖)
- [安装说明](#安装说明)
- [快速开始](#quickstart)
- [核心概念](#核心概念)
- [命令系统](#commands)
- [服务端配置与审批](#server-config)
- [数据格式与配置说明](#data-format)
- [多世界 / 多服务器文件夹机制](#多世界--多服务器文件夹机制)
- [架构设计](#架构设计)
- [模块划分](#模块划分)
- [核心算法说明](#核心算法说明)
- [文件结构](#文件结构)
- [相关文档](#docs)
- [模组兼容性](#compatibility)
- [构建与开发](#development)
- [已知限制与说明](#已知限制与说明)

---

## 适用版本与依赖

### 基础环境

当前源码版本为 `4.6.0`。下表来自 `settings.gradle` 中实际启用的构建目标和 `gradle.properties` 中的依赖参数；它表示当前源码的构建配置，不代表所有版本都已发布或完成游戏内验证。下载时请以 Modrinth 对应文件标注的 Minecraft 版本为准。

| Minecraft 构建目标 | Java 最低版本 | Fabric Loader 最低版本 | 构建使用的 Fabric API |
|---|---|---|---|
| `1.20.1` | `17` | `0.16.14` | `0.92.12+1.20.1` |
| `1.20.4`（开发基线） | `17` | `0.16.14` | `0.97.2+1.20.4` |
| `1.20.6` | `21` | `0.16.14` | `0.100.8+1.20.6` |
| `1.21.1` | `21` | `0.16.14` | `0.116.17+1.21.1` |
| `1.21.4` | `21` | `0.19.5` | `0.119.4+1.21.4` |

每个 JAR 对应一个 Minecraft 构建目标，请安装匹配的文件与 Fabric API。`1.21.5`、`1.21.8`、`1.21.11` 的依赖虽已声明，但尚未加入当前构建列表；`26.x` 仍属于实验性规划。

### 可选兼容 / 集成

- **LuckPerms**：权限节点兼容
- **BlueMap**：地图标记相关扩展
- **Xaero's Minimap / Xaero's World Map**：域名边界、填充与名称覆盖层；需要匹配下方兼容性表中的版本
- **Mod Menu**：可选配置界面集成
- **Sodium**：已在 `fabric.mod.json` 中作为建议兼容项列出
- **VulkanMod**：实验性 Vulkan 渲染模式所需的可选环境

---

## 安装说明

### 客户端安装
1. 为目标 Minecraft 版本安装 **Fabric Loader**，使用上表对应的 Java 版本
2. 安装与 Minecraft 版本匹配的 **Fabric API**
3. 从 [Modrinth](https://modrinth.com/mod/area-hints) 下载匹配的本模组 `.jar`，放入 `mods` 文件夹
4. 启动游戏

### 服务端安装
1. 安装 **Fabric Loader**
2. 安装 **Fabric API**
3. 将本模组的 `.jar` 文件放入服务端 `mods` 文件夹
4. 启动服务端

### 推荐安装方式
为了获得完整功能，建议**客户端与服务端同时安装**。
在单人世界中，客户端与集成服务端会共同工作。

---

<a id="quickstart"></a>

## 快速开始

第一次使用时，建议按下面流程体验：

1. 安装模组并进入世界
2. 执行 `/areahint easyadd`
3. 按交互提示完成：
   - 域名名称
   - 域名等级
   - 上级域名（如果有）
   - 使用绑定键记录顶点
   - 高度范围
   - 颜色设置
4. 保存后进入该区域，查看顶部提示效果；普通玩家在默认开启的 `submit_approval` 配置下需要等待 OP 审批，OP 的修改即时生效
5. 可使用 `/areahint addsubtitle` 为域名添加副字幕，或使用 `/areahint adddescription` 添加书本式描述
6. 可使用 `/areahint check` 查看联合域名列表，用 `/areahint boundviz` 查看边界
7. 若手动修改了普通配置文件或区域文件，可使用 `/areahint reload` 重新加载；服务端全局 `server-config.json` 的变更需要重启服务端

---

## 核心概念

### 1. 域名
本项目中的“域名”不是网络域名，而是**你为某个游戏区域定义的名称单位**。
例如：

- 王国
- 主城
- 商业区
- 王座厅
- 地下矿井

### 2. 一级顶点 `vertices`
域名多边形本体的顶点集合，用于真正的区域形状判断。

### 3. 二级顶点 `second-vertices`
二级顶点是对区域边界的辅助描述，常用于包围盒（AABB）预筛选等快速判断。
你也可以理解为用于加速检测的边界矩形信息。

### 4. 域名等级 `level`
- `1`：顶级域名
- `2`：二级域名
- `3`：三级域名
- 以此类推，**数字越大，层级越低**
- 普通域名等级应为整数
- 维度域名在概念上属于 `0.5` 级，位于顶级域名之上；显示时普通区域优先，维度域名单独存放在维度配置中

### 5. 上级域名 `base-name`
指向该域名所属的上一级域名。
普通父子域名的等级相差 `1`；顶级域名没有普通上级时可使用 `null`。
例如：

- `王座厅` 的 `base-name` 可以是 `主城`
- `主城` 的 `base-name` 可以是 `王国`

### 6. 联合域名 / 表面域名 `surfacename`
用于显示组合后的名称，或用于构造更适合展示的表面名称。

### 7. 创建者 `signature`
记录该域名由谁创建。
可用于权限判断、管理与追踪。

### 8. 扩展签名 `signatures`
`signatures` 是额外授权玩家列表，用于让多个玩家共同维护同一个域名或其下级域名。

### 9. 副字幕 `subtitle` / `subtitlecolor`
副字幕会显示在域名标题下方。没有 `subtitle` 字段或内容为空时不会显示副字幕；`subtitlecolor` 支持十六进制颜色，也支持模组内的闪烁颜色值。

### 10. 域名类型 `type`
缺少 `type` 的域名按 `rural`（乡村）处理。通过城市化申请并获批，或由权限等级至少为 2 的 OP 执行城市化后，域名保存为 `"type": "urban"`（城市）。城市化申请由服务端配置 `submit_urban_area` 独立控制，默认关闭。

### 11. 域名游戏模式 `gamemode`

可选值为 `survival`、`creative`、`adventure`、`spectator`。仅在服务端开启 `force_use_areahint` 时应用；当前最深层域名没有合法值或玩家离开所有域名时，使用服务端 `default_gamemode`，不继承父域名模式。

---

<a id="commands"></a>

## 命令系统

> 更细的操作说明可参考 [`COMMAND_USAGE.md`](./COMMAND_USAGE.md)。
> 命令的实际权限表现可能受权限系统、服务器配置与版本实现影响，下面按常见使用场景整理。

### 命令入口与绑定键

命令可以直接在聊天栏输入 `/areahint ...` 执行，也可以在空闲状态下**长按绑定键约 0.5 秒**打开指令可视化主面板。可视化面板中的按钮仍然会走同一套 `/areahint` 命令逻辑，不会绕过权限、签名或服务端校验。

绑定键是模组内统一的操作键，配置字段为 `recordKey`，默认值为 `88`，对应键盘 `X` 键。它的用途会根据当前流程自动切换：

| 使用场景 | 绑定键行为 |
|---|---|
| `easyadd`、`expandarea`、`shrinkarea`、`dividearea`、`addhint` 等录点流程中 | 短按记录当前玩家所在坐标作为顶点 |
| 图形化录点流程中 | 短按记录坐标，长按约 0.5 秒打开继续、完成、取消等操作面板 |
| 没有交互流程正在进行时 | 短按查询当前位置的域名 / 维度域名描述，长按打开指令可视化主面板 |
| 指令可视化界面已打开时 | 按绑定键可关闭当前指令可视化界面 |

更换绑定键时执行 `/areahint replacebutton`，按提示按下新的按键，再执行或点击 `/areahint replacebutton confirm` 保存；使用 `/areahint replacebutton cancel` 可取消本次更换。`Esc`、`Enter`、`Tab`、`Shift`、`Ctrl`、`Alt` 和未知键不能设置为绑定键。绑定键配置只影响当前客户端，并会写入客户端 `config.json`。

### 基础命令

| 命令 | 说明 | 常见权限 |
|---|---|---|
| `/areahint help` | 显示命令帮助 | 所有人 |
| `/areahint reload` | 重新加载配置与域名文件 | 管理员 |
| `/areahint on` | 启用客户端域名提示 | 所有人（可由 LuckPerms 限制） |
| `/areahint off` | 关闭客户端域名提示 | 所有人（可由 LuckPerms 限制） |

### 域名查看与管理

| 命令 | 说明 | 常见权限 |
|---|---|---|
| `/areahint check` | 显示所有联合域名列表 | 所有人 |
| `/areahint check <联合域名>` | 查看指定联合域名详情 | 所有人 |
| `/areahint delete` | 启动交互式域名删除 | 所有人（通常仅限可操作域名） |
| `/areahint tcp [域名]` | 传送到域名中心点附近的安全位置 | 传送权限 |
| `/areahint udp [域名]` | 随机传送到域名内的安全位置 | 传送权限 |
| `/areahint settp [命令头]` | 设置传送使用的命令头，例如 `tp` 或 `minecraft:tp` | 传送设置权限 |

### 域名创建与编辑

| 命令 | 说明 | 常见权限 |
|---|---|---|
| `/areahint add <JSON>` | 通过 JSON 添加域名（保留兼容，推荐使用 `easyadd`） | 管理员 |
| `/areahint easyadd` | 启动交互式域名添加 | 所有人 |
| `/areahint addarea` | `easyadd` 的完整别名，支持同一套交互子命令 | 所有人 |
| `/areahint rename` | 启动交互式域名重命名 | 所有人 |
| `/areahint addsignature` | 为域名添加扩展签名 | 默认要求当前域名主签名或扩展签名包含当前玩家，或管理员等级至少 2 |
| `/areahint deletesignature` | 删除域名扩展签名 | 默认要求当前域名主签名或扩展签名包含当前玩家，或管理员等级至少 2 |
| `/areahint replaceareagamemode` | 选择域名并设置生存 / 创造 / 冒险 / 旁观模式；需要开启 `force_use_areahint` | 默认管理员等级 2，可由 LuckPerms 配置 |

### 域名几何编辑

| 命令 | 说明 | 常见权限 |
|---|---|---|
| `/areahint expandarea` | 从列表中选择域名扩展 | 所有人 |
| `/areahint expandarea <域名>` | 直接指定域名进行扩展（推荐） | 所有人 |
| `/areahint shrinkarea` | 从列表中选择域名收缩 | 默认要求当前域名主签名或扩展签名包含当前玩家，或管理员等级至少 2 |
| `/areahint shrinkarea <域名>` | 直接指定域名进行收缩（推荐） | 默认要求当前域名主签名或扩展签名包含当前玩家，或管理员等级至少 2 |
| `/areahint dividearea` | 启动交互式域名分割 | 所有人 |

`shrinkarea`、`addsignature`、`deletesignature` 的默认归属判断只检查当前域名自己的 `signature` 与 `signatures`，不采用上级域名的签名。`expandarea` 保留原有的当前域名或直属上级域名签名规则，管理员等级至少为 2 时也可操作。这些编辑命令仍优先使用明确配置的 LuckPerms 节点结果。

### 域名城市化

| 命令 | 说明 | 常见权限 |
|---|---|---|
| `/areahint applyurbanarea` | 选择服务端当前维度的域名申请城市化；需要开启 `submit_urban_area` | 默认等级 0；普通玩家限当前域名的 `signature` 为本人或 `signatures` 包含本人，OP 等级至少 2 可操作全部符合条件的域名 |

服务端全局 `areas-hint/server-config.json` 中的 `submit_urban_area` 默认 `false`，关闭时隐藏指令与可视化入口。开启后，普通玩家提交申请，OP 审批同意后才会写入 `"type": "urban"`；OP 自己操作则立即写入并重载。即使普通修改审批 `submit_approval` 关闭，城市化仍按此流程审批。

LuckPerms 节点为 `areahint.command.applyurbanarea`，不会替代当前域名主签名或扩展签名的归属检查，也不会授予 OP 审批身份。上级域名的签名不授予城市化权限。城市化申请复用可视化预览、临时顶点编辑、打回修改、历史意见和离线回复，详见 [城市化与审批使用说明](COMMAND_USAGE.md#域名城市化)。

### 单个顶点编辑

| 命令 | 说明 | 常见权限 |
|---|---|---|
| `/areahint addhint` | 启动交互式顶点添加 | 所有人 |
| `/areahint deletehint` | 启动交互式顶点删除 | 所有人 |

### 样式与显示设置

| 命令 | 说明 | 常见权限 |
|---|---|---|
| `/areahint recolor` | 启动交互式域名重新着色 | 所有人 |
| `/areahint sethigh` | 启动交互式域名高度设置 | 所有人 |
| `/areahint hintrender` | 查看当前渲染模式 | 所有人 |
| `/areahint hintrender <cpu\|opengl\|vulkan>` | 设置渲染模式 | 所有人 |
| `/areahint titlestyle` | 启动交互式域名标题样式选择 | 所有人 |
| `/areahint titlesize` | 启动交互式域名标题大小选择 | 所有人 |
| `/areahint addsubtitle` | 添加或替换域名副字幕 | 管理员或可操作签名者 |
| `/areahint replacesubtitle` | `addsubtitle` 的替换语义别名 | 管理员或可操作签名者 |
| `/areahint deletesubtitle` | 删除域名副字幕 | 管理员或可操作签名者 |
| `/areahint replacesubtitlecolor` | 修改域名副字幕颜色 | 管理员或可操作签名者 |
| `/areahint replacesubtitlesize` | 修改本客户端副字幕大小 | 所有人 |
| `/areahint frequency` | 显示当前检测频率 | 所有人 |
| `/areahint frequency <1-60>` | 设置检测频率，支持小数 | 所有人 |

### 域名切换音效

| 命令 | 说明 |
|---|---|
| `/areahintc replacesoundevent` | 打开音效分类与选择流程 |
| `/areahintc replacesoundevent select <声音ID> <音高>` | 选择可用音效，音高范围 `0.5–2.0` |
| `/areahintc replacesoundevent none` | 关闭域名切换音效 |
| `/areahintc soundlevel` | 打开音量选择 |
| `/areahintc soundlevel <0-1>` | 设置音量，支持小数，`0` 为静音 |

音效配置只影响当前客户端，默认不播放声音。可视化面板提供相同入口；旧写法 `/areahint replacesoundevent` 和 `/areahint soundlevel` 仍保留兼容入口。

### 描述系统

| 命令 | 说明 | 常见权限 |
|---|---|---|
| `/areahint adddescription` | 添加普通域名描述 | 管理员或可操作签名者 |
| `/areahint replacedescription` | 替换普通域名描述 | 管理员或可操作签名者 |
| `/areahint deletedescription` | 删除普通域名描述，只删除描述文件 | 管理员或可操作签名者 |
| `/areahint adddimensionalitydescription` | 添加维度域名描述 | 管理员或维度域名签名者 |
| `/areahint replacedimensionalitydescription` | 替换维度域名描述 | 管理员或维度域名签名者 |
| `/areahint deletedimensionalitydescription` | 删除维度域名描述，只删除描述文件 | 管理员或维度域名签名者 |

### 维度域名管理

| 命令 | 说明 | 常见权限 |
|---|---|---|
| `/areahint dimensionalityname` | 启动交互式维度域名管理 | 视服务器配置而定 |
| `/areahint dimensionalitycolor` | 启动交互式维度域名颜色管理 | 视服务器配置而定 |
| `/areahint firstdimname <名称>` | 首次进入无名新维度时设置维度域名 | 所有人（可由 LuckPerms 限制） |
| `/areahint firstdimname_skip` | 跳过首次维度域名设置 | 所有人（可由 LuckPerms 限制） |

### 其他功能

| 命令 | 说明 | 常见权限 |
|---|---|---|
| `/areahint replacebutton` | 启动交互式绑定键更换 | 所有人（可由 LuckPerms 限制） |
| `/areahint boundviz` | 切换边界可视化显示 | 所有人（可由 LuckPerms 限制） |
| `/areahint language` | 启动交互式语言选择 | 所有人（可由 LuckPerms 限制） |
| `/areahint debug` | 切换调试模式 | 管理员 |
| `/areahint debug on` | 启用调试模式 | 管理员 |
| `/areahint debug off` | 关闭调试模式 | 管理员 |
| `/areahint debug status` | 查看调试状态 | 管理员 |
| `/areahint serverlanguage <语言代码>` | 设置服务端日志语言 | 控制台 / 高权限 |

> 默认权限节点位于 `areahint.command.*` 下，例如 `areahint.command.easyadd`、`areahint.command.addsubtitle`、`areahint.command.teleport`。未安装 LuckPerms 或节点未定义时，模组会回退到原版权限等级与签名者规则。

以下个人设置与首次维度命名指令可以通过独立节点控制；这些节点默认使用权限等级 0 回退，因此不配置 LuckPerms 时仍保持原有开放行为：

| 指令 | LuckPerms 节点 |
|---|---|
| `/areahint on` | `areahint.command.on` |
| `/areahint off` | `areahint.command.off` |
| `/areahint firstdimname` | `areahint.command.firstdimname` |
| `/areahint firstdimname_skip` | `areahint.command.firstdimname_skip` |
| `/areahint replacebutton` | `areahint.command.replacebutton` |
| `/areahint language` | `areahint.command.language` |
| `/areahint boundviz` | `areahint.command.boundviz` |

副字幕指令的权限映射如下，其中 `replacesubtitle` 与 `addsubtitle` 使用同一套添加或替换流程：

| 指令 | LuckPerms 节点 |
|---|---|
| `/areahint addsubtitle`、`/areahint replacesubtitle` | `areahint.command.addsubtitle` |
| `/areahint deletesubtitle` | `areahint.command.deletesubtitle` |
| `/areahint replacesubtitlecolor` | `areahint.command.replacesubtitlecolor` |
| `/areahint replacesubtitlesize` | `areahint.command.replacesubtitlesize` |

其他完整别名继续复用实际操作节点：`addarea` 使用 `areahint.command.easyadd`，`tcp` 与 `udp` 使用 `areahint.command.teleport`，描述替换指令分别复用对应的描述添加节点。

---

<details>
<summary><strong>展开查看子命令速查</strong></summary>

## 子命令速查

这一节按当前命令注册结构列出常用入口。交互式流程中的点击按钮会自动补全对应子命令，通常不需要手写全部步骤；空闲状态下长按绑定键也可以打开指令可视化主面板来选择常用命令。

### 基础与开关

- `/areahint help`
- `/areahint reload`
- `/areahint on`
- `/areahint off`
- `/areahint debug`
- `/areahint debug on`
- `/areahint debug off`
- `/areahint debug status`
- `/areahint serverlanguage <语言代码>`

### 查看、删除与传送

- `/areahint check`
- `/areahint check <联合域名>`
- `/areahint delete`
- `/areahint delete select <域名>`
- `/areahint delete confirm`
- `/areahint delete cancel`
- `/areahint tcp`
- `/areahint tcp <域名>`
- `/areahint udp`
- `/areahint udp <域名>`
- `/areahint settp`
- `/areahint settp <命令头>`

### 维度域名

- `/areahint dimensionalityname`
- `/areahint dimensionalityname select <维度>`
- `/areahint dimensionalityname name <新名称>`
- `/areahint dimensionalityname confirm`
- `/areahint dimensionalityname cancel`
- `/areahint dimensionalitycolor`
- `/areahint dimensionalitycolor select <维度>`
- `/areahint dimensionalitycolor color <颜色>`
- `/areahint dimensionalitycolor confirm`
- `/areahint dimensionalitycolor cancel`
- `/areahint firstdimname <名称>`
- `/areahint firstdimname_skip`

### 创建与基础编辑

- `/areahint add <JSON>`
- `/areahint easyadd`
- `/areahint easyadd cancel`
- `/areahint easyadd level <1-3>`
- `/areahint easyadd base <上级域名>`
- `/areahint easyadd continue`
- `/areahint easyadd finish`
- `/areahint easyadd altitude auto`
- `/areahint easyadd altitude custom`
- `/areahint easyadd altitude unlimited`
- `/areahint easyadd color <颜色>`
- `/areahint easyadd save`
- `/areahint addarea`
- `/areahint addarea cancel`
- `/areahint addarea level <1-3>`
- `/areahint addarea base <上级域名>`
- `/areahint addarea continue`
- `/areahint addarea finish`
- `/areahint addarea altitude auto`
- `/areahint addarea altitude custom`
- `/areahint addarea altitude unlimited`
- `/areahint addarea color <颜色>`
- `/areahint addarea save`
- `/areahint rename`
- `/areahint rename select <域名>`
- `/areahint rename confirm`
- `/areahint rename cancel`
- `/areahint recolor`
- `/areahint recolor select <域名>`
- `/areahint recolor color <颜色>`
- `/areahint recolor confirm`
- `/areahint recolor cancel`
- `/areahint sethigh`
- `/areahint sethigh <域名>`
- `/areahint sethigh custom <域名>`
- `/areahint sethigh unlimited <域名>`
- `/areahint sethigh cancel`
- `/areahint replaceareagamemode`
- `/areahint applyurbanarea`

### 几何与顶点编辑

- `/areahint expandarea`
- `/areahint expandarea <域名>`
- `/areahint expandarea select <域名>`
- `/areahint expandarea continue`
- `/areahint expandarea save`
- `/areahint expandarea cancel`
- `/areahint shrinkarea`
- `/areahint shrinkarea <域名>`
- `/areahint shrinkarea select <域名>`
- `/areahint shrinkarea continue`
- `/areahint shrinkarea save`
- `/areahint shrinkarea cancel`
- `/areahint dividearea`
- `/areahint dividearea select <域名>`
- `/areahint dividearea continue`
- `/areahint dividearea save`
- `/areahint dividearea name <新域名名称>`
- `/areahint dividearea level <1-3>`
- `/areahint dividearea base <上级域名>`
- `/areahint dividearea color <颜色>`
- `/areahint dividearea cancel`
- `/areahint addhint`
- `/areahint addhint select <域名>`
- `/areahint addhint continue`
- `/areahint addhint submit`
- `/areahint addhint cancel`
- `/areahint deletehint`
- `/areahint deletehint select <域名>`
- `/areahint deletehint toggle <顶点索引>`
- `/areahint deletehint submit`
- `/areahint deletehint cancel`

### 显示与客户端配置

- `/areahint frequency`
- `/areahint frequency <1-60>`（支持小数）
- `/areahint hintrender`
- `/areahint hintrender <cpu|opengl|vulkan>`
- `/areahint titlestyle`
- `/areahint titlestyle select <style>`
- `/areahint titlestyle cancel`
- `/areahint titlesize`
- `/areahint titlesize select <size>`
- `/areahint titlesize cancel`
- `/areahint replacesubtitlesize`
- `/areahint replacesubtitlesize select <size>`
- `/areahint replacesubtitlesize cancel`
- `/areahint replacebutton`
- `/areahint replacebutton confirm`
- `/areahint replacebutton cancel`
- `/areahint language`
- `/areahint language select <语言代码>`
- `/areahint language cancel`
- `/areahint boundviz`
- `/areahintc replacesoundevent`
- `/areahintc replacesoundevent category <分类> [页码]`
- `/areahintc replacesoundevent instrument <声音ID> [页码]`
- `/areahintc replacesoundevent select <声音ID> <音高>`
- `/areahintc replacesoundevent none`
- `/areahintc replacesoundevent cancel`
- `/areahintc soundlevel [0-1]`

### 副字幕、描述与签名

- `/areahint addsubtitle`
- `/areahint addsubtitle select <域名>`
- `/areahint addsubtitle color <颜色>`
- `/areahint addsubtitle confirm`
- `/areahint addsubtitle cancel`
- `/areahint replacesubtitle`
- `/areahint replacesubtitle select <域名>`
- `/areahint replacesubtitle color <颜色>`
- `/areahint replacesubtitle confirm`
- `/areahint replacesubtitle cancel`
- `/areahint deletesubtitle`
- `/areahint deletesubtitle select <域名>`
- `/areahint deletesubtitle confirm`
- `/areahint deletesubtitle cancel`
- `/areahint replacesubtitlecolor`
- `/areahint replacesubtitlecolor select <域名>`
- `/areahint replacesubtitlecolor color <颜色>`
- `/areahint replacesubtitlecolor confirm`
- `/areahint replacesubtitlecolor cancel`
- `/areahint adddescription`
- `/areahint adddescription search [关键词]`
- `/areahint adddescription select <域名>`
- `/areahint adddescription text <描述>`
- `/areahint adddescription confirm`
- `/areahint adddescription cancel`
- `/areahint replacedescription`
- `/areahint replacedescription search [关键词]`
- `/areahint replacedescription select <域名>`
- `/areahint replacedescription text <描述>`
- `/areahint replacedescription confirm`
- `/areahint replacedescription cancel`
- `/areahint deletedescription`
- `/areahint deletedescription search [关键词]`
- `/areahint deletedescription select <域名>`
- `/areahint deletedescription confirm`
- `/areahint deletedescription confirm2`
- `/areahint deletedescription cancel`
- `/areahint adddimensionalitydescription`
- `/areahint adddimensionalitydescription search [关键词]`
- `/areahint adddimensionalitydescription select <维度域名>`
- `/areahint adddimensionalitydescription text <描述>`
- `/areahint adddimensionalitydescription confirm`
- `/areahint adddimensionalitydescription cancel`
- `/areahint replacedimensionalitydescription`
- `/areahint replacedimensionalitydescription search [关键词]`
- `/areahint replacedimensionalitydescription select <维度域名>`
- `/areahint replacedimensionalitydescription text <描述>`
- `/areahint replacedimensionalitydescription confirm`
- `/areahint replacedimensionalitydescription cancel`
- `/areahint deletedimensionalitydescription`
- `/areahint deletedimensionalitydescription search [关键词]`
- `/areahint deletedimensionalitydescription select <维度域名>`
- `/areahint deletedimensionalitydescription confirm`
- `/areahint deletedimensionalitydescription confirm2`
- `/areahint deletedimensionalitydescription cancel`
- `/areahint addsignature`
- `/areahint addsignature select <域名>`
- `/areahint addsignature name <玩家名>`
- `/areahint addsignature confirm`
- `/areahint addsignature cancel`
- `/areahint deletesignature`
- `/areahint deletesignature select <域名>`
- `/areahint deletesignature name <玩家名>`
- `/areahint deletesignature confirm`
- `/areahint deletesignature confirm2`
- `/areahint deletesignature cancel`

---

## ExpandArea 与 ShrinkArea 补充说明

### ExpandArea（域名扩展）

#### 方式一：直接指定域名（推荐）
```text
/areahint expandarea <域名>
```
- 支持 Tab 补全
- 只显示你有权限扩展的域名
- 直接进入记录顶点流程

#### 方式二：从列表选择
```text
/areahint expandarea
```
- 显示可点击的域名列表
- 点击对应按钮选择域名

### ShrinkArea（域名收缩）

#### 方式一：直接指定域名（推荐）
```text
/areahint shrinkarea <域名>
```
- 支持 Tab 补全
- 只显示你有权限收缩的域名
- 直接进入记录顶点流程

#### 方式二：从列表选择
```text
/areahint shrinkarea
```
- 显示可点击的域名列表
- 显示权限提示
- 点击对应按钮选择域名

### 常见流程子命令

#### ExpandArea 子命令
- `/areahint expandarea continue`
- `/areahint expandarea save`
- `/areahint expandarea cancel`

#### ShrinkArea 子命令
- `/areahint shrinkarea continue`
- `/areahint shrinkarea save`
- `/areahint shrinkarea cancel`

</details>

---

<a id="server-config"></a>

## 服务端配置与审批

服务端启动时读取运行目录下的全局 `areas-hint/server-config.json`。修改此文件后重启服务端；客户端世界目录中的同名文件是连接时同步的副本。

```json
{
  "submit_approval": true,
  "submit_urban_area": false,
  "force_use_areahint": false,
  "default_gamemode": "survival"
}
```

| 配置项 | 默认值 | 行为 |
|---|---|---|
| `submit_approval` | `true` | 普通玩家已获授权的域名修改先提交审批；OP 即时修改 |
| `submit_urban_area` | `false` | 独立启用城市化申请，关闭时隐藏命令和可视化入口 |
| `force_use_areahint` | `false` | 登录前校验客户端模组，并启用服务端域名游戏模式控制 |
| `default_gamemode` | 服务器默认模式 | 域名未指定合法模式或玩家离开所有域名时使用 |

示例中的 `survival` 不是固定的补齐值：服务端会从自己的默认模式初始化 `default_gamemode`，专用服务器对应 `server.properties` 的 `gamemode`。已有合法值会保留。缺失字段或非法值会自动补齐 / 修复；损坏的 JSON 会先备份为 `server-config.invalid-*.json.bak`，再生成默认配置。

### 提交、预览与回复

1. 普通玩家完成已获授权的创建、删除、几何编辑、重命名、颜色、高度、签名、副字幕或描述修改后，服务端保存申请，正式域名等待审批后更新。
2. 长按绑定键打开可视化面板。OP 使用“提交审批”查看申请，普通玩家使用“审批回复”查看自己的结果。
3. OP 可填写意见并选择“同意”“不同意”或“打回”。申请详情支持临时边界预览、传送及增删顶点；这些编辑不会提前写入正式文件。
4. 被打回的申请可继续修改并重新申请，历史意见会保留。离线玩家的回复会在下次连接时同步。

审批不扩大玩家原有权限，命令权限节点也不会授予 OP 审批权。若正式域名在申请期间发生变化，审批不会直接覆盖新数据。仅开启 `submit_urban_area` 时，城市化仍使用这一套审批流程，不受 `submit_approval` 关闭影响。

预览按钮会自动调用以下分支，通常无需手动输入；索引从 `0` 开始：

```text
/areahint tcp approval <申请ID> [索引]
/areahint addhint approval <申请ID> [索引]
/areahint deletehint approval <申请ID> [索引]
```

### 客户端模组校验与域名游戏模式

开启 `force_use_areahint` 后，玩家必须安装支持登录握手的客户端模组，且模组版本不低于服务端。缺少模组或版本过旧时会拒绝进入并提供下载地址。

此时服务端根据自己的玩家坐标、当前维度和正式域名数据选择最深层域名，应用其 `gamemode`；缺失或非法时使用 `default_gamemode`，不继承父域名模式。关闭客户端标题提示不会关闭这一功能。

使用 `/areahint replaceareagamemode` 选择域名和游戏模式，或通过可视化面板进入同一向导。默认需要权限等级 `2`，LuckPerms 节点为 `areahint.command.replaceareagamemode`。保存后会完整重载并同步域名。

详细操作与存储规则见 [命令使用说明](./COMMAND_USAGE.md)。

---

<a id="data-format"></a>

## 数据格式与配置说明

### 域名数据格式（JSON）

下面是单个域名对象。`overworld.json`、`the_nether.json`、`the_end.json` 的根节点应为对象数组，例如 `[ { ... } ]`。一级顶点与二级顶点均使用 `x` / `z` 坐标，高度由 `altitude` 单独控制。

```json
{
  "name": "精灵森林",
  "vertices": [
    { "x": 0, "z": 10 },
    { "x": 10, "z": 0 },
    { "x": 0, "z": -10 },
    { "x": -10, "z": 0 }
  ],
  "second-vertices": [
    { "x": -10, "z": 10 },
    { "x": 10, "z": 10 },
    { "x": 10, "z": -10 },
    { "x": -10, "z": -10 }
  ],
  "altitude": {
    "max": 100,
    "min": 0
  },
  "level": 2,
  "base-name": "蛮荒大陆",
  "signature": "player_name",
  "signatures": ["friend_name"],
  "color": "#55FFAA",
  "surfacename": "精灵森林",
  "subtitle": "林间风声与古树低语",
  "subtitlecolor": "#AAFFCC",
  "type": "rural",
  "gamemode": "adventure"
}
```

示例中的二级域名需要已有一级域名“蛮荒大陆”作为上级；独立的顶级域名可改为 `"level": 1`、`"base-name": null`。

### 字段说明

| 字段 | 含义 | 说明 |
|---|---|---|
| `name` | 域名名称 | 玩家进入区域时看到的名称基础 |
| `vertices` | 一级顶点 | 多边形本体顶点，决定真实区域形状 |
| `second-vertices` | 二级顶点 | 辅助边界顶点，通常用于快速预筛选 |
| `altitude.min` | 最低高度 | 玩家高度低于此值时不触发 |
| `altitude.max` | 最高高度 | 玩家高度高于此值时不触发 |
| `level` | 域名等级 | 普通域名使用整数；数值越大层级越低 |
| `base-name` | 上级域名 | 指向上一级域名；顶级域名可为 `null` |
| `signature` | 创建者 | 可为玩家名或 `null` |
| `signatures` | 扩展签名 | 额外可操作玩家列表，可为空数组或省略 |
| `color` | 域名颜色 | 推荐使用十六进制颜色值，也支持模组定义的闪烁颜色 |
| `surfacename` | 联合 / 表面域名 | 用于显示层面的联合名称，可为 `null` |
| `subtitle` | 域名副字幕 | 可选字段；为空或不存在时不显示 |
| `subtitlecolor` | 副字幕颜色 | 可选字段；支持十六进制颜色或闪烁颜色 |
| `type` | 域名类型 | `rural`（乡村）或 `urban`（城市）；缺失时按 `rural` 处理 |
| `gamemode` | 域名游戏模式 | 可选字段；仅在 `force_use_areahint` 开启时应用，缺失或非法时使用服务端默认模式 |

### 关于 `level`
- `1` 表示顶级域名
- `2` 表示二级域名
- `3` 表示三级域名
- 依此类推
- **数字越大，层级越低**
- 维度域名在概念上属于 `0.5` 级，但通常单独存储，不直接与普通区域 JSON 混写

### 配置文件格式（`config.json`）

客户端配置位于游戏运行目录下的 `areas-hint/config.json`。以下为默认值；未锁定的语言设置可以跟随游戏语言。

```json
{
  "frequency": 1,
  "hintRender": "OpenGL",
  "titleStyle": "mixed",
  "enabled": true,
  "recordKey": 88,
  "titleSize": "medium",
  "subtitlesize": "auto",
  "boundVizEnabled": false,
  "xaeroMinimapOverlayEnabled": true,
  "xaeroWorldMapOverlayEnabled": true,
  "language": "zh_cn",
  "languageLocked": false,
  "teleportFormat": "tp",
  "soundEvent": "none",
  "soundPitch": 1.0,
  "soundLevel": 1.0
}
```

### 配置项说明

| 字段 | 含义 | 说明 |
|---|---|---|
| `frequency` | 每秒最大检测次数 | 默认 `1`；命令范围 `1–60`，支持小数；数值越高，检测间隔越短 |
| `hintRender` | 提示文字渲染模式 | 可选 `CPU`、`OpenGL`、`Vulkan`（实验性） |
| `titleStyle` | 域名标题样式 | 可选 `full`、`simple`、`mixed` |
| `enabled` | 客户端提示开关 | `true` 时显示域名提示，`false` 时关闭 |
| `recordKey` | 绑定键键码 | 默认 `88`，对应 X 键；录点、描述查询和指令可视化入口共用此键 |
| `titleSize` | 域名标题大小 | 可选 `extra_large` 到 `extra_small`，也支持 `custom:<倍率>` |
| `subtitlesize` | 副字幕大小 | 默认 `auto`，也支持标题大小同一套预设和 `custom:<倍率>`；兼容读取旧字段 `subtitleSize` |
| `boundVizEnabled` | 边界可视化开关 | 控制 `/areahint boundviz` 的持久状态 |
| `xaeroMinimapOverlayEnabled` | Xaero 小地图覆盖层 | 默认 `true`，需安装匹配版本的小地图 |
| `xaeroWorldMapOverlayEnabled` | Xaero 世界地图覆盖层 | 默认 `true`，需安装匹配版本的世界地图 |
| `language` | 模组语言 | 对应语言文件名，例如 `zh_cn`、`en_us` |
| `languageLocked` | 语言锁定 | 为 `true` 时不跟随游戏语言自动切换 |
| `teleportFormat` | 传送命令头 | 默认 `tp`，可设置为 `minecraft:tp`、`teleport` 等有效命令头 |
| `soundEvent` | 切换音效 | 默认 `none`（关闭），或可用的声音事件 ID |
| `soundPitch` | 音高 | 默认 `1.0`，范围 `0.5–2.0` |
| `soundLevel` | 音量 | 默认 `1.0`，范围 `0.0–1.0` |

### 域名标题样式说明

| 样式 | 说明 |
|---|---|
| `full` | 显示完整域名路径 |
| `simple` | 仅显示当前所在层级的域名 |
| `mixed` | 根据层级与场景组合显示，适合作为默认选项 |

### 渲染模式说明

| 模式 | 说明 |
|---|---|
| `CPU` | 兼容性较高的软件渲染方案 |
| `OpenGL` | 默认推荐模式 |
| `Vulkan` | 当前为实验性实现；检测到 VulkanMod 时优先允许使用，否则会回退到 OpenGL |

### 维度域名配置格式（`dimensional_names.json`）

```json
[
  {
    "dimensionId": "minecraft:overworld",
    "displayName": "蛮荒大陆",
    "color": "#FFFFFF",
    "signature": null
  },
  {
    "dimensionId": "minecraft:the_nether",
    "displayName": "恶堕之域",
    "color": "#AA0000",
    "signature": null
  },
  {
    "dimensionId": "minecraft:the_end",
    "displayName": "终末之地",
    "color": "#AA00AA",
    "signature": null
  }
]
```

### 维度域名说明

- 维度域名用于在玩家**不处于任何普通区域**时显示当前维度名称
- 在概念上，维度域名的优先级低于普通区域
- 玩家刚进入世界、或离开所有普通区域时，可显示维度域名
- `color` 控制维度域名显示颜色，`signature` 用于维度描述等权限判断

### 描述文件格式

域名描述和维度域名描述不直接写入普通域名 JSON，而是存放在当前世界文件夹的 `database/` 目录中。描述文件内容为独立 JSON：

```json
{
  "description": "这里是可以通过书本界面编辑的域名描述"
}
```

---

## 多世界 / 多服务器文件夹机制

本模组支持为不同世界、不同服务器自动创建独立文件夹。
这样做的好处是：**不同存档、不同服务器之间的域名与维度域名配置互不干扰。**

### 示例目录结构

路径相对于客户端或服务端的游戏运行目录，位于 `areas-hint/`，不是 `config/areas-hint/`。以下以客户端目录为例；服务端配置和审批历史保存在服务端自己的全局目录。

```text
.minecraft/areas-hint/
├── config.json
├── lang/
├── server-config.json             # 服务端全局配置（服务端 / 集成服务端使用）
├── approval-requests/              # 服务端申请及审批历史，按 worldKey 分目录
├── 192.168.1.100_MyServer/
│   ├── overworld.json
│   ├── the_nether.json
│   ├── the_end.json
│   ├── dimensional_names.json
│   ├── server-config.json          # 连接时同步的客户端副本
│   ├── approval-replies.json       # 当前玩家的审批回复与历史
│   └── database/
│       ├── overworld/
│       ├── the_nether/
│       └── the_end/
├── localhost_SinglePlayer/
│   ├── overworld.json
│   ├── dimensional_names.json
│   └── database/
└── localhost_Server/
    ├── overworld.json
    ├── the_nether.json
    ├── the_end.json
    ├── dimensional_names.json
    └── database/
```

### 工作原理

1. **连接检测**：加入世界或服务器时，检测当前连接信息
2. **文件夹创建**：若不存在对应文件夹，则自动创建
3. **文件初始化**：在对应文件夹中生成维度 JSON、`dimensional_names.json` 与 `database/` 描述目录
4. **路径重定向**：后续域名、维度域名、描述与传送操作都使用当前世界 / 当前服务器的数据路径
5. **同步一致性**：服务端与客户端通过网络同步保证当前世界上下文一致

服务端申请文件位于 `areas-hint/approval-requests/<worldKey>/<申请UUID>.json`，其中 `worldKey` 由服务端存档路径生成。客户端回复文件也记录当前玩家 UUID，避免切换账号后混淆回复。

---

## 架构设计

模组采用**客户端 - 服务端分离架构**。

### 客户端部分
- 负责读取配置与同步后的区域数据
- 执行区域检测逻辑
- 负责提示文字渲染与动画显示
- 提供交互式 UI、语言切换、描述书本界面、边界可视化与 Mod Menu 配置

### 服务端部分
- 负责区域数据与维度数据管理
- 处理命令、权限、描述文件、签名与数据同步
- 为客户端提供当前世界上下文与区域数据
- 保存与处理审批申请、城市化及回复历史
- 执行登录握手校验，并在开启强制模组时独立计算域名游戏模式

---

## 模块划分

### 1. 核心模块（Core）
- 模组初始化
- 生命周期管理
- 注册命令与事件监听

### 2. 数据模型模块（Data Model）
- 域名数据结构
- 配置数据结构
- 维度域名数据结构
- 副字幕、扩展签名与描述数据结构

### 3. 文件管理模块（File Management）
- 配置目录创建
- JSON 读取与写入
- 默认配置生成
- 当前世界 `database/` 描述目录管理

### 4. 区域检测模块（Area Detection）
- AABB 预筛选
- 高度过滤
- 射线法区域判断
- 区域层级与优先级计算

### 5. 渲染模块（Rendering）
- CPU / OpenGL / Vulkan 渲染实现
- 区域提示动画
- 标题、副字幕、样式与大小控制

### 6. 网络模块（Networking）
- 服务端向客户端同步区域与维度数据
- 处理各种交互式功能的数据包

### 7. 世界文件夹管理模块（World Folder Management）
- 不同世界 / 服务器的数据路径隔离
- 自动创建与切换世界文件夹

### 8. 维度域名模块（Dimensional Names）
- 维度名称管理
- 离开普通区域时的名称显示
- 维度相关同步与 UI

### 9. 命令模块（Commands）
- 域名创建、删除、重命名
- 几何编辑
- 描述、副字幕、传送和扩展签名
- 显示设置
- 调试功能

### 10. 权限模块（Permission）
- 权限节点定义
- 权限服务封装
- LuckPerms 兼容
- 域名创建者与扩展签名者操作判断

### 11. 地图集成模块（Map Integration）
- BlueMap 兼容桥接
- 动态标记状态与颜色支持
- Xaero 小地图 / 世界地图的边界、填充与名称覆盖层

### 12. 描述与副字幕模块（Description / Subtitle）
- 普通域名与维度域名描述的查询、添加、替换、删除
- 原版书本界面编辑描述
- 域名副字幕文本、颜色与大小管理

### 13. 传送模块（Teleport）
- 域名中心点传送
- 域名范围内随机安全落点搜索
- 客户端传送命令头配置

### 14. 服务端管理模块
- 服务端配置自动补齐与修复
- 提交审批、预览、回复与打回后重新申请
- 域名城市化与域名游戏模式
- 客户端登录握手校验

### 15. 音效与版本适配模块
- 客户端域名切换音效、音高与音量
- Stonecutter 多版本编排
- 版本目录中的网络、渲染和第三方兼容实现

---

## 核心算法说明

### 射线法（Ray Casting）
模组使用射线法判断玩家当前位置是否位于多边形内部：

- 向一侧发射一条射线
- 计算它与多边形边界的交点数
- **奇数次相交**：点在多边形内
- **偶数次相交**：点在多边形外

### 优化机制
在进行精确判断前，通常会先执行：

1. **高度预筛选**
2. **AABB 包围盒快速排除**
3. **再进入射线法精确判断**

这样可以减少不必要的计算开销。

---

## 文件结构

```text
areas-hint/
├── README.md                       # 英文首页
├── README.zh-CN.md                 # 简体中文说明
├── src/
│   ├── main/java/areahint/         # 服务端与共享代码
│   │   ├── Areashint.java          # 模组入口
│   │   ├── approval/              # 提交审批、预览与回复
│   │   ├── config/                # 服务端配置
│   │   ├── gamemode/              # 服务端域名游戏模式
│   │   ├── handshake/             # 客户端模组登录校验
│   │   ├── urban/                 # 城市化申请
│   │   ├── permission/            # 权限节点与 LuckPerms
│   │   ├── map/                   # BlueMap 集成
│   │   └── ...                    # 命令、数据、文件、网络及各编辑功能
│   ├── main/resources/
│   │   ├── fabric.mod.json
│   │   └── assets/areas-hint/      # 图标与各语言翻译
│   ├── client/java/areahint/       # 客户端检测、渲染和交互
│   │   ├── AreashintClient.java
│   │   ├── commandui/             # 指令可视化面板
│   │   ├── detection/             # 高度过滤、AABB 与多边形检测
│   │   ├── render/                # 标题与副字幕渲染
│   │   ├── soundevent/            # 域名切换音效
│   │   ├── xaero/                 # 地图覆盖层共享逻辑
│   │   └── ...
│   ├── client/resources/          # 客户端 Mixin 配置
│   └── test/java/areahint/         # 现有 JUnit 测试
├── versions/<Minecraft版本>/
│   ├── src/                       # 版本适配与第三方兼容实现
│   └── build/libs/                # 对应版本的构建产物
├── docs/plans/                    # 功能与多版本设计记录
├── tools/                         # 维护与核对脚本
├── gradle/wrapper/                # Gradle Wrapper
├── build.gradle                   # 各版本共享构建脚本
├── settings.gradle                # 当前启用的构建目标
├── stonecutter.gradle             # 开发活跃版本
├── gradle.properties              # 模组版本与各目标依赖
└── LICENSE
```

---

<a id="docs"></a>

## 相关文档

如果你想查看更细的使用或开发信息，可以继续阅读以下文档：

- [`COMMAND_USAGE.md`](./COMMAND_USAGE.md) —— 命令使用说明
- [`LOG_FEATURE.md`](./LOG_FEATURE.md) —— 日志功能说明
- [`RECOLOR_REFACTOR_SUMMARY.md`](./RECOLOR_REFACTOR_SUMMARY.md) —— 重新着色相关重构说明
- [`REPLACEBUTTON_IMPLEMENTATION_SUMMARY.md`](./REPLACEBUTTON_IMPLEMENTATION_SUMMARY.md) —— 替换绑定键实现说明
- [`REPLACEBUTTON_EXPANDAREA_SHRINKAREA_INTEGRATION.md`](./REPLACEBUTTON_EXPANDAREA_SHRINKAREA_INTEGRATION.md) —— 替换绑定键与扩展 / 收缩流程集成说明
- [`SHRINKAREA_REFACTOR.md`](./SHRINKAREA_REFACTOR.md) —— 收缩域名重构说明
- [`SHRINKAREA_CHANGES.md`](./SHRINKAREA_CHANGES.md) —— 收缩域名变更记录
- [`TITLESIZE_GUIDE.md`](./TITLESIZE_GUIDE.md) —— 域名标题大小说明
- [`TITLESTYLE_INTERACTIVE_GUIDE.md`](./TITLESTYLE_INTERACTIVE_GUIDE.md) —— 域名标题样式交互说明
- [`TRANSLATION_GUIDE.md`](./TRANSLATION_GUIDE.md) —— 翻译指南
- [`TRANSLATION_REPORT.md`](./TRANSLATION_REPORT.md) —— 翻译报告
- [`CLAUDE.md`](./CLAUDE.md) —— 项目开发与结构说明
- [多版本规划](./docs/plans/multiversion/README.md) —— 版本适配规划与历史构建记录

历史设计文档可能包含尚未实施的方案，实际行为以当前源码和构建配置为准。

---

<a id="compatibility"></a>

## 模组兼容性

| 可选模组 | 联动用途 |
|---|---|
| LuckPerms | `areahint.command.*` 权限节点；未定义节点时按原版权限等级与域名操作规则回退 |
| BlueMap | 服务端域名标记与颜色更新 |
| Mod Menu | 客户端配置界面 |
| Xaero's Minimap | 小地图上的域名覆盖层 |
| Xaero's World Map | 世界地图上的域名覆盖层与交互 |
| VulkanMod | 实验性 Vulkan 渲染模式 |

Xaero 联动按精确版本检查；未安装或版本不匹配时，对应覆盖层不会启用。请使用与 Minecraft 目标版本匹配的 Fabric 文件：

| Minecraft | Xaero's Minimap | Xaero's World Map |
|---|---|---|
| `1.20.1` | `25.3.2` | `1.39.2` |
| `1.20.4` | `25.2.0` | `1.39.2` |
| `1.20.6` | `25.2.10` | `1.39.12` |
| `1.21.1` | `25.3.2` | `1.39.2` |
| `1.21.4` | `25.3.2` | `1.39.2` |

Sodium 在模组元数据中列为建议项。其他渲染、性能或地图模组的共存情况取决于 Minecraft 版本和整合包组合；这些可选模组不随 Areas Hint 一起打包。

---

<a id="development"></a>

## 构建与开发

### 常用开发命令

从仓库根目录使用 Gradle Wrapper。构建全部目标时使用 **JDK 21**；`settings.gradle` 决定参与构建的版本，`stonecutter.gradle` 当前活跃版本为 `1.20.4`。

```bash
# 构建所有已启用的版本
./gradlew build

# 只构建一个目标
./gradlew :1.20.4:build

# 运行指定版本的开发环境
./gradlew :1.20.4:runClient
./gradlew :1.20.4:runServer
./gradlew :1.20.4:runDatagen

# 现有测试与清理任务
./gradlew :1.20.4:test
./gradlew clean
```

Windows PowerShell 中将 `./gradlew` 换成 `.\gradlew.bat`，例如：

```powershell
.\gradlew.bat :1.20.4:build
```

### 构建产物命名

当前 Stonecutter 构建产物位于 `versions/<Minecraft版本>/build/libs/`，文件名遵循「模组名-平台-MC版本-模组版本」格式：

```text
versions/1.20.4/build/libs/areas-hint-fabric-1.20.4-4.6.0.jar
versions/1.21.4/build/libs/areas-hint-fabric-1.21.4-4.6.0.jar
```

安装普通 `.jar`，不要安装 `-sources.jar` 或 `-dev.jar`。`mod_loader` 只参与产物命名，修改它不会把 Fabric 模组转换为 Forge / NeoForge 模组。

### 开发建议
- 调试区域检测时可使用 `/areahint debug on`
- 测试区域边界时可配合 `/areahint boundviz`
- 修改配置文件或区域 JSON 后可执行 `/areahint reload`
- 修改描述功能时同时检查当前世界 `database/` 目录
- 对区域层级、包围盒与高度条件进行联调时，建议同时查看客户端与服务端日志

---

## 已知限制与说明

- **Vulkan 渲染目前仍偏实验性**，不建议作为默认选项依赖；没有 VulkanMod 环境时会回退到 OpenGL
- 兼容性列表会随着整合包环境、Fabric 生态版本、显卡驱动和其他模组更新而变化
- 某些命令的可用性与权限表现可能受服务器权限配置影响
- 描述命令只删除或修改描述文件，不会删除域名本体
- 传送功能依赖当前维度域名数据和安全落点搜索，复杂地形中可能找不到可站立位置
- 若你手动编辑 JSON，请确保字段命名与层级关系正确，否则可能导致域名不显示或层级异常

---

## 许可证

本项目采用 [MIT 许可证](./LICENSE)。

[返回顶部](#top) · [English](./README.md)
