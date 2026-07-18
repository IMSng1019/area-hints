package areahint.log;

import areahint.AreashintClient;
import areahint.data.AreaData;
import areahint.detection.AreaDetector;
import net.minecraft.util.Identifier;

import java.util.Objects;

/**
 * 区域变化追踪器
 * 用于追踪玩家进入和离开区域，并发送日志消息
 */
public class AreaChangeTracker {
    // 当前所在的区域数据
    private static AreaData currentAreaData = null;
    // 将名称、维度和版本作为一个不可变对象发布，保证 Xaero 每次读取到完整一致的判定状态
    private static volatile DetectionState detectionState = new DetectionState(null, null, 0L);

    /**
     * 检测区域变化并发送日志消息
     * @param areaDetector 区域检测器
     * @param x 玩家X坐标
     * @param y 玩家Y坐标
     * @param z 玩家Z坐标
     * @param currentDimension 当前维度标识符
     * @return 格式化后的区域名称（用于显示）
     */
    public static String detectAndLogAreaChange(AreaDetector areaDetector, double x, double y, double z, Identifier currentDimension) {
        // 获取当前区域数据（未格式化的原始数据）
        AreaData newAreaData = areaDetector.findAreaRaw(x, y, z);

        // 获取维度域名
        String dimensionalName = null;
        if (currentDimension != null) {
            dimensionalName = areahint.dimensional.ClientDimensionalNameManager.getDimensionalName(currentDimension.toString());
        }

        // 检测区域变化
        boolean areaChanged = false;
        AreaData oldAreaData = currentAreaData;

        if (currentAreaData == null && newAreaData != null) {
            // 进入新区域
            areaChanged = true;
            currentAreaData = newAreaData;

            // 发送进入消息
            ClientLogNetworking.sendEnterAreaMessage(
                newAreaData.getName(),
                newAreaData.getLevel(),
                newAreaData.getSurfacename(),
                dimensionalName
            );

        } else if (currentAreaData != null && newAreaData == null) {
            // 离开区域
            areaChanged = true;

            // 发送离开消息
            ClientLogNetworking.sendLeaveAreaMessage(
                currentAreaData.getName(),
                currentAreaData.getLevel(),
                currentAreaData.getSurfacename(),
                dimensionalName
            );

            currentAreaData = null;

        } else if (currentAreaData != null && newAreaData != null &&
                   !currentAreaData.getName().equals(newAreaData.getName())) {
            // 从一个区域进入另一个区域
            areaChanged = true;

            // 先发送离开旧区域的消息
            ClientLogNetworking.sendLeaveAreaMessage(
                currentAreaData.getName(),
                currentAreaData.getLevel(),
                currentAreaData.getSurfacename(),
                dimensionalName
            );

            // 再发送进入新区域的消息
            ClientLogNetworking.sendEnterAreaMessage(
                newAreaData.getName(),
                newAreaData.getLevel(),
                newAreaData.getSurfacename(),
                dimensionalName
            );

            currentAreaData = newAreaData;
        }

        // 无论域名是否变化都同步当前维度，只有有效状态变化时才会增加版本
        publishDetectionState(currentAreaData, currentDimension);

        // 直接用已有的AreaData格式化，避免二次findArea()
        if (newAreaData != null) {
            return areaDetector.formatAreaNameFromData(newAreaData);
        }

        return null;
    }

    /**
     * 处理异步检测的预计算结果（在主线程调用）
     * @param newAreaData 异步检测到的区域数据
     * @param currentDimension 当前维度标识符
     * @return 是否发生了区域变化
     */
    public static boolean handlePrecomputedChange(AreaData newAreaData, Identifier currentDimension) {
        String dimensionalName = null;
        if (currentDimension != null) {
            dimensionalName = areahint.dimensional.ClientDimensionalNameManager.getDimensionalName(currentDimension.toString());
        }

        boolean areaChanged = false;
        if (currentAreaData == null && newAreaData != null) {
            currentAreaData = newAreaData;
            ClientLogNetworking.sendEnterAreaMessage(newAreaData.getName(), newAreaData.getLevel(), newAreaData.getSurfacename(), dimensionalName);
            areaChanged = true;
        } else if (currentAreaData != null && newAreaData == null) {
            ClientLogNetworking.sendLeaveAreaMessage(currentAreaData.getName(), currentAreaData.getLevel(), currentAreaData.getSurfacename(), dimensionalName);
            currentAreaData = null;
            areaChanged = true;
        } else if (currentAreaData != null && newAreaData != null && !currentAreaData.getName().equals(newAreaData.getName())) {
            ClientLogNetworking.sendLeaveAreaMessage(currentAreaData.getName(), currentAreaData.getLevel(), currentAreaData.getSurfacename(), dimensionalName);
            ClientLogNetworking.sendEnterAreaMessage(newAreaData.getName(), newAreaData.getLevel(), newAreaData.getSurfacename(), dimensionalName);
            currentAreaData = newAreaData;
            areaChanged = true;
        }
        // 异步检测只负责计算，回到主线程后统一发布供标题、日志和 Xaero 共用的状态
        publishDetectionState(currentAreaData, currentDimension);
        return areaChanged;
    }

    /**
     * 重置当前区域（在切换维度或世界时调用）
     */
    public static void reset() {
        currentAreaData = null;
        publishDetectionState(null, null);
    }

    /**
     * 玩家进入世界时调用
     */
    public static void onWorldEnter() {
        ClientLogManager.onWorldEnter();
        reset();
    }

    /**
     * 获取当前区域数据
     * @return 当前区域数据
     */
    public static AreaData getCurrentAreaData() {
        return currentAreaData;
    }

    /**
     * 获取已经由模组完成判定的不可变状态，Xaero 只消费该结果而不重复检测。
     * @return 同一时刻的实际域名名称、维度标识和状态版本
     */
    public static DetectionState getDetectionState() {
        return detectionState;
    }

    /**
     * 仅在实际域名或维度变化时递增版本，避免普通检测帧使填充差集失效。
     */
    private static synchronized void publishDetectionState(AreaData area, Identifier dimension) {
        String areaName = area == null ? null : area.getName();
        String dimensionId = dimension == null ? null : dimension.toString();
        DetectionState previous = detectionState;
        if (Objects.equals(previous.areaName(), areaName)
                && Objects.equals(previous.dimensionId(), dimensionId)) {
            return;
        }
        detectionState = new DetectionState(areaName, dimensionId, previous.revision() + 1L);
    }

    /**
     * 域名判定的只读快照，记录中的三个字段通过同一次 volatile 写入原子发布。
     */
    public record DetectionState(String areaName, String dimensionId, long revision) {
    }
}
