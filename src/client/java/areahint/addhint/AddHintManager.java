package areahint.addhint;

import areahint.commandui.CommandVisualLaunchContext;
import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.i18n.I18nManager;
import areahint.util.AreaDataConverter;
import areahint.util.ClientAreaText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * AddHint功能管理器
 * 交互式向已有域名添加顶点
 */
public class AddHintManager {
    private static AddHintManager instance;
    private final MinecraftClient client;

    private boolean isActive = false;
    private boolean isRecording = false;
    private AreaData selectedArea;
    private String selectedDimensionId;
    private List<Double[]> newVertices = new ArrayList<>();

    public static AddHintManager getInstance() {
        if (instance == null) {
            instance = new AddHintManager();
        }
        return instance;
    }

    private AddHintManager() {
        this.client = MinecraftClient.getInstance();
    }

    /**
     * 启动AddHint流程
     */
    public void start() {
        if (client.player == null) return;

        isActive = true;
        List<AreaData> modifiableAreas = getModifiableAreas();

        if (modifiableAreas.isEmpty()) {
            client.player.sendMessage(Text.of(I18nManager.translate("addhint.error.area.modify")), false);
            isActive = false;
            return;
        }

        showAreaSelection(modifiableAreas);
    }

    /**
     * 启动图形选择流程，只初始化状态并返回可修改域名。
     */
    public List<AreaData> beginVisualSelection() {
        if (client.player == null) {
            return List.of();
        }

        isActive = true;
        List<AreaData> modifiableAreas = getModifiableAreas();
        if (modifiableAreas.isEmpty()) {
            client.player.sendMessage(Text.of(I18nManager.translate("addhint.error.area.modify")), false);
            isActive = false;
        }
        return modifiableAreas;
    }

    /**
     * 选择域名
     */
    public void selectArea(String areaName) {
        if (!isActive || client.player == null) return;

        String cleanedName = areaName.trim();
        if (cleanedName.startsWith("\"") && cleanedName.endsWith("\"") && cleanedName.length() > 1) {
            cleanedName = cleanedName.substring(1, cleanedName.length() - 1);
        }

        List<AreaData> modifiableAreas = getModifiableAreas();
        AreaData area = null;
        for (AreaData a : modifiableAreas) {
            if (a.getName().equals(cleanedName)) {
                area = a;
                break;
            }
        }

        if (area == null) {
            client.player.sendMessage(Text.of(I18nManager.translate("addhint.error.area") + cleanedName + I18nManager.translate("addhint.message.permission")), false);
            return;
        }

        selectedArea = area;
        // 锁定域名来自的维度，防止录点途中切维度后误写新维度中的同名域名。
        selectedDimensionId = client.world == null ? null
            : client.world.getRegistryKey().getValue().toString();
        isRecording = true;
        newVertices.clear();

        client.player.sendMessage(Text.of(I18nManager.translate("addhint.prompt.area") + AreaDataConverter.getDisplayName(area)), false);
        client.player.sendMessage(Text.of(I18nManager.translate("addhint.message.general_2") + areahint.keyhandler.UnifiedKeyHandler.getRecordKeyDisplayName() + I18nManager.translate("addhint.message.vertex.record")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("addhint.button.record.finish")), false);
    }

    /**
     * 记录当前位置
     */
    public void recordCurrentPosition() {
        if (!isRecording || client.player == null) return;

        int x = (int) Math.round(client.player.getX());
        int z = (int) Math.round(client.player.getZ());
        newVertices.add(new Double[]{(double) x, (double) z});

        client.player.sendMessage(Text.of(I18nManager.translate("addhint.message.vertex.record_2") + newVertices.size() + ": §6(" + x + ", " + z + ")"), false);

        // 更新边界可视化
        List<BlockPos> blockPosList = new ArrayList<>();
        for (Double[] v : newVertices) {
            blockPosList.add(new BlockPos(v[0].intValue(), (int) client.player.getY(), v[1].intValue()));
        }
        areahint.boundviz.BoundVizManager.getInstance().setTemporaryVertices(blockPosList, true);

        showPointRecordedOptions();
    }

