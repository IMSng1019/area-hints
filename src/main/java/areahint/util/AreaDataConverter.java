package areahint.util;

import areahint.data.AreaData;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.ArrayList;

/**
 * AreaData与JsonObject之间的转换工具类
 * 支持新的surfacename字段和SurfaceNameHandler集成
 */
public class AreaDataConverter {
    
    /**
     * 将AreaData转换为JsonObject
     * @param areaData AreaData对象
     * @return JsonObject
     */
    public static JsonObject toJsonObject(AreaData areaData) {
        if (areaData == null) {
            return null;
        }
        
        JsonObject jsonObject = new JsonObject();
        
        // 基本字段
        jsonObject.addProperty("name", areaData.getName());
        jsonObject.addProperty("level", areaData.getLevel());
        jsonObject.addProperty("base-name", areaData.getBaseName());
        jsonObject.addProperty("signature", areaData.getSignature());
        if (!areaData.getSignatures().isEmpty()) {
            JsonArray signaturesArray = new JsonArray();
            for (String signature : areaData.getSignatures()) {
                signaturesArray.add(signature);
            }
            jsonObject.add("signatures", signaturesArray);
        }
        jsonObject.addProperty("color", areaData.getColor());
        jsonObject.addProperty("surfacename", areaData.getSurfacename());
        
        // vertices数组
        if (areaData.getVertices() != null) {
            JsonArray verticesArray = new JsonArray();
            for (AreaData.Vertex vertex : areaData.getVertices()) {
                JsonObject vertexObj = new JsonObject();
                vertexObj.addProperty("x", vertex.getX());
                vertexObj.addProperty("z", vertex.getZ());
                verticesArray.add(vertexObj);
            }
            jsonObject.add("vertices", verticesArray);
        }
        
        // second-vertices数组
        if (areaData.getSecondVertices() != null) {
            JsonArray secondVerticesArray = new JsonArray();
            for (AreaData.Vertex vertex : areaData.getSecondVertices()) {
                JsonObject vertexObj = new JsonObject();
                vertexObj.addProperty("x", vertex.getX());
                vertexObj.addProperty("z", vertex.getZ());
                secondVerticesArray.add(vertexObj);
            }
            jsonObject.add("second-vertices", secondVerticesArray);
        }
        
        // altitude对象
        if (areaData.getAltitude() != null) {
            JsonObject altitudeObj = new JsonObject();
            if (areaData.getAltitude().getMax() != null) {
                altitudeObj.addProperty("max", areaData.getAltitude().getMax());
            }
            if (areaData.getAltitude().getMin() != null) {
                altitudeObj.addProperty("min", areaData.getAltitude().getMin());
            }
            jsonObject.add("altitude", altitudeObj);
        }
        
        return jsonObject;
    }
    
