package areahint.shrinkarea;

import areahint.commandui.CommandVisualLaunchContext;
import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.i18n.I18nManager;
import areahint.util.AreaDataConverter;
import areahint.shrinkarea.ShrinkAreaClientNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * 域名收缩管理器
 *
 * 负责整个域名收缩流程的管理和协调，是 ExpandAreaManager 的反义版本
 *
 * === 功能对比 ===
 *
 * ExpandAreaManager（扩展）:
 * - 在原域名外部添加新区域，扩大域名范围
 * - 删除内部顶点，保留外部顶点
 *
 * ShrinkAreaManager（收缩）:
 * - 在原域名内部标记收缩区域，缩小域名范围
 * - 删除外部顶点，保留内部顶点
 *
 * === 权限规则（与 expandarea 一致）===
 *
 * 1. 管理员（权限等级2）：可以收缩所有域名
 * 2. 普通玩家：
 *    - 可以收缩自己创建的域名（signature 等于玩家名）
 *    - 可以收缩 basename 引用自己的域名（basename 对应域名的 signature 等于玩家名）
 *
 * === 工作流程 ===
 *
 * 1. 启动收缩流程 (start)
 * 2. 加载可收缩的域名列表
 * 3. 玩家选择要收缩的域名
 * 4. 记录收缩区域的顶点（按记录键）
 * 5. 完成记录并计算收缩后的域名
 * 6. 发送到服务端保存并同步
 */
public class ShrinkAreaManager {
    private static ShrinkAreaManager instance;
    
    // 状态管理
    private boolean isActive = false;
    private ShrinkState currentState = ShrinkState.IDLE;
    
    // 域名相关
    private List<AreaData> availableAreas = new ArrayList<>();
    private AreaData selectedArea = null;
    private String selectedDimensionId = null;
    private String playerName = "";
    private boolean isAdmin = false;
    
    // 收缩区域顶点
    private List<AreaData.Vertex> shrinkVertices = new ArrayList<>();
    private boolean isRecording = false;
    
    // UI管理
    private ShrinkAreaUI ui;
    
    /**
     * 状态枚举
     */
    public enum ShrinkState {
        IDLE,           // 空闲状态
        SELECTING_AREA, // 选择域名状态
        RECORDING,      // 记录顶点状态
        CALCULATING,    // 计算状态
        CONFIRMING      // 确认状态
    }
    
    private ShrinkAreaManager() {
        this.ui = new ShrinkAreaUI(this);
    }
    
    public static ShrinkAreaManager getInstance() {
        if (instance == null) {
            instance = new ShrinkAreaManager();
        }
        return instance;
    }
    
