# 22 · 渲染宿主差异与 RenderHost 草案（阶段 4 主体前置调研）

> 任务 task-15（owner mig-client-core）。本文只做**只读调研 + 接口草案**：不改源码/构建脚本、不跑 gradle、不 git 提交。
> 扫描范围 **src/ 全部 267 个 .java**（main + client）；实测工具 grep/read + javap（JDK 21，C:\Program Files\Java\jdk-21\bin\javap.exe）。
> 本文所有「1.21.1 实测」都指附录 A 列出的缓存 jar 里的**类文件签名**，不是推测；没有实测到的写法一律标 **待验证**。

---

## 零、结论速览（给 Lead 拍板用）

| 符号 | 1.20.4（现状） | 1.21.1 实测 | 是否断点 | 处置 |
|---|---|---|---|---|
| **HudRenderCallback.onHudRender** | (DrawContext, float) | (DrawContext, **RenderTickCounter**) | **是** | 4 文件 / 8 行；可由 RenderHost 消化 |
| **DrawContext** | 本模组用到 12 个方法 | 12 个方法签名**逐一相同**，1.21.1 只增不减 | **否** | 37 个文件零改动 |
| **MatrixStack** | getMatrices + push/pop/translate/scale | 全部存在 | **否** | 无需改 |
| **Screen.render / renderBackground** | (DrawContext, int, int, float) | 同 | **否** | 无需改 |
| **RenderSystem** | 用到 12 个方法 | 12 个全部存在 | **否** | 2 个文件无需改 |
| **WorldRenderer.render** | 8 参数（含 MatrixStack、float、long） | 7 参数（RenderTickCounter、无 MatrixStack、两个 Matrix4f） | **是** | mixin 按版本隔离（第五节） |
| **WorldRenderer.renderChunkDebugInfo** | (MatrixStack, VertexConsumerProvider, Camera) | **描述符完全相同**，调用点仍在 render 内 | 否 | 注入点本身两代都成立 |
| **TranslationStorage.get** | (String, String) | 同，且无重载 | **否** | mixin 留 src/ |
| **HudLayerRegistrationCallback / IdentifiedLayer** | 不存在 | **1.21.1 上也不存在** | —— | 见 2.1：26.x 才换宿主，且类名是 HudElementRegistry |

**三条与初始预期不同的结论**

1. **1.21.1 上 HUD 宿主没有改名。** HudRenderCallback 仍在（fabric-rendering-v1 5.2.1），只有回调第二参数 float 变成 RenderTickCounter。
2. **DrawContext / MatrixStack 不是 1.21.1 的断点。** 本模组用到的成员在 1.21.1 上原子集存在，37 个 DrawContext 文件可零改动编译。
3. **真正需要 RenderHost 的是 26.x 代际。** 26.1.2 的 fabric-rendering-v1 23.3.1 里 HudRenderCallback **已删除**，改为 net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry / HudElement / VanillaHudElements（实测类清单）。

所以 1.21.1 的「解锁成本」很小（4 文件 8 行 + 1 个 mixin）；RenderHost 建议按 05 文档照建，但**定位成「为 26.x 预留的最小接口」**，不要顺手抽象 37 个 Screen。

---

## 一、真实扫描计数（Q1）

口径：Node 只读扫描 src/ 267 个 .java；**行**=含该符号的行数，**次**=出现次数，**文件**=去重文件数；未剔除注释命中（与 13 号清单同口径，便于对照）。

| 符号 | 行 | 次 | 文件 | 备注 |
|---|---:|---:|---:|---|
| HudRenderCallback | 8 | 8 | 4 | 与 13 号清单 8/4 完全一致 |
| DrawContext | 98 | 98 | 37 | 13 号未单列 |
| MatrixStack | 19 | 19 | 8 | 13 号未单列 |
| RenderSystem | 26 | 26 | 2 | 13 号未单列 |
| TextRenderer | 24 | 24 | 5 | —— |
| Matrix4f | 21 | 23 | 5 | —— |
| VertexConsumerProvider | 11 | 11 | 4 | 其中 GLRender 只有 import（实际未用） |
| BufferBuilder | 9 | 9 | 2 | 世界渲染（boundviz / xaero 叠层） |
| Tessellator | 4 | 5 | 2 | 同上 |
| WorldRenderer | 5 | 5 | 1 | 仅 WorldRendererMixin |
| TranslationStorage | 3 | 3 | 1 | 仅 TranslationStorageMixin |
| import net.fabricmc.fabric.api.client.rendering.* | 4 | 4 | 4 | 就是下面 1.1 的 4 个文件 |
| HudLayerRegistrationCallback / IdentifiedLayer / LayeredDrawer / RenderTickCounter | 0 | 0 | 0 | 新式 HUD API 在本仓库**零痕迹** |

### 1.1 HudRenderCallback：8 行 / 4 文件（全部真实调用）

