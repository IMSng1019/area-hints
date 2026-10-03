// 阶段 6 第三方隔离搬迁：BlueMap API（1.20.1 节点）的集成实现。
// 与 1.20.4 节点同源复制（机制 ② 的代价）；BlueMap API 版本不同时在本文件内单独适配。

package areahint.map;

import areahint.Areashint;
import areahint.data.AreaData;
import areahint.dimensional.DimensionalNameManager;
import areahint.util.AreaDataConverter;
import areahint.util.ColorUtil;
import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AreaData 到 BlueMap Marker 的转换工厂。
 */
public final class BlueMapMarkerFactory {
    private static final String DEFAULT_DIMENSION_BADGE_COLOR = "#5C7CFA";
    private static final String MUTED_TEXT_COLOR = "#A9B4C2";
    private static final String PANEL_BACKGROUND = "rgba(15, 23, 42, 0.35)";
    private static final String PANEL_BORDER = "rgba(148, 163, 184, 0.28)";

    private BlueMapMarkerFactory() {
    }

    public static String createMarkerId(AreaData area, String dimensionType) {
        String safeName = sanitizeName(area != null ? area.getName() : "unknown");
        int hash = Objects.hash(
            dimensionType,
            area != null ? area.getName() : null,
            area != null ? area.getLevel() : null,
            area != null ? area.getBaseName() : null,
            area != null ? area.getSignature() : null,
            area != null ? area.getColor() : null,
            area != null ? area.getSurfacename() : null,
            area != null && area.getAltitude() != null ? area.getAltitude().getMin() : null,
            area != null && area.getAltitude() != null ? area.getAltitude().getMax() : null,
            area != null && area.getVertices() != null ? area.getVertices().size() : 0
        );
        return "area-" + sanitizeName(dimensionType) + "-" + safeName + "-" + Integer.toUnsignedString(hash, 16);
    }

    public static ExtrudeMarker createMarker(AreaData area, ServerWorld world, String dimensionType) {
        return createMarkerDefinition(area, world, dimensionType, System.currentTimeMillis()).marker();
    }

    public static MarkerBuildResult createMarkerDefinition(AreaData area, ServerWorld world, String dimensionType, long timeMs) {
        if (area == null) {
            throw new IllegalArgumentException("area 不能为空");
        }
        if (world == null) {
            throw new IllegalArgumentException("world 不能为空");
        }
        if (area.getVertices() == null || area.getVertices().size() < 3) {
            throw new IllegalArgumentException("一级顶点不足，至少需要 3 个点");
        }
        if (area.getSecondVertices() == null || area.getSecondVertices().size() != 4) {
            Areashint.LOGGER.warn("BlueMap 同步区域 '{}' 时检测到 second-vertices 异常，当前值数量: {}",
                area.getName(), area.getSecondVertices() == null ? 0 : area.getSecondVertices().size());
        }

        String markerId = createMarkerId(area, dimensionType);
        HeightRange heightRange = resolveHeightRange(area, world);
        PolygonData polygonData = createPolygonData(area.getVertices());
        String dimensionId = world.getRegistryKey().getValue().toString();
        String displayName = resolveDisplayName(area);
        String stablePhaseKey = area.getName() != null && !area.getName().isBlank() ? area.getName() : displayName;
        MarkerStyle markerStyle = resolveMarkerStyle(area.getColor(), area.getLevel(), stablePhaseKey, timeMs);
        String detail = buildDetail(area, displayName, dimensionId, dimensionType, heightRange, markerStyle);

        ExtrudeMarker marker = ExtrudeMarker.builder()
            .label(displayName)
            .position(new Vector3d(polygonData.centerX(), heightRange.minY(), polygonData.centerZ()))
            .shape(polygonData.shape(), (float) heightRange.minY(), (float) heightRange.maxY())
            .lineColor(markerStyle.lineColor())
            .fillColor(markerStyle.fillColor())
            .lineWidth(markerStyle.lineWidth())
            .detail(detail)
            .depthTestEnabled(true)
            .build();

        BlueMapDynamicMarkerState dynamicState = null;
        if (markerStyle.originalFlashMode() != null) {
            dynamicState = new BlueMapDynamicMarkerState(
                markerId,
                marker,
                markerStyle.originalFlashMode(),
                markerStyle.phaseOffsetMs(),
                markerStyle.lineWidth(),
                markerStyle.fillAlpha(),
                markerStyle.phaseBucket(),
                markerStyle.effectiveRgb()
            );
        }

        return new MarkerBuildResult(markerId, marker, dynamicState);
    }

