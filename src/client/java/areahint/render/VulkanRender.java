package areahint.render;

import areahint.AreashintClient;
import areahint.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 * Vulkan 渲染实现类。
 * 结构与 GLRender 保持一致，由 VulkanModCompat 负责可用性判定。
 */
public class VulkanRender implements RenderManager.IRender {
    // Minecraft客户端实例
    private final MinecraftClient client;

    // 当前动画状态
    private AnimationState animationState = AnimationState.NONE;

    // 当前显示的文本
    private String currentText = null;

    // 当前显示的副字幕文本
    private String currentSubtitle = null;

    // 当前颜色
    private String currentColor = "#FFFFFF";

    // 当前副字幕颜色
    private String currentSubtitleColor = "#FFFFFF";

    // 动画开始时间
    private long animationStartTime = 0;

    // 动画持续时间（毫秒）
    private static final long ANIMATION_IN_DURATION = 500; // 进入动画持续时间
    private static final long ANIMATION_STAY_DURATION = 3000; // 显示持续时间
    private static final long ANIMATION_OUT_DURATION = 300; // 退出动画持续时间

    // 上一帧的Y偏移和透明度，用于平滑插值
    private float lastYOffset = 0;
    private float lastAlpha = 0;

    // 插值系数（0-1之间，越小越平滑，但延迟越大）
    private static final float INTERPOLATION_FACTOR = 0.15f;