| 文件 | 行号 | 内容 |
|---|---|---|
| src/client/java/areahint/render/CPURender.java | 11(import)、85 | HudRenderCallback.EVENT.register(this::onHudRender) |
| src/client/java/areahint/render/GLRender.java | 12(import)、63 | 同上 |
| src/client/java/areahint/render/VulkanRender.java | 5(import)、61 | 同上 |
| src/client/java/areahint/easyadd/EasyAddRecordingHud.java | 5(import)、26 | HudRenderCallback.EVENT.register(EasyAddRecordingHud::render) |

回调体签名（4 处，统一）：CPURender:93、GLRender:71、VulkanRender:69、EasyAddRecordingHud:30，都是 (DrawContext, float)。

### 1.2 MatrixStack：19 行 / 8 文件

| 文件 | 行号 |
|---|---|
| src/client/java/areahint/render/CPURender.java | 8(import)、154 |
| src/client/java/areahint/render/GLRender.java | 9(import)、132 |
| src/client/java/areahint/render/VulkanRender.java | 9(import)、134 |
| src/client/java/areahint/render/TitleRenderHelper.java | 7(import)、228（drawCenteredLine 形参） |
| src/client/java/areahint/mixin/client/WorldRendererMixin.java | 8(import)、26、30 |
| src/client/java/areahint/boundviz/BoundVizRenderer.java | 10(import)、48、568 |
| src/client/java/areahint/xaero/minimap/AreaMinimapRenderer.java | 15(import)、107 |
| src/client/java/areahint/xaero/worldmap/AreaWorldMapRenderer.java | 14(import)、72、148 |

用法只有三种：drawContext.getMatrices()（CPURender:154 / GLRender:132 / VulkanRender:134）、push/translate/scale/pop（TitleRenderHelper:234-236、249）、以及 xaero/boundviz 的世界渲染。

### 1.3 RenderSystem：26 行 / 2 文件

| 文件 | 行号 | 调用方法集合 |
|---|---|---|
| src/client/java/areahint/boundviz/BoundVizRenderer.java | 7(import)、59、66-71、89-91 | —— |
| src/client/java/areahint/xaero/OverlayRenderHelper.java | 3(import)、41-46、115-137 | —— |
| 合计 12 个方法 | —— | getProjectionMatrix、enableBlend、defaultBlendFunc、enableDepthTest、depthMask、disableCull、setShader、enableCull、disableBlend、disableDepthTest、getShader、blendFuncSeparate |

### 1.4 DrawContext：98 行 / 37 文件（按目录分组）

| 目录 / 文件 | 行数 |
|---|---|
| commandui/CommandListScreen.java | 5 |
| commandui/CommandUiScreen.java | 5 |
| commandui/DeleteHintVertexScreen.java | 3 |
| commandui/HelpCommandScreen.java | 3 |
| commandui/PlaceholderCommandScreen.java | 2 |
| commandui/RecordCommandActionScreen.java | 2 |
| commandui/WizardConfirmScreen.java | 2 |
| commandui/WizardExecutionScreen.java | 2 |
| commandui/WizardLongTextInputScreen.java | 2 |
| commandui/WizardOptionScreen.java | 4 |
| commandui/WizardSelectionListScreen.java | 3 |
| commandui/WizardTextInputScreen.java | 2 |
| description/AddDescriptionVisualController.java | 2 |
| description/AddDimensionalityDescriptionVisualController.java | 2 |
| description/DeleteDescriptionVisualController.java | 2 |
| description/DeleteDimensionalityDescriptionVisualController.java | 2 |
| description/DescriptionBookEditScreen.java | 3 |
| description/ReplaceDescriptionVisualController.java | 2 |
| description/ReplaceDimensionalityDescriptionVisualController.java | 2 |
| easyadd/EasyAddConfirmScreen.java | 2 |
| easyadd/EasyAddRecordingActionScreen.java | 2 |
| easyadd/EasyAddRecordingHud.java | 2 |
| gui/AreasHintConfigScreen.java | 4 |
| management/client/AreaManagementClient.java | 2 |
| management/client/AreaManagementScreen.java | 4 |
| render/CPURender.java | 2 |
| render/GLRender.java | 2 |
| render/TitleRenderHelper.java | 3 |
| render/VulkanRender.java | 2 |
| boundviz/BoundVizVisualController.java | 2 |
| debug/DebugVisualController.java | 2 |
| replacebutton/ReplaceButtonVisualController.java | 2 |
| soundevent/SoundEventCategoryScreen.java | 3 |
| soundevent/SoundEventListScreen.java | 3 |
| soundevent/SoundLevelScreen.java | 3 |
| xaero/minimap/AreaMinimapRenderer.java | 2 |
| xaero/worldmap/AreaWorldMapRenderer.java | 6 |

**其中只有 render/ 的 4 个文件属于 05 文档说的「渲染业务」；其余 33 个是 Screen/UI 业务编排，1.21.1 上零改动（证据见 2.2、2.7）。**

