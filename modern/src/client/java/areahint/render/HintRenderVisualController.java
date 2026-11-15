package areahint.render;

import areahint.commandui.CommandUiActions;
import areahint.commandui.WizardOptionScreen;
import areahint.config.ClientConfig;
import areahint.i18n.I18nManager;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * hintrender 图形流程控制器，只负责把渲染模式选择转换为现有 /areahint hintrender 指令。
 */
public final class HintRenderVisualController {
    private HintRenderVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        List<WizardOptionScreen.OptionSpec> options = List.of(
            option("commandui.hintrender.cpu", () -> runAndClose(parent, "areahint hintrender CPU")),
            option("commandui.hintrender.opengl", () -> runAndClose(parent, "areahint hintrender OpenGL")),
            option("commandui.hintrender.vulkan", () -> runAndClose(parent, "areahint hintrender Vulkan")),
            option("commandui.common.current", () -> runAndClose(parent, "areahint hintrender"))
        );
        setScreen(new WizardOptionScreen(parent,
            "commandui.hintrender.title",
            "commandui.hintrender.prompt",
            renderDetail(),
            options,
            null));
    }

    private static WizardOptionScreen.OptionSpec option(String labelKey, Runnable action) {
        return new WizardOptionScreen.OptionSpec(labelKey, "", -1, action);
    }

    private static void runAndClose(Screen parent, String command) {
        // 继续走原指令链，由服务端权限和客户端渲染环境校验处理实际切换。
        CommandUiActions.runCommandAndClose(parent, command);
    }

    private static String renderDetail() {
        String currentMode = ClientConfig.getHintRender();
        return I18nManager.translate("commandui.hintrender.detail")
            .replace("%s", currentMode)
            .replace("{0}", currentMode);
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(screen);
        }
    }
}
