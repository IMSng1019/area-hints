# Repository Guidelines

## Project Structure & Module Organization

Areas Hint is a Minecraft Fabric 1.20.4 mod built with Gradle and Java 17. Server/shared code lives in `src/main/java/areahint/`, with the main entry point at `Areashint.java`. Client-only code lives in `src/client/java/areahint/`, including detection, rendering, UI, and key handling. Resources are split the same way: `src/main/resources/` contains `fabric.mod.json`, mixin config, the icon, and language files under `assets/areas-hint/lang/`; `src/client/resources/` contains client mixin config. Documentation and feature notes are kept in root Markdown files such as `README.md`, `COMMAND_USAGE.md`, and `TRANSLATION_GUIDE.md`.

## Build, Test, and Development Commands

Use the Gradle wrapper from the repository root:

- `./gradlew build` or `.\gradlew.bat build`: compiles, tests, remaps, and packages the mod.
- `./gradlew test`: runs the JUnit 5 test suite.
- `./gradlew runClient`: launches a local Minecraft client for manual testing.
- `./gradlew runServer`: launches a development server.
- `./gradlew runDatagen`: regenerates Fabric data outputs.
- `./gradlew clean`: removes build artifacts.

## Coding Style & Naming Conventions

Use Java 17 and keep package names under `areahint`. Follow the existing feature-package layout: server networking in `src/main/java/areahint/<feature>/`, client managers and UI in `src/client/java/areahint/<feature>/`. Name classes by role, for example `ExpandAreaManager`, `ExpandAreaUI`, and `ExpandAreaServerNetworking`. Keep JSON keys compatible with existing area data (`vertices`, `second-vertices`, `base-name`, `surfacename`). Prefer clear, localized translatable text entries in `assets/areas-hint/lang/*.json`.

## Testing Guidelines

JUnit 5 is configured in `build.gradle`. Add tests under `src/test/java/areahint/`, mirroring the package under test, and name classes `*Test`. Prioritize unit tests for geometry, color validation, JSON conversion, permissions, and world-folder logic. For gameplay behavior, also verify manually with `runClient` or `runServer`, using `/areahint debug on`, `/areahint boundviz`, and `/areahint reload` where relevant.

## Commit & Pull Request Guidelines

Recent commits use concise version-number messages such as `4.3.6`; keep release commits in that style. For feature work, use short imperative summaries that identify the changed area, for example `Add teleport permission checks`. Pull requests should describe behavior changes, list test commands run, mention affected client/server paths, and include screenshots or short recordings for UI, rendering, or in-game command changes.

## Agent-Specific Instructions

Do not edit generated build output in `build/`, runtime data in `run/`, or local IDE files unless explicitly requested. Preserve user-facing Chinese terminology around “域名” and area JSON fields unless changing documented behavior.

# 建议
- 我的话会涉及到一些概念主要是在域名文件json格式当中：完整格式为{"name": "这里是区域名称（我将其定义为域名 这个定义上下文通用）", "vertices": [这是多边形的一个点（一级顶点）,{"x":横坐标,"z":纵坐标},{"x":横坐标,"z":纵坐标},{"x":横坐标,"z":纵坐标}],"second-vertices":[{"x":横向最小坐标值,"y":纵向最大坐标值},{"x":横向最大坐标值,"y":纵向最大坐标值},{"x":横向最大坐标值,"y":纵向最小坐标值},{"x":横向最小坐标值,"y":纵向最小坐标值}],"altitude": {"max":最大的高度,"min":最小的高度"},"level": 指域名等级（ 必须为整数 数字越大域名等级越小 1为顶级域名 2为二级域名 3为三级域名 2和3为次级域名 注意 1对于域名等级并不是最大值 每个维度有维度域名 维度域名等级为0.5 ） ,"base-name":"这里是该域名所指向的是上一级的域名（指向的域名的域名等级必须等于该域名等级-1,null就是无上级域名）,"signature":null(的是该域名的创建者 null指无创建者) ,"color":"这里是域名颜色用的是十六进制表示法","surfacename":"这里指联合域名（或者叫表面域名）"} 每个世界都有一个世界文件夹 second-vertices为二级顶点

绑定键指模组内统一的按键，用于记录顶点等等

- 注意不要写测试 我会自己测试
- 代码注释详细 使用中文注释 单处注释尽量只用一行
- 代码文件尽量保持与原架构一致
- 我的提示词中涉及需要参考的对象都是可以正常运作的 你需要详细读取代码 理解它的实现
- 添加或者修改翻译文件时必须将所有的翻译文件都进行对应语言的修改 不要偷懒 不要用英文批量替换 非不必要不进行翻译文件的行数的减少
- 所有的子agent也是直接在main上修改，不需要创额外的worktree
- 不要commit 直接在main上修改