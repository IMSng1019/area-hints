# Xaero Modrinth Dependencies Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 让本地与 GitHub Actions 都从 Modrinth Maven 获取固定版本的 Xaero 编译依赖，不再依赖被 Git 忽略的 `.deps` 目录。

**Architecture:** 在 Gradle 仓库中加入仅服务于 `maven.modrinth` 组的 Modrinth Maven 源，并在 `gradle.properties` 集中固定两个 Fabric 版本。依赖同时加入 `modCompileOnly` 与 `modLocalRuntime`，从而允许兼容代码编译和本地联调，同时不把 Xaero 模组打进 Areas Hint 产物。

**Tech Stack:** Gradle 8、Fabric Loom 1.10、Modrinth Maven、Java 17

---

### Task 1: 将 Xaero 本地 JAR 改为固定版本的远程依赖

**Files:**
- Modify: `build.gradle`
- Modify: `gradle.properties`
- Test: 不新增测试文件；仓库明确要求由用户自行测试，本次使用无 `.deps` 的 Gradle 编译作为回归验证

**Step 1: 确认修复前失败基线**

临时隐藏 `.deps` 后运行：

```powershell
.\gradlew.bat compileClientJava --rerun-tasks
```

Expected: FAIL，`compileClientJava` 报告 `xaero.*` 包不存在。该失败基线已于 2026-07-17 复现，共 48 个编译错误。

**Step 2: 固定远程版本**

在 `gradle.properties` 的依赖版本区域加入：

```properties
xaero_minimap_version=25.2.0_Fabric_1.20.4
xaero_world_map_version=1.39.2_Fabric_1.20.4
```

**Step 3: 配置 Modrinth Maven**

在 `build.gradle` 的 `repositories` 中加入仅解析 `maven.modrinth` 组的仓库：

```groovy
maven {
	name = 'Modrinth'
	url = 'https://api.modrinth.com/maven'
	content {
		includeGroup 'maven.modrinth'
	}
}
```

**Step 4: 替换本地文件依赖**

删除 `.deps` 文件对象和 `exists()` 条件分支，加入：

```groovy
modCompileOnly "maven.modrinth:xaeros-minimap:${project.xaero_minimap_version}"
modLocalRuntime "maven.modrinth:xaeros-minimap:${project.xaero_minimap_version}"
modCompileOnly "maven.modrinth:xaeros-world-map:${project.xaero_world_map_version}"
modLocalRuntime "maven.modrinth:xaeros-world-map:${project.xaero_world_map_version}"
```

### Task 2: 验证云端等价构建与产物边界

**Files:**
- Verify: `build.gradle`
- Verify: `gradle.properties`
- Verify: `build/libs/areas-hint-4.5.1.jar`

**Step 1: 在无本地 JAR 条件下执行完整构建**

临时隐藏 `.deps` 后运行：

```powershell
.\gradlew.bat build --refresh-dependencies
```

Expected: BUILD SUCCESSFUL，且命令结束后恢复 `.deps`。

**Step 2: 确认未重新分发 Xaero 类**

检查最终 JAR 条目，确认不存在根包 `xaero/`；项目自身的 `areahint/xaero/` 兼容类应继续存在。

**Step 3: 检查变更质量**

运行：

```powershell
git diff --check
git status --short
```

Expected: 无空白错误，只包含计划文档、`build.gradle` 与 `gradle.properties` 的未提交修改。

本计划不创建 worktree、不新增测试、不执行提交。
