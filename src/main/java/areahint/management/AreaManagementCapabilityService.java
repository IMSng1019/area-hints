package areahint.management;

import areahint.data.AreaData;
import areahint.network.Packets;
import areahint.permission.PermissionNodes;
import areahint.permission.PermissionService;
import areahint.util.AreaPermissionUtil;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        // 21 个操作共用同一份局部索引，避免每个操作内部反复全表扫描
        AreaIndex index = AreaIndex.of(allAreas);
        List<String> allowed = new ArrayList<>();
        for (String operation : ORDERED_OPERATIONS) {
            if (canPerform(player, operation, area, index)) {
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
        // 单次判断同样走索引：索引只存活于本次调用，allAreas 变化后不会读到旧数据
        return canPerform(player, operation, area, AreaIndex.of(allAreas));
    }

    private static boolean canPerform(ServerPlayerEntity player, String operation, AreaData area, AreaIndex index) {
        if (player == null || operation == null || area == null) {
            return false;
        }
        String playerName = player.getGameProfile().getName();

        return switch (operation) {
            case TCP, UDP -> PermissionService.hasCommandPermission(player, PermissionNodes.TELEPORT, 0);
            case RENAME -> PermissionService.hasNodeOr(player, PermissionNodes.RENAME,
                () -> player.hasPermissionLevel(2) || AreaPermissionUtil.isSignedBy(area, playerName));
            case RECOLOR -> PermissionService.hasNodeOr(player, PermissionNodes.RECOLOR,
                () -> player.hasPermissionLevel(2) || AreaPermissionUtil.isSignedBy(area, playerName));
            case SET_HIGH -> canSetHigh(player, area, index, playerName);
            case ADD_DESCRIPTION, REPLACE_DESCRIPTION ->
                PermissionService.hasCommandPermission(player, PermissionNodes.ADD_DESCRIPTION, 0)
                    && index.canModifyArea(player, area, playerName);
            case DELETE_DESCRIPTION ->
                PermissionService.hasCommandPermission(player, PermissionNodes.DELETE_DESCRIPTION, 0)
                    && index.canModifyArea(player, area, playerName);
            case ADD_SUBTITLE, REPLACE_SUBTITLE -> PermissionService.hasNodeOr(player, PermissionNodes.ADD_SUBTITLE,
                () -> index.canModifyArea(player, area, playerName));
            case DELETE_SUBTITLE -> area.hasSubtitle()
                && PermissionService.hasNodeOr(player, PermissionNodes.DELETE_SUBTITLE,
                    () -> player.hasPermissionLevel(2) || AreaPermissionUtil.isSignedBy(area, playerName));
            case REPLACE_SUBTITLE_COLOR -> area.hasSubtitle()
                && PermissionService.hasNodeOr(player, PermissionNodes.REPLACE_SUBTITLE_COLOR,
                    () -> index.canModifyArea(player, area, playerName));
            case REPLACE_SUBTITLE_SIZE ->
                PermissionService.hasCommandPermission(player, PermissionNodes.REPLACE_SUBTITLE_SIZE, 0);
            case ADD_SIGNATURE -> canModifySignature(player, area, index, PermissionNodes.ADDSIGNATURE);
            case DELETE_SIGNATURE -> !area.getSignatures().isEmpty()
                && canModifySignature(player, area, index, PermissionNodes.DELETESIGNATURE);
            case EXPAND_AREA -> PermissionService.hasNodeOr(player, PermissionNodes.EXPANDAREA,
                () -> index.canModifyArea(player, area, playerName));
            case SHRINK_AREA -> PermissionService.hasNodeOr(player, PermissionNodes.SHRINKAREA,
                () -> player.hasPermissionLevel(2)
                    || index.isBaseSignedByPlayer(area.getBaseName(), playerName));
            case DIVIDE_AREA -> PermissionService.hasNodeOr(player, PermissionNodes.DIVIDEAREA,
                () -> index.canModifyArea(player, area, playerName));
            case ADD_HINT -> PermissionService.hasNodeOr(player, PermissionNodes.ADDHINT,
                () -> index.canModifyArea(player, area, playerName));
            case DELETE_HINT -> PermissionService.hasNodeOr(player, PermissionNodes.DELETEHINT,
                () -> index.canModifyArea(player, area, playerName));
            case DELETE -> !index.hasChildren(area)
                && PermissionService.hasNodeOr(player, PermissionNodes.DELETE,
                    () -> player.hasPermissionLevel(2) || AreaPermissionUtil.isSignedBy(area, playerName));
            default -> false;
        };
    }

    private static boolean canSetHigh(ServerPlayerEntity player, AreaData area,
                                      AreaIndex index, String playerName) {
        return PermissionService.hasNodeOr(player, PermissionNodes.SETHIGH, () -> {
            if (player.hasPermissionLevel(2) || AreaPermissionUtil.isSignedBy(area, playerName)) {
                return true;
            }
            // 只遍历以本域名为上级的域名，命中顺序与原来的全表扫描一致
            for (AreaData childArea : index.childrenOf(area.getName())) {
                if (AreaPermissionUtil.isSignedBy(childArea, playerName)) {
                    return true;
                }
            }
            return false;
        });
    }

    private static boolean canModifySignature(ServerPlayerEntity player, AreaData area,
                                              AreaIndex index, String permissionNode) {
        return PermissionService.hasNodeOr(player, permissionNode, () -> {
            if (player.hasPermissionLevel(2)) {
                return true;
            }
            return index.isBaseSignedByPlayer(area.getBaseName(), player.getGameProfile().getName());
        });
    }

    /**
     * 单次能力判断内复用的局部索引：父域名 -> 子域名、域名 -> 域名数据。
     * 只在当前调用内构建，allAreas 变化后不会残留旧数据。
     */
    private static final class AreaIndex {
        private final List<AreaData> areas;
        private Map<String, List<AreaData>> childrenByBaseName;
        private Map<String, AreaData> areasByName;

        private AreaIndex(List<AreaData> areas) {
            this.areas = areas;
        }

        private static AreaIndex of(List<AreaData> allAreas) {
            return new AreaIndex(allAreas == null ? List.of() : allAreas);
        }

        // 按需构建：只做权限节点判断的操作不会触发任何遍历
        private Map<String, List<AreaData>> childrenIndex() {
            if (childrenByBaseName == null) {
                Map<String, List<AreaData>> index = new HashMap<>();
                for (AreaData candidate : areas) {
                    if (candidate == null || candidate.getBaseName() == null) {
                        continue;
                    }
                    index.computeIfAbsent(candidate.getBaseName(), key -> new ArrayList<>()).add(candidate);
                }
                childrenByBaseName = index;
            }
            return childrenByBaseName;
        }

        // 同名域名取先出现的那个，与 AreaPermissionUtil.findByName 的语义一致
        private Map<String, AreaData> nameIndex() {
            if (areasByName == null) {
                Map<String, AreaData> index = new HashMap<>();
                for (AreaData candidate : areas) {
                    if (candidate != null && candidate.getName() != null) {
                        index.putIfAbsent(candidate.getName(), candidate);
                    }
                }
                areasByName = index;
            }
            return areasByName;
        }

        private List<AreaData> childrenOf(String baseName) {
            if (baseName == null) {
                return List.of();
            }
            List<AreaData> children = childrenIndex().get(baseName);
            return children == null ? List.of() : children;
        }

        // 与原来的 hasChildren 全表扫描等价；域名为空时视为没有子域名
        private boolean hasChildren(AreaData area) {
            String name = area.getName();
            return name != null && childrenIndex().containsKey(name);
        }

        // 与 AreaPermissionUtil.canModifyArea 等价，只把上级域名查找换成索引
        private boolean canModifyArea(ServerPlayerEntity player, AreaData area, String playerName) {
            if (player.hasPermissionLevel(2)) {
                return true;
            }
            if (AreaPermissionUtil.isSignedBy(area, playerName)) {
                return true;
            }
            return isBaseSignedByPlayer(area.getBaseName(), playerName);
        }

        // 与 AreaPermissionUtil.isBaseSignedByPlayer 等价：先查同名域名签名，再查维度域名签名
        private boolean isBaseSignedByPlayer(String baseName, String playerName) {
            String cleanedBaseName = cleanName(baseName);
            if (cleanedBaseName == null || playerName == null) {
                return false;
            }
            AreaData baseArea = nameIndex().get(cleanedBaseName);
            if (AreaPermissionUtil.isSignedBy(baseArea, playerName)) {
                return true;
            }
            return AreaPermissionUtil.isDimensionalNameSignedBy(cleanedBaseName, playerName);
        }

        // 复刻 AreaPermissionUtil 内部的文本清洗规则，保证判断结果一致
        private static String cleanName(String value) {
            if (value == null) {
                return null;
            }
            String cleaned = value.trim();
            return cleaned.isEmpty() ? null : cleaned;
        }
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
