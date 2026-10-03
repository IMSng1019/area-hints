package areahint.recolor;

import areahint.AreashintClient;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import areahint.network.NetworkBuf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Recolor功能管理器
 * 负责交互式域名重新着色的整个流程管理
 */
public class RecolorManager {

    /**
     * Recolor状态枚举
     */
    public enum RecolorState {
        IDLE,               // 空闲状态
        AREA_SELECTION,     // 域名选择
        COLOR_SELECTION,    // 颜色选择
        CONFIRM_CHANGE      // 确认修改
    }

    // 单例实例
    private static RecolorManager instance;

    // 当前状态
    private RecolorState currentState = RecolorState.IDLE;

    // 数据收集
    private List<AreaData> editableAreas = new ArrayList<>();
    private String selectedAreaName = null;
    private String selectedColor = null;
    private String currentDimension = null;
    private String originalColor = null;
    private boolean visualFlowActive = false;

    // 私有构造函数（单例模式）
    private RecolorManager() {}

    /**
     * 获取单例实例
     */
    public static RecolorManager getInstance() {
        if (instance == null) {
            instance = new RecolorManager();
        }
        return instance;
    }

    /**
     * 启动Recolor流程
     * @param areas 可编辑的域名列表
     * @param dimension 当前维度
     */
    public void startRecolor(List<AreaData> areas, String dimension) {
        boolean visualRequested = RecolorVisualController.consumeVisualStartRequest();
        if (currentState != RecolorState.IDLE) {
            MinecraftClient.getInstance().player.sendMessage(Text.of(I18nManager.translate("message.error.general_3")), false);
            if (visualRequested) {
                RecolorVisualController.showInfo("message.error.general_3");
                RecolorVisualController.clear();
            }
            return;
        }

        this.editableAreas = new ArrayList<>(areas);
        this.currentDimension = dimension;
        this.visualFlowActive = visualRequested;

        // 设置状态并显示UI
        currentState = RecolorState.AREA_SELECTION;
        showAreaSelection();
    }

    /**
     * 处理域名选择
     * @param areaName 选择的域名名称
     */
    public void handleAreaSelection(String areaName) {
        if (currentState != RecolorState.AREA_SELECTION) {
            return;
        }

        // 查找选择的域名
        AreaData selectedArea = null;
        for (AreaData area : editableAreas) {
            if (area.getName().equals(areaName)) {
                selectedArea = area;
                break;
            }
        }

        if (selectedArea == null) {
            MinecraftClient.getInstance().player.sendMessage(Text.of(I18nManager.translate("message.error.area_2") + areaName), false);
            return;
        }

        this.selectedAreaName = areaName;
        this.originalColor = selectedArea.getColor();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.of(I18nManager.translate("message.prompt.area") + areaName), false);
            client.player.sendMessage(Text.of(I18nManager.translate("message.message.color_3") + originalColor), false);
        }

        // 进入颜色选择状态
        currentState = RecolorState.COLOR_SELECTION;
        showColorSelection(areaName, originalColor);
    }

    /**
     * 处理颜色选择
     * @param colorInput 颜色输入
     */
    public void handleColorSelection(String colorInput) {
        if (currentState != RecolorState.COLOR_SELECTION) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // 验证颜色格式
        String normalizedColor = areahint.util.ColorUtil.normalizeColor(colorInput);
        if (!areahint.util.ColorUtil.isValidColor(normalizedColor)) {
            client.player.sendMessage(Text.of(I18nManager.translate("gui.error.color")), false);
            if (visualFlowActive) {
                RecolorVisualController.showCustomColorScreen(selectedAreaName, originalColor, "commandui.color.error.invalid");
            }
            return;
        }

        this.selectedColor = normalizedColor;

        // 进入确认状态
        currentState = RecolorState.CONFIRM_CHANGE;
        showConfirm();
    }

    /**
     * 确认颜色修改
     */
    public void confirmChange() {
        if (currentState != RecolorState.CONFIRM_CHANGE) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        try {
            // 发送重新着色请求到服务端
            sendRecolorRequest(selectedAreaName, selectedColor, currentDimension);

            client.player.sendMessage(Text.of(I18nManager.translate("message.prompt.color.modify")), false);

            // 重置状态
            resetState();

        } catch (Exception e) {
            client.player.sendMessage(Text.of(I18nManager.translate("message.error.general") + e.getMessage()), false);
            AreashintClient.LOGGER.error(I18nManager.translate("message.error.general_32"), e);
        }
    }

    /**
     * 取消Recolor流程
     */
    public void cancelRecolor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.of(I18nManager.translate("message.message.cancel_2")), false);
        }
        resetState();
    }

    /**
     * 重置状态
     */
    private void resetState() {
        currentState = RecolorState.IDLE;
        editableAreas.clear();
        selectedAreaName = null;
        selectedColor = null;
        currentDimension = null;
        originalColor = null;
        visualFlowActive = false;
        RecolorVisualController.clear();
    }

    /**
     * 发送重新着色请求到服务端
     * @param areaName 域名名称
     * @param color 新颜色
     * @param dimension 维度
     */
    private void sendRecolorRequest(String areaName, String color, String dimension) {
        try {
            NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
            buf.writeString(areaName);
            buf.writeString(color);
            buf.writeString(dimension);

            ClientNetworkCompat.get().sendToServer(Channel.C2S_RECOLOR_REQUEST, buf);

            AreashintClient.LOGGER.info(I18nManager.translate("message.prompt.area.color.dimension"),
                areaName, color, dimension);

        } catch (Exception e) {
            AreashintClient.LOGGER.error(I18nManager.translate("message.error.general_30") + e.getMessage(), e);
        }
    }

    // Getters
    public RecolorState getCurrentState() { return currentState; }
    public String getSelectedAreaName() { return selectedAreaName; }
    public String getSelectedColor() { return selectedColor; }
    public String getOriginalColor() { return originalColor; }

    private void showAreaSelection() {
        if (visualFlowActive) {
            RecolorVisualController.showAreaSelectionScreen(editableAreas);
        } else {
            RecolorUI.showAreaSelectionScreen(editableAreas);
        }
    }

    private void showColorSelection(String areaName, String currentColor) {
        if (visualFlowActive) {
            RecolorVisualController.showColorSelectionScreen(areaName, currentColor);
        } else {
            RecolorUI.showColorSelectionScreen(areaName, currentColor);
        }
    }

    private void showConfirm() {
        if (visualFlowActive) {
            RecolorVisualController.showConfirmScreen(selectedAreaName, originalColor, selectedColor);
        } else {
            RecolorUI.showConfirmScreen(selectedAreaName, originalColor, selectedColor);
        }
    }
}