    /**
     * 从JsonObject创建AreaData
     * @param jsonObject JsonObject
     * @return AreaData对象
     */
    public static AreaData fromJsonObject(JsonObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        
        AreaData areaData = new AreaData();
        
        // 基本字段
        if (jsonObject.has("name") && !jsonObject.get("name").isJsonNull()) {
            areaData.setName(jsonObject.get("name").getAsString());
        }
        if (jsonObject.has("level")) {
            areaData.setLevel(jsonObject.get("level").getAsInt());
        }
        if (jsonObject.has("base-name") && !jsonObject.get("base-name").isJsonNull()) {
            areaData.setBaseName(jsonObject.get("base-name").getAsString());
        }
        if (jsonObject.has("signature") && !jsonObject.get("signature").isJsonNull()) {
            areaData.setSignature(jsonObject.get("signature").getAsString());
        }
        if (jsonObject.has("signatures") && jsonObject.get("signatures").isJsonArray()) {
            JsonArray signaturesArray = jsonObject.getAsJsonArray("signatures");
            ArrayList<String> signatures = new ArrayList<>();
            for (int i = 0; i < signaturesArray.size(); i++) {
                if (!signaturesArray.get(i).isJsonNull() && signaturesArray.get(i).isJsonPrimitive()) {
                    signatures.add(signaturesArray.get(i).getAsString());
                }
            }
            areaData.setSignatures(signatures);
        }
        if (jsonObject.has("color") && !jsonObject.get("color").isJsonNull()) {
            areaData.setColor(jsonObject.get("color").getAsString());
        }
        if (jsonObject.has("surfacename") && !jsonObject.get("surfacename").isJsonNull()) {
            areaData.setSurfacename(jsonObject.get("surfacename").getAsString());
        }
        
        // vertices数组
        if (jsonObject.has("vertices") && jsonObject.get("vertices").isJsonArray()) {
            JsonArray verticesArray = jsonObject.getAsJsonArray("vertices");
            // 确保vertices列表已初始化
            if (areaData.getVertices() == null) {
                areaData.setVertices(new ArrayList<>());
            }
            for (int i = 0; i < verticesArray.size(); i++) {
                JsonObject vertexObj = verticesArray.get(i).getAsJsonObject();
                double x = vertexObj.get("x").getAsDouble();
                double z = vertexObj.get("z").getAsDouble();
                areaData.getVertices().add(new AreaData.Vertex(x, z));
            }
        }
        
        // second-vertices数组
        if (jsonObject.has("second-vertices") && jsonObject.get("second-vertices").isJsonArray()) {
            JsonArray secondVerticesArray = jsonObject.getAsJsonArray("second-vertices");
            // 确保secondVertices列表已初始化
            if (areaData.getSecondVertices() == null) {
                areaData.setSecondVertices(new ArrayList<>());
            }
            for (int i = 0; i < secondVerticesArray.size(); i++) {
                JsonObject vertexObj = secondVerticesArray.get(i).getAsJsonObject();
                double x = vertexObj.get("x").getAsDouble();
                double z = vertexObj.get("z").getAsDouble();
                areaData.getSecondVertices().add(new AreaData.Vertex(x, z));
            }
        }
        
        // altitude对象
        if (jsonObject.has("altitude") && jsonObject.get("altitude").isJsonObject()) {
            JsonObject altitudeObj = jsonObject.getAsJsonObject("altitude");
            AreaData.AltitudeData altitudeData = new AreaData.AltitudeData();
            
            if (altitudeObj.has("max") && !altitudeObj.get("max").isJsonNull()) {
                altitudeData.setMax(altitudeObj.get("max").getAsDouble());
            }
            if (altitudeObj.has("min") && !altitudeObj.get("min").isJsonNull()) {
                altitudeData.setMin(altitudeObj.get("min").getAsDouble());
            }
            
            areaData.setAltitude(altitudeData);
        }
        
        return areaData;
    }
    
    /**
     * 获取域名的显示名称（优先surfacename，否则name）
     * 直接解析AreaData字段，避免为读取两个字符串而重建整个JSON对象树
     * @param areaData AreaData对象
     * @return 显示名称
     */
    public static String getDisplayName(AreaData areaData) {
        if (areaData == null) {
            return "未知域名";
        }
        
        // 语义与SurfaceNameHandler.getDisplayName(JsonObject)一致：surfacename非空白时返回其原值
        String surfaceName = areaData.getSurfacename();
        if (surfaceName != null && !surfaceName.trim().isEmpty()) {
            return surfaceName;
        }
        
        // surfacename缺失或空白时回退到name
        String name = areaData.getName();
        if (name != null) {
            return name;
        }
        
        return "未知域名";
    }
    
    /**
     * 获取域名的实际名称（name字段）
     * @param areaData AreaData对象
     * @return 实际域名名称
     */
    public static String getActualName(AreaData areaData) {
        if (areaData == null) {
            return null;
        }
        
        return areaData.getName();
    }
    
    /**
     * 获取域名的联合名称（surfacename字段）
     * @param areaData AreaData对象
     * @return 联合域名名称
     */
    public static String getSurfaceName(AreaData areaData) {
        if (areaData == null) {
            return null;
        }
        
        return areaData.getSurfacename();
    }
} 