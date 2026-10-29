package areahint.easyadd;

import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.debug.ClientDebugManager;
import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * EasyAdd功能管理器
 * 负责交互式域名添加的整个流程管理
 */
public class EasyAddManager {
    
    /**
     * EasyAdd状态枚举
     */
    public enum EasyAddState {
        IDLE,           // 空闲状态
        INPUT_NAME,     // 输入域名名称
        INPUT_SURFACE_NAME, // 输入联合域名名称
        INPUT_LEVEL,    // 输入域名等级
        SELECT_BASE,    // 选择上级域名
        RECORDING_POINTS, // 记录坐标点
        HEIGHT_SELECTION, // 高度选择
        COLOR_SELECTION, // 颜色选择（新增）
        COLOR_INPUT,    // 自定义颜色输入（新增）
        CONFIRM_SAVE    // 确认保存
    }
    
    // 单例实例
    private static EasyAddManager instance;
    
    // 当前状态
    private EasyAddState currentState = EasyAddState.IDLE;
    
    // 域名数据收集
    private String areaName = null;
    private String surfaceName = null;  // 联合域名
    private int areaLevel = 1;
    private String baseName = null;
    private List<BlockPos> recordedPoints = new ArrayList<>();
    private String currentDimension = null;
    private List<AreaData> availableParentAreas = new ArrayList<>();
    private AreaData.AltitudeData customAltitudeData = null; // 自定义高度数据
    private String selectedColor = "#FFFFFF"; // 选择的颜色（新增）
    private AreaData previewAreaData = null; // 图形确认页使用的预览数据
    private boolean visualMode = false; // 图形流程模式，屏蔽旧聊天按钮刷屏
    
    // 聊天监听器注册状态
    private boolean chatListenerRegistered = false;
    
    // 私有构造函数（单例模式）
    private EasyAddManager() {}
    
    /**
     * 获取单例实例
     */
    public static EasyAddManager getInstance() {
        if (instance == null) {
            instance = new EasyAddManager();
        }
        return instance;
    }
    
