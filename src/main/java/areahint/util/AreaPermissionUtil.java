package areahint.util;

import areahint.data.AreaData;
import areahint.dimensional.DimensionalNameManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Map;

public final class AreaPermissionUtil {
    private AreaPermissionUtil() {}

    public static boolean isSignedBy(AreaData area, String playerName) {
        return area != null && playerName != null && area.hasSignature(playerName);
    }

    // 语义较宽：允许域名签名者或上级域名签名者修改，不适合 delete/shrink 等特殊权限场景。
    public static boolean canModifyArea(ServerPlayerEntity player, AreaData area, List<AreaData> allAreas) {
        if (player == null || area == null) return false;
        if (player.hasPermissionLevel(2)) return true;
        String playerName = areahint.util.ProfileCompat.name(player.getGameProfile());
        if (isSignedBy(area, playerName)) return true;
        return isBaseSignedByPlayer(area.getBaseName(), allAreas, playerName);
    }

    public static boolean canModifyAreaName(ServerPlayerEntity player, String areaName, List<AreaData> allAreas) {
        return canModifyArea(player, findByName(allAreas, areaName), allAreas);
    }

    public static AreaData findByName(List<AreaData> areas, String name) {
        if (areas == null || name == null) return null;
        for (AreaData area : areas) {
            if (area != null && name.equals(area.getName())) return area;
        }
        return null;
    }

    public static boolean isBaseSignedByPlayer(String baseName, List<AreaData> allAreas, String playerName) {
        String cleanedBaseName = cleanText(baseName);
        if (cleanedBaseName == null || playerName == null) {
            return false;
        }

        AreaData baseArea = findByName(allAreas, cleanedBaseName);
        if (isSignedBy(baseArea, playerName)) {
            return true;
        }

        return isDimensionalNameSignedBy(cleanedBaseName, playerName);
    }

    /**
     * 按维度ID或维度显示名查维度域名签名。
     *
     * <p>普通一级域名的 base-name 可能写的是“蛮荒大陆”这样的维度域名显示名，
     * 也可能写 minecraft:overworld 这样的维度ID，所以两种都要兼容。</p>
     */
    public static boolean isDimensionalNameSignedBy(String dimensionNameOrId, String playerName) {
        String cleanedValue = cleanText(dimensionNameOrId);
        String cleanedPlayerName = cleanText(playerName);
        if (cleanedValue == null || cleanedPlayerName == null) {
            return false;
        }

        String directSignature = DimensionalNameManager.getDimensionalSignature(cleanedValue);
        if (cleanedPlayerName.equals(cleanText(directSignature))) {
            return true;
        }

        for (Map.Entry<String, String> entry : DimensionalNameManager.getAllDimensionalNames().entrySet()) {
            if (cleanedValue.equals(entry.getKey()) || cleanedValue.equals(entry.getValue())) {
                String signature = DimensionalNameManager.getDimensionalSignature(entry.getKey());
                return cleanedPlayerName.equals(cleanText(signature));
            }
        }

        return false;
    }

    private static String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
