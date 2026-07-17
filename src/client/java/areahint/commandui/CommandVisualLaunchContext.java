package areahint.commandui;

import areahint.data.AreaData;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 保存地图入口选中的域名，并只允许下一次匹配的域名选择列表消费。
 */
public final class CommandVisualLaunchContext {
    private static final long TIMEOUT_MILLIS = 20_000L;
    private static Context current;

    private CommandVisualLaunchContext() {
    }

    public static synchronized void begin(String operationId, String areaName, String dimensionId, long requestId) {
        current = new Context(operationId, areaName, dimensionId, requestId,
            System.currentTimeMillis() + TIMEOUT_MILLIS);
    }

    public static synchronized boolean isActiveFor(String operationId) {
        return isCurrentValid() && current.operationId().equals(operationId);
    }

    public static synchronized <T> T findMatchingValue(
        String operationId, List<WizardSelectionListScreen.SelectionItem<T>> items) {
        if (!isActiveFor(operationId) || items == null) {
            return null;
        }
        for (WizardSelectionListScreen.SelectionItem<T> item : items) {
            if (current.areaName().equals(extractName(item.value()))) {
                return item.value();
            }
        }
        return null;
    }

    public static synchronized void consume() {
        current = null;
    }

    public static synchronized void clear() {
        current = null;
    }

    public static synchronized void tick() {
        isCurrentValid();
    }

    public static synchronized List<AreaData> ensureTargetIncluded(String operationId,
                                                                   List<AreaData> filtered,
                                                                   List<AreaData> allAreas) {
        List<AreaData> result = new ArrayList<>(filtered == null ? List.of() : filtered);
        if (!isActiveFor(operationId) || allAreas == null) {
            return result;
        }
        for (AreaData area : result) {
            if (area != null && current.areaName().equals(area.getName())) {
                return result;
            }
        }
        for (AreaData area : allAreas) {
            if (area != null && current.areaName().equals(area.getName())) {
                result.add(area);
                break;
            }
        }
        return result;
    }

    public static boolean requiresAreaTarget(String operationId) {
        return !"replacesubtitlesize".equals(operationId);
    }

    private static boolean isCurrentValid() {
        if (current == null) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        String currentDimension = client != null && client.world != null
            ? client.world.getRegistryKey().getValue().toString() : null;
        if (System.currentTimeMillis() > current.expiresAtMillis()
            || currentDimension == null || !current.dimensionId().equals(currentDimension)) {
            current = null;
            return false;
        }
        return true;
    }

    private static String extractName(Object value) {
        if (value instanceof AreaData area) {
            return area.getName();
        }
        if (value instanceof String string) {
            return string;
        }
        if (value == null) {
            return null;
        }
        for (String methodName : List.of("name", "getName", "id", "areaName")) {
            try {
                Method method = value.getClass().getMethod(methodName);
                Object result = method.invoke(value);
                if (result instanceof String string) {
                    return string;
                }
            } catch (Exception ignored) {
                // 不同流程使用不同列表项类型，只尝试已知的无参名称访问器。
            }
        }
        return null;
    }

    private record Context(String operationId, String areaName, String dimensionId,
                           long requestId, long expiresAtMillis) {
    }
}