---

## 二、1.21.1 实测对照（Q2）

证据 jar（绝对路径见附录 A）：
- MC 1.20.4（Yarn 名）：minecraft-clientonly-1.20.4-net.fabricmc.yarn.1_20_4.1.20.4+build.3-v2.jar
- MC 1.21.1（Yarn 名）：minecraft-clientonly-1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2.jar
- Fabric API 1.21.1（Yarn 重映射）：fabric-rendering-v1-5.2.1+f44c2eea19.jar（Loom 缓存，对应 gradle.properties 的 fabric 0.116.17+1.21.1）
- Fabric API 1.20.4（intermediary）：fabric-rendering-v1-3.0.12+78d798af4f.jar
- Fabric API 26.1.2（intermediary）：fabric-rendering-v1-23.3.1+e9207d814c.jar

### 2.1 HUD 回调：三个世代的实测结果（本调研最关键的一条）

~~~
1.20.4  javap … HudRenderCallback
  public abstract void onHudRender(net.minecraft.class_332, float);
1.21.1  javap … HudRenderCallback
  public abstract void onHudRender(net.minecraft.client.gui.DrawContext,
                                   net.minecraft.client.render.RenderTickCounter);
26.1.2  javap … HudRenderCallback  → 错误: 找不到类
        jar tf 该模块：…/rendering/v1/hud/HudElement.class
                        …/rendering/v1/hud/HudElementRegistry.class
                        …/rendering/v1/hud/VanillaHudElements.class
~~~

- **1.21.1 没有 HudLayerRegistrationCallback / IdentifiedLayer**：javap 在这三个 jar 上全部报「找不到类」。Lead 提到的这两个名字在本机缓存里没有任何实测证据，**标为待验证**（推测属于 1.21.6–1.21.9 区间的过渡 API，26.x 又换成了 HudElementRegistry）。
- 1.21.1 的可执行改法只有一件事：回调第二参数从 float 换成 RenderTickCounter（而且这个参数**本模组根本没用到**，见 2.4）。

### 2.2 DrawContext：本模组用到的 12 个方法，1.20.4 与 1.21.1 签名逐一相同

| 方法 | 1.20.4 | 1.21.1 |
|---|---|---|
| drawTextWithShadow(TextRenderer, Text/String/OrderedText, int,int,int) | 有 | 有（3 个重载全同） |
| drawCenteredTextWithShadow(TextRenderer, Text/String/OrderedText, int,int,int) | 有 | 有（3 个重载全同） |
| drawText(TextRenderer, …, int, boolean) | 有 | 有（3 个重载全同） |
| drawTextWrapped(TextRenderer, StringVisitable, int,int,int,int) | 有 | 有 |
| drawTexture(Identifier, …) | 5 个重载 | 5 个重载全同 |
| drawOrderedTooltip(TextRenderer, List, int, int) | 有 | 有 |
| fill(int,int,int,int,int) | 有 | 有 |
| drawBorder(int,int,int,int,int) | 有 | 有 |
| getMatrices() → MatrixStack | 有 | 有 |
| getScaledWindowWidth() / getScaledWindowHeight() | 有 | 有 |
| enableScissor(int,int,int,int) / disableScissor() | 有 | 有 |

全量方法名 diff（javap 去重）：**1.21.1 = 1.20.4 + {drawTextWithBackground, scissorContains}，没有任何删除**。
结论：**37 个 DrawContext 文件在 1.21.1 上不需要改**。

### 2.3 MatrixStack：成员只增不减

~~~
1.21.1  MatrixStack:  push() pop() translate(double,double,double) translate(float,float,float)
                      scale(float,float,float) multiply(Quaternionf[,float,float,float])
                      peek() isEmpty() loadIdentity() multiplyPositionMatrix(Matrix4f)
1.21.1  MatrixStack$Entry:  getPositionMatrix() getNormalMatrix() transformNormal(...) copy()
~~~

- 本模组用到 push/pop/translate/scale（TitleRenderHelper:234-236、249）与 getPositionMatrix（1 处），**全部存在**。
- MatrixStack$Entry 在 1.21.1 **没有** transformPosition；本模组没有用 Entry.transformPosition（AreaMinimapRenderer:111 的 matrix.transformPosition 是 JOML Matrix4f 的方法，不受影响）。

### 2.4 RenderTickCounter：本模组 4 个 HUD 回调都没用到第二参数

~~~
1.21.1  RenderTickCounter:  float getLastFrameDuration(); float getTickDelta(boolean);
                            float getLastDuration(); ZERO; ONE;
~~~

实测「tickDelta 出现次数」：

| 文件 | 出现位置 | 是否使用 |
|---|---|---|
| render/CPURender.java | 91(javadoc)、93(签名) | **未使用** |
| render/GLRender.java | 69(javadoc)、71(签名) | **未使用** |
| render/VulkanRender.java | 67(javadoc)、69(签名) | **未使用** |
| easyadd/EasyAddRecordingHud.java | 30(签名) | **未使用** |
| boundviz/BoundVizRenderer.java | 48(签名 render(MatrixStack, float)) | **未使用**（方法体内无引用） |

