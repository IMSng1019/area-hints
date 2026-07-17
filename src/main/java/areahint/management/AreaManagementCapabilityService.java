package areahint.management;

import areahint.data.AreaData;
import areahint.network.Packets;
import areahint.permission.PermissionNodes;
import areahint.permission.PermissionService;
import areahint.util.AreaPermissionUtil;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 域名管理操作的统一能力判断，列表查询与实际写入都必须调用这里。
 */
public final class AreaManagementCapabilityService {
    public static final String TCP = "tcp";
    public static final String UDP = "udp";
    public static final String RENAME = "rename";
    public static final String RECOLOR = "recolor";
    public static final String SET_HIGH = "sethigh";
    public static final String ADD_DESCRIPTION = "adddescription";
    public static final String REPLACE_DESCRIPTION = "replacedescription";
    public static final String DELETE_DESCRIPTION = "deletedescription";
    public static final String ADD_SUBTITLE = "addsubtitle";
    public static final String REPLACE_SUBTITLE = "replacesubtitle";
    public static final String DELETE_SUBTITLE = "deletesubtitle";
    public static final String REPLACE_SUBTITLE_COLOR = "replacesubtitlecolor";
    public static final String REPLACE_SUBTITLE_SIZE = "replacesubtitlesize";
    public static final String ADD_SIGNATURE = "addsignature";
    public static final String DELETE_SIGNATURE = "deletesignature";
    public static final String EXPAND_AREA = "expandarea";
    public static final String SHRINK_AREA = "shrinkarea";
    public static final String DIVIDE_AREA = "dividearea";
    public static final String ADD_HINT = "addhint";
    public static final String DELETE_HINT = "deletehint";
    public static final String DELETE = "delete";

    public static final List<String> ORDERED_OPERATIONS = List.of(
        TCP, UDP,
        RENAME, RECOLOR, SET_HIGH,
        ADD_DESCRIPTION, REPLACE_DESCRIPTION, DELETE_DESCRIPTION,
        ADD_SUBTITLE, REPLACE_SUBTITLE, DELETE_SUBTITLE, REPLACE_SUBTITLE_COLOR, REPLACE_SUBTITLE_SIZE,
        ADD_SIGNATURE, DELETE_SIGNATURE,
        EXPAND_AREA, SHRINK_AREA, DIVIDE_AREA, ADD_HINT, DELETE_HINT,
        DELETE
    );

    private AreaManagementCapabilityService() {
    }

    public static List<String> getAllowedOperations(ServerPlayerEntity player, AreaData area, List<AreaData> allAreas) {
        List<String> allowed = new ArrayList<>();
        for (String operation : ORDERED_OPERATIONS) {
            if (canPerform(player, operation, area, allAreas)) {
                allowed.add(operation);
            }
        }
        return List.copyOf(allowed);
    }

    /**
     * 校验客户端提交的维度是否仍为玩家当前维度，兼容完整维度ID和内部维度类型。
     */
    public static boolean isCurrentDimension(ServerPlayerEntity player, String requestedDimension) {
        if (player == null || player.getWorld() == null) {
            return false;
        }
        String currentType = Packets.convertDimensionPathToType(
            player.getWorld().getRegistryKey().getValue().getPath());
        String requestedType = normalizeDimensionType(requestedDimension);
        return currentType != null && currentType.equals(requestedType);
    }

