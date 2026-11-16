package areahint.xaero;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** 接回 Xaero 由 CursorBox 改为 Tooltip 后失去的旧版协变回调，加载本类不依赖地图模组。 */
public final class XaeroTooltipFallback {
    private static final ClassValue<Map<String, Method>> GETTERS = new ClassValue<>() {
        @Override
        protected Map<String, Method> computeValue(Class<?> type) {
            Map<String, Method> methods = new HashMap<>();
            for (String name : new String[]{"getRenderer", "getElement", "getReader", "getContext"}) {
                try {
                    Method method = type.getMethod(name);
                    if (method.trySetAccessible()) methods.put(name, method);
                } catch (ReflectiveOperationException ignored) {
                    // 同一缓存同时服务悬浮项与渲染器，只保留各自实际提供的无参访问器。
                }
            }
            return Map.copyOf(methods);
        }
    };

    private static final ClassValue<Optional<Method>> CALLBACKS = new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            // 只修复本模组的域名提示，绝不代替其他地图扩展执行回调。
            if (!type.getName().startsWith("areahint.xaero.worldmap.")) return Optional.empty();
            for (Method method : type.getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (method.getName().equals("getTooltip") && !method.isBridge()
                        && parameters.length == 3 && parameters[2] == boolean.class
                        && method.trySetAccessible()) {
                    return Optional.of(method);
                }
            }
            return Optional.empty();
        }
    };

    private XaeroTooltipFallback() { }

    /** 原生已有提示时原样返回；新父类返回 null 时只调用本模组原有的类型化提示方法。 */
    public static Object resolve(Object nativeTooltip, Object holder, boolean overMenu) {
        if (nativeTooltip != null || holder == null) return nativeTooltip;
        try {
            Object renderer = getter(holder, "getRenderer");
            Object reader = getter(renderer, "getReader");
            if (reader == null) return null;
            Method callback = CALLBACKS.get(reader.getClass()).orElse(null);
            if (callback == null) return null;
            Object element = getter(holder, "getElement");
            Object context = getter(renderer, "getContext");
            Class<?>[] parameters = callback.getParameterTypes();
            if (!parameters[0].isInstance(element) || !parameters[1].isInstance(context)) return null;
            return callback.invoke(reader, element, context, overMenu);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // 可选接口不符或回调失败时保留 Xaero 的原生空提示，不打断地图绘制。
            return null;
        }
    }

    private static Object getter(Object target, String name) throws ReflectiveOperationException {
        if (target == null) return null;
        Method method = GETTERS.get(target.getClass()).get(name);
        return method == null ? null : method.invoke(target);
    }
}