    private static String resolveDisplayName(AreaData area) {
        String displayName = AreaDataConverter.getDisplayName(area);
        if (displayName == null || displayName.isBlank()) {
            return area.getName() != null && !area.getName().isBlank() ? area.getName() : "未知域名";
        }
        return displayName;
    }

    private static PolygonData createPolygonData(List<AreaData.Vertex> vertices) {
        List<Vector2d> points = new ArrayList<>();
        double centerX = 0;
        double centerZ = 0;

        for (AreaData.Vertex vertex : vertices) {
            if (vertex == null) {
                continue;
            }
            points.add(new Vector2d(vertex.getX(), vertex.getZ()));
            centerX += vertex.getX();
            centerZ += vertex.getZ();
        }

        if (points.size() >= 2 && samePoint(points.get(0), points.get(points.size() - 1))) {
            points.remove(points.size() - 1);
        }

        if (points.size() < 3) {
            throw new IllegalArgumentException("有效多边形顶点不足，至少需要 3 个点");
        }

        centerX /= points.size();
        centerZ /= points.size();

        Shape shape;
        try {
            shape = new Shape(points.toArray(new Vector2d[0]));
        } catch (IllegalArgumentException primaryException) {
            List<Vector2d> reversed = new ArrayList<>(points);
            java.util.Collections.reverse(reversed);
            shape = new Shape(reversed.toArray(new Vector2d[0]));
            Areashint.LOGGER.debug("BlueMap 多边形按原顺序构造失败，已使用反向顶点顺序重试。");
        }

        return new PolygonData(shape, centerX, centerZ);
    }

    private static boolean samePoint(Vector2d first, Vector2d second) {
        return Double.compare(first.getX(), second.getX()) == 0
            && Double.compare(first.getY(), second.getY()) == 0;
    }

    private static HeightRange resolveHeightRange(AreaData area, ServerWorld world) {
        double bottomY = world.getBottomY();
        // 1.21.1 及以下用 getTopY()；1.21.4 起的副本改用 getTopYInclusive()
        double topY = world.getTopY();

        Double configuredMin = null;
        Double configuredMax = null;
        if (area.getAltitude() != null) {
            configuredMin = area.getAltitude().getMin();
            configuredMax = area.getAltitude().getMax();
        }

        String source = resolveHeightSource(configuredMin, configuredMax);

        double originalMinY = configuredMin != null ? configuredMin : bottomY;
        double originalMaxY = configuredMax != null ? configuredMax : topY;
        double minY = clamp(originalMinY, bottomY, topY);
        double maxY = clamp(originalMaxY, bottomY, topY);

        boolean clamped = Double.compare(originalMinY, minY) != 0 || Double.compare(originalMaxY, maxY) != 0;
        boolean expandedToVisibleVolume = false;

        if (maxY <= minY) {
            expandedToVisibleVolume = true;
            maxY = Math.min(topY, minY + 1);
            if (maxY <= minY) {
                minY = Math.max(bottomY, topY - 1);
                maxY = topY;
            }
        }

        return new HeightRange(minY, maxY, source, clamped, expandedToVisibleVolume);
    }

