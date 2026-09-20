package areahint.detection;

import areahint.AreashintClient;
import areahint.data.AreaData;
import areahint.data.AreaData.AltitudeData;
import areahint.debug.ClientDebugManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 高度预筛选器
 * 根据玩家的Y坐标（高度）筛选符合高度条件的区域
 * 这是在进行射线检测和AABB检测之前的预筛选步骤，可以提高检测效率
 */
public class AltitudeFilter {

    /**
     * 根据玩家高度筛选符合条件的区域
     * @param playerY 玩家的Y坐标（高度）
     * @param allAreas 所有区域列表
     * @return 符合高度条件的区域列表
     */
    public static List<AreaData> filterByAltitude(double playerY, List<AreaData> allAreas) {
        if (allAreas == null || allAreas.isEmpty()) {
            return allAreas;
        }

        // 记录筛选前的区域数量
        int originalCount = allAreas.size();

        // 是否输出调试信息：关闭debug时筛选路径不产生任何调试相关的分配
        boolean debugEnabled = ClientDebugManager.isDebugEnabled();

        // 进行高度筛选（使用for循环避免Stream对象分配，每个域名的高度只判定一次）
        List<AreaData> filteredAreas = new ArrayList<>(allAreas.size());
        List<String> debugDetails = debugEnabled ? new ArrayList<>(allAreas.size()) : null;
        for (AreaData area : allAreas) {
            boolean inRange = isPlayerInAltitudeRange(playerY, area);
            if (inRange) {
                filteredAreas.add(area);
            }
            // 调试明细在同一遍循环里记录，避免为了打印日志再遍历一次重复计算高度
            if (debugEnabled) {
                debugDetails.add(String.format("  区域[%s] 高度范围=%s, 符合条件=%s",
                    area.getName(), getAltitudeInfo(area.getAltitude()), inRange ? "是" : "否"));
            }
        }

        // 记录筛选后的区域数量
        int filteredCount = filteredAreas.size();

        // 输出调试信息
        if (debugEnabled) {
            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.AREA_DETECTION, String.format(
                "高度预筛选: 玩家高度=%.1f, 原始区域数=%d, 筛选后区域数=%d",
                playerY, originalCount, filteredCount));

            // 详细记录每个区域的筛选结果（结果已在上面同一遍循环里算好）
            for (String detail : debugDetails) {
                ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.AREA_DETECTION, detail);
            }
        }

        // 记录日志（加debug守卫，关闭时不做参数装箱与varargs数组分配）
        if (AreashintClient.LOGGER.isDebugEnabled()) {
            AreashintClient.LOGGER.debug("高度预筛选完成: 玩家高度={}, 筛选前={}, 筛选后={}",
                playerY, originalCount, filteredCount);
        }

        return filteredAreas;
    }

    /**
     * 检查玩家是否在指定区域的高度范围内
     * 只做判定、不产生任何列表分配，供检测热路径直接调用
     * @param playerY 玩家的Y坐标
     * @param area 要检查的区域
     * @return 是否在高度范围内
     */
    public static boolean isPlayerInAltitudeRange(double playerY, AreaData area) {
        AltitudeData altitude = area.getAltitude();
        
        // 如果区域没有设置高度限制，则认为符合条件
        if (altitude == null) {
            return true;
        }
        
        // 使用AltitudeData的内置方法检查范围
        return altitude.isInRange(playerY);
    }

    /**
     * 获取高度信息的字符串表示
     * @param altitude 高度数据
     * @return 高度信息字符串
     */
    private static String getAltitudeInfo(AltitudeData altitude) {
        if (altitude == null) {
            return "无限制";
        }
        
        String minStr = altitude.getMin() != null ? altitude.getMin().toString() : "无限制";
        String maxStr = altitude.getMax() != null ? altitude.getMax().toString() : "无限制";
        
        return String.format("[%s, %s]", minStr, maxStr);
    }

    /**
     * 验证高度数据的有效性
     * @param altitude 高度数据
     * @return 验证结果，包含是否有效和错误信息
     */
    public static ValidationResult validateAltitude(AltitudeData altitude) {
        if (altitude == null) {
            return new ValidationResult(true, "高度数据为空，允许任意高度");
        }
        
        if (!altitude.isValid()) {
            return new ValidationResult(false, "最大高度不能小于最小高度");
        }
        
        // 检查高度值的合理性（Minecraft世界高度通常在-64到320之间）
        if (altitude.getMin() != null && (altitude.getMin() < -64 || altitude.getMin() > 320)) {
            return new ValidationResult(false, 
                String.format("最小高度 %.1f 超出合理范围 [-64, 320]", altitude.getMin()));
        }
        
        if (altitude.getMax() != null && (altitude.getMax() < -64 || altitude.getMax() > 320)) {
            return new ValidationResult(false, 
                String.format("最大高度 %.1f 超出合理范围 [-64, 320]", altitude.getMax()));
        }
        
        return new ValidationResult(true, "高度数据有效");
    }

    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format("ValidationResult{valid=%s, message='%s'}", valid, message);
        }
    }
}
