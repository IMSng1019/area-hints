# 阶段 0：Stonecutter 探针（主体已完成，只剩 D4 收尾）

> **探针实验已经实测完成**，结论见 `10-阶段0-探针结论.md`、`17-第二真实编译目标验证.md`、`18-受控实验-版本目录编译语义.md`。
> **不要再重复探针实验。** 本文件现在只剩一件事：**按 D4 把 Gradle 升到 9.x + Stonecutter 0.9.7，并确认多节点构建仍全绿。**

## 一、已完成的事实（勿重复验证）

| 项目 | 结论 |
|---|---|
| Stonecutter 与 `splitEnvironmentSourceSets()` + Loom 共存 | **可以**（0.7.11 + Gradle 8.12.1 已跑通；0.9.7 + Gradle 9.5.1 也已跑通） |
| 1.20.4 产物一致性 | `jar tf` **456 条 vs 456 条，差异 0**（基线 jar 已备份到 `probe-logs/baseline-1.20.4.jar`） |
| 版本目录语义 | **追加编译，无覆盖语义**；同名同路径 → `错误: 类重复` |
| 1.20.1 作为第二真实目标 | **可行**：`src/main` 0 错误，`src/client` 16 错误 / 12 文件 |
| 1.20.6 | **不可直接编译**：`src/main` 61 条网络层错误 |
| 产物路径变化 | 根项目在 Stonecutter 下是 tree，不执行构建任务；产物在 `versions/<v>/build/libs/` |
| 已知坑 | `jar { from("LICENSE") }` 会相对节点目录解析，必须写成 `from(rootProject.file("LICENSE"))`（已修） |

## 二、当前构建配置现状（动手前先核对，若已变动按实际为准）

- `settings.gradle`：已应用 `dev.kikugie.stonecutter` **0.7.11**，`kotlinController = false`，`centralScript = 'build.gradle'`，活跃版本 `versions '1.20.4', '1.21.1'`；`1.20.1` 与 `1.20.6` 以注释形式保留复现方式
- `gradle.properties`：已有 `deps.<版本>.{minecraft,yarn,loader,fabric,java,skeleton}` 六类键；旧扁平键全部保留
- `build.gradle`：按 `deps.<mcVersion>.<name>` 取参数（缺失回退旧键）；`skeletonOnly` 版本把 main/client 两个 source set 指向 `skeleton/`
- Gradle Wrapper：**仍是 8.12.1**（本阶段要升）
- `versions/`：`1.20.4/`（完整）、`1.20.1/`（现场保留）、`1.21.1/`（含 `skeleton/`）

## 三、本阶段剩余任务：D4 升级 Gradle + Stonecutter

### Task 1：升级 Wrapper 与插件版本

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties`
- Modify: `settings.gradle`
- Modify: `gradle.properties`

**Step 1:** 把 Wrapper 的 `distributionUrl` 从 `gradle-8.12.1-bin.zip` 改为 `gradle-9.5.1-bin.zip`（该版本已实测可用）。
**Step 2:** 把 `settings.gradle` 里的插件版本从 `'0.7.11'` 改为 `'0.9.7'`，并同步更新 `gradle.properties` 里的 `stonecutter_version` 记录值。
**Step 3:** **不要改动构建脚本的逻辑**——实测同一套 `build.gradle` / `stonecutter.gradle` 在 0.9.7 上可直接使用。
**Step 4:** 若出现 Groovy DSL 能力提示（`dev.kikugie.stonecutter.hard_mode`），保留默认即可，不要为了消除提示去改 DSL 语言。

### Task 2：验证升级后一切照旧

**Step 1:** `& .\gradlew.bat projects --console=plain` → 应列出全部版本节点，退出码 0。
**Step 2:** `& .\gradlew.bat build --console=plain` → 退出码 0。
**Step 3:** 与基线对照：`versions/1.20.4/build/libs/areas-hint-fabric-1.20.4-4.6.0.jar` 的 `jar tf` 与 `probe-logs/baseline-1.20.4-listing.txt` **逐条一致（456 条）**，文件大小 1 494 819 字节。
**Step 4:** 1.21.1 骨架 jar 仍能产出。
**Step 5:** 把结果**追加**到 `10-阶段0-探针结论.md`（不要覆盖已有实测数据），并在 `00-总纲与约定.md` 第三节把 Wrapper 版本更新为实际值。

## 四、验收标准

- Wrapper 为 Gradle 9.x，Stonecutter 为 0.9.7，`gradlew projects` 与 `gradlew build` 均退出码 0。
- 1.20.4 产物的 `jar tf` 与基线逐条一致（456 条），大小不变。
- `src/` 下没有任何文件被修改；`git status --short src` 为空。
- 未创建分支 / worktree；未 commit。
- 汇报包含：实际 Gradle 与 Stonecutter 版本、两条命令的退出码、`jar tf` 对比结论、是否出现新的弃用警告。

## 五、禁止事项

- 不重新做探针实验（已验证过的事不要再跑一遍）。
- 不改 `src/` 下任何文件。
- 不引入任何 `//? if` 条件注释。
- 不新增版本目标（扩矩阵属于阶段 1）。
- 不顺手升级 Loom 到其他版本；`1.10-SNAPSHOT` 在 Gradle 9.5.1 上已验证可用。
- 若升级后出现无法在 10 分钟内定位的失败，**回退到 0.7.11 + 8.12.1 并汇报**，不要连续试多个组合。

## 六、失败时的回退

把 `gradle-wrapper.properties`、`settings.gradle`、`gradle.properties` 恢复到 0.7.11 + 8.12.1 的状态，跑一次 `& .\gradlew.bat build` 确认回到绿色，并在结论文件里写明失败原因与完整报错。