    private static String resolveHeightSource(Double min, Double max) {
        if (min == null && max == null) {
            return "世界高度自动";
        }
        if (min != null && max != null) {
            return "最小/最大高度均为自定义";
        }
        if (min != null) {
            return "最小高度自定义，最大高度自动";
        }
        return "最小高度自动，最大高度自定义";
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static MarkerStyle resolveMarkerStyle(String rawColor, int level, String stablePhaseKey, long timeMs) {
        String normalizedColor = ColorUtil.normalizeColor(rawColor);
        boolean flashMode = ColorUtil.isFlashColor(normalizedColor);
        float fillAlpha = resolveFillAlpha(level);
        int lineWidth = resolveLineWidth(level, flashMode);

        if (!flashMode) {
            int[] rgb = ColorUtil.parseColor(normalizedColor);
            int effectiveRgb = packRgb(rgb[0], rgb[1], rgb[2]);
            Color lineColor = new Color(rgb[0], rgb[1], rgb[2]);
            return new MarkerStyle(
                normalizedColor,
                null,
                normalizedColor,
                effectiveRgb,
                lineColor,
                new Color(rgb[0], rgb[1], rgb[2], fillAlpha),
                lineWidth,
                fillAlpha,
                0L,
                -1L
            );
        }

        long phaseOffsetMs = BlueMapFlashColorEngine.resolveStablePhaseOffset(normalizedColor, stablePhaseKey);
        int effectiveRgb = BlueMapFlashColorEngine.resolveShiftedRgb(normalizedColor, timeMs, phaseOffsetMs);
        long phaseBucket = BlueMapFlashColorEngine.resolvePhaseBucket(
            normalizedColor,
            timeMs,
            phaseOffsetMs,
            BlueMapFlashColorEngine.getBucketMs(normalizedColor)
        );
        String effectiveHexColor = formatHexColor(effectiveRgb);
        Color lineColor = resolveFlashOutlineColor(effectiveRgb);

        return new MarkerStyle(
            normalizedColor,
            normalizedColor,
            effectiveHexColor,
            effectiveRgb,
            lineColor,
            BlueMapFlashColorEngine.toColor(effectiveRgb, fillAlpha),
            lineWidth,
            fillAlpha,
            phaseOffsetMs,
            phaseBucket
        );
    }

    private static float resolveFillAlpha(int level) {
        if (level <= 1) {
            return 0.10f;
        }
        if (level == 2) {
            return 0.15f;
        }
        return 0.20f;
    }

    private static int resolveLineWidth(int level, boolean flashMode) {
        return level <= 1 ? 3 : 2;
    }

    private static Color resolveFlashOutlineColor(int fillRgb) {
        int red = (fillRgb >> 16) & 0xFF;
        int green = (fillRgb >> 8) & 0xFF;
        int blue = fillRgb & 0xFF;
        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0;

        if (luminance > 0.6) {
            return new Color(
                (int) (red * 0.35),
                (int) (green * 0.35),
                (int) (blue * 0.35)
            );
        }

        return new Color(
            Math.min(255, (int) (red * 0.45 + 255 * 0.55)),
            Math.min(255, (int) (green * 0.45 + 255 * 0.55)),
            Math.min(255, (int) (blue * 0.45 + 255 * 0.55))
        );
    }

    private static String buildDetail(AreaData area, String displayName, String dimensionId, String dimensionType,
                                      HeightRange heightRange, MarkerStyle markerStyle) {
        String surfaceName = area.getSurfacename();
        boolean hasSurfaceName = surfaceName != null && !surfaceName.isBlank();
        boolean surfaceDiffers = hasSurfaceName && !surfaceName.equals(area.getName());
        String dimensionDisplayName = DimensionalNameManager.getDimensionalName(dimensionId);
        String dimensionBadgeColor = resolveDimensionBadgeColor(dimensionId);

        StringBuilder detail = new StringBuilder();
        detail.append("<div style=\"min-width:260px;line-height:1.5;\">");
        detail.append("<div style=\"font-size:1.15em;font-weight:700;color:")
            .append(markerStyle.effectiveHexColor()).append(";margin-bottom:4px;\">")
            .append(escapeHtml(displayName)).append("</div>");

        if (surfaceDiffers) {
            detail.append("<div style=\"color:").append(MUTED_TEXT_COLOR)
                .append(";margin-bottom:8px;\">当前显示为表面域名，实际域名为 ")
                .append(code(area.getName())).append("</div>");
        } else {
            detail.append("<div style=\"color:").append(MUTED_TEXT_COLOR)
                .append(";margin-bottom:8px;\">BlueMap 3D 挤出区域投影</div>");
        }

        detail.append("<div style=\"margin:0 0 10px 0;\">")
            .append(buildBadge("Lv." + area.getLevel(), markerStyle.effectiveHexColor()))
            .append(buildBadge(dimensionDisplayName, dimensionBadgeColor))
            .append("</div>");

        detail.append("<div style=\"border:1px solid ").append(PANEL_BORDER)
            .append(";border-radius:10px;padding:8px 10px;background:").append(PANEL_BACKGROUND).append(";\">");
        detail.append("<table style=\"border-collapse:collapse;width:100%;\">");
        appendRow(detail, "实际域名", code(nullToFallback(area.getName(), "无")));
        if (hasSurfaceName) {
            appendRow(detail, "表面域名", code(surfaceName));
        }
        appendRow(detail, "上级域名", area.getBaseName() == null || area.getBaseName().isBlank() ? "无" : code(area.getBaseName()));
        appendRow(detail, "创建者", area.getSignature() == null || area.getSignature().isBlank() ? "无" : escapeHtml(area.getSignature()));
        appendRow(detail, "顶点数量", Integer.toString(area.getVertices().size()));
        appendRow(detail, "高度范围",
            "<strong>" + escapeHtml(formatNumber(heightRange.minY())) + " ~ " + escapeHtml(formatNumber(heightRange.maxY())) +
                "</strong> <span style=\"color:" + MUTED_TEXT_COLOR + ";\">（" + escapeHtml(heightRange.source()) + "）</span>");
        appendRow(detail, "维度ID", code(dimensionId));
        appendRow(detail, "同步分组", code(dimensionType));
        appendRow(detail, "颜色", buildColorSwatch(markerStyle));
        detail.append("</table>");

        if (markerStyle.originalFlashMode() != null || heightRange.clamped() || heightRange.expandedToVisibleVolume()) {
            detail.append("<div style=\"margin-top:8px;padding-top:8px;border-top:1px dashed ")
                .append(PANEL_BORDER).append(";font-size:0.92em;color:").append(MUTED_TEXT_COLOR).append(";\">");
            if (markerStyle.originalFlashMode() != null) {
                detail.append("<div>")
                    .append(buildFlashModeDetail(markerStyle.originalFlashMode()))
                    .append("</div>");
            }
            if (heightRange.clamped()) {
                detail.append("<div>高度超出当前维度合法范围，已自动裁剪到世界高度内。</div>");
            }
            if (heightRange.expandedToVisibleVolume()) {
                detail.append("<div>原始高度范围不可见，已自动扩展为最小可见厚度。</div>");
            }
            detail.append("</div>");
        }

        detail.append("</div></div>");
        return detail.toString();
    }

    private static void appendRow(StringBuilder detail, String label, String htmlValue) {
        detail.append("<tr>")
            .append("<td style=\"padding:4px 10px 4px 0;vertical-align:top;white-space:nowrap;color:")
            .append(MUTED_TEXT_COLOR).append(";\">")
            .append(escapeHtml(label)).append("</td>")
            .append("<td style=\"padding:4px 0;vertical-align:top;\">")
            .append(htmlValue)
            .append("</td>")
            .append("</tr>");
    }

    private static String buildBadge(String label, String backgroundColor) {
        return "<span style=\"display:inline-block;margin:0 6px 6px 0;padding:2px 8px;border-radius:999px;background:" +
            backgroundColor + ";color:" + getContrastingTextColor(backgroundColor) +
            ";font-size:0.85em;font-weight:600;\">" + escapeHtml(label) + "</span>";
    }

    private static String buildColorSwatch(MarkerStyle markerStyle) {
        StringBuilder swatch = new StringBuilder();
        swatch.append("<span style=\"display:inline-block;vertical-align:middle;width:12px;height:12px;")
            .append("border-radius:3px;margin-right:6px;border:1px solid rgba(255,255,255,0.35);background:")
            .append(markerStyle.effectiveHexColor()).append(";\"></span>");

        if (markerStyle.originalFlashMode() != null) {
            swatch.append(code(markerStyle.originalFlashMode()))
                .append(" <span style=\"color:").append(MUTED_TEXT_COLOR)
                .append(";\">（填充为当前周期样本 ")
                .append(escapeHtml(markerStyle.effectiveHexColor()))
                .append("，边框为独立描边）</span>");
        } else {
            swatch.append(code(markerStyle.normalizedColor()));
        }

        return swatch.toString();
    }

    private static String buildFlashModeDetail(String flashMode) {
        String escapedMode = escapeHtml(flashMode);
        if (BlueMapFlashColorEngine.isPerCharMode(flashMode)) {
            return "动态颜色模式 <strong>" + escapedMode +
                "</strong> 已映射为按区域名稳定相位偏移的整体周期颜色，用于近似原始逐字效果。";
        }
        return "动态颜色模式 <strong>" + escapedMode +
            "</strong> 通过周期更新 marker 颜色保持整体动态效果。";
    }

    private static String resolveDimensionBadgeColor(String dimensionId) {
        String configuredColor = DimensionalNameManager.getDimensionalColor(dimensionId);
        if (configuredColor == null || configuredColor.isBlank()) {
            return DEFAULT_DIMENSION_BADGE_COLOR;
        }

        String normalized = ColorUtil.normalizeColor(configuredColor);
        return ColorUtil.isFlashColor(normalized) ? DEFAULT_DIMENSION_BADGE_COLOR : normalized;
    }

    private static String getContrastingTextColor(String backgroundColor) {
        int[] rgb = ColorUtil.parseColor(backgroundColor);
        double luminance = (0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]) / 255.0;
        return luminance > 0.72 ? "#111827" : "#FFFFFF";
    }