    /**
     * 启动域名收缩流程
     */
    public void start() {
        if (isActive) {
            sendMessage(I18nManager.translate("shrinkarea.error.area.shrink"), Formatting.RED);
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        
        if (player == null) {
            sendMessage(I18nManager.translate("shrinkarea.error.general"), Formatting.RED);
            return;
        }
        
        playerName = player.getGameProfile().getName();
        
        // 检查玩家权限（简单检查是否为OP，真实权限由服务端验证）
        isAdmin = client.player.hasPermissionLevel(2);
        
        isActive = true;
        currentState = ShrinkState.SELECTING_AREA;
        
        sendMessage(I18nManager.translate("shrinkarea.message.area.shrink.start"), Formatting.GREEN);
        sendMessage(I18nManager.translate("shrinkarea.message.area"), Formatting.YELLOW);
        
        // 加载可用域名
        loadAvailableAreas();
        
        // 显示域名选择界面
        ui.showAreaSelectionScreen();
    }

    /**
     * 启动图形选择流程，只初始化状态并返回可收缩域名。
     */
    public List<AreaData> beginVisualSelection() {
        if (isActive) {
            sendMessage(I18nManager.translate("shrinkarea.error.area.shrink"), Formatting.RED);
            return List.of();
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            sendMessage(I18nManager.translate("shrinkarea.error.general"), Formatting.RED);
            return List.of();
        }

        playerName = player.getGameProfile().getName();
        isAdmin = client.player.hasPermissionLevel(2);
        isActive = true;
        currentState = ShrinkState.SELECTING_AREA;

        sendMessage(I18nManager.translate("shrinkarea.message.area.shrink.start"), Formatting.GREEN);
        loadAvailableAreas();
        return List.copyOf(availableAreas);
    }
    
    /**
     * 停止域名收缩流程
     */
    public void stop() {
        if (!isActive) {
            return;
        }
        
        isActive = false;
        currentState = ShrinkState.IDLE;
        
        // 清理状态
        reset();
        
        ui.showCancelMessage();
    }
    
    /**
     * 根据域名名称选择域名
     */
    public void selectAreaByName(String areaName) {
        if (areaName == null || areaName.trim().isEmpty()) {
            sendMessage(I18nManager.translate("dividearea.error.area"), Formatting.RED);
            return;
        }

        // 移除引号（如果存在）
        String cleanedName = areaName.trim();
        if (cleanedName.startsWith("\"") && cleanedName.endsWith("\"") && cleanedName.length() > 1) {
            cleanedName = cleanedName.substring(1, cleanedName.length() - 1);
        }

        // 从已加载的可用域名列表中查找域名（而不是重新从文件加载）
        AreaData area = null;
        for (AreaData a : availableAreas) {
            if (a.getName().equals(cleanedName)) {
                area = a;
                break;
            }
        }

        if (area == null) {
            sendMessage("§c" + I18nManager.translate("addhint.message.area_2") + cleanedName + I18nManager.translate("shrinkarea.message.shrink.permission"), Formatting.RED);
            sendMessage(I18nManager.translate("shrinkarea.prompt.area.shrink.permission"), Formatting.GRAY);
            return;
        }

        // 选择该域名（权限已在loadAvailableAreas中检查）
        handleAreaSelection(area);
    }

    /**
     * 处理域名选择
     */
    public void handleAreaSelection(AreaData selectedArea) {
        this.selectedArea = selectedArea;
        MinecraftClient client = MinecraftClient.getInstance();
        // 锁定选择域名时的维度，避免录点期间切换维度后把几何结果提交给新维度的同名域名。
        this.selectedDimensionId = client.world == null ? null
            : client.world.getRegistryKey().getValue().toString();

        sendMessage(I18nManager.translate("dividearea.prompt.area") + AreaDataConverter.getDisplayName(selectedArea), Formatting.GREEN);
        sendMessage(I18nManager.translate("addhint.message.general_2") + areahint.keyhandler.UnifiedKeyHandler.getRecordKeyDisplayName() + I18nManager.translate("shrinkarea.message.vertex.record.shrink"), Formatting.YELLOW);
        sendMessage(I18nManager.translate("shrinkarea.button.area.record.save"), Formatting.GRAY);

        // 开始记录模式
        startRecording();
    }

    /**
     * 开始记录顶点
     */
    private void startRecording() {
        this.isActive = true;
        this.isRecording = true;
        this.shrinkVertices.clear();
    }

    /**
     * 检查玩家是否有权限修改指定域名
     */
    private boolean checkPermission(AreaData area) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }

        String playerName = client.player.getGameProfile().getName();

        // 检查是否为管理员（权限等级2）
        if (client.player.hasPermissionLevel(2)) {
            return true;
        }

        // 收缩权限与服务端保持一致：普通玩家需要签名在上级域名上
        if (area.getBaseName() != null) {
            AreaData baseArea = findAreaByName(area.getBaseName());
            return baseArea != null && baseArea.hasSignature(playerName);
        }