    public static boolean canPerform(ServerPlayerEntity player, String operation, AreaData area, List<AreaData> allAreas) {
        if (player == null || operation == null || area == null) {
            return false;
        }
        List<AreaData> areas = allAreas == null ? List.of() : allAreas;
        String playerName = player.getGameProfile().getName();

        return switch (operation) {
            case TCP, UDP -> PermissionService.hasCommandPermission(player, PermissionNodes.TELEPORT, 0);
            case RENAME -> PermissionService.hasNodeOr(player, PermissionNodes.RENAME,
                () -> player.hasPermissionLevel(2) || AreaPermissionUtil.isSignedBy(area, playerName));
            case RECOLOR -> PermissionService.hasNodeOr(player, PermissionNodes.RECOLOR,
                () -> player.hasPermissionLevel(2) || AreaPermissionUtil.isSignedBy(area, playerName));
            case SET_HIGH -> canSetHigh(player, area, areas, playerName);
            case ADD_DESCRIPTION, REPLACE_DESCRIPTION ->
                PermissionService.hasCommandPermission(player, PermissionNodes.ADD_DESCRIPTION, 0)
                    && AreaPermissionUtil.canModifyArea(player, area, areas);
            case DELETE_DESCRIPTION ->
                PermissionService.hasCommandPermission(player, PermissionNodes.DELETE_DESCRIPTION, 0)
                    && AreaPermissionUtil.canModifyArea(player, area, areas);
            case ADD_SUBTITLE, REPLACE_SUBTITLE -> PermissionService.hasNodeOr(player, PermissionNodes.ADD_SUBTITLE,
                () -> AreaPermissionUtil.canModifyArea(player, area, areas));
            case DELETE_SUBTITLE -> area.hasSubtitle()
                && PermissionService.hasNodeOr(player, PermissionNodes.DELETE_SUBTITLE,
                    () -> player.hasPermissionLevel(2) || AreaPermissionUtil.isSignedBy(area, playerName));
            case REPLACE_SUBTITLE_COLOR -> area.hasSubtitle()
                && PermissionService.hasNodeOr(player, PermissionNodes.REPLACE_SUBTITLE_COLOR,
                    () -> AreaPermissionUtil.canModifyArea(player, area, areas));
            case REPLACE_SUBTITLE_SIZE ->
                PermissionService.hasCommandPermission(player, PermissionNodes.REPLACE_SUBTITLE_SIZE, 0);
            case ADD_SIGNATURE -> canModifySignature(player, area, areas, PermissionNodes.ADDSIGNATURE);
            case DELETE_SIGNATURE -> !area.getSignatures().isEmpty()
                && canModifySignature(player, area, areas, PermissionNodes.DELETESIGNATURE);
            case EXPAND_AREA -> PermissionService.hasNodeOr(player, PermissionNodes.EXPANDAREA,
                () -> AreaPermissionUtil.canModifyArea(player, area, areas));
            case SHRINK_AREA -> PermissionService.hasNodeOr(player, PermissionNodes.SHRINKAREA,
                () -> player.hasPermissionLevel(2)
                    || AreaPermissionUtil.isBaseSignedByPlayer(area.getBaseName(), areas, playerName));
            case DIVIDE_AREA -> PermissionService.hasNodeOr(player, PermissionNodes.DIVIDEAREA,
                () -> AreaPermissionUtil.canModifyArea(player, area, areas));
            case ADD_HINT -> PermissionService.hasNodeOr(player, PermissionNodes.ADDHINT,
                () -> AreaPermissionUtil.canModifyArea(player, area, areas));
            case DELETE_HINT -> PermissionService.hasNodeOr(player, PermissionNodes.DELETEHINT,
                () -> AreaPermissionUtil.canModifyArea(player, area, areas));
            case DELETE -> !hasChildren(area, areas)
                && PermissionService.hasNodeOr(player, PermissionNodes.DELETE,
                    () -> player.hasPermissionLevel(2) || AreaPermissionUtil.isSignedBy(area, playerName));
            default -> false;
        };
    }

    private static boolean canSetHigh(ServerPlayerEntity player, AreaData area,
                                      List<AreaData> areas, String playerName) {
        return PermissionService.hasNodeOr(player, PermissionNodes.SETHIGH, () -> {
            if (player.hasPermissionLevel(2) || AreaPermissionUtil.isSignedBy(area, playerName)) {
                return true;
            }
            for (AreaData otherArea : areas) {
                if (area.getName().equals(otherArea.getBaseName())
                    && AreaPermissionUtil.isSignedBy(otherArea, playerName)) {
                    return true;
                }
            }
            return false;
        });
    }

    private static boolean canModifySignature(ServerPlayerEntity player, AreaData area,
                                              List<AreaData> areas, String permissionNode) {
        return PermissionService.hasNodeOr(player, permissionNode, () -> {
            if (player.hasPermissionLevel(2)) {
                return true;
            }
            return AreaPermissionUtil.isBaseSignedByPlayer(area.getBaseName(), areas,
                player.getGameProfile().getName());
        });
    }

    private static boolean hasChildren(AreaData area, List<AreaData> areas) {
        for (AreaData candidate : areas) {
            if (candidate != null && area.getName().equals(candidate.getBaseName())) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeDimensionType(String dimension) {
        if (dimension == null) {
            return null;
        }
        String normalized = dimension.trim().toLowerCase(Locale.ROOT);
        int colon = normalized.lastIndexOf(':');
        String path = colon >= 0 ? normalized.substring(colon + 1) : normalized;
        return Packets.convertDimensionPathToType(path);
    }
}
