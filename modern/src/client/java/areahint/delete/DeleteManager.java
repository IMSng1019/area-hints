package areahint.delete;

import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.debug.ClientDebugManager;
import areahint.i18n.I18nManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Delete功能管理器
 * 负责交互式域名删除的整个流程管理
 */
public class DeleteManager {

    /**
     * Delete状态枚举
     */
    public enum DeleteState {
        IDLE,           // 空闲状态
        SELECT_AREA,    // 选择要删除的域名
        CONFIRM_DELETE  // 确认删除
    }

    // 单例实例
    private static DeleteManager instance;

    // 当前状态
    private DeleteState currentState = DeleteState.IDLE;

    // 选中的域名
    private String selectedAreaName = null;
    private AreaData selectedArea = null;

    // 当前维度
    private String currentDimension = null;

    // 可删除的域名列表
    private List<AreaData> deletableAreas = new ArrayList<>();

    // 私有构造函数（单例模式）
    private DeleteManager() {}

    /**
     * 获取单例实例
     */
    public static DeleteManager getInstance() {
        if (instance == null) {
            instance = new DeleteManager();
        }
        return instance;
    }

    /**
     * 启动Delete流程
     */
    public void startDelete() {
        if (currentState != DeleteState.IDLE) {
            Minecraft.getInstance().player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.error.general_2")));
            DeleteVisualController.clear();
            return;
        }

        boolean visualRequested = DeleteVisualController.consumeVisualStartRequest();

        // 获取当前维度信息
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.player != null) {
            // 获取维度标识符并转换为维度类型
            String dimensionPath = client.level.dimension().identifier().getPath();
            currentDimension = areahint.network.Packets.convertDimensionPathToType(dimensionPath);

            if (currentDimension == null) {
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.error.dimension")));
                ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.AREA_DETECTION,
                    "无法转换维度路径: " + dimensionPath);
                if (visualRequested) {
                    DeleteVisualController.clear();
                }
                return;
            }

            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.AREA_DETECTION,
                "当前维度: " + currentDimension);

            // 设置状态
            currentState = DeleteState.SELECT_AREA;

            // 向服务端请求可删除的域名列表（服务端会根据权限判断）
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.message.area.delete.list_2")));
            DeleteNetworking.requestDeletableAreas(currentDimension);
        } else if (visualRequested) {
            DeleteVisualController.clear();
        }
    }

    /**
     * 接收服务端发送的可删除域名列表
     * @param areas 可删除的域名列表
     */
    public void receiveDeletableAreas(List<AreaData> areas) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        deletableAreas.clear();
        deletableAreas.addAll(areas);

        if (deletableAreas.isEmpty()) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.message.area.dimension.delete")));
            if (DeleteVisualController.isVisualFlowActive()) {
                DeleteVisualController.showInfo("message.message.area.dimension.delete");
            }
            resetState();
            return;
        }

        // 图形入口使用 Screen 选择，聊天入口继续使用原来的可点击聊天界面。
        if (DeleteVisualController.isVisualFlowActive()) {
            DeleteVisualController.showAreaSelection(deletableAreas);
        } else {
            DeleteUI.showAreaSelectionScreen(deletableAreas);
        }
    }


    /**
     * 处理域名选择
     */
    public void handleAreaSelection(String areaName) {
        if (currentState != DeleteState.SELECT_AREA) {
            return;
        }

        // 移除引号（如果存在）
        if (areaName.startsWith("\"") && areaName.endsWith("\"") && areaName.length() > 1) {
            areaName = areaName.substring(1, areaName.length() - 1);
        }

        // 查找选中的域名
        selectedArea = null;
        for (AreaData area : deletableAreas) {
            if (area.getName().equals(areaName)) {
                selectedArea = area;
                selectedAreaName = areaName;
                break;
            }
        }

        if (selectedArea == null) {
            Minecraft.getInstance().player.sendSystemMessage(
                Component.nullToEmpty(I18nManager.translate("message.error.area_3") + areaName));
            return;
        }

        // 进入确认删除状态
        currentState = DeleteState.CONFIRM_DELETE;
        if (DeleteVisualController.isVisualFlowActive()) {
            DeleteVisualController.showConfirmScreen(selectedArea);
        } else {
            DeleteUI.showConfirmDeleteScreen(selectedArea);
        }
    }

    /**
     * 确认删除域名
     */
    public void confirmDelete() {
        if (currentState != DeleteState.CONFIRM_DELETE) {
            return;
        }

        if (selectedArea == null || selectedAreaName == null) {
            Minecraft.getInstance().player.sendSystemMessage(
                Component.nullToEmpty(I18nManager.translate("message.error.area.delete_2")));
            cancelDelete();
            return;
        }

        try {
            // 发送删除请求到服务端
            DeleteNetworking.sendDeleteRequestToServer(selectedAreaName, currentDimension);

            Minecraft.getInstance().player.sendSystemMessage(
                Component.nullToEmpty(I18nManager.translate("message.prompt.delete")));

            resetState();

        } catch (Exception e) {
            Minecraft.getInstance().player.sendSystemMessage(
                Component.nullToEmpty(I18nManager.translate("message.error.area.delete") + e.getMessage()));
            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.AREA_DETECTION,
                "删除失败: " + e.getMessage());
        }
    }

    /**
     * 取消Delete流程
     */
    public void cancelDelete() {
        Minecraft.getInstance().player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.message.cancel")));
        resetState();
    }

    /**
     * 重置状态
     */
    private void resetState() {
        currentState = DeleteState.IDLE;
        selectedAreaName = null;
        selectedArea = null;
        currentDimension = null;
        deletableAreas.clear();
        DeleteVisualController.clear();
    }

    /**
     * 获取当前维度的文件名
     */
    private String getFileNameForCurrentDimension() {
        if (currentDimension == null) {
            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.AREA_DETECTION,
                "currentDimension 为 null");
            return null;
        }

        String fileName = areahint.network.Packets.getFileNameForDimension(currentDimension);

        if (fileName == null) {
            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.AREA_DETECTION,
                "无法为维度 " + currentDimension + " 获取文件名");
        } else {
            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.AREA_DETECTION,
                "维度 " + currentDimension + " 对应文件: " + fileName);
        }

        return fileName;
    }

    // Getters
    public DeleteState getCurrentState() { return currentState; }
    public String getSelectedAreaName() { return selectedAreaName; }
    public AreaData getSelectedArea() { return selectedArea; }
}
