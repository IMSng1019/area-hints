package areahint.file;

import areahint.data.AreaData;
import areahint.data.AreaData.Vertex;
import areahint.data.AreaData.AltitudeData;
import areahint.data.ConfigData;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON辅助工具类
 * 用于序列化和反序列化AreaData和ConfigData对象
 */
public class JsonHelper {
    
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(AreaData.class, new AreaDataDeserializer())
        .registerTypeAdapter(Vertex.class, new VertexDeserializer())
        .registerTypeAdapter(AltitudeData.class, new AltitudeDataDeserializer())
            .setPrettyPrinting()
            .create();
    
    /**
     * 将AreaData列表序列化为JSON字符串
     */
    public static String toJson(List<AreaData> areaDataList) {
        return gson.toJson(areaDataList);
    }
    
    /**
     * 将单个AreaData对象序列化为JSON字符串
     */
    public static String toJsonSingle(AreaData areaData) {
        return gson.toJson(areaData);
    }
    
    /**
     * 将JSON字符串反序列化为AreaData列表
     */
    public static List<AreaData> fromJson(String json) {
        Type listType = new TypeToken<List<AreaData>>(){}.getType();
        return gson.fromJson(json, listType);
    }
    
    /**
     * 将JSON字符串反序列化为单个AreaData对象
     */
    public static AreaData fromJsonSingle(String json) {
        return gson.fromJson(json, AreaData.class);
    }
    
    /**
     * 将ConfigData序列化为JSON字符串
     */
    public static String toJson(ConfigData configData) {
        return gson.toJson(configData);
    }
    
    /**
     * 将JSON字符串反序列化为ConfigData
     */
    public static ConfigData configFromJson(String json) {
        return gson.fromJson(json, ConfigData.class);
    }
    
    /**
     * AreaData自定义反序列化器
     */
    private static class AreaDataDeserializer implements JsonDeserializer<AreaData> {
        @Override
        public AreaData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            
            AreaData areaData = new AreaData();
            
            // 反序列化基本字段
                if (jsonObject.has("name")) {
                areaData.setName(jsonObject.get("name").getAsString());
                }
                
            if (jsonObject.has("level")) {
                areaData.setLevel(jsonObject.get("level").getAsInt());
            }
            
            if (jsonObject.has("base-name") && !jsonObject.get("base-name").isJsonNull()) {
                areaData.setBaseName(jsonObject.get("base-name").getAsString());
                                    }
            
            // 反序列化vertices
            if (jsonObject.has("vertices")) {
                JsonArray verticesArray = jsonObject.getAsJsonArray("vertices");
                List<Vertex> vertices = context.deserialize(verticesArray, new TypeToken<List<Vertex>>(){}.getType());
                areaData.setVertices(vertices);
                }
                
            // 反序列化second-vertices
            if (jsonObject.has("second-vertices")) {
                    JsonArray secondVerticesArray = jsonObject.getAsJsonArray("second-vertices");
                List<Vertex> secondVertices = context.deserialize(secondVerticesArray, new TypeToken<List<Vertex>>(){}.getType());
                areaData.setSecondVertices(secondVertices);
            }
            
            // 反序列化altitude（新增）
            if (jsonObject.has("altitude")) {
                JsonElement altitudeElement = jsonObject.get("altitude");
                if (!altitudeElement.isJsonNull()) {
                    AltitudeData altitude = context.deserialize(altitudeElement, AltitudeData.class);
                    areaData.setAltitude(altitude);
                    }
                }
                
            // 反序列化signature（签名）
            if (jsonObject.has("signature")) {
                JsonElement signatureElement = jsonObject.get("signature");
                if (!signatureElement.isJsonNull()) {
                    areaData.setSignature(signatureElement.getAsString());
                }
            }

            // 反序列化signatures（签名列表）
            if (jsonObject.has("signatures") && jsonObject.get("signatures").isJsonArray()) {
                JsonArray signaturesArray = jsonObject.getAsJsonArray("signatures");
                List<String> signatures = new ArrayList<>();
                for (JsonElement signatureElement : signaturesArray) {
                    if (signatureElement != null && !signatureElement.isJsonNull() && signatureElement.isJsonPrimitive()) {
                        signatures.add(signatureElement.getAsString());
                    }
                }
                areaData.setSignatures(signatures);
            }

