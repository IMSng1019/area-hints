# 阶段 4：渲染与 mixin 跨版本适配

> 前置：阶段 3 完成。**这是风险仅次于阶段 0 的一环**——1.21 起渲染整代重写，1.20.x ↔ 1.21.x 的注入点与 HUD 宿主都会变。

## 目标

把渲染宿主与 mixin 注入点的差异从 `src/` 赶进 `versions/`，使 `src/` 里的渲染业务代码（标题渲染、颜色、CPU 渲染的图像处理、边界可视化逻辑）完全不感知版本。

## 现状（动手前先核对）

- 客户端渲染相关包：`render/`（`RenderManager`、`CPURender`、`GLRender`、`VulkanRender`、`TitleRenderHelper`、`FlashColorHelper`）、`boundviz/`、`subtitle/`、`titlesize/`、`titlestyle/`
- `CPURender` / `GLRender` 依赖 `HudRenderCallback` + `DrawContext` + `MatrixStack`，渲染模式由配置项 `SubtitleRender` 选择
- mixin 共 3 个：`WorldRendererMixin`（注入 `WorldRenderer#render` 调 `renderChunkDebugInfo` 之后）、`TranslationStorageMixin`（注入 `get` 的 HEAD，可取消）、`ExampleMixin`（示例，无实际功能）

## 任务

### Task 1：给渲染宿主做一层薄适配

**Files:**
- Create: `src/client/java/areahint/render/host/RenderHost.java`（接口）
- Create: `src/client/java/areahint/render/host/RenderContext.java`（最小上下文数据）

**Step 1:** 接口只暴露本模组真正需要的能力：拿到文字渲染器、拿到矩阵、绘制文本/图形、获取帧增量（tick delta）或等价时间量。**不要做通用渲染框架**，不要试图拉平所有版本。
**Step 2:** 现有 `CPURender`、`GLRender`、`TitleRenderHelper` 改为通过该接口取所需能力，使它们不再直接 import `HudRenderCallback` / `DrawContext` / `MatrixStack`。
**Step 3:** 如果某一项能力在两个世代语义不同（例如帧增量从 `float` 变成帧计数器对象），**在接口里用本模组语义命名**（如"自上一帧起的时间量"），不要泄漏版本类型。
**Step 4:** 每改一个文件立即构建。

### Task 2：HUD 注册按版本落地

**Files:**
- Create: `versions/<旧世代最低版本>/src/client/java/areahint/render/host/RenderHostImpl.java`
- Create: `versions/<新世代最低版本>/src/client/java/areahint/render/host/RenderHostImpl.java`

**Step 1:** 各世代一份实现，负责在该版本的 HUD 事件上注册回调，并把参数转换成本模组的 `RenderContext`。
**Step 2:** 同代际的其它版本目录留空。
**Step 3:** 回调注册的时机与现有行为一致（不提前、不延后），注册失败要有明确日志。

### Task 3：mixin 注入点按版本隔离

**Files:**
- Modify: `src/client/resources/areas-hint.client.mixins.json`
- Create: `versions/<新世代最低版本>/src/client/resources/areas-hint.client.mixins.json`
- Create: `versions/<新世代最低版本>/src/client/java/areahint/mixin/client/WorldRendererMixin.java`

**Step 1:** 注入点签名在各版本不同的 mixin（当前只有 `WorldRendererMixin` 属于此类），**在 `versions/` 下各放一份独立类与独立配置**，各自只激活对应版本。
**Step 2:** 类名相同、方法签名随版本不同的做法**不允许**（会造成后续改名噩梦），必须按版本目录隔离。
**Step 3:** `TranslationStorageMixin` 这类签名稳定的 mixin 保持在 `src/` 不动；若某版本签名变化，再为那一代实现一份。
**Step 4:** `ExampleMixin` / `ExampleClientMixin` 是模板残留：确认无功能后**在汇报里提出**是否删除，不要自行删除。
**Step 5:** mixin 配置 `required: true` 与注入失败策略（`defaultRequire`）保持现状语义，不要为了兼容把 required 改成 false 来掩盖问题。

### Task 4：渲染模式与第三方渲染器对齐

**Files:**
- 视情况 Modify: `src/client/java/areahint/render/RenderManager.java`
- 视情况 Modify: `src/client/java/areahint/render/VulkanModCompat.java`

**Step 1:** 确认各版本下 `OpenGL` / `CPU` / `Vulkan` 三种模式是否都有实现；某版本缺某模式时，**在该版本的实现里明确降级并打日志**，不要静默失败。
**Step 2:** `VulkanModCompat` 属于第三方依赖，本阶段只保证"不因为版本差异而崩溃"，完整隔离在阶段 6 处理。

## 验收标准

- `src/` 的渲染业务文件中不再直接 import `HudRenderCallback` / `DrawContext` / `MatrixStack`（残余逐条说明理由）。
- 每个渲染断点世代下都有且仅有一份 `RenderHostImpl`。
- mixin 配置与 mixin 类按版本成对出现，不会出现"配置引用了不存在于该版本的类"。
- 所有已声明版本构建通过；`src/` 新增条件注释仅限 ≤3 行的差异且逐条说明。
- 汇报里给出：改动文件清单、各版本构建结果、需要我确认的删除项（如 ExampleMixin）。

## 禁止事项

- 不写测试。
- 不为了"统一"重写渲染管线，不引入新的渲染后端。
- 不把 `required` 改成 `false` 来让构建通过。
- 不删除任何功能（含模板残留文件）——先汇报。
