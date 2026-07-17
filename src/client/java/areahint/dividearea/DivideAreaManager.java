package areahint.dividearea;

import areahint.commandui.CommandVisualLaunchContext;
import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;

public class DivideAreaManager {
    public enum State {
        IDLE, SELECTING_AREA, RECORDING_POINTS,
        AREA1_NAME, AREA1_SURFACE_NAME, AREA1_LEVEL, AREA1_BASE, AREA1_COLOR,
        AREA2_NAME, AREA2_SURFACE_NAME, AREA2_LEVEL, AREA2_BASE, AREA2_COLOR,
        SAVING
    }

    private static DivideAreaManager instance;
    private MinecraftClient client;
    private DivideAreaUI ui;
    private State state = State.IDLE;
    private AreaData selectedArea;
    private String selectedDimensionId;
    private List<Double[]> newVertices = new ArrayList<>();
    private boolean isActive = false;
    private boolean isRecording = false;
    private boolean chatListenerRegistered = false;

    // 分割后的两组顶点
    private List<Double[]> area1Vertices;
    private List<Double[]> area2Vertices;

    // 两个新域名的配置
    private AreaData area1Config;
    private AreaData area2Config;

    public static DivideAreaManager getInstance() {
        if (instance == null) instance = new DivideAreaManager();
        return instance;
    }

    private DivideAreaManager() {
        this.client = MinecraftClient.getInstance();
        this.ui = new DivideAreaUI(this);
    }

