package areahint.render;

import areahint.AreashintClient;
import areahint.config.ClientConfig;
import net.minecraft.client.Minecraft;

/**
 * 渲染管理器类
 * 负责管理不同渲染模式和显示区域名称
 */
public class RenderManager {
    // 不同的渲染实现
    private final CPURender cpuRender;
    private final GLRender glRender;
    private final VulkanRender vulkanRender;
    
    // 当前渲染器
    private IRender currentRender;
    
    // Minecraft客户端实例
    private final Minecraft client;
    
    /**
     * 构造方法，初始化各种渲染实现
     */
    public RenderManager() {
        this.client = Minecraft.getInstance();
        this.cpuRender = new CPURender(client);
        this.glRender = new GLRender(client);
        this.vulkanRender = new VulkanRender(client);
        
        // 根据配置设置当前渲染器
        updateRenderMode();
    }
    
    /**
     * 更新当前的渲染模式
     */
    public void updateRenderMode() {
        String mode = ClientConfig.getHintRender();
        switch (mode) {
            case "CPU":
                currentRender = cpuRender;
                AreashintClient.LOGGER.info("使用CPU渲染模式");
                break;
            case "OpenGL":
                currentRender = glRender;
                AreashintClient.LOGGER.info("使用OpenGL渲染模式");
                break;
            case "Vulkan":
                if (VulkanModCompat.isUsable()) {
                    currentRender = vulkanRender;
                    AreashintClient.LOGGER.info("使用Vulkan渲染模式");
                } else {
                    currentRender = glRender;
                    AreashintClient.LOGGER.warn("Vulkan 渲染模式当前不可用，已回退到 OpenGL。配置值: {}", mode);
                }
                break;
            default:
                currentRender = glRender;
                AreashintClient.LOGGER.info("未知渲染模式，使用默认OpenGL");
                break;
        }
    }
    
    /**
     * 显示区域名称
     * @param areaName 区域名称
     */
    public void showAreaTitle(String areaName) {
        showAreaTitle(areaName, "#FFFFFF");
    }

    /**
     * 显示区域名称（带颜色）
     * @param areaName 区域名称
     * @param color 颜色值（十六进制或闪烁模式）
     */
    public void showAreaTitle(String areaName, String color) {
        showAreaTitle(areaName, color, null, null);
    }

    /**
     * 显示区域名称和副字幕
     * @param areaName 区域名称
     * @param color 主标题颜色值（十六进制或闪烁模式）
     * @param subtitle 副字幕文本，为空时不显示
     * @param subtitleColor 副字幕颜色值（十六进制或闪烁模式）
     */
    public void showAreaTitle(String areaName, String color, String subtitle, String subtitleColor) {
        if (areaName == null || areaName.isEmpty()) {
            return;
        }
        currentRender.renderTitle(areaName, color != null ? color : "#FFFFFF", subtitle, subtitleColor);
    }

    /**
     * 立即清空所有渲染器中的当前标题。
     * 三种渲染器都会注册HUD回调，因此关闭模组时需要全部清理，避免旧渲染模式残留文字。
     */
    public void clearAreaTitle() {
        cpuRender.clearTitle();
        glRender.clearTitle();
        vulkanRender.clearTitle();
    }

    /**
     * 渲染接口，定义渲染方法
     */
    public interface IRender {
        /**
         * 渲染标题（带颜色）
         * @param title 标题文本
         * @param color 颜色值
         */
        void renderTitle(String title, String color, String subtitle, String subtitleColor);

        /**
         * 清空当前标题并停止动画。
         */
        void clearTitle();
    }
} 
