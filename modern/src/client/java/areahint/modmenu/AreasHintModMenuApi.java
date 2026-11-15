package areahint.modmenu;

import areahint.gui.AreasHintConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu 兼容入口。
 * 当用户安装 Mod Menu 时，Mod Menu 会读取 fabric.mod.json 中的 modmenu 入口，
 * 并通过这里把 Areas Hint 的配置按钮指向模组已有的客户端配置界面。
 */
public final class AreasHintModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AreasHintConfigScreen::new;
    }
}
