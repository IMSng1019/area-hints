package areahint.render;

import areahint.Areashint;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

/** 26.x HUD 使用具名元素；每种提示独占标识，重建渲染器时替换回调而不重复添加元素。 */
public final class HudRegistration {
    private static final Set<Identifier> REGISTERED = new HashSet<>();

    private HudRegistration() {
    }

    public static synchronized void register(String path, HudElement element) {
        Identifier id = Identifier.fromNamespaceAndPath(Areashint.MOD_ID, path);
        if (REGISTERED.contains(id)) {
            HudElementRegistry.replaceElement(id, previous -> element);
        } else {
            // 继续在原版 HUD 后提取提示，不改变域名标题与录点面板的叠放顺序。
            HudElementRegistry.addLast(id, element);
            REGISTERED.add(id);
        }
    }
}
