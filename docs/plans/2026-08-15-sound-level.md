# 域名切换音效音量 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Repository rules explicitly prohibit new tests, worktrees, and commits for this task.

**Goal:** 为域名切换音效增加 `0.0–1.0` 独立音量配置，并接入聊天指令、配置面板滑条和指令可视化。

**Architecture:** 在共享 `ConfigData` 中保存 `soundLevel`，由客户端 `soundevent` 包统一完成设置、格式化与试听；现有声音选择和域名变化播放都读取该值。聊天指令、配置草稿和指令可视化分别复用同一校验与播放入口；服务端只提供同构命令语法镜像以支持聊天补全和着色，不增加音量处理协议。

**Tech Stack:** Java 17、Fabric API 客户端指令、Minecraft 1.20.4 Screen/SliderWidget/TextFieldWidget、Gson 注释配置、现有 I18nManager。

---

### Task 1: 扩展音量配置模型与持久化

**Files:**
- Modify: `src/main/java/areahint/data/ConfigData.java`
- Modify: `src/main/java/areahint/file/FileManager.java`
- Modify: `src/client/java/areahint/config/ClientConfig.java`

**Step 1:** 在 `ConfigData` 增加 `SOUND_LEVEL_MIN = 0.0f`、`SOUND_LEVEL_MAX = 1.0f`、默认值为 `1.0f` 的 `soundLevel` 字段，并在所有构造方法和 `copy()` 中保留它。

**Step 2:** 增加 getter、setter、`clampSoundLevel(float)` 和稳定的两位小数去尾零格式化方法；非有限值恢复 `1.0f`，其余值限制到有效范围。

**Step 3:** 在 `FileManager.createDefaultConfigFile()` 和 `writeConfigData()` 中写出带中文说明的 `soundLevel`，保持现有注释配置格式。

**Step 4:** 在读取流程中补齐旧配置缺失字段，并将非数字、非有限或越界值恢复默认值后重新保存。

**Step 5:** 在 `ClientConfig` 增加 `getSoundLevel()` 和一次保存的 `setSoundLevel(float)`。

### Task 2: 让全部声音播放使用可配置音量

**Files:**
- Modify: `src/client/java/areahint/soundevent/SoundEventManager.java`
- Modify: `src/client/java/areahint/soundevent/SoundEventVisualController.java`

**Step 1:** 将 `preview(SoundEventSelection)` 改为读取当前 `ClientConfig.getSoundLevel()`，并增加接收明确音量的重载作为共享底层入口。

**Step 2:** 用规范化音量替换 `player.playSound` 中硬编码的 `1.0f`。

**Step 3:** 立即保存声音选择时使用当前配置音量试听；配置面板草稿模式使用 `draft.getSoundLevel()` 试听，确保取消配置不会污染持久配置。

**Step 4:** 增加设置音量后试听当前声音的共享方法；当前声音为 `none` 或缺失时只保存，不产生额外错误。

### Task 3: 增加 soundlevel 客户端指令和聊天按钮

**Files:**
- Create: `src/client/java/areahint/soundevent/SoundLevelClientCommand.java`
- Create: `src/client/java/areahint/soundevent/SoundLevelChatUI.java`
- Modify: `src/client/java/areahint/AreashintClient.java`

**Step 1:** 使用 `ClientCommandRegistrationCallback` 在客户端 `/areahint` 根节点下注册 `soundlevel`，无参数打开聊天菜单，`<level>` 使用 `FloatArgumentType.floatArg(0.0f, 1.0f)`。

**Step 2:** 聊天菜单显示当前值，并为 `0`、`0.25`、`0.5`、`0.75`、`1.0` 生成独立可点击按钮和悬停说明。

**Step 3:** 直接数值和按钮命令都调用同一保存入口，成功后按需试听并显示本地成功消息。

**Step 4:** 在客户端初始化阶段注册新指令，保持服务端命令树、权限和网络协议不变。

### Task 4: 在配置面板增加滑条和精确输入