    private void registerChatListener() {
        if (!chatListenerRegistered) {
            ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
                if (state == State.AREA1_NAME || state == State.AREA1_SURFACE_NAME
                    || state == State.AREA2_NAME || state == State.AREA2_SURFACE_NAME) {
                    handleChatInput(message.getString());
                }
            });
            chatListenerRegistered = true;
        }
    }

    private void handleChatInput(String input) {
        if (client.player == null) return;
        // 去掉 <玩家名> 前缀
        if (input.startsWith("<") && input.contains(">")) {
            int end = input.indexOf(">") + 1;
            if (end < input.length()) input = input.substring(end).trim();
        }
        if (input.trim().isEmpty()) return;

        switch (state) {
            case AREA1_NAME:
                if (checkAreaNameExists(input.trim())) {
                    sendMsg(I18nManager.translate("dividearea.error.area.name") + "\"" + input.trim() + "\"" + I18nManager.translate("dividearea.prompt.general"), Formatting.RED);
                    return;
                }
                area1Config.setName(input.trim());
                sendMsg(I18nManager.translate("dividearea.message.name") + input.trim(), Formatting.GREEN);
                state = State.AREA1_SURFACE_NAME;
                sendMsg(I18nManager.translate("dividearea.title.area.surface"), Formatting.GOLD);
                sendMsg(I18nManager.translate("dividearea.prompt.area.surface"), Formatting.GREEN);
                sendMsg(I18nManager.translate("dividearea.message.area.surface.name"), Formatting.GRAY);
                sendMsg(I18nManager.translate("dividearea.prompt.area.surface_3"), Formatting.YELLOW);
                showCancelButton();
                break;
            case AREA1_SURFACE_NAME:
                if (!"skip".equalsIgnoreCase(input.trim())) {
                    area1Config.setSurfacename(input.trim());
                    sendMsg(I18nManager.translate("dividearea.message.area.surface_2") + input.trim(), Formatting.GREEN);
                } else {
                    sendMsg(I18nManager.translate("dividearea.message.area.surface"), Formatting.GRAY);
                }
                state = State.AREA1_COLOR;
                showColorSelection(1);
                break;
            case AREA2_NAME:
                if (checkAreaNameExists(input.trim())) {
                    sendMsg(I18nManager.translate("dividearea.error.area.name") + "\"" + input.trim() + "\"" + I18nManager.translate("dividearea.prompt.general"), Formatting.RED);
                    return;
                }
                area2Config.setName(input.trim());
                sendMsg(I18nManager.translate("dividearea.message.name_2") + input.trim(), Formatting.GREEN);
                state = State.AREA2_SURFACE_NAME;
                sendMsg(I18nManager.translate("dividearea.title.area.surface"), Formatting.GOLD);
                sendMsg(I18nManager.translate("dividearea.prompt.area.surface_2"), Formatting.GREEN);
                sendMsg(I18nManager.translate("dividearea.message.area.surface.name"), Formatting.GRAY);
                sendMsg(I18nManager.translate("dividearea.prompt.area.surface_3"), Formatting.YELLOW);
                showCancelButton();
                break;
            case AREA2_SURFACE_NAME:
                if (!"skip".equalsIgnoreCase(input.trim())) {
                    area2Config.setSurfacename(input.trim());
                    sendMsg(I18nManager.translate("dividearea.message.area.surface_3") + input.trim(), Formatting.GREEN);
                } else {
                    sendMsg(I18nManager.translate("dividearea.message.area.surface"), Formatting.GRAY);
                }
                state = State.AREA2_COLOR;
                showColorSelection(2);
                break;
            default:
                break;
        }
    }

    private boolean checkAreaNameExists(String name) {
        for (AreaData a : loadAllAreas())
            if (a.getName().equals(name)) return true;
        return false;
    }

    public boolean isActive() { return isActive; }
    public boolean isRecording() { return isRecording; }
    public State getState() { return state; }
    public AreaData getSelectedArea() { return selectedArea; }
    public List<Double[]> getNewVertices() { return newVertices; }

    // ===== 第1阶段：启动和域名选择 =====

    public void start() {
        if (client.player == null) return;
        registerChatListener();
        isActive = true;
        state = State.SELECTING_AREA;
        List<AreaData> modifiableAreas = getModifiableAreas();
        if (modifiableAreas.isEmpty()) {
            sendMsg(I18nManager.translate("dividearea.error.area.divide_3"), Formatting.RED);
            reset();
            return;
        }
        ui.showAreaSelection(modifiableAreas);
    }

    /**
     * 启动图形选择流程，只初始化状态并返回可分割域名。
     */
    public List<AreaData> beginVisualSelection() {
        if (client.player == null) {
            return List.of();
        }

        registerChatListener();
        isActive = true;
        state = State.SELECTING_AREA;
        List<AreaData> modifiableAreas = getModifiableAreas();
        if (modifiableAreas.isEmpty()) {
            sendMsg(I18nManager.translate("dividearea.error.area.divide_3"), Formatting.RED);
            reset();
        }
        return modifiableAreas;
    }

    public void selectAreaByName(String areaName) {
        if (areaName == null || areaName.trim().isEmpty()) {
            sendMsg(I18nManager.translate("dividearea.error.area"), Formatting.RED);
            return;
        }
        String cleaned = areaName.trim();
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() > 1)
            cleaned = cleaned.substring(1, cleaned.length() - 1);

        List<AreaData> modifiable = getModifiableAreas();
        AreaData area = null;
        for (AreaData a : modifiable) {
            if (a.getName().equals(cleaned)) { area = a; break; }
        }
        if (area == null) {
            sendMsg(I18nManager.translate("addhint.error.area") + cleaned + I18nManager.translate("addhint.message.permission"), Formatting.RED);
            return;
        }
        this.selectedArea = area;
        // 锁定选择域名时的维度，切维度后仍携带旧维度交由服务端二次校验并拒绝。
        this.selectedDimensionId = client.world == null ? null
            : client.world.getRegistryKey().getValue().toString();
        sendMsg(I18nManager.translate("dividearea.prompt.area") + areahint.util.AreaDataConverter.getDisplayName(area), Formatting.GREEN);
        sendMsg(I18nManager.translate("dividearea.prompt.general_3") + areahint.keyhandler.UnifiedKeyHandler.getRecordKeyDisplayName() + I18nManager.translate("dividearea.message.vertex.record.divide"), Formatting.YELLOW);
        state = State.RECORDING_POINTS;
        isRecording = true;
        newVertices.clear();
        ui.showRecordingInterface();
    }

    public void cancel() {
        if (!isActive) return;
        areahint.boundviz.BoundVizManager.getInstance().clearTemporaryVertices();
        ui.showCancelMessage();
        reset();
    }

    public void continueRecording() {
        if (!isRecording || client.player == null) return;
        sendMsg(I18nManager.translate("dividearea.message.record.continue") + areahint.keyhandler.UnifiedKeyHandler.getRecordKeyDisplayName() + I18nManager.translate("dividearea.message.record"), Formatting.GREEN);
    }

    // ===== 第2阶段：记录顶点 =====

    public void recordCurrentPosition() {
        if (!isRecording || client.player == null) return;
        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        newVertices.add(new Double[]{x, z});
        sendMsg(I18nManager.translate("dividearea.message.record_3") + newVertices.size() + ": §6(" + (int)Math.round(x) + ", " + String.format("%.1f", y) + ", " + (int)Math.round(z) + ")", Formatting.GREEN);

        List<net.minecraft.util.math.BlockPos> bpList = new ArrayList<>();
        for (Double[] v : newVertices)
            bpList.add(new net.minecraft.util.math.BlockPos(v[0].intValue(), (int) y, v[1].intValue()));
        areahint.boundviz.BoundVizManager.getInstance().setTemporaryVertices(bpList, true);
        ui.showPointRecordedOptions(newVertices.size());
    }

    // ===== 第3阶段：处理分割 =====

    public void finishAndSave() {
        if (!isRecording || client.player == null) return;
        if (newVertices.size() < 2) {
            sendMsg(I18nManager.translate("dividearea.error.vertex.record.divide"), Formatting.RED);
            return;
        }
        isRecording = false;
        areahint.boundviz.BoundVizManager.getInstance().clearTemporaryVertices();
        try {
            processDivision();
        } catch (Exception e) {
            sendMsg(I18nManager.translate("dividearea.error.area.divide") + e.getMessage(), Formatting.RED);
            e.printStackTrace();
            reset();
        }
    }

    private void processDivision() {
        // 1. 高度验证
        if (!validateAltitude()) return;

        // 2. 提取原域名顶点
        List<Double[]> origVerts = extractOriginalVertices();
        if (origVerts.size() < 3) {
            sendMsg(I18nManager.translate("dividearea.error.area.vertex"), Formatting.RED);
            reset(); return;
        }

        // 3. 过滤外部顶点，计算边界点
        List<Double[]> processedVerts = new ArrayList<>();
        List<Double[]> boundaryPoints = new ArrayList<>();
        processVerticesAndBoundary(origVerts, processedVerts, boundaryPoints);

        if (boundaryPoints.size() < 2) {
            sendMsg(I18nManager.translate("dividearea.error.area.divide.boundary"), Formatting.RED);
            reset(); return;
        }

        // 取前两个边界点
        Double[] bp1 = boundaryPoints.get(0);
        Double[] bp2 = boundaryPoints.get(1);

        // 4. 找到边界点在原域名边上的位置
        int bp1Edge = findEdgeIndex(bp1, origVerts);
        int bp2Edge = findEdgeIndex(bp2, origVerts);

        // 5. 将原域名顶点分成两组
        splitOriginalVertices(origVerts, bp1, bp2, bp1Edge, bp2Edge, processedVerts);

        if (area1Vertices == null || area1Vertices.size() < 3 || area2Vertices == null || area2Vertices.size() < 3) {
            sendMsg(I18nManager.translate("dividearea.error.vertex.divide"), Formatting.RED);
            reset(); return;
        }

        sendMsg(I18nManager.translate("dividearea.message.finish.divide") + area1Vertices.size() + I18nManager.translate("dividearea.message.vertex_2") + area2Vertices.size() + I18nManager.translate("dividearea.message.vertex"), Formatting.GREEN);

        // 7. 初始化两个域名配置（继承原域名属性）
        area1Config = createBaseConfig(area1Vertices);
        area2Config = createBaseConfig(area2Vertices);

        // 8. 开始配置区域1
        startArea1Config();
    }

    // ===== 第4阶段：EasyAdd式配置流程 =====

    private void startArea1Config() {
        state = State.AREA1_NAME;
        sendMsg(I18nManager.translate("dividearea.title.divide"), Formatting.GOLD);
        sendMsg(I18nManager.translate("dividearea.prompt.name"), Formatting.GREEN);
        sendMsg(I18nManager.translate("dividearea.message.general_2"), Formatting.GRAY);
        showCancelButton();
    }

    private void startArea2Config() {
        state = State.AREA2_NAME;
        sendMsg(I18nManager.translate("dividearea.title.divide_2"), Formatting.GOLD);
        sendMsg(I18nManager.translate("dividearea.prompt.name_2"), Formatting.GREEN);
        sendMsg(I18nManager.translate("dividearea.message.general_2"), Formatting.GRAY);
        showCancelButton();
    }

    private void showCancelButton() {
        if (client.player == null) return;
        net.minecraft.text.MutableText cancel = Text.literal(I18nManager.translate("addhint.error.cancel")).setStyle(net.minecraft.text.Style.EMPTY
            .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND, "/areahint dividearea cancel"))
            .withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.message.cancel.divide"))))
            .withColor(Formatting.RED));
        client.player.sendMessage(cancel, false);
    }

    public void handleNameInput(String name) {
        if (state == State.AREA1_NAME) {
            area1Config.setName(name);
            sendMsg(I18nManager.translate("dividearea.message.name") + name, Formatting.GREEN);
            state = State.AREA1_LEVEL;
            showLevelSelection(1);
        } else if (state == State.AREA2_NAME) {
            area2Config.setName(name);
            sendMsg(I18nManager.translate("dividearea.message.name_2") + name, Formatting.GREEN);
            state = State.AREA2_LEVEL;
            showLevelSelection(2);
        }
    }

    /**
     * 图形流程提交名称后继续进入表面域名输入。
     */
    public boolean handleNameInputForVisual(String name) {
        if (state == State.AREA1_NAME) {
            if (checkAreaNameExists(name.trim())) {
                sendMsg(I18nManager.translate("dividearea.error.area.name") + "\"" + name.trim() + "\"" + I18nManager.translate("dividearea.prompt.general"), Formatting.RED);
                return false;
            }
            area1Config.setName(name);
            sendMsg(I18nManager.translate("dividearea.message.name") + name, Formatting.GREEN);
            state = State.AREA1_SURFACE_NAME;
        } else if (state == State.AREA2_NAME) {
            if (checkAreaNameExists(name.trim())) {
                sendMsg(I18nManager.translate("dividearea.error.area.name") + "\"" + name.trim() + "\"" + I18nManager.translate("dividearea.prompt.general"), Formatting.RED);
                return false;
            }
            area2Config.setName(name);
            sendMsg(I18nManager.translate("dividearea.message.name_2") + name, Formatting.GREEN);
            state = State.AREA2_SURFACE_NAME;
        }
        return true;
    }

    /**
     * 图形流程提交分割后域名的表面域名，空值表示跳过。
     */
    public void handleSurfaceInput(String surfaceName) {
        String value = surfaceName == null ? "" : surfaceName.trim();
        if (state == State.AREA1_SURFACE_NAME) {
            if (!value.isEmpty()) {
                area1Config.setSurfacename(value);
                sendMsg(I18nManager.translate("dividearea.message.area.surface_2") + value, Formatting.GREEN);
            } else {
                sendMsg(I18nManager.translate("dividearea.message.area.surface"), Formatting.GRAY);
            }
            state = State.AREA1_LEVEL;
        } else if (state == State.AREA2_SURFACE_NAME) {
            if (!value.isEmpty()) {
                area2Config.setSurfacename(value);
                sendMsg(I18nManager.translate("dividearea.message.area.surface_3") + value, Formatting.GREEN);
            } else {
                sendMsg(I18nManager.translate("dividearea.message.area.surface"), Formatting.GRAY);
            }
            state = State.AREA2_LEVEL;
        }
    }

    /**
     * 图形流程提交等级，不输出旧聊天按钮。
     */
    public void handleLevelInputForVisual(int level) {
        if (state == State.AREA1_LEVEL) {
            area1Config.setLevel(level);
            sendMsg(I18nManager.translate("dividearea.message.level") + level, Formatting.GREEN);
            if (level > 1) {
                state = State.AREA1_BASE;
            } else {
                area1Config.setBaseName(null);
                state = State.AREA1_COLOR;
            }
        } else if (state == State.AREA2_LEVEL) {
            area2Config.setLevel(level);
            sendMsg(I18nManager.translate("dividearea.message.level_2") + level, Formatting.GREEN);
            if (level > 1) {
                state = State.AREA2_BASE;
            } else {
                area2Config.setBaseName(null);
                state = State.AREA2_COLOR;
            }
        }
    }

    /**
     * 图形流程提交上级域名，不输出旧聊天按钮。
     */
    public void handleBaseInputForVisual(String baseName) {
        if (state == State.AREA1_BASE) {
            area1Config.setBaseName("none".equals(baseName) ? null : baseName);
            sendMsg(I18nManager.translate("dividearea.message.area.parent_2") + (area1Config.getBaseName() == null ? I18nManager.translate("dividearea.message.general_3") : baseName), Formatting.GREEN);
            state = State.AREA1_COLOR;
        } else if (state == State.AREA2_BASE) {
            area2Config.setBaseName("none".equals(baseName) ? null : baseName);
            sendMsg(I18nManager.translate("dividearea.message.area.parent_3") + (area2Config.getBaseName() == null ? I18nManager.translate("dividearea.message.general_3") : baseName), Formatting.GREEN);
            state = State.AREA2_COLOR;
        }
    }

    /**
     * 图形流程提交颜色，区域1完成后直接进入区域2配置。
     */
    public void handleColorInputForVisual(String color) {
        if (state == State.AREA1_COLOR) {
            area1Config.setColor(color);
            sendMsg(I18nManager.translate("dividearea.message.color") + color, Formatting.GREEN);
            state = State.AREA2_NAME;
        } else if (state == State.AREA2_COLOR) {
            area2Config.setColor(color);
            sendMsg(I18nManager.translate("dividearea.message.color_2") + color, Formatting.GREEN);
            sendToServer();
        }
    }

    public int getCurrentBaseTargetLevel() {
        if (state == State.AREA1_BASE && area1Config != null) {
            return area1Config.getLevel() - 1;
        }
        if (state == State.AREA2_BASE && area2Config != null) {
            return area2Config.getLevel() - 1;
        }
        return -1;
    }

    public void handleLevelInput(int level) {
        if (state == State.AREA1_LEVEL) {
            area1Config.setLevel(level);
            sendMsg(I18nManager.translate("dividearea.message.level") + level, Formatting.GREEN);
            if (level > 1) {
                state = State.AREA1_BASE;
                showBaseSelection(1);
            } else {
                area1Config.setBaseName(null);
                state = State.AREA1_COLOR;
                showColorSelection(1);
            }
        } else if (state == State.AREA2_LEVEL) {
            area2Config.setLevel(level);
            sendMsg(I18nManager.translate("dividearea.message.level_2") + level, Formatting.GREEN);
            if (level > 1) {
                state = State.AREA2_BASE;
                showBaseSelection(2);
            } else {
                area2Config.setBaseName(null);
                state = State.AREA2_COLOR;
                showColorSelection(2);
            }
        }
    }

    public void handleBaseInput(String baseName) {
        if (state == State.AREA1_BASE) {
            area1Config.setBaseName("none".equals(baseName) ? null : baseName);
            sendMsg(I18nManager.translate("dividearea.message.area.parent_2") + (area1Config.getBaseName() == null ? I18nManager.translate("dividearea.message.general_3") : baseName), Formatting.GREEN);
            state = State.AREA1_COLOR;
            showColorSelection(1);
        } else if (state == State.AREA2_BASE) {
            area2Config.setBaseName("none".equals(baseName) ? null : baseName);
            sendMsg(I18nManager.translate("dividearea.message.area.parent_3") + (area2Config.getBaseName() == null ? I18nManager.translate("dividearea.message.general_3") : baseName), Formatting.GREEN);
            state = State.AREA2_COLOR;
            showColorSelection(2);
        }
    }

    public void handleColorInput(String color) {
        if (state == State.AREA1_COLOR) {
            area1Config.setColor(color);
            sendMsg(I18nManager.translate("dividearea.message.color") + color, Formatting.GREEN);
            // 区域1配置完成，开始配置区域2
            startArea2Config();
        } else if (state == State.AREA2_COLOR) {
            area2Config.setColor(color);
            sendMsg(I18nManager.translate("dividearea.message.color_2") + color, Formatting.GREEN);
            // 两个区域都配置完成，发送到服务端
            sendToServer();
        }
    }

    private void sendToServer() {
        state = State.SAVING;
        if (selectedDimensionId == null) {
            sendMsg(I18nManager.translate("dividearea.error.dimension"), Formatting.RED);
            reset(); return;
        }
        DivideAreaClientNetworking.sendDividedAreasToServer(
            area1Config, area2Config, selectedArea.getName(), selectedDimensionId);
        sendMsg(I18nManager.translate("dividearea.message.divide_3"), Formatting.GREEN);
        reset();
    }

    // ===== UI辅助方法 =====

    private void showLevelSelection(int areaNum) {
        if (client.player == null) return;
        sendMsg(I18nManager.translate("dividearea.prompt.general_2") + areaNum + I18nManager.translate("dividearea.message.level_3"), Formatting.GREEN);

        net.minecraft.text.MutableText l1 = Text.literal(I18nManager.translate("dividearea.button.area")).setStyle(net.minecraft.text.Style.EMPTY
            .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND, "/areahint dividearea level 1"))
            .withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.prompt.area_2"))))
            .withColor(Formatting.AQUA));
        net.minecraft.text.MutableText l2 = Text.literal(I18nManager.translate("dividearea.button.area_3")).setStyle(net.minecraft.text.Style.EMPTY
            .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND, "/areahint dividearea level 2"))
            .withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.prompt.area_3"))))
            .withColor(Formatting.YELLOW));
        net.minecraft.text.MutableText l3 = Text.literal(I18nManager.translate("dividearea.button.area_2")).setStyle(net.minecraft.text.Style.EMPTY
            .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND, "/areahint dividearea level 3"))
            .withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.prompt.area_4"))))
            .withColor(Formatting.LIGHT_PURPLE));
        net.minecraft.text.MutableText cancel = Text.literal(I18nManager.translate("addhint.error.cancel")).setStyle(net.minecraft.text.Style.EMPTY
            .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND, "/areahint dividearea cancel"))
            .withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.message.cancel.divide"))))
            .withColor(Formatting.RED));

        client.player.sendMessage(Text.empty().append(l1).append(Text.of("  ")).append(l2).append(Text.of("  ")).append(l3).append(Text.of("  ")).append(cancel), false);
        sendMsg("§7等级说明：1=顶级域名，2/3=次级域名", Formatting.GRAY);
    }

    private void showBaseSelection(int areaNum) {
        if (client.player == null) return;
        sendMsg(I18nManager.translate("dividearea.prompt.general_2") + areaNum + I18nManager.translate("dividearea.message.area.parent_4"), Formatting.GREEN);
        List<AreaData> allAreas = loadAllAreas();
        int targetLevel = (state == State.AREA1_BASE ? area1Config.getLevel() : area2Config.getLevel()) - 1;

        for (AreaData a : allAreas) {
            if (a.getLevel() == targetLevel) {
                String dn = areahint.util.AreaDataConverter.getDisplayName(a);
                net.minecraft.text.MutableText btn = Text.literal("§6[" + dn + "]").setStyle(net.minecraft.text.Style.EMPTY
                    .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND,
                        "/areahint dividearea base \"" + a.getName() + "\""))
                    .withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT,
                        Text.of(I18nManager.translate("addhint.prompt.general") + dn + I18nManager.translate("dividearea.message.area.parent"))))
                    .withColor(Formatting.GOLD));
                client.player.sendMessage(btn, false);
            }
        }

        net.minecraft.text.MutableText cancel = Text.literal(I18nManager.translate("addhint.error.cancel")).setStyle(net.minecraft.text.Style.EMPTY
            .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND, "/areahint dividearea cancel"))
            .withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.message.cancel.divide"))))
            .withColor(Formatting.RED));
        client.player.sendMessage(cancel, false);
    }

    private void showColorSelection(int areaNum) {
        if (client.player == null) return;
        sendMsg(I18nManager.translate("dividearea.title.general") + areaNum + I18nManager.translate("dividearea.title.color"), Formatting.GOLD);
        sendMsg(I18nManager.translate("dividearea.prompt.area.color"), Formatting.GREEN);

        net.minecraft.text.MutableText row1 = Text.empty()
            .append(colorBtn(I18nManager.translate("gui.message.general_24"), "#FFFFFF", "§f")).append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_23"), "#808080", "§7")).append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_18"), "#555555", "§8")).append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_31"), "#000000", "§0"));
        net.minecraft.text.MutableText row2 = Text.empty()
            .append(colorBtn(I18nManager.translate("gui.message.general_19"), "#AA0000", "§4")).append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_27"), "#FF0000", "§c")).append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_25"), "#FF55FF", "§d")).append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_17"), "#FFAA00", "§6"));
        net.minecraft.text.MutableText row3 = Text.empty()
            .append(colorBtn(I18nManager.translate("gui.message.general_30"), "#FFFF55", "§e")).append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_28"), "#55FF55", "§a")).append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_20"), "#00AA00", "§2")).append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_13"), "#55FFFF", "§b"));
        net.minecraft.text.MutableText row4 = Text.empty()
            .append(colorBtn(I18nManager.translate("gui.message.general_22"), "#00AAAA", "§3")).append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_29"), "#5555FF", "§9")).append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_21"), "#0000AA", "§1")).append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_26"), "#AA00AA", "§5"));
        net.minecraft.text.MutableText row5 = Text.empty()
            .append(colorBtn(I18nManager.translate("gui.message.general_15"), "FLASH_BW_ALL", "§7")).append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_14"), "FLASH_RAINBOW_ALL", "§b")).append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_12"), "FLASH_BW_CHAR", "§8")).append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_11"), "FLASH_RAINBOW_CHAR", "§d"));

        client.player.sendMessage(row1, false);
        client.player.sendMessage(row2, false);
        client.player.sendMessage(row3, false);
        client.player.sendMessage(row4, false);
        client.player.sendMessage(Text.of(""), false);
        sendMsg(I18nManager.translate("gui.message.general_4"), Formatting.GOLD);
        client.player.sendMessage(row5, false);
        client.player.sendMessage(Text.of(""), false);

        net.minecraft.text.MutableText cancel = Text.literal(I18nManager.translate("addhint.error.cancel")).setStyle(net.minecraft.text.Style.EMPTY
            .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND, "/areahint dividearea cancel"))
            .withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.message.cancel.divide"))))
            .withColor(Formatting.RED));
        client.player.sendMessage(cancel, false);
    }

    private net.minecraft.text.MutableText colorBtn(String name, String value, String mc) {
        return Text.literal(mc + "[" + name + "]").setStyle(net.minecraft.text.Style.EMPTY
            .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND, "/areahint dividearea color " + value))
            .withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("addhint.prompt.general") + name + I18nManager.translate("dividearea.message.area.color")))));
    }

    // ===== 几何算法 =====

    private boolean validateAltitude() {
        if (client.player == null || selectedArea.getAltitude() == null) return true;
        AreaData.AltitudeData alt = selectedArea.getAltitude();
        if (alt.getMax() == null || alt.getMin() == null) return true;

        double playerY = client.player.getY();
        double newMax = playerY + 10;
        double newMin = playerY - 10;

        if (newMax < alt.getMax() && newMin > alt.getMin()) {
            return true;
        } else {
            sendMsg(I18nManager.translate("dividearea.error.area.altitude_2"), Formatting.RED);
            sendMsg(I18nManager.translate("dividearea.error.area.altitude") + alt.getMin() + " ~ " + alt.getMax(), Formatting.RED);
            reset();
            return false;
        }
    }

    private List<Double[]> extractOriginalVertices() {
        List<Double[]> verts = new ArrayList<>();
        if (selectedArea.getVertices() == null) return verts;
        for (AreaData.Vertex v : selectedArea.getVertices())
            verts.add(new Double[]{v.getX(), v.getZ()});
        return verts;
    }

    private void processVerticesAndBoundary(List<Double[]> origVerts, List<Double[]> processedVerts, List<Double[]> boundaryPoints) {
        // 计算分割线与原域名边界的所有交点
        for (int i = 0; i < newVertices.size() - 1; i++) {
            Double[] p1 = newVertices.get(i);
            Double[] p2 = newVertices.get(i + 1);
            for (int j = 0; j < origVerts.size(); j++) {
                int k = (j + 1) % origVerts.size();
                Double[] inter = lineIntersection(p1, p2, origVerts.get(j), origVerts.get(k));
                if (inter != null) boundaryPoints.add(inter);
            }
        }

        // 保留在原域名内部的顶点
        for (Double[] v : newVertices) {
            if (isPointInPolygon(v, origVerts)) processedVerts.add(v);
        }

        // 如果边界点不足2个，从首尾点找最近边上的最近点作为边界点
        if (boundaryPoints.size() < 2) {
            Double[] first = newVertices.get(0);
            Double[] last = newVertices.get(newVertices.size() - 1);
            if (boundaryPoints.size() < 1) {
                boundaryPoints.add(findClosestPointOnPolygon(first, origVerts));
            }
            if (boundaryPoints.size() < 2) {
                boundaryPoints.add(findClosestPointOnPolygon(last, origVerts));
            }
        }
    }

    private Double[] findClosestPointOnPolygon(Double[] point, List<Double[]> polygon) {
        double minDist = Double.MAX_VALUE;
        Double[] best = null;
        for (int i = 0; i < polygon.size(); i++) {
            int j = (i + 1) % polygon.size();
            Double[] cp = closestPointOnSegment(point, polygon.get(i), polygon.get(j));
            double d = distance(point, cp);
            if (d < minDist) { minDist = d; best = cp; }
        }
        return best;
    }

    private void splitOriginalVertices(List<Double[]> origVerts, Double[] bp1, Double[] bp2,
                                        int bp1Edge, int bp2Edge, List<Double[]> innerVerts) {
        int n = origVerts.size();

        // 区域1: bp1 → 内部分割点 → bp2 → 原域名顶点(bp2Edge+1..bp1Edge) → 回到bp1
        area1Vertices = new ArrayList<>();
        area1Vertices.add(bp1);
        area1Vertices.addAll(innerVerts);
        area1Vertices.add(bp2);
        int idx = (bp2Edge + 1) % n;
        while (idx != (bp1Edge + 1) % n) {
            area1Vertices.add(origVerts.get(idx));
            idx = (idx + 1) % n;
        }

        // 区域2: bp2 → 内部分割点(反转) → bp1 → 原域名顶点(bp1Edge+1..bp2Edge) → 回到bp2
        area2Vertices = new ArrayList<>();
        area2Vertices.add(bp2);
        List<Double[]> reversedInner = new ArrayList<>(innerVerts);
        java.util.Collections.reverse(reversedInner);
        area2Vertices.addAll(reversedInner);
        area2Vertices.add(bp1);
        idx = (bp1Edge + 1) % n;
        while (idx != (bp2Edge + 1) % n) {
            area2Vertices.add(origVerts.get(idx));
            idx = (idx + 1) % n;
        }
    }

    private int findEdgeIndex(Double[] point, List<Double[]> polygon) {
        double minDist = Double.MAX_VALUE;
        int bestEdge = 0;
        for (int i = 0; i < polygon.size(); i++) {
            int j = (i + 1) % polygon.size();
            Double[] closest = closestPointOnSegment(point, polygon.get(i), polygon.get(j));
            double dist = distance(point, closest);
            if (dist < minDist) { minDist = dist; bestEdge = i; }
        }
        return bestEdge;
    }

    private AreaData createBaseConfig(List<Double[]> vertices) {
        List<AreaData.Vertex> vList = new ArrayList<>();
        for (Double[] v : vertices) vList.add(new AreaData.Vertex(Math.round(v[0]), Math.round(v[1])));

        List<AreaData.Vertex> sv = calculateAABB(vertices);

        AreaData config = new AreaData(
            "", vList, sv,
            selectedArea.getAltitude(),
            selectedArea.getLevel(),
            selectedArea.getBaseName(),
            client.player != null ? client.player.getGameProfile().getName() : selectedArea.getSignature(),
            selectedArea.getColor(),
            selectedArea.getSurfacename()
        );
        return config;
    }

    private List<AreaData.Vertex> calculateAABB(List<Double[]> vertices) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (Double[] v : vertices) {
            minX = Math.min(minX, v[0]); maxX = Math.max(maxX, v[0]);
            minZ = Math.min(minZ, v[1]); maxZ = Math.max(maxZ, v[1]);
        }
        List<AreaData.Vertex> sv = new ArrayList<>();
        sv.add(new AreaData.Vertex(Math.round(minX), Math.round(minZ)));
        sv.add(new AreaData.Vertex(Math.round(maxX), Math.round(minZ)));
        sv.add(new AreaData.Vertex(Math.round(maxX), Math.round(maxZ)));
        sv.add(new AreaData.Vertex(Math.round(minX), Math.round(maxZ)));
        return sv;
    }

    // ===== 几何工具方法 =====

    private boolean isPointInPolygon(Double[] point, List<Double[]> polygon) {
        if (polygon.size() < 3) return false;
        double x = point[0], y = point[1];
        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            double xi = polygon.get(i)[0], yi = polygon.get(i)[1];
            double xj = polygon.get(j)[0], yj = polygon.get(j)[1];
            if (((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi))
                inside = !inside;
        }
        return inside;
    }

    private Double[] lineIntersection(Double[] a, Double[] b, Double[] c, Double[] d) {
        double x1=a[0],y1=a[1], x2=b[0],y2=b[1], x3=c[0],y3=c[1], x4=d[0],y4=d[1];
        double denom = (x1-x2)*(y3-y4) - (y1-y2)*(x3-x4);
        if (Math.abs(denom) < 1e-10) return null;
        double t = ((x1-x3)*(y3-y4) - (y1-y3)*(x3-x4)) / denom;
        double u = -((x1-x2)*(y1-y3) - (y1-y2)*(x1-x3)) / denom;
        if (t >= 0 && t <= 1 && u >= 0 && u <= 1)
            return new Double[]{(double)Math.round(x1+t*(x2-x1)), (double)Math.round(y1+t*(y2-y1))};
        return null;
    }

    private Double[] closestPointOnSegment(Double[] p, Double[] a, Double[] b) {
        double dx=b[0]-a[0], dy=b[1]-a[1];
        if (dx==0 && dy==0) return new Double[]{a[0], a[1]};
        double t = Math.max(0, Math.min(1, ((p[0]-a[0])*dx + (p[1]-a[1])*dy) / (dx*dx+dy*dy)));
        return new Double[]{a[0]+t*dx, a[1]+t*dy};
    }

    private double distance(Double[] a, Double[] b) {
        double dx=a[0]-b[0], dy=a[1]-b[1];
        return Math.sqrt(dx*dx+dy*dy);
    }

    // ===== 权限和数据加载 =====

    private List<AreaData> getModifiableAreas() {
        List<AreaData> result = new ArrayList<>();
        if (client.player == null) return result;
        String playerName = client.player.getGameProfile().getName();
        boolean isAdmin = client.player.hasPermissionLevel(2);
        List<AreaData> all = loadAllAreas();
        for (AreaData area : all) {
            if (isAdmin) { result.add(area); continue; }
            if (area.hasSignature(playerName)) { result.add(area); continue; }
            if (area.getBaseName() != null) {
                for (AreaData base : all) {
                    if (base.getName().equals(area.getBaseName()) && base.hasSignature(playerName)) {
                        result.add(area); break;
                    }
                }
            }
        }
        // 地图入口只补入服务端刚确认有权分割的目标，避免客户端权限缓存与 LuckPerms 结果不一致。
        return CommandVisualLaunchContext.ensureTargetIncluded("dividearea", result, all);
    }

    private List<AreaData> loadAllAreas() {
        List<AreaData> areas = new ArrayList<>();
        try {
            if (client.world != null) {
                String dim = client.world.getRegistryKey().getValue().toString();
                String fn = getFileNameForDimension(dim);
                if (fn != null) {
                    Path p = areahint.world.ClientWorldFolderManager.getWorldDimensionFile(fn);
                    if (p.toFile().exists()) areas = FileManager.readAreaData(p);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return areas;
    }

    private String getFileNameForDimension(String dimId) {
        if (dimId == null) return null;
        if (dimId.contains("overworld")) return areahint.Areashint.OVERWORLD_FILE;
        if (dimId.contains("nether")) return areahint.Areashint.NETHER_FILE;
        if (dimId.contains("end")) return areahint.Areashint.END_FILE;
        return null;
    }

    private void sendMsg(String msg, Formatting fmt) {
        if (client.player != null)
            client.player.sendMessage(Text.literal(msg).formatted(fmt), false);
    }

    public void reset() {
        state = State.IDLE;
        isActive = false;
        isRecording = false;
        selectedArea = null;
        selectedDimensionId = null;
        newVertices.clear();
        area1Vertices = null;
        area2Vertices = null;
        area1Config = null;
        area2Config = null;
    }
}
