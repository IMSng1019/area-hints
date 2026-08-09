# 域名切换音效 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Follow this plan task-by-task in the current main workspace. Repository rules explicitly prohibit new tests, commits, and worktrees for this task.

**Goal:** 增加一个默认关闭、纯客户端运行的域名切换音效系统，支持全部已注册声音、音符盒乐器与 25 音阶、配置面板、聊天指令和指令可视化。

**Architecture:** 使用 `ConfigData` 保存声音 ID 与音高；客户端 `soundevent` 功能包懒加载并分类 `Registries.SOUND_EVENT`，同时提供聊天按钮与 Screen 两套选择入口。`AreaChangeTracker` 发布实际域名状态时通知一个常量时间的播放追踪器，追踪器只在“维度 + 当前普通域名/维度域名”发生变化时播放一次。

**Tech Stack:** Java 17、Fabric API 0.97.2、Minecraft/Yarn 1.20.4、Brigadier 客户端指令、Minecraft Screen/Widget、Gson 配置。

---

### Task 1: 扩展个人配置模型与持久化

**Files:**
- Modify: `src/main/java/areahint/data/ConfigData.java`
- Modify: `src/main/java/areahint/file/FileManager.java`
- Modify: `src/client/java/areahint/config/ClientConfig.java`

**Step 1:** 在 `ConfigData` 增加 `soundEvent`、`soundPitch` 字段，默认分别为 `none`、`1.0f`；所有构造方法和 `copy()` 都保留该配置。

**Step 2:** 增加声音 ID、音高的 getter/setter 和共享校验。`none` 始终有效；音高只允许有限数值并限制在原版音符盒范围 `0.5–2.0`。

**Step 3:** 在 `FileManager.createDefaultConfigFile()` 与 `writeConfigData()` 中写出带中文说明的两个字段，并正确处理 JSON 字符串转义。

**Step 4:** 在 `applyMissingConfigDefaults()` 和读取校验流程中补全旧配置缺失或无效的声音字段，不改变其他个人设置。

**Step 5:** 在 `ClientConfig` 增加读取、设置与一次保存声音选择的接口，供聊天指令、图形界面和播放追踪器共用。

### Task 2: 建立动态声音目录和选择值模型

**Files:**
- Create: `src/client/java/areahint/soundevent/SoundEventSelection.java`
- Create: `src/client/java/areahint/soundevent/SoundEventCatalog.java`

**Step 1:** 使用不可变选择值保存声音 ID、音高、音符盒音阶和显示信息；为 `none`、普通声音、音符盒声音提供明确工厂方法。

**Step 2:** 在目录首次访问时遍历 `Registries.SOUND_EVENT.getIds()`，按完整 ID 排序并缓存，后续界面打开复用缓存。

**Step 3:** 根据路径前缀将全部声音归入 `note_block`、`ambient_weather`、`block`、`entity`、`item`、`music`、`ui`、`event_particle`、`other`；任何未知声音进入 `other`。

**Step 4:** 对 `block.note_block.*` 按乐器 ID 分组，并为每种乐器生成 0–24 音阶；音高公式为 `(float) Math.pow(2.0, (note - 12) / 12.0)`。

**Step 5:** 提供非空分类、分类声音、音符盒乐器、指定乐器音阶、按 ID 查找声音等只读查询接口。

### Task 3: 实现声音试听、保存和域名变化播放

**Files:**
- Create: `src/client/java/areahint/soundevent/SoundEventManager.java`
- Create: `src/client/java/areahint/soundevent/DomainSoundTracker.java`

**Step 1:** 在 `SoundEventManager` 中集中完成选择校验、配置保存、配置草稿写入回调、当前选择格式化和本地试听。

**Step 2:** 通过客户端声音系统以玩家音效类别、音量 `1.0f` 和已选音高播放；`none` 不播放。

**Step 3:** 对注册表中不存在的声音跳过播放，对同一缺失 ID 每次客户端会话只记录一次警告，并保留原配置。

**Step 4:** 在 `DomainSoundTracker` 保存上一次域名键；普通域名键包含维度 ID 与原始域名名称，域名外使用维度 ID 表示维度域名。

**Step 5:** 新键与旧键不同时播放一次；`dimensionId == null`、模组关闭或断开连接时只静默重置。

### Task 4: 实现纯客户端聊天指令流程

**Files:**
- Create: `src/client/java/areahint/soundevent/ReplaceSoundEventClientCommand.java`
- Create: `src/client/java/areahint/soundevent/SoundEventChatUI.java`
- Modify: `src/client/java/areahint/AreashintClient.java`

**Step 1:** 使用 `ClientCommandRegistrationCallback` 注册 `/areahint replacesoundevent`，只增加客户端子树，不修改 `ServerCommands`、权限节点或网络包。

**Step 2:** 注册 `category <category> <page>`、`instrument <soundId>`、`notes <soundId> <page>`、`select <soundId> <pitch>`、`none`、`cancel` 等客户端子指令；参数在客户端再次验证。