**Files:**
- Modify: `src/client/java/areahint/gui/AreasHintConfigScreen.java`

**Step 1:** 在声音分组的声音事件行后增加音量行，并按检测频率布局创建 `SoundLevelSlider` 与 `TextFieldWidget`。

**Step 2:** 滑条将归一化位置映射为 `0.00–1.00`，以 `0.01` 为显示精度；拖动只更新草稿和输入框，不持续播放声音。

**Step 3:** 输入框接受有限小数并只在有效范围内更新草稿和滑条；允许输入过程中暂时出现空值或未完成小数点。

**Step 4:** 保持完成、取消、重置语义，并确保重建列表时控件从当前草稿恢复。

### Task 5: 增加 soundlevel 指令可视化流程

**Files:**
- Create: `src/client/java/areahint/soundevent/SoundLevelVisualController.java`
- Create: `src/client/java/areahint/soundevent/SoundLevelScreen.java`
- Modify: `src/client/java/areahint/commandui/CommandVisualRegistry.java`

**Step 1:** 新建继承 `CommandUiScreen` 的音量界面，展示当前值、`0.0–1.0` 滑条、精确输入框和五个常用档位按钮。

**Step 2:** 输入和滑条共享一个规范化草稿值；非法输入禁用确认或显示范围错误，不保存部分内容。

**Step 3:** 确认时调用客户端音量保存入口并返回父面板，取消时不修改配置；绑定键关闭行为沿用 `CommandUiScreen`。

**Step 4:** 在 `CommandVisualRegistry` 的显示分类注册 `soundlevel`，默认命令为 `areahint soundlevel`。

### Task 6: 同步全部语言文件

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

**Step 1:** 增加 `screen.areahint.config.sound_level`、`help.command.soundlevel`、`soundlevel.*` 和 `commandui.soundlevel.*` 统一键集合。

**Step 2:** 为每个文件分别使用对应语言翻译，不减少或重排无关键。

**Step 3:** 解析全部 JSON 并比较新增键集合，确认没有缺失、重复或语法错误。

### Task 7: 构建与最终验证

**Files:**
- Verify: all modified files

**Step 1:** 运行 `git diff --check`，预期无空白错误。

**Step 2:** 解析 11 个语言 JSON 并核对新增键集合完全一致。

**Step 3:** 运行 `.\gradlew.bat build`，预期编译、资源处理、现有测试任务和 remapJar 全部成功；不新增测试文件。

**Step 4:** 检查 `git diff --stat`、`git status --short` 和相关完整差异，确认没有修改 `build/`、`run/`、权限、网络协议或域名 JSON；服务端命令只允许增加客户端指令的同构语法镜像。

### Task 8: 修复 soundlevel 聊天补全与语法着色

**Files:**
- Modify: `src/main/java/areahint/command/ServerCommands.java`

**Step 1:** 按 `@superpowers:systematic-debugging` 对照已正常工作的 `replacesoundevent`，确认客户端执行正常但服务端同步树缺少 `soundlevel` 是唯一差异。

**Step 2:** 在 `/areahint` 根节点中紧邻 `createReplaceSoundEventCommand()` 接入 `createSoundLevelCommand()`，使输入 `/areahint ` 时能获得 `soundlevel` 字面量。

**Step 3:** 增加与客户端树同构的最小语法镜像：

```java
private static LiteralArgumentBuilder<ServerCommandSource> createSoundLevelCommand() {
    return literal("soundlevel")
        .executes(ServerCommands::executeClientOnlyCommandPlaceholder)
        .then(argument("level", FloatArgumentType.floatArg(
                ConfigData.SOUND_LEVEL_MIN, ConfigData.SOUND_LEVEL_MAX))
            .executes(ServerCommands::executeClientOnlyCommandPlaceholder));
}
```

该方法只复用现有占位执行器，不增加权限、网络包、固定数值建议或服务端配置写入。

**Step 4:** 运行 `git diff --check`、`.\gradlew.bat build` 和最终差异复查；遵照仓库要求不新增测试文件、不创建 worktree、不提交。
