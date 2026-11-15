package areahint.rename;

import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.debug.ClientDebugManager;
import areahint.i18n.I18nManager;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Rename功能管理器
 * 负责交互式域名重命名的整个流程管理
 */
public class RenameManager {

    /**
     * Rename状态枚举
     */
    public enum RenameState {
        IDLE,                   // 空闲状态
        SELECT_AREA,            // 选择要重命名的域名
        INPUT_NEW_NAME,         // 输入新域名名称
        INPUT_NEW_SURFACE_NAME, // 输入新联合域名
        CONFIRM_RENAME          // 确认重命名
    }

    // 单例实例
    private static RenameManager instance;

    // 当前状态
    private RenameState currentState = RenameState.IDLE;

    // 域名数据收集
    private String selectedAreaName = null;      // 选中的域名名称
    private String newAreaName = null;           // 新域名名称
    private String newSurfaceName = null;        // 新联合域名
    private String currentDimension = null;      // 当前维度
    private List<AreaData> availableAreas = new ArrayList<>();  // 可重命名的域名列表
    private boolean visualFlowActive = false;    // 是否由图形指令入口启动

    // 聊天监听器注册状态
    private boolean chatListenerRegistered = false;

    // 私有构造函数（单例模式）
    private RenameManager() {}

    /**
     * 获取单例实例
     */
    public static RenameManager getInstance() {
        if (instance == null) {
            instance = new RenameManager();
        }
        return instance;
    }

    /**
     * 启动Rename流程
     * @param areas 可重命名的域名列表
     * @param dimension 当前维度
     */
    public void startRename(List<AreaData> areas, String dimension) {
        boolean visualRequested = RenameVisualController.consumeVisualStartRequest();
        if (currentState != RenameState.IDLE) {
            Minecraft.getInstance().player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.error.general_4")));
            if (visualRequested) {
                RenameVisualController.showInfo("message.error.general_4");
                RenameVisualController.clear();
            }
            return;
        }

        this.availableAreas = new ArrayList<>(areas);
        this.currentDimension = dimension;
        this.visualFlowActive = visualRequested;

        if (areas.isEmpty()) {
            Minecraft.getInstance().player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.error.area.dimension.rename_2")));
            if (visualFlowActive) {
                RenameVisualController.showInfo("message.error.area.dimension.rename_2");
                currentDimension = null;
                availableAreas.clear();
                visualFlowActive = false;
            }
            return;
        }

        if (!visualFlowActive) {
            // 聊天流程才需要监听玩家后续输入，图形流程直接由 Screen 提交。
            registerChatListener();
        }

        // 设置状态并显示UI
        currentState = RenameState.SELECT_AREA;
        showAreaSelection();
    }

