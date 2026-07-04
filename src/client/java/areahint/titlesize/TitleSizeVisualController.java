package areahint.titlesize;

import areahint.commandui.CommandUiActions;
import areahint.commandui.WizardOptionScreen;
import areahint.config.ClientConfig;
import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

/**
 * titlesize 指令图形流程控制器，只把按钮选择转换为现有 /areahint titlesize 指令。
 */
public final class TitleSizeVisualController {
    private TitleSizeVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        setScreen(new WizardOptionScreen(parent,
            "commandui.titlesize.title",
            "commandui.titlesize.prompt",
            currentSizeDetail(),
            sizeOptions(),
            () -> CommandUiActions.runCommand("areahint titlesize cancel")));
    }

    private static List<WizardOptionScreen.OptionSpec> sizeOptions() {
        // TitleSizeManager 只接受这七个预设值，自定义缩放由配置界面负责。
        return List.of(
            option("commandui.common.size.extra_large", "extra_large"),
            option("commandui.common.size.large", "large"),
            option("commandui.common.size.medium_large", "medium_large"),
            option("commandui.common.size.medium", "medium"),
            option("commandui.common.size.medium_small", "medium_small"),
            option("commandui.common.size.small", "small"),
            option("commandui.common.size.extra_small", "extra_small")
        );
    }

    private static WizardOptionScreen.OptionSpec option(String labelKey, String size) {
        return new WizardOptionScreen.OptionSpec(labelKey, "", -1, () -> runSize(size));
    }

    private static void runSize(String size) {
        // 先启动旧状态机再提交选择，保留服务端权限检查和现有保存、重载提示。
        closeToGame();
        CommandUiActions.runCommand("areahint titlesize");
        CommandUiActions.runCommand("areahint titlesize select " + size);
    }

    private static String currentSizeDetail() {
        return format("commandui.titlesize.detail", sizeDisplayName(ClientConfig.getTitleSize()));
    }

    private static String sizeDisplayName(String size) {
        String key = "commandui.common.size." + size;
        return I18nManager.hasKey(key) ? I18nManager.translate(key) : size;
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