动画全部走 System.currentTimeMillis()（CPURender:109 等），不依赖帧增量。
=> 1.21.1 修复可以只是「换参数类型/名字」；RenderHost 的 frameDelta 能力当前 **0 个调用点**（见 3.4 的取舍建议）。

### 2.5 RenderSystem：12 个在用方法在 1.21.1 全部存在

1.21.1 javap 实测存在：disableDepthTest、enableDepthTest、depthMask(boolean)、enableBlend、disableBlend、blendFuncSeparate(int,int,int,int) 与 (GlStateManager 枚举,×4)、enableCull、disableCull、defaultBlendFunc、setShader(Supplier<ShaderProgram>)、getShader()、getProjectionMatrix()。
结论：boundviz/BoundVizRenderer.java 与 xaero/OverlayRenderHelper.java **不需要因 1.21.1 改动**（xaero 那侧仍归阶段 6）。

### 2.6 TextRenderer / MinecraftClient：稳定

- 1.21.1 TextRenderer：public final int fontHeight；getWidth(String/Visitable/OrderedText)；trimToWidth(String,int)。
- 1.21.1 MinecraftClient：public final TextRenderer textRenderer 字段仍在。
两个后端用到的 client.textRenderer、getWidth、trimToWidth、fontHeight 全部可用。

### 2.7 Screen（影响 33 个 UI 文件）：签名一致

- render(DrawContext,int,int,float)、renderWithTooltip、renderBackground(DrawContext,int,int,float)、init() 在 1.20.4 与 1.21.1 **完全相同**。
- 本模组调用 renderBackground 的 4 个文件（CommandUiScreen:105/107、AreasHintConfigScreen:239/241、AreaManagementClient:171/173、AreaManagementScreen:69/71）不受影响。
- 1.21.1 新增 renderPanoramaBackground / renderDarkening / 带参数的 renderBackgroundTexture；1.20.4 的 renderBackgroundTexture(DrawContext) 在 1.21.1 变为带参数版本——**本模组未调用 renderBackgroundTexture**（0 命中），所以无影响。

### 2.8 TranslationStorage：稳定（详见 5.2）

1.21.1 javap：public String get(String, String)；public boolean hasTranslation(String)；无其它 get 重载。与 1.20.4 一致。

### 2.9 WorldRenderer：唯一的真断点（详见第五节）

~~~
1.20.4  public void render(MatrixStack, float, long, boolean, Camera, GameRenderer,
                          LightmapTextureManager, Matrix4f);                      // 8 参数
        private void renderChunkDebugInfo(MatrixStack, VertexConsumerProvider, Camera);
1.21.1  public void render(RenderTickCounter, boolean, Camera, GameRenderer,
                          LightmapTextureManager, Matrix4f, Matrix4f);             // 7 参数
        private void renderChunkDebugInfo(MatrixStack, VertexConsumerProvider, Camera);  // 描述符相同
~~~

---

## 三、RenderHost 接口草案（Q3）

### 3.1 设计原则

- 接口与上下文**零 net.minecraft.* import**（12 号契约规则 1 对 (a) 类的要求），放 src/。
- 只覆盖「本模组真正需要的能力」：全部能力都能对应到现有调用点（下表逐个给出）。
- 不抽象 DrawContext 给 37 个 Screen 用；不引入新的渲染后端；不做通用渲染框架。
- 与 NetworkCompat 同形：接口在 src/ + **每版本目录一份同名工厂**（规则 5 接法 1）+ 入口一行 install。

### 3.2 RenderContext（最小上下文，零 MC import）

~~~java
package areahint.render.host;

/** 一帧 HUD 渲染的最小能力面：只覆盖标题/副字幕/EasyAdd 提示的真实需要。 */
public interface RenderContext {
    int screenWidth();
    int screenHeight();
    void pushMatrix();
    void translate(double x, double y, double z);
    void scale(float x, float y, float z);
    void popMatrix();
    void fill(int x1, int y1, int x2, int y2, int argb);
    void drawBorder(int x, int y, int width, int height, int argb);
    void drawText(String text, int x, int y, int argb);
    void drawCenteredText(String text, int centerX, int y, int argb);
    int textWidth(String text);
    int fontHeight();
    String trimToWidth(String text, int maxWidth);
}
~~~

### 3.3 RenderHost（注册 + 交接）

~~~java
package areahint.render.host;

/** 渲染宿主接口：注册 HUD 回调，零 net.minecraft.* import。 */
public interface RenderHost {
    /** HUD 一帧回调：参数是本模组自己的上下文，不泄漏 DrawContext/RenderTickCounter */
    @FunctionalInterface
    interface HudCallback {
        void render(RenderContext context);
    }