    /**
     * 构造方法
     * @param client Minecraft客户端实例
     */
    public VulkanRender(MinecraftClient client) {
        this.client = client;

        // 注册Tick事件用于更新动画状态
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        // 注册HUD渲染事件
        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    /**
     * HUD渲染事件处理
     * @param drawContext 绘制上下文
     * @param tickDelta tick间隔时间
     */
    private void onHudRender(DrawContext drawContext, float tickDelta) {
        if (animationState == AnimationState.NONE || currentText == null || client.player == null) {
            return;
        }

        if (!VulkanModCompat.isUsable()) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // 计算文本在屏幕上的位置
        int x = screenWidth / 2;
        int y = screenHeight / 4; // 屏幕1/4位置

        // 根据动画状态计算Y偏移和透明度
        float alpha = 1.0f;
        float yOffset = 0.0f;

        long elapsedTime = System.currentTimeMillis() - animationStartTime;
        float progress = 0;

        switch (animationState) {
            case IN:
                // 确保渐入动画的进度计算更加平滑
                progress = Math.min(1.0f, (float) elapsedTime / ANIMATION_IN_DURATION);
                progress = easeOutCubic(progress); // 使用缓动函数
                alpha = progress;
                // 渐入动画时不使用Y偏移，保持固定位置
                yOffset = 0.0f;
                break;
            case STAY:
                // 保持原位，完全不透明
                alpha = 1.0f;
                yOffset = 0.0f;
                break;
            case OUT:
                progress = Math.min(1.0f, (float) elapsedTime / ANIMATION_OUT_DURATION);
                progress = easeInCubic(progress); // 使用缓动函数
                yOffset = -15.0f * progress; // 使用浮点数计算，避免整数截断导致的闪烁
                alpha = 1.0f - progress;
                break;
            case NONE:
                return; // 不渲染
        }

        // 应用平滑插值，减少闪烁
        // 对于渐入动画，直接使用计算值，不进行插值处理
        if (animationState == AnimationState.IN && elapsedTime < 100) {
            // 渐入动画开始阶段，直接使用计算值
            lastYOffset = yOffset;
            lastAlpha = alpha;
        } else {
            // 其他情况使用插值
            yOffset = lastYOffset * (1.0f - INTERPOLATION_FACTOR) + yOffset * INTERPOLATION_FACTOR;
            alpha = lastAlpha * (1.0f - INTERPOLATION_FACTOR) + alpha * INTERPOLATION_FACTOR;

            // 保存当前值用于下一帧插值
            lastYOffset = yOffset;
            lastAlpha = alpha;
        }

        // 渲染主标题和副字幕，逻辑与 OpenGL 保持一致。
        TextRenderer textRenderer = client.textRenderer;
        MatrixStack matrixStack = drawContext.getMatrices();
        matrixStack.push();
        matrixStack.translate(x, y + yOffset, 0);

        float titleScale = TitleRenderHelper.getTitleScale();
        TitleRenderHelper.drawCenteredLine(drawContext, textRenderer, matrixStack, currentText, currentColor, alpha, titleScale, 0, false);

        if (TitleRenderHelper.hasNormalizedSubtitle(currentSubtitle)) {
            float subtitleScale = TitleRenderHelper.getSubtitleScale();
            int subtitleY = TitleRenderHelper.getSubtitleStartY(textRenderer, titleScale);
            int lineHeight = TitleRenderHelper.getLineHeight(textRenderer, subtitleScale);
            java.util.List<String> subtitleLines = TitleRenderHelper.buildSubtitleLinesNormalized(currentSubtitle, textRenderer, screenWidth, subtitleScale);

            for (int i = 0; i < subtitleLines.size(); i++) {
                TitleRenderHelper.drawCenteredLine(drawContext, textRenderer, matrixStack,
                    subtitleLines.get(i), currentSubtitleColor, alpha, subtitleScale, subtitleY + i * lineHeight, false);
            }
        }

        // 恢复矩阵状态
        matrixStack.pop();

        // 输出调试信息
        if (animationState == AnimationState.IN) {
            AreashintClient.LOGGER.debug("VulkanRender: 渐入动画 - 区域标题: {}, 进度: {}, 透明度: {}",
                currentText, progress, alpha);
        }
    }

    /**
     * 客户端Tick事件处理，用于更新动画状态
     * @param minecraftClient Minecraft客户端实例
     */
    private void onClientTick(MinecraftClient minecraftClient) {
        if (animationState == AnimationState.NONE || currentText == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - animationStartTime;

        // 更新动画状态
        if (animationState == AnimationState.IN && elapsedTime >= ANIMATION_IN_DURATION) {
            animationState = AnimationState.STAY;
            animationStartTime = currentTime;
            AreashintClient.LOGGER.debug("VulkanRender: 动画状态更新 IN → STAY");
        } else if (animationState == AnimationState.STAY && elapsedTime >= ANIMATION_STAY_DURATION) {
            animationState = AnimationState.OUT;
            animationStartTime = currentTime;
            // 重置插值变量以确保平滑过渡
            lastYOffset = 0.0f;
            lastAlpha = 1.0f;
            AreashintClient.LOGGER.debug("VulkanRender: 动画状态更新 STAY → OUT");
        } else if (animationState == AnimationState.OUT && elapsedTime >= ANIMATION_OUT_DURATION) {
            animationState = AnimationState.NONE;
            currentText = null;
            currentSubtitle = null;
            AreashintClient.LOGGER.debug("VulkanRender: 动画状态更新 OUT → NONE");
        }
    }

    /**
     * 根据配置获取文本缩放比例
     * @return 文本缩放比例
     */
    private float getTextScale() {
        String size = ClientConfig.getTitleSize();
        switch (size) {
            case "extra_large":
                return 3.0f;
            case "large":
                return 2.5f;
            case "medium_large":
                return 2.0f;
            case "medium":
                return 1.5f;
            case "medium_small":
                return 1.2f;
            case "small":
                return 1.0f;
            case "extra_small":
                return 0.8f;
            default:
                return 1.5f; // 默认中等大小
        }
    }

    /**
     * 将RGB颜色和透明度转换为ARGB颜色值
     * @param rgb RGB颜色值
     * @param alpha 透明度（0.0-1.0）
     * @return ARGB颜色值
     */
    private int getAlphaColor(int rgb, float alpha) {
        int a = Math.min(255, Math.max(0, (int) (alpha * 255))) << 24;
        return a | (rgb & 0x00FFFFFF);
    }

    @Override
    public void renderTitle(String title, String color, String subtitle, String subtitleColor) {
        if (title == null || title.isEmpty()) {
            return;
        }

        if (animationState != AnimationState.NONE) {
            if (title.equals(currentText) && sameSubtitle(subtitle, currentSubtitle)) {
                return;
            }
        }

        currentText = title;
        currentSubtitle = TitleRenderHelper.normalizeSubtitleText(subtitle);
        currentColor = color != null ? color : "#FFFFFF";
        currentSubtitleColor = subtitleColor != null ? subtitleColor : "#FFFFFF";
        animationState = AnimationState.IN;
        animationStartTime = System.currentTimeMillis();
        lastYOffset = 0.0f;
        lastAlpha = 0.0f;

        AreashintClient.LOGGER.info("VulkanRender: 开始显示区域标题: {}, 动画状态: {}", title, animationState);
    }

    @Override
    public void clearTitle() {
        // 关闭模组时立即停止当前标题动画，确保Vulkan渲染路径也不残留域名。
        currentText = null;
        currentSubtitle = null;
        currentColor = "#FFFFFF";
        currentSubtitleColor = "#FFFFFF";
        animationState = AnimationState.NONE;
        animationStartTime = 0;
        lastYOffset = 0.0f;
        lastAlpha = 0.0f;
    }

    private boolean sameSubtitle(String newSubtitle, String oldSubtitle) {
        String normalizedNew = TitleRenderHelper.normalizeSubtitleText(newSubtitle);
        String normalizedOld = TitleRenderHelper.normalizeSubtitleText(oldSubtitle);
        return normalizedNew == null ? normalizedOld == null : normalizedNew.equals(normalizedOld);
    }

    private static int parseHexColor(String hex) {
        try {
            if (hex != null && hex.startsWith("#") && hex.length() == 7) {
                return Integer.parseInt(hex.substring(1), 16);
            }
        } catch (Exception ignored) {}
        return 0xFFFFFF;
    }

    /**
     * 动画状态枚举
     */
    private enum AnimationState {
        NONE, // 无动画
        IN,   // 进入动画
        STAY, // 停留
        OUT   // 退出动画
    }

    /**
     * 缓入三次方缓动函数
     * @param x 进度 (0.0 - 1.0)
     * @return 缓动值
     */
    private float easeInCubic(float x) {
        return x * x * x;
    }

    /**
     * 缓出三次方缓动函数
     * @param x 进度 (0.0 - 1.0)
     * @return 缓动值
     */
    private float easeOutCubic(float x) {
        return 1 - (float) Math.pow(1 - x, 3);
    }
}
