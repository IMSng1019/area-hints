package areahint.keyhandler;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import areahint.easyadd.EasyAddManager;
import areahint.expandarea.ExpandAreaManager;
import areahint.shrinkarea.ShrinkAreaManager;
import areahint.config.ClientConfig;

/**
 * 统一的记录键处理器
 * 避免多个模块同时注册同一个键造成冲突
 * 根据当前激活的模块分发按键事件
 * 支持动态更改按键
 */
public class UnifiedKeyHandler {

    // 记录键绑定
    private static KeyBinding recordKeyBinding;

    // 是否已注册tick事件
    private static boolean tickEventRegistered = false;
    // 长按约0.5秒打开指令可视化主面板。
    private static final int COMMAND_PANEL_HOLD_TICKS = 10;
    private static int commandPanelHoldTicks = 0;
    private static boolean waitingForIdleRelease = false;
    private static boolean commandPanelOpenedForHold = false;
    private static boolean waitingForEasyAddVisualRecordRelease = false;
    private static int easyAddVisualRecordHoldTicks = 0;
    private static boolean easyAddVisualRecordPanelOpened = false;
    private static boolean waitingForCommandVisualRecordRelease = false;
    private static int commandVisualRecordHoldTicks = 0;
    private static boolean commandVisualRecordPanelOpened = false;
    private static boolean suppressUntilRecordKeyReleased = false;