        return false;
    }

    /**
     * 根据名称查找域名
     */
    private AreaData findAreaByName(String name) {
        List<AreaData> areas = loadAllAreas();
        for (AreaData area : areas) {
            if (area.getName().equals(name)) {
                return area;
            }
        }
        return null;
    }

    /**
     * 加载当前维度的所有域名
     */
    private List<AreaData> loadAllAreas() {
        List<AreaData> areas = new ArrayList<>();
        try {
            // 获取当前维度
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                String currentDimension = client.world.getRegistryKey().getValue().toString();
                String fileName = getFileNameForCurrentDimension(currentDimension);

                if (fileName != null) {
                    java.nio.file.Path areaPath = areahint.world.ClientWorldFolderManager.getWorldDimensionFile(fileName);
                    System.out.println("DEBUG: 尝试加载域名文件: " + areaPath);

                    if (areaPath.toFile().exists()) {
                        areas = FileManager.readAreaData(areaPath);
                        System.out.println("DEBUG: 从 " + fileName + " 加载了 " + areas.size() + " 个域名");
                    } else {
                        System.out.println("DEBUG: 文件不存在: " + fileName);
                    }
                } else {
                    System.out.println("DEBUG: 无法确定当前维度的文件名");
                }
            } else {
                System.out.println("DEBUG: 无法获取当前世界信息");
            }
        } catch (Exception e) {
            System.out.println("DEBUG: 加载域名文件失败: " + e.getMessage());
            e.printStackTrace();
        }
        return areas;
    }

    /**
     * 根据维度ID获取对应的文件名
     */
    private String getFileNameForCurrentDimension(String dimensionId) {
        if (dimensionId == null) return null;

        if (dimensionId.contains("overworld")) {
            return areahint.Areashint.OVERWORLD_FILE;
        } else if (dimensionId.contains("nether")) {
            return areahint.Areashint.NETHER_FILE;
        } else if (dimensionId.contains("end")) {
            return areahint.Areashint.END_FILE;
        }
        return null;
    }
    
    /**
     * 重置管理器状态
     */
    public void reset() {
        this.selectedArea = null;
        this.selectedDimensionId = null;
        this.shrinkVertices.clear();
        this.isRecording = false;
        this.isActive = false;  // 重置活动状态
        this.availableAreas.clear();

        // 清除边界可视化的临时顶点
        areahint.boundviz.BoundVizManager.getInstance().clearTemporaryVertices();
    }
    
    /**
     * 加载可用域名
     * 按照提示词：
     * - 如果是管理员（权限等级为2），列出所有域名
     * - 如果是普通玩家，列出被basename引用的玩家的域名（即signature等于玩家名的域名）
     */
    private void loadAvailableAreas() {
        try {
            // 使用 loadAllAreas 方法加载所有域名
            List<AreaData> allAreas = loadAllAreas();

            availableAreas.clear();

            for (AreaData area : allAreas) {
                // 如果是管理员，显示所有域名
                if (isAdmin) {
                    availableAreas.add(area);
                } else {
                    // 收缩权限与服务端保持一致：普通玩家只能收缩上级域名由自己签名的域名
                    if (area.getBaseName() != null) {
                        AreaData baseArea = findAreaByNameInList(area.getBaseName(), allAreas);
                        if (baseArea != null && baseArea.hasSignature(playerName)) {
                            availableAreas.add(area);
                        }
                    }
                }
            }

            // LuckPerms 可单独授权地图所选域名，客户端仅补入这一个已由服务端确认的目标。
            List<AreaData> launchAreas = CommandVisualLaunchContext.ensureTargetIncluded(
                "shrinkarea", availableAreas, allAreas);
            availableAreas.clear();
            availableAreas.addAll(launchAreas);

            if (availableAreas.isEmpty()) {
                sendMessage(I18nManager.translate("shrinkarea.error.area.shrink_7"), Formatting.RED);
                sendMessage(I18nManager.translate("shrinkarea.message.area.shrink_4"), Formatting.YELLOW);
                stop();
                return;
            }

            sendMessage(I18nManager.translate("shrinkarea.message.general_2") + availableAreas.size() + I18nManager.translate("shrinkarea.message.area.shrink"), Formatting.GREEN);

        } catch (Exception e) {
            sendMessage(I18nManager.translate("shrinkarea.error.area") + e.getMessage(), Formatting.RED);
            e.printStackTrace();
            stop();
        }
    }

    /**
     * 在指定列表中查找域名（辅助方法）
     */
    private AreaData findAreaByNameInList(String name, List<AreaData> areas) {
        for (AreaData area : areas) {
            if (area.getName().equals(name)) {
                return area;
            }
        }
        return null;
    }


    /**
     * 处理X键按下事件
     */
    public void handleXKeyPress() {
        if (!isActive || !isRecording) {
            return;
        }

        // 直接记录当前位置
        recordCurrentPosition();
    }

    /**
     * 记录当前位置为顶点
     */
    public void recordCurrentPosition() {
        if (!isRecording) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) {
            return;
        }

        BlockPos pos = player.getBlockPos();
        AreaData.Vertex vertex = new AreaData.Vertex(pos.getX(), pos.getZ());
        shrinkVertices.add(vertex);

        sendMessage(I18nManager.translate("shrinkarea.message.vertex.record") + shrinkVertices.size() + ": §6(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")", Formatting.GREEN);

        // 更新边界可视化的临时顶点
        List<BlockPos> blockPosList = new java.util.ArrayList<>();
        for (AreaData.Vertex v : shrinkVertices) {
            blockPosList.add(new BlockPos((int)v.getX(), pos.getY(), (int)v.getZ()));
        }
        areahint.boundviz.BoundVizManager.getInstance().setTemporaryVertices(blockPosList, true);

        // 显示选项按钮（参考EasyAdd的实现）
        ui.showPointRecordedOptions(shrinkVertices.size());
    }
    
    /**
     * 继续记录更多顶点
     */
    public void continueRecording() {
        if (!isRecording) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        sendMessage(I18nManager.translate("expandarea.message.vertex.record.continue") + areahint.keyhandler.UnifiedKeyHandler.getRecordKeyDisplayName() + I18nManager.translate("expandarea.message.record"), Formatting.GREEN);
    }
    
    /**
     * 完成记录并保存域名收缩
     */
    public void finishAndSave() {
        if (!isRecording || !isActive) {
            return;
        }

        if (shrinkVertices.size() < 3) {
            sendMessage(I18nManager.translate("expandarea.error.vertex.record"), Formatting.RED);
            return;
        }

        // 停止记录模式
        this.isRecording = false;

        // 使用现有的处理逻辑
        try {
            processAreaShrinking();
        } catch (Exception e) {
            sendMessage(I18nManager.translate("shrinkarea.error.area.shrink_4") + e.getMessage(), Formatting.RED);
            e.printStackTrace();
        }
    }
    
    /**
     * 处理域名收缩的核心逻辑
     */
    private void processAreaShrinking() {
        sendMessage(I18nManager.translate("shrinkarea.message.area.shrink_5"), Formatting.YELLOW);

        try {
            // 1. 高度验证
            if (!validateHeights()) {
                return;
            }

            // 2. 几何计算和顶点处理
            ShrinkGeometryCalculator calculator = new ShrinkGeometryCalculator(selectedArea, shrinkVertices);
            AreaData shrunkArea = calculator.shrinkArea();

            if (shrunkArea == null) {
                sendMessage(I18nManager.translate("shrinkarea.error.area.shrink_5"), Formatting.RED);
                return;
            }

            // 3. 获取当前维度信息
            MinecraftClient client = MinecraftClient.getInstance();
            if (selectedDimensionId == null) {
                sendMessage(I18nManager.translate("dividearea.error.dimension"), Formatting.RED);
                return;
            }

            // 4. 发送给服务端
            ShrinkAreaClientNetworking.sendShrunkAreaToServer(shrunkArea, selectedDimensionId);

            sendMessage(I18nManager.translate("shrinkarea.message.area.finish.shrink"), Formatting.GREEN);

        } catch (Exception e) {
            sendMessage(I18nManager.translate("shrinkarea.error.area.shrink_3") + e.getMessage(), Formatting.RED);
            e.printStackTrace();
        } finally {
            // 重置状态
            reset();
        }
    }
    
    /**
     * 验证新收缩区域的高度
     */
    private boolean validateHeights() {
        if (selectedArea == null || selectedArea.getAltitude() == null) {
            sendMessage(I18nManager.translate("shrinkarea.error.area.altitude"), Formatting.RED);
            return false;
        }
        
        // 收缩区域继承原域名的高度设置
        return true;
    }
    
    /**
     * 发送消息给玩家
     */
    private void sendMessage(String message, Formatting color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message).formatted(color), false);
        }
    }

    // Getters
    public boolean isActive() { return isActive; }
    public ShrinkState getCurrentState() { return currentState; }
    public List<AreaData> getAvailableAreas() { return availableAreas; }
    public AreaData getSelectedArea() { return selectedArea; }
    public List<AreaData.Vertex> getShrinkVertices() { return shrinkVertices; }
    public boolean isRecording() { return isRecording; }
    public boolean isAdmin() { return isAdmin; }
    public String getPlayerName() { return playerName; }
}
