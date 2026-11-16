package areahint.compat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** 根据实际处理器选择注册入口，兼容 Xaero 将 add 参数从旧渲染器提升到共同父类的改动。 */
public final class RendererRegistration {
    private RendererRegistration() {
    }

    public static void register(Object handler, Object renderer) throws ReflectiveOperationException {
        Method selected = null;
        for (Method method : handler.getClass().getMethods()) {
            if (!method.getName().equals("add") || method.getParameterCount() != 1
                    || Modifier.isStatic(method.getModifiers())
                    || !method.getParameterTypes()[0].isInstance(renderer)) {
                continue;
            }
            // 兼容处理器保留多个重载时，优先使用参数类型更具体的入口。
            if (selected == null || selected.getParameterTypes()[0].isAssignableFrom(method.getParameterTypes()[0])) {
                selected = method;
            }
        }
        if (selected == null) {
            throw new NoSuchMethodException(handler.getClass().getName() + ".add(" + renderer.getClass().getName() + ")");
        }
        // 注册失败直接上抛，由调用方禁用当前地图；不重试其他重载以免重复注册。
        selected.invoke(handler, renderer);
    }
}
