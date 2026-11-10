package areahint.vizedit;

import areahint.data.AreaData.Vertex;
import areahint.vizedit.geometry.VizEditGeometry;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

// 编辑器提交和正式记录共用坐标格式，数值仍是 JSON 数字，但始终保留一位小数而不是依赖 double 的默认输出。
public final class VizEditVertexFormat {
    private VizEditVertexFormat() { }

    // 量化规则与几何优化器一致；每个顶点都复制，不能通过服务端规范化改动基线或调用方的草稿列表。
    public static List<Vertex> quantize(List<Vertex> vertices) {
        List<Vertex> result = new ArrayList<>(vertices.size());
        for (Vertex vertex : vertices) {
            if (vertex == null) throw invalidCoordinate();
            result.add(new Vertex(quantize(vertex.getX()), quantize(vertex.getZ())));
        }
        return List.copyOf(result);
    }

    // 编码阶段拒绝尚未量化的数值，避免仅为格式化而悄悄移动已经展示给玩家的边界。
    public static JsonArray encode(List<Vertex> vertices) {
        JsonArray result = new JsonArray();
        for (Vertex vertex : vertices) {
            if (vertex == null) throw invalidCoordinate();
            JsonObject point = new JsonObject();
            point.add("x", number(vertex.getX()));
            point.add("z", number(vertex.getZ()));
            result.add(point);
        }
        return result;
    }

    private static double quantize(double value) {
        checkCoordinate(value);
        double result = Math.rint(value * 10) / 10;
        return result == 0 ? 0 : result;
    }

    private static JsonPrimitive number(double value) {
        checkCoordinate(value);
        try {
            return new JsonPrimitive(BigDecimal.valueOf(value).setScale(1, RoundingMode.UNNECESSARY));
        } catch (ArithmeticException exception) {
            throw invalidCoordinate();
        }
    }

    private static void checkCoordinate(double value) {
        if (!Double.isFinite(value) || Math.abs(value) > VizEditGeometry.MAX_COORDINATE) throw invalidCoordinate();
    }

    private static VizEditGeometry.GeometryException invalidCoordinate() {
        return new VizEditGeometry.GeometryException("vizedit.error.geometry_coordinates");
    }
}