    void registerHudCallback(HudCallback callback);

    static void install(RenderHost impl) { Holder.instance = impl; }

    static RenderHost get() { /* 与 NetworkCompat.get() 同形，未安装即抛异常 */ }

    final class Holder { static RenderHost instance; }
}
~~~

### 3.4 能力映射表（每个方法在 1.20.4 / 1.21.1 分别由什么实现）

| RenderContext 方法 | 现有调用点 | 1.20.4 实现 | 1.21.1 实现 | 26.x（待验证） |
|---|---|---|---|---|
| screenWidth/Height | CPURender:98-99、GLRender:76-77、VulkanRender:78-79 | drawContext.getScaledWindowWidth/Height() | 同 | 同（DrawContext 仍在） |
| pushMatrix/translate/scale/popMatrix | TitleRenderHelper:234-236、249 | drawContext.getMatrices() → MatrixStack 同名方法 | 同 | 待验证 |
| fill | TitleRenderHelper:245 | drawContext.fill(int×5) | 同 | 同 |
| drawBorder | EasyAddRecordingHud:56 | drawContext.drawBorder(int×5) | 同 | 同 |
| drawText | TitleRenderHelper:263/268/271 | drawContext.drawTextWithShadow(client.textRenderer, Text.literal(s), x, y, argb) | 同 | 同 |
| drawCenteredText | EasyAddRecordingHud:58 | drawContext.drawCenteredTextWithShadow(client.textRenderer, Text.literal(s), cx, y, argb) | 同 | 同 |
| textWidth | TitleRenderHelper:149/239/264 | client.textRenderer.getWidth(s) | 同 | 同 |
| fontHeight | TitleRenderHelper:214/221/245 | client.textRenderer.fontHeight | 同 | 同 |
| trimToWidth | EasyAddRecordingHud:73 | client.textRenderer.trimToWidth(s, w) | 同 | 同 |
| frameDelta()（**建议不加**） | **0 个调用点** | 回调的 float tickDelta | tickCounter.getTickDelta(false) | 待验证 |
| registerHudCallback | CPURender:85 等 4 处 | HudRenderCallback.EVENT.register((ctx, delta) -> cb.render(new Ctx1204(ctx, delta))) | HudRenderCallback.EVENT.register((ctx, tickCounter) -> cb.render(new Ctx1211(ctx, tickCounter))) | **HudElementRegistry 形态**，需另一份实现 |

**逐版本实现差异只有两行**：注册 lambda 的第二参数类型、以及 frameDelta 的取值方式；其余 11 个方法两代逐字相同。这正是「薄适配」应有的体量。

**frameDelta 的取舍**：05 文档 Step 1 列了「获取帧增量」，但实测本模组 0 个调用点（2.4）。建议**先不放进接口**；若 Lead 想保留接口前瞻性，就按上表保留并在实现里写死，且注明「当前无调用方」。

### 3.5 交接点与放置位置

| 文件 | 位置 | 依据 |
|---|---|---|
| RenderHost.java / RenderContext.java | src/client/java/areahint/render/host/ | 接口零 MC import（规则 1） |
| RenderHostFactory.java（同名工厂） | versions/1.20.4/src/client/java/areahint/render/host/ 与 versions/1.21.1/… | 规则 5 接法 1 |
| RenderHostImpl1204.java / RenderHostImpl1211.java | 同上两个版本目录 | 规则 2（不同 FQN；12 号契约要求类名带版本后缀） |
| 入口 install 一行 | src/client/java/areahint/AreashintClient.java（:91 已有一行 ClientNetworkCompat.install，照抄该形态） | 规则 5 |

**注意 1.21.1 当前是骨架节点**（gradle.properties：deps.1.21.1.skeleton=true，build.gradle:112-119 用 setSrcDirs 整体替换）。因此：
- 现在写 1.21.1 实现 → versions/1.21.1/**skeleton**/client/java/areahint/render/host/；
- 等 1.21.1 转成真实节点（skeleton=false）→ 移到 versions/1.21.1/**src**/client/java/areahint/render/host/。
这条与 12 号契约 §5.2 的「节点类型决定手工实现放哪」完全一致。

**与 05 文档的命名差异（需 Lead 确认）**：05 Task 2 写的是两代各一份 RenderHostImpl.java（同名）；12 号契约要求实现类名带版本后缀。本文按「实现带后缀 + 工厂同名」处理，两边都不违反（同名只保留在工厂上）。

### 3.6 一个必须拍板的开放点：TitleRenderHelper 的分行缓存 key

现状（TitleRenderHelper:191-207）：分行缓存用 **WeakReference<TextRenderer>** 做 key，资源重载后自动失效。接口化后 RenderContext 不能再暴露 TextRenderer，三个选项：

