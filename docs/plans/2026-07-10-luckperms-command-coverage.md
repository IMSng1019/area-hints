# LuckPerms Command Coverage Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 7 个尚未接入权限服务的 `/areahint` 顶层指令补齐 LuckPerms 联动。

**Architecture:** 沿用 `PermissionNodes` 与 `PermissionService` 的显式权限节点架构，在 Brigadier 顶层节点统一使用权限等级 0 作为回退。对会修改服务端维度域名数据的首次命名网络包增加同节点二次校验。

**Tech Stack:** Java 17、Fabric Command API、Fabric Networking API、LuckPerms API 5.4、Gradle

---

### Task 1: 补充权限节点常量

**Files:**
- Modify: `src/main/java/areahint/permission/PermissionNodes.java`

新增 `ON`、`OFF`、`FIRST_DIMNAME`、`FIRST_DIMNAME_SKIP`、`REPLACE_BUTTON`、`LANGUAGE`、`BOUND_VIZ` 常量，节点名与顶层指令名称一致。

### Task 2: 保护 Brigadier 顶层指令

**Files:**
- Modify: `src/main/java/areahint/command/ServerCommands.java`

为 7 个顶层指令增加 `PermissionService.hasCommandPermission(..., 0)`，未安装 LuckPerms 或节点未定义时保持当前默认开放行为。

### Task 3: 保护首次维度命名网络入口

**Files:**
- Modify: `src/main/java/areahint/network/ServerNetworking.java`

处理 `C2S_FIRST_DIMNAME` 前检查 `PermissionNodes.FIRST_DIMNAME`，拒绝时使用现有权限错误消息并停止写入。

### Task 4: 更新权限文档

**Files:**
- Modify: `README.md`

补充 7 个新节点，并说明副字幕和别名当前使用的权限映射。

### Task 5: 验证

按仓库要求不新增测试。运行 `\.\gradlew.bat build`，然后使用 `rg` 静态核对 7 个指令及首次命名网络入口均引用对应权限常量。
