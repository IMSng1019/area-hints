package areahint.titlestyle;

import areahint.commandui.CommandUiActions;
import areahint.commandui.WizardOptionScreen;
import areahint.config.ClientConfig;
import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

/**
 * titlestyle 指令图形流程控制器，只把按钮选择转换为现有 /areahint titlestyle 指令。
 */
public final class TitleStyleVisualController {
    private TitleStyleVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        setScreen(new WizardOptionScreen(parent,
            "commandui.titlestyle.title",
            "commandui.titlestyle.prompt",
            currentStyleDetail(),
            styleOptions(),
            () -> CommandUiActions.runCommand("areahint titlestyle cancel")));
    }

    private static List<WizardOptionScreen.OptionSpec> styleOptions() {
        return List.of(
            option("commandui.titlestyle.full", "full"),
            option("commandui.titlestyle.simple", "simple"),
            option("commandui.titlestyle.mixed", "mixed")
        );
    }

    private static WizardOptionScreen.OptionSpec option(String labelKey, String style) {
        return new WizardOptionScreen.OptionSpec(labelKey, "", -1, () -> runStyle(style));
    }

    private static void runStyle(String style) {
        // 先启动旧状态机再提交选择，保留服务端权限检查和现有保存、重载提示。
        closeToGame();
        CommandUiActions.runCommand("areahint titlestyle");
        CommandUiActions.runCommand("areahint titlestyle select " + style);
    }

    private static String currentStyleDetail() {
        return format("commandui.titlestyle.detail", styleDisplayName(ClientConfig.getTitleStyle()));
    }

    private static String styleDisplayName(String style) {
        String key = "commandui.titlestyle." + style;
        return I18nManager.hasKey(key) ? I18nManager.translate(key) : style;
    }

    private static String format(String key, Object... args) {
        String template = I18nManager.translate(key);
        if (!template.contains("%")) {
            for (int i = 0; i < args.length; i++) {
                template = template.replace("{" + i + "}", String.valueOf(args[i]));
            }
            return template;
        }
        try {
            return String.format(template, args);
        } catch (IllegalArgumentException e) {
            for (int i = 0; i < args.length; i++) {
                template = template.replace("{" + i + "}", String.valueOf(args[i]));
            }
            return template;
        }
    }

    private static void setScreen(Screen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(screen);
        }
    }

    private static void closeToGame() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(null);
        }
    }
}