    /**
     * 注册统一的记录键处理器
     */
    public static void register() {
        // 获取配置的按键代码
        int keyCode = ClientConfig.getRecordKey();

        // 注册记录键绑定
        recordKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.areahint.unified.record", // 翻译键
            InputUtil.Type.KEYSYM,
            keyCode, // 使用配置的按键
            "category.areahint.general" // 通用类别
        ));

        // 只注册一次tick事件
        if (!tickEventRegistered) {
            // 注册客户端tick事件监听器
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                handleRecordKeyTick(client);
            });
            tickEventRegistered = true;
        }
    }

    /**
     * 重新注册按键（当按键配置改变时调用）
     */
    public static void reregisterKey() {
        if (recordKeyBinding == null) {
            System.out.println("DEBUG: recordKeyBinding 为 null，无法重新注册");
            return;
        }

        // 获取新的按键代码
        int keyCode = ClientConfig.getRecordKey();

        // 使用 setBoundKey 方法更新按键绑定
        recordKeyBinding.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(keyCode));

        // 刷新按键映射表（重要：让 Minecraft 重新索引按键）
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            // 调用 KeyBinding 的静态方法来更新所有按键映射
            KeyBinding.updateKeysByCode();

            // 保存按键绑定设置到 Minecraft 的配置文件
            client.options.write();
        }

        System.out.println("DEBUG: 记录键已更新为键码 " + keyCode + "，按键名称：" + recordKeyBinding.getBoundKeyLocalizedText().getString());
    }

    /**
     * 处理记录键tick，区分即时录点、短按描述查询和长按指令面板。
     */
    private static void handleRecordKeyTick(MinecraftClient client) {
        if (recordKeyBinding == null) {
            return;
        }

        if (suppressUntilRecordKeyReleased) {
            resetHoldState();
            drainRecordKeyPresses();
            if (!(client.currentScreen instanceof areahint.commandui.CommandUiScreen)
                    && !isRecordKeyPhysicallyPressed(client)) {
                suppressUntilRecordKeyReleased = false;
            }
            return;
        }

        boolean pressedThisTick = recordKeyBinding.wasPressed();
        if (pressedThisTick && handleImmediateRecordKeyPress()) {
            resetHoldState();
            return;
        }

        if (waitingForEasyAddVisualRecordRelease) {
            handleEasyAddVisualRecordHoldProgress(client);
        } else if (waitingForCommandVisualRecordRelease) {
            handleCommandVisualRecordHoldProgress(client);
        } else if (waitingForIdleRelease) {
            handleIdleHoldProgress(client);
        } else if (!pressedThisTick) {
            areahint.description.DescriptionKeyHandler.clearSuppressedRecordKeyPress();
        }
    }

    /**
     * 处理必须在按下瞬间响应的流程。
     */
    private static boolean handleImmediateRecordKeyPress() {
        System.out.println("DEBUG: 记录键被按下");

        if (areahint.description.DescriptionKeyHandler.consumeSuppressedRecordKeyPress()) {
            return true;
        }
        if (areahint.description.DescriptionKeyHandler.closeCurrentDescriptionBookScreen()) {
            return true;
        }
        if (areahint.commandui.CommandUiScreen.closeIfCommandUiOpen()) {
            suppressUntilRecordKeyReleased = true;
            return true;
        }

        // 检查EasyAdd是否活跃且在记录状态
        EasyAddManager easyAddManager = EasyAddManager.getInstance();
        if (easyAddManager.getCurrentState() == EasyAddManager.EasyAddState.RECORDING_POINTS) {
            if (easyAddManager.isVisualMode()) {
                waitingForEasyAddVisualRecordRelease = true;
                easyAddVisualRecordHoldTicks = 0;
                easyAddVisualRecordPanelOpened = false;
                return false;
            }
            System.out.println("DEBUG: EasyAdd 处理记录键");
            easyAddManager.recordCurrentPosition();
            return true;
        }

        // 检查ExpandArea是否活跃且在记录状态
        ExpandAreaManager expandAreaManager = ExpandAreaManager.getInstance();
        System.out.println("DEBUG: ExpandArea - isActive: " + expandAreaManager.isActive() + ", isRecording: " + expandAreaManager.isRecording());
        if (expandAreaManager.isActive() && expandAreaManager.isRecording()) {
            if (areahint.commandui.CommandVisualController.isVisualRecordMode("expandarea")) {
                waitingForCommandVisualRecordRelease = true;
                commandVisualRecordHoldTicks = 0;
                commandVisualRecordPanelOpened = false;
                return false;
            }
            System.out.println("DEBUG: ExpandArea 处理记录键");
            expandAreaManager.recordCurrentPosition();
            return true;
        }

        // 检查ShrinkArea是否活跃且在记录状态
        ShrinkAreaManager shrinkAreaManager = ShrinkAreaManager.getInstance();
        if (shrinkAreaManager.isActive() && shrinkAreaManager.isRecording()) {
            if (areahint.commandui.CommandVisualController.isVisualRecordMode("shrinkarea")) {
                waitingForCommandVisualRecordRelease = true;
                commandVisualRecordHoldTicks = 0;
                commandVisualRecordPanelOpened = false;
                return false;
            }
            System.out.println("DEBUG: ShrinkArea 处理记录键");
            shrinkAreaManager.handleXKeyPress();
            return true;
        }

        // 检查DivideArea是否活跃且在记录状态
        areahint.dividearea.DivideAreaManager divideAreaManager = areahint.dividearea.DivideAreaManager.getInstance();
        if (divideAreaManager.isActive() && divideAreaManager.isRecording()) {
            if (areahint.commandui.CommandVisualController.isVisualRecordMode("dividearea")) {
                waitingForCommandVisualRecordRelease = true;
                commandVisualRecordHoldTicks = 0;
                commandVisualRecordPanelOpened = false;
                return false;
            }
            System.out.println("DEBUG: DivideArea 处理记录键");
            divideAreaManager.recordCurrentPosition();
            return true;
        }

        // 检查AddHint是否活跃且在记录状态
        areahint.addhint.AddHintManager addHintManager = areahint.addhint.AddHintManager.getInstance();
        if (addHintManager.isActive() && addHintManager.isRecording()) {
            if (areahint.commandui.CommandVisualController.isVisualRecordMode("addhint")) {
                waitingForCommandVisualRecordRelease = true;
                commandVisualRecordHoldTicks = 0;
                commandVisualRecordPanelOpened = false;
                return false;
            }
            addHintManager.recordCurrentPosition();
            return true;
        }

        // 非录点状态才进入短按/长按判断。
        if (shouldBlockIdleRecordKey()) {
            return true;
        }

        waitingForIdleRelease = true;
        commandPanelHoldTicks = 0;
        commandPanelOpenedForHold = false;
        return false;
    }

    /**
     * 非录点状态下，短按保留描述查询，长按打开指令可视化主面板。
     */
    private static void handleIdleHoldProgress(MinecraftClient client) {
        if (recordKeyBinding.isPressed()) {
            commandPanelHoldTicks++;
            if (!commandPanelOpenedForHold && commandPanelHoldTicks >= COMMAND_PANEL_HOLD_TICKS) {
                commandPanelOpenedForHold = true;
                waitingForIdleRelease = false;
                // 长按打开面板后忽略本次仍未松开的绑定键，避免重复按键事件立刻关闭界面。
                suppressRecordKeyUntilRelease();
                // 审批预览临时收起后，长按优先恢复原申请；普通状态仍打开统一菜单。
                if (!areahint.approval.ApprovalClientManager.restorePanel()) {
                    client.setScreen(new areahint.commandui.CommandPanelScreen());
                }
            }
            return;
        }

        if (!commandPanelOpenedForHold) {
            System.out.println("DEBUG: 没有模块处理记录键，查询当前域名描述");
            areahint.description.DescriptionKeyHandler.queryCurrentDescription();
        }
        resetCommandPanelHoldState();
    }

    /**
     * 图形EasyAdd录点时，短按录点，长按打开完成/取消面板。
     */
    private static void handleEasyAddVisualRecordHoldProgress(MinecraftClient client) {
        EasyAddManager manager = EasyAddManager.getInstance();
        if (!manager.isVisualMode()
                || manager.getCurrentState() != EasyAddManager.EasyAddState.RECORDING_POINTS) {
            resetEasyAddVisualRecordHoldState();
            return;
        }

        if (recordKeyBinding.isPressed()) {
            easyAddVisualRecordHoldTicks++;
            if (!easyAddVisualRecordPanelOpened
                    && easyAddVisualRecordHoldTicks >= COMMAND_PANEL_HOLD_TICKS) {
                easyAddVisualRecordPanelOpened = true;
                waitingForEasyAddVisualRecordRelease = false;
                suppressRecordKeyUntilRelease();
                client.setScreen(new areahint.easyadd.EasyAddRecordingActionScreen(null));
            }
            return;
        }

        if (!easyAddVisualRecordPanelOpened) {
            System.out.println("DEBUG: EasyAdd 图形录点短按记录坐标");
            manager.recordCurrentPosition();
        }
        resetEasyAddVisualRecordHoldState();
    }

    /**
     * 指令可视化录点时，短按录点，长按打开继续/完成/取消面板。
     */
    private static void handleCommandVisualRecordHoldProgress(MinecraftClient client) {
        if (!hasActiveCommandVisualRecording()) {
            resetCommandVisualRecordHoldState();
            areahint.commandui.CommandVisualController.clearVisualRecordMode();
            return;
        }

        if (recordKeyBinding.isPressed()) {
            commandVisualRecordHoldTicks++;
            if (!commandVisualRecordPanelOpened
                    && commandVisualRecordHoldTicks >= COMMAND_PANEL_HOLD_TICKS) {
                commandVisualRecordPanelOpened = true;
                waitingForCommandVisualRecordRelease = false;
                suppressRecordKeyUntilRelease();
                areahint.commandui.RecordCommandActionScreen.openActive(null);
            }
            return;
        }

        if (!commandVisualRecordPanelOpened) {
            recordActiveCommandVisualPosition();
        }
        resetCommandVisualRecordHoldState();
    }

    private static void resetCommandPanelHoldState() {
        commandPanelHoldTicks = 0;
        waitingForIdleRelease = false;
        commandPanelOpenedForHold = false;
    }

    private static void resetEasyAddVisualRecordHoldState() {
        easyAddVisualRecordHoldTicks = 0;
        waitingForEasyAddVisualRecordRelease = false;
        easyAddVisualRecordPanelOpened = false;
    }

    private static void resetCommandVisualRecordHoldState() {
        commandVisualRecordHoldTicks = 0;
        waitingForCommandVisualRecordRelease = false;
        commandVisualRecordPanelOpened = false;
    }

    private static void resetHoldState() {
        resetCommandPanelHoldState();
        resetEasyAddVisualRecordHoldState();
        resetCommandVisualRecordHoldState();
    }

    private static boolean hasActiveCommandVisualRecording() {
        return areahint.commandui.CommandVisualController.isVisualRecordMode("addhint")
            && areahint.addhint.AddHintManager.getInstance().isActive()
            && areahint.addhint.AddHintManager.getInstance().isRecording()
            || areahint.commandui.CommandVisualController.isVisualRecordMode("expandarea")
            && ExpandAreaManager.getInstance().isActive()
            && ExpandAreaManager.getInstance().isRecording()
            || areahint.commandui.CommandVisualController.isVisualRecordMode("shrinkarea")
            && ShrinkAreaManager.getInstance().isActive()
            && ShrinkAreaManager.getInstance().isRecording()
            || areahint.commandui.CommandVisualController.isVisualRecordMode("dividearea")
            && areahint.dividearea.DivideAreaManager.getInstance().isActive()
            && areahint.dividearea.DivideAreaManager.getInstance().isRecording();
    }

    private static void recordActiveCommandVisualPosition() {
        if (areahint.commandui.CommandVisualController.isVisualRecordMode("addhint")) {
            areahint.addhint.AddHintManager.getInstance().recordCurrentPosition();
        } else if (areahint.commandui.CommandVisualController.isVisualRecordMode("expandarea")) {
            ExpandAreaManager.getInstance().recordCurrentPosition();
        } else if (areahint.commandui.CommandVisualController.isVisualRecordMode("shrinkarea")) {
            ShrinkAreaManager.getInstance().handleXKeyPress();
        } else if (areahint.commandui.CommandVisualController.isVisualRecordMode("dividearea")) {
            areahint.dividearea.DivideAreaManager.getInstance().recordCurrentPosition();
        }
    }

    private static boolean shouldBlockIdleRecordKey() {
        // 当前已有界面或描述流程时，不打开面板，也不触发默认描述查询。
        if (areahint.description.DescriptionKeyHandler.shouldSkipDefaultRecordKeyQuery()) {
            return true;
        }
        return EasyAddManager.getInstance().getCurrentState() != EasyAddManager.EasyAddState.IDLE
            || ExpandAreaManager.getInstance().isActive()
            || ShrinkAreaManager.getInstance().isActive()
            || areahint.dividearea.DivideAreaManager.getInstance().isActive()
            || areahint.addhint.AddHintManager.getInstance().isActive()
            || areahint.deletehint.DeleteHintManager.getInstance().isActive()
            || areahint.delete.DeleteManager.getInstance().getCurrentState() != areahint.delete.DeleteManager.DeleteState.IDLE
            || areahint.dimensional.DimensionalNameUIManager.getInstance().getCurrentState() != areahint.dimensional.DimensionalNameUIManager.State.IDLE
            || areahint.rename.RenameManager.getInstance().getCurrentState() != areahint.rename.RenameManager.RenameState.IDLE
            || areahint.recolor.RecolorManager.getInstance().getCurrentState() != areahint.recolor.RecolorManager.RecolorState.IDLE
            || areahint.signature.SignatureManager.getInstance().isActive()
            || areahint.language.LanguageManager.getInstance().getCurrentState() != areahint.language.LanguageManager.State.IDLE;
    }

    /**
     * 指令可视化界面用绑定键关闭后，忽略同一次按键直到松开。
     */
    public static void suppressRecordKeyUntilRelease() {
        suppressUntilRecordKeyReleased = true;
        drainRecordKeyPresses();
        resetHoldState();
    }

    /**
     * 指令可视化界面判断绑定键是否还处于需要丢弃的同一次按压。
     */
    public static boolean isRecordKeySuppressedUntilRelease() {
        return suppressUntilRecordKeyReleased;
    }

    /**
     * 指令可视化界面收到绑定键释放事件后，结束本次长按抑制。
     */
    public static boolean releaseSuppressedRecordKeyIfMatches(int keyCode, int scanCode) {
        if (!suppressUntilRecordKeyReleased || !matchesRecordKey(keyCode, scanCode)) {
            return false;
        }
        suppressUntilRecordKeyReleased = false;
        drainRecordKeyPresses();
        return true;
    }

    /**
     * 判断当前按键事件是否为记录绑定键。
     */
    public static boolean matchesRecordKey(int keyCode, int scanCode) {
        return recordKeyBinding != null
            && !recordKeyBinding.isUnbound()
            && recordKeyBinding.matchesKey(keyCode, scanCode);
    }

    /**
     * 获取记录键绑定
     */
    public static KeyBinding getRecordKeyBinding() {
        return recordKeyBinding;
    }

    /**
     * 获取记录键显示名称
     */
    public static String getRecordKeyDisplayName() {
        if (recordKeyBinding != null) {
            return recordKeyBinding.getBoundKeyLocalizedText().getString();
        }
        return "X";
    }

    private static void drainRecordKeyPresses() {
        if (recordKeyBinding == null) {
            return;
        }
        while (recordKeyBinding.wasPressed()) {
            // 清空长按或关闭界面期间积压的绑定键事件，避免松开后再次触发短按。
        }
    }

    private static boolean isRecordKeyPhysicallyPressed(MinecraftClient client) {
        int keyCode = ClientConfig.getRecordKey();
        if (client != null && client.getWindow() != null
                && keyCode > GLFW.GLFW_KEY_UNKNOWN && keyCode <= GLFW.GLFW_KEY_LAST) {
            return GLFW.glfwGetKey(client.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
        }
        return recordKeyBinding != null && recordKeyBinding.isPressed();
    }
} 