            // 反序列化color（颜色）
            if (jsonObject.has("color")) {
                JsonElement colorElement = jsonObject.get("color");
                if (!colorElement.isJsonNull()) {
                    areaData.setColor(colorElement.getAsString());
                }
            }
            
            // 反序列化surfacename（联合域名）
            if (jsonObject.has("surfacename")) {
                JsonElement surfacenameElement = jsonObject.get("surfacename");
                if (!surfacenameElement.isJsonNull()) {
                    areaData.setSurfacename(surfacenameElement.getAsString());
                }
            }

            // 非字符串模式按未设置处理，避免一个错误属性导致整个维度的域名无法加载。
            // 保留服务端确认的域名类型，旧文件缺失时由 AreaData 返回 rural。
            JsonElement areaType = jsonObject.get("type");
            if (areaType != null && areaType.isJsonPrimitive() && areaType.getAsJsonPrimitive().isString()) {
                areaData.setType(areaType.getAsString());
            }
            JsonElement gamemode = jsonObject.get("gamemode");
            if (gamemode != null && gamemode.isJsonPrimitive() && gamemode.getAsJsonPrimitive().isString()) {
                areaData.setGamemode(gamemode.getAsString());
            }

            // 反序列化副字幕字段；客户端聊天按钮的悬浮提示依赖这里保留 subtitle。
            if (jsonObject.has("subtitle")) {
                JsonElement subtitleElement = jsonObject.get("subtitle");
                if (!subtitleElement.isJsonNull()) {
                    areaData.setSubtitle(subtitleElement.getAsString());
                }
            }

            // 反序列化副字幕颜色，避免替换颜色流程进入确认界面时丢失原颜色。
            if (jsonObject.has("subtitlecolor")) {
                JsonElement subtitleColorElement = jsonObject.get("subtitlecolor");
                if (!subtitleColorElement.isJsonNull()) {
                    areaData.setSubtitleColor(subtitleColorElement.getAsString());
                }
            }
            
            return areaData;
        }
    }
    
    /**
     * Vertex自定义反序列化器
     * 支持两种格式：[x, z] 数组格式和 {"x": x, "z": z} 对象格式
     */
    private static class VertexDeserializer implements JsonDeserializer<Vertex> {
        @Override
        public Vertex deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
                throws JsonParseException {
            if (json.isJsonArray()) {
                // 处理数组格式 [x, z]
                JsonArray array = json.getAsJsonArray();
                if (array.size() >= 2) {
                    double x = array.get(0).getAsDouble();
                    double z = array.get(1).getAsDouble();
                    System.out.println("JsonHelper: 转换数组格式顶点 [" + x + ", " + z + "] 为对象格式");
                    return new Vertex(x, z);
                }
            } else if (json.isJsonObject()) {
                // 处理对象格式 {"x": x, "z": z}
                JsonObject obj = json.getAsJsonObject();
                if (obj.has("x") && obj.has("z")) {
                    double x = obj.get("x").getAsDouble();
                    double z = obj.get("z").getAsDouble();
                    return new Vertex(x, z);
                }
            }
            
            throw new JsonParseException("无效的顶点格式: " + json.toString());
        }
    }
    
    /**
     * AltitudeData自定义反序列化器
     * 处理altitude字段，支持null值
     */
    private static class AltitudeDataDeserializer implements JsonDeserializer<AltitudeData> {
        @Override
        public AltitudeData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
                throws JsonParseException {
            if (json.isJsonNull()) {
                return null;
            }
            
            JsonObject jsonObject = json.getAsJsonObject();
            AltitudeData altitude = new AltitudeData();
            
            // 处理max字段
            if (jsonObject.has("max")) {
                JsonElement maxElement = jsonObject.get("max");
                if (!maxElement.isJsonNull()) {
                    altitude.setMax(maxElement.getAsDouble());
                        }
                    }
            
            // 处理min字段
            if (jsonObject.has("min")) {
                JsonElement minElement = jsonObject.get("min");
                if (!minElement.isJsonNull()) {
                    altitude.setMin(minElement.getAsDouble());
                }
            }
            
            return altitude;
        }
    }
} 