    /**
     * 提交添加的顶点
     */
    public void submit() {
        if (!isActive || selectedArea == null || client.player == null) return;

        if (newVertices.isEmpty()) {
            client.player.sendMessage(Text.of(I18nManager.translate("addhint.error.vertex.record")), false);
            return;
        }

        isRecording = false;

        try {
            // 合并原有顶点和新顶点
            List<Double[]> allVertices = new ArrayList<>();
            if (selectedArea.getVertices() != null) {
                for (AreaData.Vertex v : selectedArea.getVertices()) {
                    allVertices.add(new Double[]{v.getX(), v.getZ()});
                }
            }
            allVertices.addAll(newVertices);

            // 按角度排序防止交叉
            List<Double[]> sorted = sortVerticesByAngle(allVertices);

            // 转换为Vertex列表
            List<AreaData.Vertex> vertices = new ArrayList<>();
            for (Double[] v : sorted) {
                vertices.add(new AreaData.Vertex(v[0], v[1]));
            }

            // 重新计算二级顶点(AABB)
            List<AreaData.Vertex> secondVertices = calculateBoundingBox(sorted);

            // 更新域名数据
            selectedArea.setVertices(vertices);
            selectedArea.setSecondVertices(secondVertices);

            if (selectedDimensionId == null) {
                client.player.sendMessage(Text.of(I18nManager.translate("dividearea.error.dimension")), false);
                return;
            }

            // 发送到服务端
            AddHintClientNetworking.sendToServer(selectedArea, selectedDimensionId);

            client.player.sendMessage(Text.of(I18nManager.translate("addhint.message.area") + selectedArea.getName() + I18nManager.translate("addhint.message.vertex_2")), false);

        } catch (Exception e) {
            client.player.sendMessage(Text.of(I18nManager.translate("addhint.error.vertex") + e.getMessage()), false);
        } finally {
            reset();
        }
    }

    /**
     * 取消流程
     */
    public void cancel() {
        if (!isActive) return;
        areahint.boundviz.BoundVizManager.getInstance().clearTemporaryVertices();
        if (client.player != null) {
            client.player.sendMessage(Text.of(I18nManager.translate("addhint.error.vertex.cancel.add")), false);
        }
        reset();
    }

    private void reset() {
        isActive = false;
        isRecording = false;
        selectedArea = null;
        selectedDimensionId = null;
        newVertices.clear();
        areahint.boundviz.BoundVizManager.getInstance().clearTemporaryVertices();
    }

    /**
     * 按角度排序顶点，防止多边形线段交叉
     */
    private List<Double[]> sortVerticesByAngle(List<Double[]> vertices) {
        // 计算质心
        double cx = 0, cz = 0;
        for (Double[] v : vertices) {
            cx += v[0];
            cz += v[1];
        }
        cx /= vertices.size();
        cz /= vertices.size();

        final double fcx = cx, fcz = cz;
        List<Double[]> sorted = new ArrayList<>(vertices);
        sorted.sort((a, b) -> {
            double angleA = Math.atan2(a[1] - fcz, a[0] - fcx);
            double angleB = Math.atan2(b[1] - fcz, b[0] - fcx);
            return Double.compare(angleA, angleB);
        });
        return sorted;
    }

    /**
     * 计算AABB包围盒作为二级顶点
     */
    private List<AreaData.Vertex> calculateBoundingBox(List<Double[]> vertices) {
        double minX = vertices.get(0)[0], maxX = vertices.get(0)[0];
        double minZ = vertices.get(0)[1], maxZ = vertices.get(0)[1];
        for (Double[] v : vertices) {
            minX = Math.min(minX, v[0]);
            maxX = Math.max(maxX, v[0]);
            minZ = Math.min(minZ, v[1]);
            maxZ = Math.max(maxZ, v[1]);
        }
        List<AreaData.Vertex> box = new ArrayList<>();
        box.add(new AreaData.Vertex(minX, minZ));
        box.add(new AreaData.Vertex(maxX, minZ));
        box.add(new AreaData.Vertex(maxX, maxZ));
        box.add(new AreaData.Vertex(minX, maxZ));
        return box;
    }

