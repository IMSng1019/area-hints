package areahint.boundviz;

import areahint.AreashintClient;
import areahint.config.ClientConfig;
import areahint.data.AreaData;
import areahint.file.FileManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 边界可视化管理器
 * 负责管理域名边界的可视化显示
 */
public class BoundVizManager {
    private static BoundVizManager instance;
    private boolean enabled = false;
    private List<AreaData> currentDimensionAreas = new ArrayList<>();
    private String currentDimension = null;
    private int version = 0;

    // 临时顶点记录（用于easyadd、expandarea、shrinkarea）
    private List<BlockPos> temporaryVertices = new ArrayList<>();
    private boolean showTemporaryVertices = false;

    private BoundVizManager() {
        // 从配置加载初始状态
        this.enabled = ClientConfig.isBoundVizEnabled();
    }

    public static BoundVizManager getInstance() {
        if (instance == null) {
            instance = new BoundVizManager();
        }
        return instance;
    }

    /**
     * 切换边界可视化状态
     */
    public void toggle() {
        enabled = !enabled;
        ClientConfig.setBoundVizEnabled(enabled);

        if (enabled) {
            loadCurrentDimensionAreas();
            AreashintClient.LOGGER.info("边界可视化已开启");
        } else {
            currentDimensionAreas.clear();
            AreashintClient.LOGGER.info("边界可视化已关闭");
        }
    }

    /**
     * 设置边界可视化状态
     */
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            toggle();
        }
    }

    /**
     * 从已保存的客户端配置同步运行时状态，不重复写配置文件。
     */
    public void applyConfigEnabled(boolean enabled) {
        this.enabled = enabled;
        reload();
    }

    /**
     * 获取边界可视化状态
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 加载当前维度的所有域名数据
     */
    public void loadCurrentDimensionAreas() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) {
                return;
            }

            Identifier dimensionId = client.world.getRegistryKey().getValue();
            String dimension = dimensionId.toString();
            if (dimension.equals(currentDimension)) {
                return; // 已加载
            }

            currentDimension = dimension;
            currentDimensionAreas.clear();

            String fileName = getFileNameForDimension(dimensionId);
            if (fileName == null) {
                AreashintClient.LOGGER.warn("无法确定维度文件名: {}", dimension);
                version++;
                return;
            }

            Path areaFile = areahint.world.ClientWorldFolderManager.getWorldDimensionFile(fileName);
            currentDimensionAreas = FileManager.readAreaData(areaFile);
            version++;

            AreashintClient.LOGGER.info("已加载 {} 个域名边界用于可视化", currentDimensionAreas.size());

        } catch (Exception e) {
            AreashintClient.LOGGER.error("加载域名边界数据失败", e);
            currentDimensionAreas.clear();
            version++;
        }
    }

    /**
     * 获取当前维度的所有域名数据
     */
    public List<AreaData> getCurrentDimensionAreas() {
        if (enabled) {
            loadCurrentDimensionAreas();
        }
        return new ArrayList<>(currentDimensionAreas);
    }

    /**
     * 直接获取域名列表（无拷贝，仅供渲染器使用）
     */
    public List<AreaData> getCurrentDimensionAreasDirect() {
        if (enabled) {
            loadCurrentDimensionAreas();
        }
        return currentDimensionAreas;
    }

    /**
     * 获取缓存版本号
     */
    public int getVersion() {
        return version;
    }

    /**
     * 设置临时顶点（用于easyadd、expandarea、shrinkarea）
     */
    public void setTemporaryVertices(List<BlockPos> vertices, boolean show) {
        this.temporaryVertices = new ArrayList<>(vertices);
        this.showTemporaryVertices = show;
        version++;
    }

    /**
     * 清除临时顶点
     */
    public void clearTemporaryVertices() {
        this.temporaryVertices.clear();
        this.showTemporaryVertices = false;
        version++;
    }

    /**
     * 获取临时顶点
     */
    public List<BlockPos> getTemporaryVertices() {
        return new ArrayList<>(temporaryVertices);
    }

    /**
     * 直接获取临时顶点列表（无拷贝，仅供渲染器使用）
     */
    public List<BlockPos> getTemporaryVerticesDirect() {
        return temporaryVertices;
    }

    /**
     * 是否显示临时顶点
     */
    public boolean shouldShowTemporaryVertices() {
        return showTemporaryVertices && !temporaryVertices.isEmpty();
    }

    /**
     * 重新加载当前维度的域名数据
     */
    public void reload() {
        currentDimensionAreas.clear();
        currentDimension = null;
        version++;
        if (enabled) {
            loadCurrentDimensionAreas();
        }
    }

    /**
     * 根据维度ID获取文件名
     */
    private String getFileNameForDimension(Identifier dimension) {
        return AreashintClient.getDimensionFileName(dimension);
    }
}
