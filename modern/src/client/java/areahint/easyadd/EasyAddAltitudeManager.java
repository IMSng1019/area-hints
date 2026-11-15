package areahint.easyadd;

import areahint.data.AreaData;
import areahint.debug.ClientDebugManager;
import areahint.i18n.I18nManager;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * EasyAdd高度管理器
 * 负责处理域名高度的自定义设置
 */
public class EasyAddAltitudeManager {
    
    /**
     * 高度选择类型
     */
    public enum AltitudeType {
        AUTOMATIC,  // 自动计算（原逻辑）
        CUSTOM,     // 自定义高度
        UNLIMITED   // 不限制高度
    }
    
    // 高度选择状态
    public enum AltitudeInputState {
        SELECTING_TYPE,     // 选择高度类型
        INPUT_MIN_HEIGHT,   // 输入最低高度
        INPUT_MAX_HEIGHT    // 输入最高高度
    }
    
    // 当前输入状态
    private static AltitudeInputState currentInputState = AltitudeInputState.SELECTING_TYPE;
    
    // 自定义高度值
    private static Double customMinHeight = null;
    private static Double customMaxHeight = null;
    
    // 选择的高度类型
    private static AltitudeType selectedType = AltitudeType.AUTOMATIC;
    
    /**
     * 开始高度选择流程
     * @param recordedPoints 记录的坐标点
     */
    public static void startAltitudeSelection(List<BlockPos> recordedPoints) {
        reset();
        
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        // 图形模式下打开 Screen，普通模式继续使用聊天按钮。
        if (EasyAddManager.getInstance().isVisualMode()) {
            EasyAddVisualController.showAltitudeScreen(recordedPoints);
        } else {
            EasyAddUI.showAltitudeSelectionScreen(recordedPoints);
        }
        
        ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
            "开始高度选择流程");
    }
    
    /**
     * 处理高度类型选择
     * @param type 选择的高度类型
     */
    public static void handleAltitudeTypeSelection(AltitudeType type) {
        selectedType = type;
        
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        if (type == AltitudeType.AUTOMATIC) {
            // 选择自动计算，直接进入下一步
            if (!EasyAddManager.getInstance().isVisualMode()) {
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.prompt.altitude_2")));
            }
            EasyAddManager.getInstance().proceedWithAltitudeData(null);
        } else if (type == AltitudeType.UNLIMITED) {
            // 选择不限制高度，使用null值表示无高度限制
            if (!EasyAddManager.getInstance().isVisualMode()) {
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.prompt.altitude")));
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.message.area.coordinate")));
            }
            AreaData.AltitudeData unlimitedAltitude = new AreaData.AltitudeData(null, null);
            EasyAddManager.getInstance().proceedWithAltitudeData(unlimitedAltitude);
        } else {
            // 选择自定义，普通模式走聊天输入，图形模式走双输入框。
            currentInputState = AltitudeInputState.INPUT_MIN_HEIGHT;
            if (EasyAddManager.getInstance().isVisualMode()) {
                EasyAddVisualController.showCustomAltitudeScreen(null);
            } else {
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.prompt.altitude_3")));
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.prompt.altitude_5")));
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.message.altitude")));
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("command.error.cancel")));
            }
        }
        
        ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
            "选择高度类型: " + type);
    }
    
    /**
     * 处理高度数值输入
     * @param input 用户输入的字符串
     * @return 是否成功处理输入
     */
    public static boolean handleAltitudeInput(String input) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        
        // 检查取消操作
        if (input.trim().isEmpty() || input.contains("取消")) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("command.error.altitude.cancel_2")));
            EasyAddManager.getInstance().cancelEasyAdd();
            return true;
        }
        
        try {
            double value = Double.parseDouble(input.trim());
            
            switch (currentInputState) {
                case INPUT_MIN_HEIGHT:
                    customMinHeight = value;
                    client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("command.message.altitude_10") + value));
                    client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.prompt.altitude_4")));
                    client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.message.altitude_2")));
                    client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.error.cancel")));
                    
                    currentInputState = AltitudeInputState.INPUT_MAX_HEIGHT;
                    return true;
                    
                case INPUT_MAX_HEIGHT:
                    if (customMinHeight != null && value <= customMinHeight) {
                        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.error.altitude") + customMinHeight));
                        return true;
                    }
                    
                    customMaxHeight = value;
                    client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("command.message.altitude_11") + value));
                    client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("command.message.altitude_12") + customMinHeight + " ~ " + customMaxHeight));
                    
                    // 创建自定义高度数据并继续流程
                    AreaData.AltitudeData customAltitude = new AreaData.AltitudeData(customMaxHeight, customMinHeight);
                    EasyAddManager.getInstance().proceedWithAltitudeData(customAltitude);
                    return true;
                    
                default:
                    return false;
            }
            
        } catch (NumberFormatException e) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("command.error.general_12")));
            return true;
        }
    }
    
    /**
     * 获取当前输入状态
     */
    public static AltitudeInputState getCurrentInputState() {
        return currentInputState;
    }
    
    /**
     * 获取选择的高度类型
     */
    public static AltitudeType getSelectedType() {
        return selectedType;
    }
    
    /**
     * 重置所有状态
     */
    public static void reset() {
        currentInputState = AltitudeInputState.SELECTING_TYPE;
        customMinHeight = null;
        customMaxHeight = null;
        selectedType = AltitudeType.AUTOMATIC;
        
        ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
            "重置高度管理器状态");
    }
    
    /**
     * 检查是否正在进行高度输入
     */
    public static boolean isInputtingAltitude() {
        return currentInputState == AltitudeInputState.INPUT_MIN_HEIGHT || 
               currentInputState == AltitudeInputState.INPUT_MAX_HEIGHT;
    }
}