    /**
     * 获取可修改的域名列表
     */
    private List<AreaData> getModifiableAreas() {
        List<AreaData> result = new ArrayList<>();
        if (client.player == null || client.world == null) return result;

        String playerName = client.player.getGameProfile().getName();
        boolean isAdmin = client.player.hasPermissionLevel(2);

        List<AreaData> allAreas = loadAllAreas();
        for (AreaData area : allAreas) {
            if (isAdmin) {
                result.add(area);
            } else if (area.hasSignature(playerName)) {
                result.add(area);
            } else if (area.getBaseName() != null) {
                AreaData baseArea = findAreaByName(area.getBaseName(), allAreas);
                if (baseArea != null && baseArea.hasSignature(playerName)) {
                    result.add(area);
                }
            }
        }
        // 地图管理入口的目标已经过能力查询，只为当前上下文补入该目标。
        return CommandVisualLaunchContext.ensureTargetIncluded("addhint", result, allAreas);
    }

    private AreaData findAreaByName(String name, List<AreaData> areas) {
        for (AreaData a : areas) {
            if (a.getName().equals(name)) return a;
        }
        return null;
    }

    private List<AreaData> loadAllAreas() {
        try {
            String dim = client.world.getRegistryKey().getValue().toString();
            String fileName = getFileNameForDimension(dim);
            if (fileName != null) {
                Path path = areahint.world.ClientWorldFolderManager.getWorldDimensionFile(fileName);
                if (path.toFile().exists()) {
                    return FileManager.readAreaData(path);
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return new ArrayList<>();
    }

    private String getFileNameForDimension(String dim) {
        if (dim.contains("overworld")) return areahint.Areashint.OVERWORLD_FILE;
        if (dim.contains("nether")) return areahint.Areashint.NETHER_FILE;
        if (dim.contains("end")) return areahint.Areashint.END_FILE;
        return null;
    }

    // UI方法

    private void showAreaSelection(List<AreaData> areas) {
        client.player.sendMessage(Text.of(I18nManager.translate("addhint.title.area.vertex.add")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("addhint.prompt.area.vertex.add")), false);

        for (AreaData area : areas) {
            String displayName = AreaDataConverter.getDisplayName(area);
            MutableText btn = Text.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/areahint addhint select \"" + area.getName() + "\""))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.of(I18nManager.translate("addhint.prompt.general") + displayName + I18nManager.translate("addhint.message.general") + ClientAreaText.signature(area))))
                    .withColor(Formatting.GOLD));
            client.player.sendMessage(btn, false);
        }

        MutableText cancelBtn = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint addhint cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("addhint.message.cancel"))))
                .withColor(Formatting.RED));
        client.player.sendMessage(cancelBtn, false);
    }

    private void showPointRecordedOptions() {
        int count = newVertices.size();
        client.player.sendMessage(Text.of(I18nManager.translate("addhint.message.record") + count + I18nManager.translate("addhint.message.vertex")), false);

        MutableText continueBtn = Text.literal(I18nManager.translate("addhint.button.record.continue"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint addhint continue"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("addhint.message.record.continue"))))
                .withColor(Formatting.GREEN));

        MutableText submitBtn = Text.literal(I18nManager.translate("addhint.button.general"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint addhint submit"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("addhint.message.vertex_3"))))
                .withColor(Formatting.AQUA));

        MutableText cancelBtn = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint addhint cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("addhint.message.cancel"))))
                .withColor(Formatting.RED));

        MutableText row = Text.empty()
            .append(continueBtn).append(Text.of("  "))
            .append(submitBtn).append(Text.of("  "))
            .append(cancelBtn);
        client.player.sendMessage(row, false);
    }

    // Getters
    public boolean isActive() { return isActive; }
    public boolean isRecording() { return isRecording; }
    public AreaData getSelectedArea() { return selectedArea; }
    public List<Double[]> getNewVertices() { return newVertices; }
}
