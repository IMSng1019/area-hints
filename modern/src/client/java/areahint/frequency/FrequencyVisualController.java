package areahint.frequency;

import areahint.commandui.CommandUiActions;
import areahint.commandui.WizardTextInputScreen;
import areahint.config.ClientConfig;
import areahint.data.ConfigData;
import areahint.i18n.I18nManager;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * frequency 图形流程控制器，只负责把输入频率转换为现有 /areahint frequency 指令。
 */
public final class FrequencyVisualController {
    private FrequencyVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        openInput(parent, null);
    }

    private static void openInput(Screen parent, String errorKey) {
        String currentFrequency = ConfigData.formatFrequency(ClientConfig.getFrequency());
        setScreen(new WizardTextInputScreen(parent,
            "commandui.frequency.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.frequency.label",
                "commandui.frequency.placeholder", currentFrequency, 8)),
            "commandui.frequency.prompt",
            frequencyDetail(currentFrequency),
            errorKey,
            values -> handleSubmit(parent, values.isEmpty() ? "" : values.get(0)),
            null));
    }

    private static void handleSubmit(Screen parent, String value) {
        String input = value == null ? "" : value.trim();
        try {
            double frequency = Double.parseDouble(input);
            if (!Double.isFinite(frequency) || frequency < 1.0 || frequency > 60.0) {
                openInput(parent, "commandui.frequency.error.range");
                return;
            }

            // 继续走原指令链，由服务端权限和客户端配置同步逻辑处理实际修改。
            CommandUiActions.runCommandAndClose(parent,
                "areahint frequency " + ConfigData.formatFrequency(frequency));
        } catch (NumberFormatException e) {
            openInput(parent, "commandui.frequency.error.number");
        }
    }

    private static String frequencyDetail(String currentFrequency) {
        return I18nManager.translate("commandui.frequency.detail")
            .replace("%s", currentFrequency)
            .replace("{0}", currentFrequency);
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, screen);
        }
    }
}