    /**
     * 注册聊天监听器来捕获用户输入
     */
    private void registerChatListener() {
        if (!chatListenerRegistered) {
            ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
                if (currentState != RenameState.IDLE) {
                    handleChatInput(message.getString());
                }
            });
            chatListenerRegistered = true;
        }
    }

    /**
     * 处理用户聊天输入
     */
    private void handleChatInput(String input) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // 移除前缀符号（如果有的话）
        if (input.startsWith("<") && input.contains(">")) {
            int endIndex = input.indexOf(">") + 1;
            if (endIndex < input.length()) {
                input = input.substring(endIndex).trim();
            }
        }

        switch (currentState) {
            case INPUT_NEW_NAME:
                handleNewNameInput(input);
                break;

            case INPUT_NEW_SURFACE_NAME:
                handleSurfaceNameInput(input);
                break;

            default:
                break;
        }
    }

    /**
     * 处理新域名名称输入，聊天和图形流程共用同一套校验。
     */
    public void handleNewNameInput(String input) {
        if (currentState != RenameState.INPUT_NEW_NAME) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        String candidateName = input == null ? "" : input.trim();
        if (candidateName.isEmpty()) {
            showNewNameInput("commandui.common.error.empty");
            return;
        }

        // 检查新域名名称是否已存在
        if (checkAreaNameExists(candidateName)) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.error.area") + candidateName + I18nManager.translate("easyadd.message.dimension")));
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.prompt.area.name")));
            showNewNameInput(I18nManager.translate("addhint.error.area") + candidateName + I18nManager.translate("easyadd.message.dimension"));
            return;
        }

        // 检查新名称是否与原名称相同
        if (candidateName.equals(selectedAreaName)) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.error.area.name")));
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.prompt.area.name")));
            showNewNameInput("message.error.area.name");
            return;
        }

        newAreaName = candidateName;
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.message.area.name") + newAreaName));

        // 进入联合域名输入
        currentState = RenameState.INPUT_NEW_SURFACE_NAME;
        showSurfaceNameInput();
    }

    /**
     * 处理新联合域名输入，空内容表示移除表面域名。
     */
    public void handleSurfaceNameInput(String input) {
        if (currentState != RenameState.INPUT_NEW_SURFACE_NAME) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // 联合域名可以为空
        newSurfaceName = input == null || input.trim().isEmpty() ? null : input.trim();
        if (newSurfaceName != null) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.message.area.surface") + newSurfaceName));
        } else {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("dividearea.message.area.surface")));
        }

        // 进入确认状态
        currentState = RenameState.CONFIRM_RENAME;
        showConfirmScreen();
    }

    /**
     * 处理域名选择（从命令调用）
     */
    public void handleAreaSelection(String areaName) {
        if (currentState != RenameState.SELECT_AREA) {
            return;
        }

        // 移除引号（如果存在）
        if (areaName.startsWith("\"") && areaName.endsWith("\"") && areaName.length() > 1) {
            areaName = areaName.substring(1, areaName.length() - 1);
        }

        // 验证域名是否在可选列表中
        boolean found = false;
        for (AreaData area : availableAreas) {
            if (area.getName().equals(areaName)) {
                found = true;
                break;
            }
        }

        if (!found) {
            Minecraft.getInstance().player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.error.area_2") + areaName));
            return;
        }

        selectedAreaName = areaName;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.prompt.area") + areaName));
        }

        // 进入新名称输入状态
        currentState = RenameState.INPUT_NEW_NAME;
        showNewNameInput(null);
    }

    /**
     * 确认重命名
     */
    public void confirmRename() {
        if (currentState != RenameState.CONFIRM_RENAME) {
            return;
        }

        try {
            // 发送到服务端
            RenameNetworking.sendRenameRequest(selectedAreaName, newAreaName, newSurfaceName, currentDimension);

            Minecraft.getInstance().player.sendSystemMessage(
                Component.nullToEmpty(I18nManager.translate("message.prompt.rename")));

            resetState();

        } catch (Exception e) {
            Minecraft.getInstance().player.sendSystemMessage(
                Component.nullToEmpty(I18nManager.translate("message.error.area.rename") + e.getMessage()));
            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.NETWORK,
                "重命名失败: " + e.getMessage());
        }
    }

    /**
     * 取消Rename流程
     */
    public void cancelRename() {
        Minecraft.getInstance().player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.message.cancel_3")));
        resetState();
    }

    /**
     * 重置状态
     */
    private void resetState() {
        currentState = RenameState.IDLE;
        selectedAreaName = null;
        newAreaName = null;
        newSurfaceName = null;
        currentDimension = null;
        availableAreas.clear();
        visualFlowActive = false;
        RenameVisualController.clear();
    }

    /**
     * 检查域名名称是否已存在于当前维度
     * @param areaName 要检查的域名名称
     * @return 如果域名名称已存在返回true，否则返回false
     */
    private boolean checkAreaNameExists(String areaName) {
        try {
            String fileName = getFileNameForCurrentDimension();
            if (fileName == null) {
                ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.NETWORK,
                    "无法确定当前维度文件名，跳过查重");
                return false;
            }

            // 读取当前维度的所有域名数据
            List<AreaData> existingAreas = FileManager.readAreaData(
                areahint.world.ClientWorldFolderManager.getWorldDimensionFile(fileName));

            // 检查是否存在相同的域名名称（name字段）
            for (AreaData area : existingAreas) {
                if (area.getName().equals(areaName)) {
                    ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.NETWORK,
                        "发现重复域名名称: " + areaName);
                    return true;
                }
            }

            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.NETWORK,
                "域名名称 \"" + areaName + "\" 未重复，可以使用");
            return false;

        } catch (Exception e) {
            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.NETWORK,
                "检查域名名称时发生错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前维度的文件名
     */
    private String getFileNameForCurrentDimension() {
        if (currentDimension == null) return null;

        if (currentDimension.contains("overworld")) {
            return areahint.Areashint.OVERWORLD_FILE;
        } else if (currentDimension.contains("nether")) {
            return areahint.Areashint.NETHER_FILE;
        } else if (currentDimension.contains("end")) {
            return areahint.Areashint.END_FILE;
        }
        return null;
    }

    // Getters
    public RenameState getCurrentState() { return currentState; }
    public String getSelectedAreaName() { return selectedAreaName; }
    public String getNewAreaName() { return newAreaName; }
    public String getNewSurfaceName() { return newSurfaceName; }

    private void showAreaSelection() {
        if (visualFlowActive) {
            RenameVisualController.showAreaSelection(availableAreas);
        } else {
            RenameUI.showAreaSelectScreen(availableAreas);
        }
    }

    private void showNewNameInput(String errorTextOrKey) {
        if (visualFlowActive) {
            RenameVisualController.showNewNameInput(selectedAreaName, errorTextOrKey);
        } else {
            RenameUI.showNewNameInputScreen();
        }
    }

    private void showSurfaceNameInput() {
        if (visualFlowActive) {
            RenameVisualController.showSurfaceNameInput(null);
        } else {
            RenameUI.showSurfaceNameInputScreen();
        }
    }

    private void showConfirmScreen() {
        if (visualFlowActive) {
            RenameVisualController.showConfirmScreen(selectedAreaName, newAreaName, newSurfaceName);
        } else {
            RenameUI.showConfirmScreen(selectedAreaName, newAreaName, newSurfaceName);
        }
    }
}
