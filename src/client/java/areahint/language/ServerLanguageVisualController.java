package areahint.language;

import areahint.commandui.CommandUiActions;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * serverlanguage 图形流程控制器，只负责把语言选择转换为现有 /areahint serverlanguage 指令。
 */
public final class ServerLanguageVisualController {
    private static final List<String> SERVER_LANGUAGES = List.of(
        "zh_cn", "zh_tw", "en_us", "en_pt", "ja_jp", "ko_kr", "fr_fr", "de_de", "es_es", "ru_ru", "zh_cn_neko");

    private ServerLanguageVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        setScreen(new WizardSelectionListScreen<>(parent,
            "commandui.serverlanguage.title",
            "commandui.serverlanguage.prompt",
            languageItems(),
            language -> openConfirm(parent, language),
            null));
    }

    private static List<WizardSelectionListScreen.SelectionItem<String>> languageItems() {
        List<WizardSelectionListScreen.SelectionItem<String>> items = new ArrayList<>();
        for (String language : SERVER_LANGUAGES) {
            String displayText = displayText(language);
            items.add(new WizardSelectionListScreen.SelectionItem<>(language, displayText,
                format("commandui.serverlanguage.item.detail", displayText)));
        }
        return items;
    }

    private static void openConfirm(Screen parent, String language) {
        String displayText = displayText(language);
        setScreen(new WizardConfirmScreen(parent,
            "commandui.serverlanguage.title",
            I18nManager.translate("commandui.common.confirm.prompt"),
            List.of(format("commandui.serverlanguage.confirm", displayText),
                "/areahint serverlanguage " + language),
            "commandui.button.execute",
            () -> CommandUiActions.runCommand("areahint serverlanguage " + language),
            null));
    }

    private static String displayText(String language) {
        return I18nManager.getLanguageDisplayName(language) + " (" + language + ")";
    }

    private static String format(String key, String value) {
        // 兼容当前翻译文件中的 %s，同时支持后续统一为 {0} 的占位符。
        return I18nManager.translate(key, value).replace("%s", value);
    }

    private static void setScreen(Screen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(screen);
        }
    }
}
