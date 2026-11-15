package areahint.deletehint;

import areahint.util.TextEventCompat;

import areahint.commandui.CommandVisualLaunchContext;
import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.i18n.I18nManager;
import areahint.util.AreaDataConverter;
import areahint.util.ClientAreaText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DeleteHint功能管理器
 * 交互式从已有域名删除顶点
 */
public class DeleteHintManager {
    private static DeleteHintManager instance;
    private final Minecraft client;

    private boolean isActive = false;
    private AreaData selectedArea;
    private String selectedDimensionId;
    private Set<Integer> markedIndices = new HashSet<>();
    // 审批草稿使用回调接收顶点变更，避免发送正式域名修改包。
    private java.util.function.Consumer<AreaData> temporarySubmit;

    public static DeleteHintManager getInstance() {
        if (instance == null) {
            instance = new DeleteHintManager();
        }
        return instance;
    }

    private DeleteHintManager() {
        this.client = Minecraft.getInstance();
    }

    /**
     * 启动DeleteHint流程
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
        // 锁定域名来自的维度，防止选择顶点期间切换维度后误写新维度中的同名域名。
        selectedDimensionId = client.level == null ? null
            : client.level.dimension().identifier().toString();
        markedIndices.clear();

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.prompt.area") + AreaDataConverter.getDisplayName(area)));
        showVertexButtons();
    }

    /**
     * 图形流程选择域名，不再输出旧聊天按钮。
     */
    public boolean selectAreaForVisual(String areaName) {
        if (!isActive || client.player == null) {
            return false;
        }

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
            return false;
        }