| 选项 | 做法 | 代价 |
|---|---|---|
| A | RenderContext 提供 Object fontToken()（不透明身份），缓存改 WeakReference<Object> | 接口多一个"不透明句柄"，但语义仍是"字体换没换" |
| B | 缓存 key 去掉字体，改由 RenderHost 在资源重载时清空 | 需要新增一个"资源重载"通知能力（现在没有） |
| C | 分行缓存整体下沉到 RenderHost 实现里 | 把纯算法推进 versions/，违反规则 4 的精神 |

建议 A（改动最小、语义等价），但这是接口形状问题，请 Lead 定。

---

## 四、改动清单（Q4）

### 4.1 路线甲：只为解锁 1.21.1（不建 RenderHost）

| 文件 | 改法 | 预计改动 | 去向 |
|---|---|---|---|
| render/CPURender.java | import HudRenderCallback → 无；onHudRender(DrawContext,float) 里丢弃第二参数，改由版本相关注册 | ~4 行 | src/ + 条件注释或 versions/ |
| render/GLRender.java | 同上 | ~4 行 | 同上 |
| render/VulkanRender.java | 同上 | ~4 行 | 同上 |
| easyadd/EasyAddRecordingHud.java | 同上 | ~3 行 | 同上 |
| mixin/client/WorldRendererMixin.java | 整块进版本目录（见第五节） | 36 行搬迁 | versions/ |

注意：这条路线的 4 个文件仍然直接 import HudRenderCallback（05 验收标准「src/ 渲染业务不再 import HudRenderCallback」不满足），且 26.x 还要再返工一次。**只推荐作为「先解锁 1.21.1 构建」的临时手段。**

### 4.2 路线乙：按 05 建 RenderHost（推荐，一次到位）

| 文件 | 类型 | 改法 | 预计改动 |
|---|---|---|---|
| src/client/java/areahint/render/host/RenderContext.java | 新增 | 3.2 的接口 | ~30 行 |
| src/client/java/areahint/render/host/RenderHost.java | 新增 | 3.3 的接口 + install/get | ~35 行 |
| versions/1.20.4/src/client/java/areahint/render/host/RenderHostImpl1204.java | 新增 | DrawContext 包装 | ~70 行 |
| versions/1.20.4/…/RenderHostFactory.java | 新增 | 同名工厂 | ~12 行 |
| versions/1.21.1/skeleton/client/java/areahint/render/host/RenderHostImpl1211.java | 新增 | 同 1204，仅注册 lambda 与 frameDelta 不同 | ~70 行 |
| versions/1.21.1/skeleton/client/java/areahint/render/host/RenderHostFactory.java | 新增 | 同名工厂 | ~12 行 |
| src/client/java/areahint/AreashintClient.java | 修改 | +1 行 install（:91 旁边） | 1 行 |
| src/client/java/areahint/render/CPURender.java | 修改 | 删 3 条 import（HudRenderCallback/DrawContext/MatrixStack/TextRenderer 视情况）、注册 1 行、签名 1 行、:153-168 改走 ctx | ~12-15 行 |
| src/client/java/areahint/render/GLRender.java | 修改 | 同上 | ~12-15 行 |
| src/client/java/areahint/render/VulkanRender.java | 修改 | 同上 | ~12-15 行 |
| src/client/java/areahint/render/TitleRenderHelper.java | 修改 | drawCenteredLine 形参换 RenderContext；buildSubtitleLines*/getSubtitleStartY/getLineHeight 的 TextRenderer 换成 ctx；缓存 key 按 3.6 | ~15-20 行 |
| src/client/java/areahint/easyadd/EasyAddRecordingHud.java | 修改 | 注册 1 行、签名 1 行、5 处绘制改 ctx、trim 改 ctx.trimToWidth | ~10-14 行 |
| mixin/client/WorldRendererMixin.java | 迁移 | 见第五节 | 36 行 |

**合计：新增 6 个文件（src 2 + versions 4），修改 6 个文件，约 75-95 行改动。**

### 4.3 明确「不需要动」的（避免过度改造）