**Step 3:** 根指令显示当前选择、无声音按钮和非空分类按钮；普通分类和音阶列表按固定数量分页。

**Step 4:** 每个声音或音阶输出独立可点击文本按钮，悬停显示完整 ID、分类、音高；导航按钮包含上一页、下一页、返回分类和取消。

**Step 5:** 选择后立即试听、保存并输出本地成功消息，整个流程不调用 `sendClientCommand` 或客户端网络包。

### Task 5: 实现可滚动图形选择器并接入配置面板

**Files:**
- Create: `src/client/java/areahint/soundevent/SoundEventVisualController.java`
- Create: `src/client/java/areahint/soundevent/SoundEventCategoryScreen.java`
- Create: `src/client/java/areahint/soundevent/SoundEventListScreen.java`
- Modify: `src/client/java/areahint/gui/AreasHintConfigScreen.java`

**Step 1:** 分类 Screen 使用按钮展示无声音和全部非空分类，音符盒分类进入乐器按钮页。

**Step 2:** 声音 Screen 使用 `ElementListWidget`，每个目录项创建稳定尺寸的独立按钮；普通声音显示 ID，音符盒显示乐器 ID 与音阶编号。

**Step 3:** 点击按钮时试听并调用注入的选择回调；为配置草稿模式和立即保存模式使用同一套 Screen，不复制目录逻辑。

**Step 4:** 配置面板在常规设置组增加音效行，按钮显示 `无声音`、声音 ID 或音符盒音阶；点击后打开选择器，选择结果只更新 `draft` 并返回原配置面板。

**Step 5:** 保持配置面板现有“完成/取消/重置”语义，确保 `new ConfigData()` 重置为无声音。

### Task 6: 接入指令可视化与域名状态发布

**Files:**
- Modify: `src/client/java/areahint/commandui/CommandVisualRegistry.java`
- Modify: `src/client/java/areahint/log/AreaChangeTracker.java`
- Modify: `src/client/java/areahint/AreashintClient.java`

**Step 1:** 在指令可视化注册表的显示分类加入 `replacesoundevent`，打开图形选择器并使用立即保存模式。

**Step 2:** 在 `AreaChangeTracker.publishDetectionState()` 真正发布新状态后调用 `DomainSoundTracker.update(areaName, dimensionId)`；相同状态的早返回继续阻止重复播放。

**Step 3:** `reset()` 发布空维度时让声音追踪器静默重置；这样断线、模组关闭和维度切换中的中间清理不会产生离开声音。

**Step 4:** 检查登录同步检测、异步检测、强制重新检测和维度切换均经过同一发布点，避免在多个 tick 分支重复添加播放调用。

### Task 7: 更新全部翻译文件

**Files:**
- Modify: `src/main/resources/assets/areas-hint/lang/de_de.json`
- Modify: `src/main/resources/assets/areas-hint/lang/en_pt.json`
- Modify: `src/main/resources/assets/areas-hint/lang/en_us.json`
- Modify: `src/main/resources/assets/areas-hint/lang/es_es.json`
- Modify: `src/main/resources/assets/areas-hint/lang/fr_fr.json`
- Modify: `src/main/resources/assets/areas-hint/lang/ja_jp.json`
- Modify: `src/main/resources/assets/areas-hint/lang/ko_kr.json`
- Modify: `src/main/resources/assets/areas-hint/lang/ru_ru.json`
- Modify: `src/main/resources/assets/areas-hint/lang/zh_cn.json`
- Modify: `src/main/resources/assets/areas-hint/lang/zh_cn_neko.json`
- Modify: `src/main/resources/assets/areas-hint/lang/zh_tw.json`

**Step 1:** 定义统一的 `soundevent.*`、`commandui.replacesoundevent.*`、`screen.areahint.config.sound_event` 和 `help.command.replacesoundevent` 键集合。

**Step 2:** 为每个语言文件分别写入该语言的分类、按钮、当前值、选择成功、缺失声音、分页和音符盒文本，不使用英文批量替换。

**Step 3:** 保留现有键与文件结构，不减少无关行数；动态声音 ID 保持原始 ID，不伪造无法可靠取得的翻译名称。

### Task 8: 构建与静态验证

**Files:**
- Verify: all modified files

**Step 1:** 运行 `git diff --check`，预期无空白错误。

**Step 2:** 使用 PowerShell `ConvertFrom-Json` 逐个解析 11 个语言文件，确认 JSON 合法且新键集合一致。

**Step 3:** 运行 `.\gradlew.bat build`，预期 Java 编译、资源处理、测试任务和 remapJar 全部成功；不新增测试文件。

**Step 4:** 检查 `git diff --stat` 和 `git diff`，确认未修改 `build/`、`run/`、域名 JSON、服务端命令、权限或网络协议。

**Step 5:** 静态核对播放调用只位于状态发布链路，声音注册表扫描只位于目录懒加载路径，聊天指令全部使用客户端命令 API。
