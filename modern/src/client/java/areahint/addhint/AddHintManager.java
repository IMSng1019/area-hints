package areahint.addhint;

import areahint.util.TextEventCompat;

import areahint.commandui.CommandVisualLaunchContext;
import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.i18n.I18nManager;
import areahint.util.AreaDataConverter;
import areahint.util.ClientAreaText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * AddHint功能管理器
 * 交互式向已有域名添加顶点
 */
public class AddHintManager {
    private static AddHintManager instance;
    private final Minecraft client;

    private boolean isActive = false;
    private boolean isRecording = false;
    private AreaData selectedArea;
    private String selectedDimensionId;
    private List<Double[]> newVertices = new ArrayList<>();
    // 审批临时模式只回传副本，绝不走普通域名保存网络包。
    private java.util.function.Consumer<AreaData> temporarySubmit;

    public static AddHintManager getInstance() {
        if (instance == null) {
            instance = new AddHintManager();
        }
        return instance;
    }

    private AddHintManager() {
        this.client = Minecraft.getInstance();
    }

    /**
     * 启动AddHint流程
     */
    public void start() {
        if (client.player == null) return;

        temporarySubmit = null;

        isActive = true;
        List<AreaData> modifiableAreas = getModifiableAreas();

        if (modifiableAreas.isEmpty()) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.error.area.modify")));
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

        temporarySubmit = null;
        isActive = true;
        List<AreaData> modifiableAreas = getModifiableAreas();
        if (modifiableAreas.isEmpty()) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.error.area.modify")));
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
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.error.area") + cleanedName + I18nManager.translate("addhint.message.permission")));
            return;
        }

        selectedArea = area;
        // 锁定域名来自的维度，防止录点途中切维度后误写新维度中的同名域名。
        selectedDimensionId = client.level == null ? null
            : client.level.dimension().identifier().toString();
        isRecording = true;
        newVertices.clear();

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.prompt.area") + AreaDataConverter.getDisplayName(area)));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.message.general_2") + areahint.keyhandler.UnifiedKeyHandler.getRecordKeyDisplayName() + I18nManager.translate("addhint.message.vertex.record")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.button.record.finish")));
    }

    // 复用现有录点和顶点排序流程，但临时草稿由审批会话持有并决定何时上传。
    public void beginTemporary(AreaData area, String dimensionId, java.util.function.Consumer<AreaData> callback) {
        reset();
        if (area == null || callback == null || client.player == null) return;
        selectedArea = new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(area), AreaData.class);
        selectedDimensionId = dimensionId;
        temporarySubmit = callback;
        isActive = true;
        isRecording = true;
        areahint.commandui.CommandVisualController.beginVisualRecordMode("addhint");
    }

    /**
     * 记录当前位置
     */
    public void recordCurrentPosition() {
        if (!isRecording || client.player == null) return;
        if (temporarySubmit != null && (client.level == null
                || !client.level.dimension().identifier().toString().equals(selectedDimensionId))) {
            client.player.sendSystemMessage(Component.literal(I18nManager.translate("approval.client.wrong_dimension")));
            return;
        }

        int x = (int) Math.round(client.player.getX());
        int z = (int) Math.round(client.player.getZ());
        newVertices.add(new Double[]{(double) x, (double) z});

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.message.vertex.record_2") + newVertices.size() + ": §6(" + x + ", " + z + ")"));

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
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.error.vertex.record")));
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
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("dividearea.error.dimension")));
                return;
            }

            // 临时模式保留审批草稿，普通模式仍按原流程发送到服务端。
            if (temporarySubmit != null) {
                temporarySubmit.accept(selectedArea);
                return;
            }
            AddHintClientNetworking.sendToServer(selectedArea, selectedDimensionId);

            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.message.area") + selectedArea.getName() + I18nManager.translate("addhint.message.vertex_2")));

        } catch (Exception e) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.error.vertex") + e.getMessage()));
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
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.error.vertex.cancel.add")));
        }
        reset();
    }

    private void reset() {
        isActive = false;
        isRecording = false;
        selectedArea = null;
        selectedDimensionId = null;
        temporarySubmit = null;
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
        if (client.player == null || client.level == null) return result;

        String playerName = areahint.util.ProfileCompat.name(client.player.getGameProfile());
        boolean isAdmin = areahint.util.VanillaPermissionCompat.hasPermissionLevel(client.player, 2);

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
            String dim = client.level.dimension().identifier().toString();
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
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.title.area.vertex.add")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.prompt.area.vertex.add")));

        for (AreaData area : areas) {
            String displayName = AreaDataConverter.getDisplayName(area);
            MutableComponent btn = Component.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint addhint select \"" + area.getName() + "\""))
                    .withHoverEvent(TextEventCompat.showText(
                        Component.nullToEmpty(I18nManager.translate("addhint.prompt.general") + displayName + I18nManager.translate("addhint.message.general") + ClientAreaText.signature(area))))
                    .withColor(ChatFormatting.GOLD));
            client.player.sendSystemMessage(btn);
        }

        MutableComponent cancelBtn = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint addhint cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("addhint.message.cancel"))))
                .withColor(ChatFormatting.RED));
        client.player.sendSystemMessage(cancelBtn);
    }

    private void showPointRecordedOptions() {
        int count = newVertices.size();
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.message.record") + count + I18nManager.translate("addhint.message.vertex")));

        MutableComponent continueBtn = Component.literal(I18nManager.translate("addhint.button.record.continue"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint addhint continue"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("addhint.message.record.continue"))))
                .withColor(ChatFormatting.GREEN));

        MutableComponent submitBtn = Component.literal(I18nManager.translate("addhint.button.general"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint addhint submit"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("addhint.message.vertex_3"))))
                .withColor(ChatFormatting.AQUA));

        MutableComponent cancelBtn = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint addhint cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("addhint.message.cancel"))))
                .withColor(ChatFormatting.RED));

        MutableComponent row = Component.empty()
            .append(continueBtn).append(Component.nullToEmpty("  "))
            .append(submitBtn).append(Component.nullToEmpty("  "))
            .append(cancelBtn);
        client.player.sendSystemMessage(row);
    }

    // Getters
    public boolean isActive() { return isActive; }
    public boolean isRecording() { return isRecording; }
    public AreaData getSelectedArea() { return selectedArea; }
    public List<Double[]> getNewVertices() { return newVertices; }
}