    /**
     * 启动EasyAdd流程
     */
    public void startEasyAdd() {
        if (currentState != EasyAddState.IDLE) {
            MinecraftClient.getInstance().player.sendMessage(Text.of(I18nManager.translate("easyadd.error.general")), false);
            EasyAddVisualController.clear();
            return;
        }
        
        // 获取当前维度信息
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            currentDimension = client.world.getRegistryKey().getValue().toString();
            
            // 注册聊天监听器
            registerChatListener();
            visualMode = EasyAddVisualController.consumeVisualStartRequest();
            
            // 设置状态并显示UI
            currentState = EasyAddState.INPUT_NAME;
            if (visualMode) {
                EasyAddVisualController.showNameScreen(null);
            } else {
                EasyAddUI.showNameInputScreen();
            }
        }
    }
    
    /**
     * 注册聊天监听器来捕获用户输入
     */
    private void registerChatListener() {
        if (!chatListenerRegistered) {
            ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
                if (currentState != EasyAddState.IDLE) {
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (visualMode) {
            return;
        }
        
        // 移除前缀符号（如果有的话）
        if (input.startsWith("<") && input.contains(">")) {
            int endIndex = input.indexOf(">") + 1;
            if (endIndex < input.length()) {
                input = input.substring(endIndex).trim();
            }
        }
        
        switch (currentState) {
            case INPUT_NAME:
                processNameInput(input, false);
                break;
                
            case INPUT_SURFACE_NAME:
                processSurfaceNameInput(input, false);
                break;
                
            case HEIGHT_SELECTION:
                // 处理高度输入
                if (EasyAddAltitudeManager.isInputtingAltitude()) {
                    EasyAddAltitudeManager.handleAltitudeInput(input);
                }
                break;
                
            case COLOR_INPUT:
                // 处理自定义颜色输入
                handleCustomColorInput(input);
                break;
                
            default:
                break;
        }
    }

    /**
     * 处理图形界面的域名名称输入。
     */
    public void handleVisualNameInput(String input) {
        processNameInput(input, true);
    }

    /**
     * 处理图形界面的联合域名输入。
     */
    public void handleVisualSurfaceNameInput(String input) {
        processSurfaceNameInput(input, true);
    }

    /**
     * 处理图形界面的自定义高度输入。
     */
    public void handleVisualCustomAltitudeInput(String minInput, String maxInput) {
        try {
            double min = Double.parseDouble(minInput.trim());
            double max = Double.parseDouble(maxInput.trim());
            if (max <= min) {
                EasyAddVisualController.showCustomAltitudeScreen("commandui.easyadd.error.altitude_order");
                return;
            }
            proceedWithAltitudeData(new AreaData.AltitudeData(max, min));
        } catch (NumberFormatException e) {
            EasyAddVisualController.showCustomAltitudeScreen("commandui.easyadd.error.number");
        }
    }

    /**
     * 处理图形界面的自定义颜色输入。
     */
    public void handleVisualCustomColorInput(String colorInput) {
        String normalizedColor = normalizeStrictColor(colorInput);
        if (normalizedColor == null) {
            EasyAddVisualController.showCustomColorScreen("commandui.easyadd.error.color");
            return;
        }
        proceedWithColorSelection(normalizedColor);
    }

    /**
     * 统一处理域名名称，聊天和图形流程共用同一套校验。
     */
    private void processNameInput(String input, boolean fromVisual) {
        if (currentState != EasyAddState.INPUT_NAME) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String trimmed = input == null ? "" : input.trim();
        if (trimmed.isEmpty()) {
            if (fromVisual) {
                EasyAddVisualController.showNameScreen("commandui.easyadd.error.name_empty");
            }
            return;
        }

        areaName = trimmed;

        // 检查域名名称是否已存在（不检查联合域名）
        if (checkAreaNameExists(areaName)) {
            if (visualMode || fromVisual) {
                EasyAddVisualController.showNameScreen("commandui.easyadd.error.name_duplicate");
            } else {
                client.player.sendMessage(Text.of("§c" + I18nManager.translate("easyadd.message.area.name_4") + areaName + I18nManager.translate("easyadd.message.dimension")), false);
                client.player.sendMessage(Text.of(I18nManager.translate("easyadd.prompt.area.name")), false);
            }
            return;
        }

        if (!visualMode) {
            client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.area.name_2") + areaName), false);
        }

        currentState = EasyAddState.INPUT_SURFACE_NAME;
        if (visualMode) {
            EasyAddVisualController.showSurfaceNameScreen();
        } else {
            EasyAddUI.showSurfaceNameInputScreen();
        }
    }

    /**
     * 统一处理联合域名，空字符串表示不设置联合域名。
     */
    private void processSurfaceNameInput(String input, boolean fromVisual) {
        if (currentState != EasyAddState.INPUT_SURFACE_NAME) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        surfaceName = input == null || input.trim().isEmpty() ? null : input.trim();
        if (!visualMode) {
            if (surfaceName != null) {
                client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.area.surface") + surfaceName), false);
            } else {
                client.player.sendMessage(Text.of(I18nManager.translate("dividearea.message.area.surface")), false);
            }
        }

        currentState = EasyAddState.INPUT_LEVEL;
        if (visualMode) {
            EasyAddVisualController.showLevelScreen();
        } else {
            EasyAddUI.showLevelInputScreen();
        }
    }
    
    /**
     * 处理等级输入（从命令调用）
     */
    public void handleLevelInput(int level) {
        if (currentState != EasyAddState.INPUT_LEVEL) {
            return;
        }
        
        areaLevel = level;
        MinecraftClient client = MinecraftClient.getInstance();
        if (!visualMode && client.player != null) {
            client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.area.level") + level), false);
        }
        
        if (level == 1) {
            // 顶级域名，直接开始记录坐标
            baseName = null;
            currentState = EasyAddState.RECORDING_POINTS;
            if (visualMode) {
                EasyAddVisualController.showRecordingHud();
            } else if (client.player != null) {
                client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.coordinate.record_2") + EasyAddConfig.getRecordKey() + I18nManager.translate("easyadd.message.record_2")), false);
            }
        } else {
            // 需要选择上级域名
            loadAvailableParentAreas();
            if (currentState == EasyAddState.IDLE) {
                return;
            }
            currentState = EasyAddState.SELECT_BASE;
            if (visualMode) {
                EasyAddVisualController.showBaseSelectScreen(availableParentAreas);
            } else {
                EasyAddUI.showBaseSelectScreen(availableParentAreas);
            }
        }
    }
    
    /**
     * 处理上级域名选择（从命令调用）
     */
    public void handleBaseSelection(String selectedBaseName) {
        if (currentState != EasyAddState.SELECT_BASE) {
            return;
        }
        
        // 移除引号（如果存在）
        baseName = selectedBaseName;
        if (baseName.startsWith("\"") && baseName.endsWith("\"") && baseName.length() > 1) {
            baseName = baseName.substring(1, baseName.length() - 1);
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (!visualMode && client.player != null) {
            client.player.sendMessage(Text.of(I18nManager.translate("easyadd.prompt.area.parent") + baseName), false);
        }
        
        // 开始记录坐标点
        currentState = EasyAddState.RECORDING_POINTS;
        if (visualMode) {
            EasyAddVisualController.showRecordingHud();
        } else if (client.player != null) {
            client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.coordinate.record_2") + EasyAddConfig.getRecordKey() + I18nManager.translate("easyadd.message.record_2")), false);
        }
    }

    /**
     * 加载可选的上级域名
     */
    private void loadAvailableParentAreas() {
        availableParentAreas.clear();
        
        try {
            String fileName = getFileNameForCurrentDimension();
            if (fileName != null) {
                List<AreaData> allAreas = FileManager.readAreaData(areahint.world.ClientWorldFolderManager.getWorldDimensionFile(fileName));
                
                // 筛选出等级为当前等级-1的域名
                int targetLevel = areaLevel - 1;
                for (AreaData area : allAreas) {
                    if (area.getLevel() == targetLevel) {
                        availableParentAreas.add(area);
                    }
                }
            }
        } catch (Exception e) {
            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD, 
                "加载上级域名失败: " + e.getMessage());
        }
        
        if (availableParentAreas.isEmpty()) {
            MinecraftClient.getInstance().player.sendMessage(
                Text.of(I18nManager.translate("easyadd.error.level") + (areaLevel - 1) + I18nManager.translate("easyadd.message.area.parent")), false);
            boolean wasVisualMode = visualMode;
            cancelEasyAdd();
            if (wasVisualMode && MinecraftClient.getInstance() != null) {
                MinecraftClient.getInstance().setScreen(null);
            }
            return;
        }
    }
    
    /**
     * 开始坐标点记录
     */
    private void startPointRecording() {
        recordedPoints.clear();
        MinecraftClient.getInstance().player.sendMessage(
            Text.of(I18nManager.translate("easyadd.message.area.vertex.record") + EasyAddConfig.getRecordKey() + I18nManager.translate("easyadd.message.record_2")), false);
        MinecraftClient.getInstance().player.sendMessage(
            Text.of(I18nManager.translate("easyadd.message.record_3")), false);
    }
    
    /**
     * 记录当前玩家位置作为顶点
     */
    public void recordCurrentPosition() {
        if (currentState != EasyAddState.RECORDING_POINTS) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        BlockPos pos = client.player.getBlockPos();
        recordedPoints.add(pos);

        if (!visualMode) {
            client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.coordinate.record") + recordedPoints.size() + ": §6(" +
                pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"), false);
        }

        // 显示当前状态和选项
        if (!visualMode) {
            EasyAddUI.showPointRecordedScreen(recordedPoints, pos);
        }

        // 更新边界可视化的临时顶点
        areahint.boundviz.BoundVizManager.getInstance().setTemporaryVertices(recordedPoints, true);

        ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
            "记录坐标点: " + pos + ", 总计: " + recordedPoints.size());
    }
    
    /**
     * 完成坐标记录，进入高度选择阶段
     */
    public void finishPointRecording() {
        if (currentState != EasyAddState.RECORDING_POINTS) {
            return;
        }
        
        if (recordedPoints.size() < 3) {
            if (!visualMode) {
                MinecraftClient.getInstance().player.sendMessage(
                    Text.of(I18nManager.translate("easyadd.error.record")), false);
            }
            return;
        }
        
        // 清除临时顶点（记录完成后不再显示）
        areahint.boundviz.BoundVizManager.getInstance().clearTemporaryVertices();

        // 进入高度选择状态
        currentState = EasyAddState.HEIGHT_SELECTION;

        // 开始高度选择流程
        EasyAddAltitudeManager.startAltitudeSelection(recordedPoints);
    }
    
    /**
     * 继续高度选择后的流程
     * @param altitudeData 高度数据，null表示使用自动计算
     */
    public void proceedWithAltitudeData(AreaData.AltitudeData altitudeData) {
        if (currentState != EasyAddState.HEIGHT_SELECTION) {
            return;
        }
        
        // 保存高度数据
        customAltitudeData = altitudeData;
        
        // 进入颜色选择状态（新增）
        currentState = EasyAddState.COLOR_SELECTION;
        
        // 显示颜色选择界面
        if (visualMode) {
            EasyAddVisualController.showColorScreen();
        } else {
            EasyAddUI.showColorSelectionScreen();
        }
    }
    
    /**
     * 处理颜色选择后的流程
     * @param selectedColor 选择的颜色
     */
    public void proceedWithColorSelection(String selectedColor) {
        if (currentState != EasyAddState.COLOR_SELECTION && currentState != EasyAddState.COLOR_INPUT) {
            return;
        }
        
        // 保存选择的颜色
        this.selectedColor = selectedColor;
        
        // 进入确认保存状态
        currentState = EasyAddState.CONFIRM_SAVE;
        
        // 计算二级顶点和其他数据
        try {
            AreaData areaData = buildAreaData();
            previewAreaData = areaData;
            
            // 验证域名有效性
            if (validateAreaData(areaData)) {
                if (visualMode) {
                    EasyAddVisualController.showConfirmScreen(areaData);
                } else {
                    EasyAddUI.showConfirmSaveScreen(areaData);
                }
            } else {
                if (!visualMode) {
                    MinecraftClient.getInstance().player.sendMessage(
                        Text.of(I18nManager.translate("easyadd.error.area.coordinate")), false);
                }
                cancelEasyAdd();
            }
        } catch (Exception e) {
            if (!visualMode) {
                MinecraftClient.getInstance().player.sendMessage(
                    Text.of(I18nManager.translate("easyadd.error.area_2") + e.getMessage()), false);
            }
                cancelEasyAdd();
        }
    }
    
    /**
     * 处理颜色选择命令
     * @param colorInput 颜色输入
     */
    public void handleColorSelection(String colorInput) {
        if (currentState != EasyAddState.COLOR_SELECTION) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // 处理自定义颜色输入
        if ("custom".equals(colorInput)) {
            currentState = EasyAddState.COLOR_INPUT;
            if (visualMode) {
                EasyAddVisualController.showCustomColorScreen(null);
            } else {
                client.player.sendMessage(Text.of(I18nManager.translate("easyadd.prompt.color")), false);
                client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.general_9")), false);
                client.player.sendMessage(Text.of(I18nManager.translate("command.error.cancel")), false);
            }
            return;
        }
        
        // 验证颜色格式
        String normalizedColor = normalizeStrictColor(colorInput);
        if (normalizedColor == null) {
            if (visualMode) {
                EasyAddVisualController.showColorScreen();
            } else {
                client.player.sendMessage(Text.of(I18nManager.translate("gui.error.color")), false);
            }
            return;
        }
        
        // 处理颜色选择
        proceedWithColorSelection(normalizedColor);
    }
    
    /**
     * 处理自定义颜色输入
     * @param colorInput 用户输入的颜色
     */
    private void handleCustomColorInput(String colorInput) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // 验证颜色格式
        String normalizedColor = normalizeStrictColor(colorInput);
        if (normalizedColor == null) {
            client.player.sendMessage(Text.of(I18nManager.translate("easyadd.error.color")), false);
            client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.general_10")), false);
            return;
        }
        
        // 处理颜色选择
        proceedWithColorSelection(normalizedColor);
    }
    
    /**
     * 构建AreaData对象
     */
    private AreaData buildAreaData() {
        // 计算二级顶点（AABB包围盒）
        List<AreaData.Vertex> secondVertices = EasyAddGeometry.calculateBoundingBox(recordedPoints);
        
        // 转换一级顶点
        List<AreaData.Vertex> vertices = new ArrayList<>();
        for (BlockPos pos : recordedPoints) {
            vertices.add(new AreaData.Vertex(pos.getX(), pos.getZ()));
        }
        
        // 选择高度数据：自定义优先，否则自动计算
        AreaData.AltitudeData altitude;
        if (customAltitudeData != null) {
            altitude = customAltitudeData;
        } else {
            altitude = EasyAddGeometry.calculateAltitudeRange(recordedPoints);
        }
        
        // 获取玩家名字作为签名
        String signature = MinecraftClient.getInstance().player.getName().getString();
        
        return new AreaData(areaName, vertices, secondVertices, altitude, areaLevel, baseName, signature, selectedColor, surfaceName);
    }
    
    /**
     * 验证域名数据的有效性
     */
    private boolean validateAreaData(AreaData areaData) {
        if (baseName != null) {
            // 查找上级域名
            AreaData parentArea = findParentArea();
            if (parentArea != null) {
                return EasyAddGeometry.validateAreaInParent(areaData, parentArea);
            } else {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(Text.of(I18nManager.translate("easyadd.error.area.parent") + baseName), false);
                }
                return false;
            }
        }
        return true; // 顶级域名无需验证
    }
    
    /**
     * 查找上级域名
     */
    private AreaData findParentArea() {
        for (AreaData area : availableParentAreas) {
            if (area.getName().equals(baseName)) {
                return area;
            }
        }
        return null;
    }
    
    /**
     * 确认保存域名
     * @return 数据是否已提交发送并结束本次创建流程，不表示服务端已保存成功
     */
    public boolean confirmSave() {
        if (currentState != EasyAddState.CONFIRM_SAVE) {
            return false;
        }
        
        try {
            AreaData areaData = buildAreaData();
            
            // 发送失败时保留确认状态和已记录的域名数据，避免误报成功并清空本次创建内容。
            if (!EasyAddNetworking.sendAreaDataToServer(areaData, currentDimension)) {
                return false;
            }
            
            MinecraftClient.getInstance().player.sendMessage(
                Text.of(I18nManager.translate("easyadd.message.area_3") + areaName + I18nManager.translate("easyadd.message.general")), false);
            
            resetState();
            return true;
            
        } catch (Exception e) {
            MinecraftClient.getInstance().player.sendMessage(
                Text.of(I18nManager.translate("easyadd.error.area.save") + e.getMessage()), false);
            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD, 
                "保存失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 取消EasyAdd流程
     */
    public void cancelEasyAdd() {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.of(I18nManager.translate("easyadd.message.cancel")), false);
        }
        resetState();
    }
    
    /**
     * 重置状态
     */
    private void resetState() {
        currentState = EasyAddState.IDLE;
        areaName = null;
        surfaceName = null;
        areaLevel = 1;
        baseName = null;
        recordedPoints.clear();
        currentDimension = null;
        availableParentAreas.clear();
        customAltitudeData = null;
        selectedColor = "#FFFFFF"; // 重置颜色
        previewAreaData = null;
        visualMode = false;
        EasyAddVisualController.clear();

        // 清除边界可视化的临时顶点
        areahint.boundviz.BoundVizManager.getInstance().clearTemporaryVertices();

        // 重置高度管理器
        EasyAddAltitudeManager.reset();
    }
    
    /**
     * 检查域名名称是否已存在于当前维度
     * 注意：只检查域名名称（name字段），不检查联合域名（surfacename字段）
     * @param areaName 要检查的域名名称
     * @return 如果域名名称已存在返回true，否则返回false
     */
    private boolean checkAreaNameExists(String areaName) {
        try {
            String fileName = getFileNameForCurrentDimension();
            if (fileName == null) {
                ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
                    "无法确定当前维度文件名，跳过查重");
                return false;
            }

            // 读取当前维度的所有域名数据
            List<AreaData> existingAreas = FileManager.readAreaData(
                areahint.world.ClientWorldFolderManager.getWorldDimensionFile(fileName));

            // 检查是否存在相同的域名名称（name字段）
            for (AreaData area : existingAreas) {
                if (area.getName().equals(areaName)) {
                    ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
                        "发现重复域名名称: " + areaName);
                    return true;
                }
            }

            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
                "域名名称 \"" + areaName + "\" 未重复，可以使用");
            return false;

        } catch (Exception e) {
            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
                "检查域名名称时发生错误: " + e.getMessage());
            // 发生错误时，为了安全起见，允许继续（返回false）
            // 服务端还会再次检查
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
    public EasyAddState getCurrentState() { return currentState; }
    public String getAreaName() { return areaName; }
    public int getAreaLevel() { return areaLevel; }
    public String getBaseName() { return baseName; }
    public List<BlockPos> getRecordedPoints() { return new ArrayList<>(recordedPoints); }
    public String getCurrentDimension() { return currentDimension; }
    public String getSelectedColor() { return selectedColor; }
    public String getSurfaceName() { return surfaceName; }
    public List<AreaData> getAvailableParentAreas() { return new ArrayList<>(availableParentAreas); }
    public AreaData.AltitudeData getCustomAltitudeData() { return customAltitudeData; }
    public AreaData getPreviewAreaData() { return previewAreaData; }
    public boolean isVisualMode() { return visualMode; }

    /**
     * 严格校验颜色，避免无效输入被 ColorUtil.normalizeColor 静默转换为白色。
     */
    private String normalizeStrictColor(String colorInput) {
        if (colorInput == null || colorInput.trim().isEmpty()) {
            return null;
        }
        String trimmed = colorInput.trim();
        if (areahint.util.ColorUtil.isFlashColor(trimmed)) {
            return trimmed;
        }
        String namedColor = areahint.util.ColorUtil.getColorHex(trimmed);
        if (namedColor != null) {
            return namedColor;
        }
        String normalized = trimmed.toUpperCase();
        if (!normalized.startsWith("#")) {
            normalized = "#" + normalized;
        }
        return normalized.matches("^#[0-9A-F]{6}$") ? normalized : null;
    }
}
