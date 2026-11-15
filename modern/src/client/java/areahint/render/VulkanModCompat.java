package areahint.render;

import areahint.AreashintClient;
import net.fabricmc.loader.api.FabricLoader;

/**
 * VulkanMod 客户端兼容层。
 * 不直接依赖 VulkanMod API 类型，避免未安装时触发类加载错误。
 */
public final class VulkanModCompat {
    private static final String VULKANMOD_MOD_ID = "vulkanmod";
    private static final String INITIALIZER_CLASS = "net.vulkanmod.Initializer";
    private static final String RENDERER_CLASS = "net.vulkanmod.render.chunk.build.frapi.VulkanModRenderer";

    private static volatile boolean initialized;
    private static volatile boolean loaded;
    private static volatile boolean usable;
    private static volatile Class<?> initializerClass;
    private static volatile Class<?> rendererClass;

    private VulkanModCompat() {
    }

    public static boolean isLoaded() {
        ensureInitialized();
        return loaded;
    }

    public static boolean isUsable() {
        ensureInitialized();
        return loaded && usable && initializerClass != null && rendererClass != null;
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }

        initialized = true;
        loaded = FabricLoader.getInstance().isModLoaded(VULKANMOD_MOD_ID);

        if (!loaded) {
            AreashintClient.LOGGER.info("VulkanMod 未安装，跳过 Vulkan 渲染兼容层初始化。");
            return;
        }

        try {
            initializerClass = Class.forName(INITIALIZER_CLASS);
            rendererClass = Class.forName(RENDERER_CLASS);
            usable = true;
            AreashintClient.LOGGER.info("已检测到 VulkanMod，Vulkan 渲染兼容层已启用。");
        } catch (Throwable t) {
            usable = false;
            initializerClass = null;
            rendererClass = null;
            AreashintClient.LOGGER.warn("VulkanMod 已安装但兼容层初始化失败，将回退到 OpenGL 路径。", t);
        }
    }
}