| 范围 | 文件数 | 理由（证据） |
|---|---:|---|
| 用 DrawContext 的 Screen/UI | 33（37 减去 render/ 的 4 个） | 2.2 / 2.7：方法签名与 Screen API 在 1.21.1 完全相同 |
| RenderSystem 两个文件 | 2 | 2.5：12 个在用方法全部存在 |
| boundviz/BoundVizRenderer.java | 1 | 用 MatrixStack + Tessellator/BufferBuilder，1.21.1 上这些类型与用法仍在；它的问题只在 mixin 注入（第五节） |
| xaero/* | 4（含本清单内的） | 归阶段 6 的第三方隔离 |

---

## 五、mixin 差异（Q5）

### 5.1 WorldRendererMixin：注入点仍有效，失效的是回调签名

现状（src/client/java/areahint/mixin/client/WorldRendererMixin.java:22-35）：

~~~java
@Inject(method = "render",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/render/WorldRenderer;renderChunkDebugInfo(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/Camera;)V",
                 shift = At.Shift.AFTER))
private void onRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline,
                      Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager,
                      Matrix4f projectionMatrix, CallbackInfo ci) { BoundVizRenderer.render(matrices, tickDelta); }
~~~

1.21.1 javap 实测（-c -p 字节码）：
- **renderChunkDebugInfo 的描述符在 1.21.1 完全没变**（(MatrixStack, VertexConsumerProvider, Camera)V），**并且仍然在 render 方法体内被调用**（1.21.1 字节码偏移 2617，1.20.4 偏移 2713）=> **@At 的 target 两代都能解析**。
- 失效的是回调参数列表：它必须照抄 WorldRenderer.render 的签名，而 1.21.1 的 render 变成 (RenderTickCounter, boolean, Camera, GameRenderer, LightmapTextureManager, Matrix4f, Matrix4f) —— 少了 MatrixStack、float+long 合并成 RenderTickCounter、多了一个 Matrix4f。
- 1.21.1 的 render 里 MatrixStack 是**局部变量**（字节码偏移 728：new MatrixStack + invokespecial <init>），不再是参数，不能从回调签名里拿。

三种改法：

| 方案 | 做法 | 优点 | 风险 |
|---|---|---|---|
| A（05 文档已选） | versions/1.20.4 与 versions/1.21.1 各一份 WorldRendererMixin（类名带版本后缀）+ 各一份 mixin 配置 | 语义最直白，两代互不影响 | 需要解决"配置怎么按版本进产物"（5.4） |
| B | 改为注入 renderChunkDebugInfo 的 TAIL（该方法描述符两代相同，MatrixStack 是它的参数） | 一份 mixin 可能通吃两代，代码更少 | TAIL 在"方法内有早退"时的触发语义**未实测**（本次只确认了方法存在与描述符，未统计早退分支）；行为是否等价需验证 |
| C | 仍注入 render，用 MixinExtras @Local 取局部 MatrixStack | 保持原注入点语义 | 需要引入 mixinextras 依赖（本仓库当前没有），且 @Local 依赖调试信息；**待验证** |

**建议**：按 05 的 A 方案执行（与既有决策一致），把 B 作为「1.21.6+ 再评估」的备选。无论哪种，BoundVizRenderer.render(matrices, tickDelta) 的 tickDelta 当前未被使用（2.4），1.21.1 侧可以直接传 0；若将来要用，取 tickCounter.getTickDelta(false)。

### 5.2 TranslationStorageMixin：稳定，留 src/

- 1.21.1 javap：TranslationStorage.get(String, String) 仍存在且是唯一 get 重载；类仍继承 net.minecraft.util.Language；hasTranslation(String) 仍在。
- 现状 mixin（method = "get"，HEAD，cancellable，(String key, String fallback, CallbackInfoReturnable<String>)）**不需要改**，符合 05 Task 3 Step 3「签名稳定的 mixin 保持 src/ 不动」。

### 5.3 模板残留（只提出，不删除）

ExampleMixin（src/main/java/areahint/mixin/ExampleMixin.java）与 ExampleClientMixin（areas-hint.client.mixins.json:6）无功能。按 05 Task 3 Step 4，**是否删除由 Lead 决定**，本调研不动。

### 5.4 mixin 配置的打包问题（05 方案里唯一没有实测证据的环节）

现状：src/client/resources/areas-hint.client.mixins.json 里 client 段是 [ExampleClientMixin, WorldRendererMixin, TranslationStorageMixin]，required=true、defaultRequire=1。
05 Task 3 的计划是「改 src 的配置 + 在新世代版本目录各放一份配置」。需要 Lead 先确认：
- 版本目录的 areas-hint.client.mixins.json 与 src/client/resources 里的**同名资源**在 processResources 阶段谁覆盖谁（Stonecutter 是追加编译，资源同名行为**未实测**）；
- fabric.mod.json 的 mixins 列表是否需要按版本改（若两份配置同名则不用改，但覆盖顺序决定成败）；
- 若某版本的配置里仍列着 WorldRendererMixin（而该类已被移走），mixin 会报"类不存在"——这与 05 验收标准「不会出现配置引用了不存在于该版本的类」直接冲突，必须先解决。

**建议**：把 WorldRendererMixin 从共享配置里摘掉，改由各版本目录提供自己的客户端 mixin 配置（同名文件或改名+改 fabric.mod.json，二选一），并在动第一行代码前先做一次"资源同名覆盖"的实测（1.20.4 与 1.21.1 各构建一次对比 jar 内配置内容）。

### 5.5 顺带发现的配置项

areas-hint.client.mixins.json:4 的 compatibilityLevel 是 JAVA_17；1.21.1 用 Java 21。Mixin 的 compatibilityLevel 是"最低要求"语义，JAVA_17 在 Java 21 运行时通常可用，但**未实测**，解锁 1.21.1 时留意是否有相关警告。

---

## 六、未验证清单（不得当作结论使用）

1. HudLayerRegistrationCallback / IdentifiedLayer 的具体出现版本与签名——本机缓存没有 1.21.6–1.21.9 的 fabric-rendering-v1，**未实测**。
2. 26.1.2 的 HudElementRegistry 具体注册方法签名——本次只确认了类存在（jar tf），**未 javap 方法**。
3. renderChunkDebugInfo 是否存在早退分支（影响 5.1 方案 B 的 TAIL 语义）——本次只统计了方法存在与描述符，**未统计分支**。
4. 版本目录资源同名覆盖顺序（5.4）——**未实测**。
5. 1.21.1 真实编译结果——**未跑 gradle**（任务硬性要求），本文所有"可编译"判断都是基于 javap 的成员存在性与签名比对。

---

## 附录 A：javap 命令与 jar 绝对路径

jar 路径（Windows 原文）：

~~~
MC 1.20.4 named:
  C:\Users\郁小悟520\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-clientonly\1.20.4-net.fabricmc.yarn.1_20_4.1.20.4+build.3-v2\minecraft-clientonly-1.20.4-net.fabricmc.yarn.1_20_4.1.20.4+build.3-v2.jar
  C:\Users\郁小悟520\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-common\1.20.4-net.fabricmc.yarn.1_20_4.1.20.4+build.3-v2\minecraft-common-1.20.4-net.fabricmc.yarn.1_20_4.1.20.4+build.3-v2.jar
MC 1.21.1 named:
  C:\Users\郁小悟520\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-clientonly\1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2\minecraft-clientonly-1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2.jar
  C:\Users\郁小悟520\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-common\1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2\minecraft-common-1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2.jar
Fabric API 1.21.1（rendering 模块，Yarn 重映射，Loom 生成）:
  J:\mc\mods\versions\1.21.1\build\loom-cache\remapped_working\net_fabricmc_yarn_1_21_1_1_21_1_build_3_v2.net.fabricmc.fabric-api-fabric-rendering-v1-5.2.1+f44c2eea19.jar
Fabric API 1.20.4（rendering 模块，intermediary）:
  C:\Users\郁小悟520\.gradle\caches\modules-2\files-2.1\net.fabricmc.fabric-api\fabric-rendering-v1\3.0.12+78d798af4f\dd38df571635a4dd825189834444c2e7c063f41b\fabric-rendering-v1-3.0.12+78d798af4f.jar
Fabric API 1.21.1（rendering 模块，intermediary）:
  C:\Users\郁小悟520\.gradle\caches\modules-2\files-2.1\net.fabricmc.fabric-api\fabric-rendering-v1\5.1.0+ab4c25a019\456ceeaedacf9396bda2ff84ce2f616ef090b7ca\fabric-rendering-v1-5.1.0+ab4c25a019.jar
Fabric API 26.1.2（rendering 模块，intermediary）:
  C:\Users\郁小悟520\.gradle\caches\modules-2\files-2.1\net.fabricmc.fabric-api\fabric-rendering-v1\23.3.1+e9207d814c\8eafada331931df678e665e1141642a986518f8c\fabric-rendering-v1-23.3.1+e9207d814c.jar
~~~

命令（Windows PowerShell 原文，classpath 用分号连接 clientonly 与 common）：

~~~powershell
$cp = '<clientonly jar>;<common jar>'
javap -classpath $cp net.minecraft.client.gui.DrawContext
javap -classpath $cp net.minecraft.client.util.math.MatrixStack
javap -classpath $cp 'net.minecraft.client.util.math.MatrixStack$Entry'
javap -classpath $cp net.minecraft.client.render.RenderTickCounter
javap -classpath $cp net.minecraft.client.font.TextRenderer
javap -classpath $cp net.minecraft.client.resource.language.TranslationStorage
javap -classpath $cp net.minecraft.client.MinecraftClient
javap -classpath $cp net.minecraft.client.gui.screen.Screen
javap -classpath $cp com.mojang.blaze3d.systems.RenderSystem
javap -p -classpath $cp net.minecraft.client.render.WorldRenderer
javap -c -p -classpath $cp net.minecraft.client.render.WorldRenderer   # 用于定位 renderChunkDebugInfo 调用点与局部 MatrixStack
javap -classpath '<fabric-rendering-v1 jar>' net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
jar tf '<fabric-rendering-v1 jar>' | Select-String 'rendering/v1'
~~~

## 附录 B：扫描口径与命令

- 扫描对象：src/ 下全部 267 个 .java（递归），只读。
- 计数口径与 13 号清单一致（行 / 次 / 文件），因此 HudRenderCallback 的 8/8/4 与 13 号完全对齐，可交叉验证。
- 判定注释命中的方式：逐行判断是否以 // 或 * 开头；本清单中 MatrixStack/WorldRenderer/TranslationStorage 的 import 行已单独标注。
- **未运行任何 gradle 任务**（任务硬性要求，也避免与 Lead 的构建抢文件锁）。