    private static String code(String value) {
        return "<code style=\"background:rgba(15,23,42,0.45);padding:1px 6px;border-radius:6px;\">" +
            escapeHtml(value) + "</code>";
    }

    private static String nullToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String formatNumber(double value) {
        if (Math.floor(value) == value) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static int packRgb(int red, int green, int blue) {
        return (red << 16) | (green << 8) | blue;
    }

    private static String formatHexColor(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    private static String sanitizeName(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        String sanitized = value.trim().toLowerCase()
            .replaceAll("[^a-z0-9_-]+", "-")
            .replaceAll("-{2,}", "-")
            .replaceAll("^-|-$", "");

        if (sanitized.isBlank()) {
            return "unknown";
        }
        if (sanitized.length() > 48) {
            return sanitized.substring(0, 48);
        }
        return sanitized;
    }

    private static String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    public record MarkerBuildResult(String markerId, ExtrudeMarker marker, BlueMapDynamicMarkerState dynamicState) {
    }

    private record PolygonData(Shape shape, double centerX, double centerZ) {
    }

    private record HeightRange(double minY, double maxY, String source, boolean clamped, boolean expandedToVisibleVolume) {
    }

    private record MarkerStyle(
        String normalizedColor,
        String originalFlashMode,
        String effectiveHexColor,
        int effectiveRgb,
        Color lineColor,
        Color fillColor,
        int lineWidth,
        float fillAlpha,
        long phaseOffsetMs,
        long phaseBucket
    ) {
    }
}
