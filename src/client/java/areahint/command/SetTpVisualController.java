package areahint.command;

import areahint.commandui.CommandUiActions;
import areahint.commandui.WizardOptionScreen;
import areahint.config.ClientConfig;
import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

/**
 * settp 指令图形流程控制器，只把格式选择转换为现有 /areahint settp 指令。
 */
public final class SetTpVisualController {
    private SetTpVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        // 打开格式选择页，详情区显示当前正在使用的传送命令头。
        setScreen(new WizardOptionScreen(parent,
            "commandui.settp.title",
            "commandui.settp.prompt",
            currentFormatDetail(),
            formatOptions(),
            null));
    }

    private static List<WizardOptionScreen.OptionSpec> formatOptions() {
        // settp 当前只允许这四种命令头，按钮选项避免用户输入无效格式。
        return List.of(
            option("commandui.settp.option.tp", "tp"),
            option("commandui.settp.option.minecraft_tp", "minecraft:tp"),
            option("commandui.settp.option.teleport", "teleport"),
            option("commandui.settp.option.minecraft_teleport", "minecraft:teleport")
        );
    }

    private static WizardOptionScreen.OptionSpec option(String labelKey, String format) {
        // 每个按钮都只提交对应格式，不直接修改 ClientConfig。
        return new WizardOptionScreen.OptionSpec(labelKey, "", -1, () -> runSetTp(format));
    }

    private static void runSetTp(String format) {
        // 关闭界面后执行旧命令，让服务端权限和客户端保存流程保持原样。
        closeToGame();
        CommandUiActions.runCommand("areahint settp " + format);
    }

    private static String currentFormatDetail() {
        // 兼容旧翻译中的 %s 占位符，避免详情页显示未替换文本。
        return format("commandui.settp.detail", ClientConfig.getTeleportFormat());
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