        selectedArea = area;
        selectedDimensionId = client.level == null ? null
            : client.level.dimension().identifier().toString();
        markedIndices.clear();
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.prompt.area") + AreaDataConverter.getDisplayName(area)));
        return true;
    }

    /**
     * 标记/取消标记顶点
     */
    public void toggleVertex(int index) {
        if (!isActive || selectedArea == null || client.player == null) return;

        List<AreaData.Vertex> vertices = selectedArea.getVertices();
        if (index < 0 || index >= vertices.size()) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("deletehint.error.vertex")));
            return;
        }

        if (markedIndices.contains(index)) {
            markedIndices.remove(index);
        } else {
            // 确保删除后至少剩余3个顶点
            int remaining = vertices.size() - markedIndices.size() - 1;
            if (remaining < 3) {
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("deletehint.error.vertex.delete")));
                return;
            }
            markedIndices.add(index);
        }

        showVertexButtons();
    }

    // 临时选择绕过磁盘域名查找，仍复用删除标记和至少三个顶点的原有校验。
    public void beginTemporary(AreaData area, String dimensionId, java.util.function.Consumer<AreaData> callback) {
        reset();
        if (area == null || callback == null || client.player == null) return;
        selectedArea = new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(area), AreaData.class);
        selectedDimensionId = dimensionId;
        temporarySubmit = callback;
        isActive = true;
    }

    /**
     * 提交删除
     */
    public void submit() {
        if (!isActive || selectedArea == null || client.player == null) return;

        if (markedIndices.isEmpty()) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("deletehint.error.vertex.delete_3")));
            return;
        }

        try {
            // 收集未被删除的顶点
            List<Double[]> remaining = new ArrayList<>();
            List<AreaData.Vertex> vertices = selectedArea.getVertices();
            for (int i = 0; i < vertices.size(); i++) {
                if (!markedIndices.contains(i)) {
                    remaining.add(new Double[]{vertices.get(i).getX(), vertices.get(i).getZ()});
                }
            }

            if (remaining.size() < 3) {
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("deletehint.error.vertex.delete_2")));
                return;
            }

            // 按角度排序防止交叉
            List<Double[]> sorted = sortVerticesByAngle(remaining);

            // 转换为Vertex列表
            List<AreaData.Vertex> newVertices = new ArrayList<>();
            for (Double[] v : sorted) {
                newVertices.add(new AreaData.Vertex(v[0], v[1]));
            }

            // 重新计算二级顶点(AABB)
            List<AreaData.Vertex> secondVertices = calculateBoundingBox(sorted);

            // 更新域名数据
            selectedArea.setVertices(newVertices);
            selectedArea.setSecondVertices(secondVertices);

            if (selectedDimensionId == null) {
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("dividearea.error.dimension")));
                return;
            }

            // 审批临时模式仅回传几何副本，普通删除仍由原网络流程处理。
            if (temporarySubmit != null) {
                temporarySubmit.accept(selectedArea);
                return;
            }
            DeleteHintClientNetworking.sendToServer(selectedArea, selectedDimensionId);

            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.message.area") + selectedArea.getName() + I18nManager.translate("deletehint.message.vertex.delete")));

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
        if (client.player != null) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("deletehint.error.vertex.cancel.delete")));
        }
        reset();
    }

    private void reset() {
        isActive = false;
        selectedArea = null;
        selectedDimensionId = null;
        temporarySubmit = null;
        markedIndices.clear();
    }

    // ===== UI方法 =====

    private void showAreaSelection(List<AreaData> areas) {
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("deletehint.title.area.vertex.delete")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("deletehint.prompt.area.vertex.delete")));

        for (AreaData area : areas) {
            String displayName = AreaDataConverter.getDisplayName(area);
            MutableComponent btn = Component.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint deletehint select \"" + area.getName() + "\""))
                    .withHoverEvent(TextEventCompat.showText(
                        Component.nullToEmpty(I18nManager.translate("addhint.prompt.general") + displayName + I18nManager.translate("addhint.message.general") + ClientAreaText.signature(area)
                            + I18nManager.translate("deletehint.message.vertex_3") + area.getVertices().size())))
                    .withColor(ChatFormatting.GOLD));
            client.player.sendSystemMessage(btn);
        }

        MutableComponent cancelBtn = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint deletehint cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("addhint.message.cancel"))))
                .withColor(ChatFormatting.RED));
        client.player.sendSystemMessage(cancelBtn);
    }

    private void showVertexButtons() {
        List<AreaData.Vertex> vertices = selectedArea.getVertices();
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("deletehint.title.vertex.delete")));
        client.player.sendSystemMessage(Component.nullToEmpty("§7" + I18nManager.translate("deletehint.message.delete") + "/" + I18nManager.translate("deletehint.message.cancel")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("deletehint.message.general_2") + markedIndices.size() + I18nManager.translate("deletehint.message.vertex")
            + (vertices.size() - markedIndices.size()) + I18nManager.translate("deletehint.message.general")));

        for (int i = 0; i < vertices.size(); i++) {
            AreaData.Vertex v = vertices.get(i);
            boolean marked = markedIndices.contains(i);
            String prefix = marked ? "§c✗ " : "§a";
            String label = prefix + I18nManager.translate("deletehint.message.vertex_2") + (i + 1) + ": (" + (int) v.getX() + ", " + (int) v.getZ() + ")]";

            MutableComponent btn = Component.literal(label)
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint deletehint toggle " + i))
                    .withHoverEvent(TextEventCompat.showText(
                        Component.nullToEmpty(marked ? I18nManager.translate("deletehint.message.cancel") : I18nManager.translate("deletehint.message.delete"))))
                    .withColor(marked ? ChatFormatting.RED : ChatFormatting.GREEN));
            client.player.sendSystemMessage(btn);
        }

        // 操作按钮
        MutableComponent submitBtn = Component.literal(I18nManager.translate("deletehint.button.delete"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint deletehint submit"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("deletehint.message.vertex.delete_2"))))
                .withColor(ChatFormatting.AQUA));

        MutableComponent cancelBtn = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint deletehint cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("addhint.message.cancel"))))
                .withColor(ChatFormatting.RED));

        MutableComponent row = Component.empty()
            .append(submitBtn).append(Component.nullToEmpty("  "))
            .append(cancelBtn);
        client.player.sendSystemMessage(row);
    }

    // ===== 工具方法 =====

    private List<Double[]> sortVerticesByAngle(List<Double[]> vertices) {
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
        return CommandVisualLaunchContext.ensureTargetIncluded("deletehint", result, allAreas);
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

    // Getters
    public boolean isActive() { return isActive; }
    public AreaData getSelectedArea() { return selectedArea; }
    public Set<Integer> getMarkedIndices() { return markedIndices; }
}
