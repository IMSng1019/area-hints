package areahint.teleport;

import areahint.commandui.CommandUiActions;
import areahint.commandui.CommandUiData;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.config.ClientConfig;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

/**
 * tcp 指令图形流程控制器，只把域名选择转换为现有 /areahint tcp 指令。
 */
public final class TcpVisualController {
    private static final String MODE_LABEL = "TCP";

    private TcpVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        List<AreaData> areas = CommandUiData.validAreas(CommandUiData.loadCurrentDimensionAreas());
        if (areas.isEmpty()) {
            showInfo(parent, "commandui.teleport.empty");
            return;
        }

        // 这里只读取当前维度有效域名，实际权限和传送仍交给旧命令链处理。
        setScreen(new WizardSelectionListScreen<>(parent,
            "commandui.tcp.title",
            format("commandui.teleport.prompt", MODE_LABEL),
            CommandUiData.areaItems(areas),
            area -> showConfirm(parent, area),
            null));
    }

    private static void showConfirm(Screen parent, AreaData area) {
        setScreen(new WizardConfirmScreen(parent,
            "commandui.tcp.title",
            format("commandui.teleport.confirm", MODE_LABEL, area.getName()),
            List.of(format("commandui.teleport.format", ClientConfig.getTeleportFormat()),
                "/areahint tcp " + CommandUiData.quote(area.getName())),
            "commandui.button.confirm",
            () -> runTcp(area),
            null));
    }

    private static void runTcp(AreaData area) {
        // 执行原指令，服务端会再转发 tcp_select 并复用现有传送请求流程。
        CommandUiActions.runCommand("areahint tcp " + CommandUiData.quote(area.getName()));
    }

    private static void showInfo(Screen parent, String messageKey) {
        setScreen(new WizardConfirmScreen(parent,
            "commandui.tcp.title",
            I18nManager.translate(messageKey),
            List.of(),
            "commandui.button.close",
            null,
            null));
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
}
