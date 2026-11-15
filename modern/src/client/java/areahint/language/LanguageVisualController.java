package areahint.language;

import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.i18n.I18nManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * language 图形流程控制器，只把语言选择交给现有 LanguageManager 状态机。
 */
public final class LanguageVisualController {
    private LanguageVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        List<String> languages = availableLanguages();
        if (languages.isEmpty()) {
            showEmptyLanguageScreen(parent);
            return;
        }

        LanguageManager.getInstance().startVisualLanguageSelection();
        setScreen(new WizardSelectionListScreen<>(parent,
            "commandui.language.title",
            "commandui.language.prompt",
            languageItems(languages),
            LanguageVisualController::selectLanguage,
            () -> LanguageManager.getInstance().cancelLanguageSelection()));
    }

    private static List<String> availableLanguages() {
        List<String> languages = new ArrayList<>(I18nManager.getAvailableLanguages());
        Collections.sort(languages);
        return languages;
    }

    private static List<WizardSelectionListScreen.SelectionItem<String>> languageItems(List<String> languages) {
        List<WizardSelectionListScreen.SelectionItem<String>> items = new ArrayList<>();
        String currentLanguage = I18nManager.getCurrentLanguage();
        for (String language : languages) {
            String displayName = I18nManager.getLanguageDisplayName(language);
            boolean current = language.equals(currentLanguage);
            String title = (current ? "* " : "") + displayName + " (" + language + ")";
            String detail = current
                ? I18nManager.translate("commandui.language.current") + displayName + " (" + language + ")"
                : I18nManager.translate("commandui.language.item.detail", displayName, language);
            items.add(new WizardSelectionListScreen.SelectionItem<>(language, title, detail));
        }
        return items;
    }

    private static void selectLanguage(String language) {
        closeToGame();
        LanguageManager.getInstance().handleLanguageSelection(language);
    }

    private static void showEmptyLanguageScreen(Screen parent) {
        setScreen(new WizardConfirmScreen(parent,
            "commandui.language.title",
            I18nManager.translate("commandui.language.empty"),
            List.of(),
            "commandui.button.close",
            () -> {
            },
            null));
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(screen);
        }
    }

    private static void closeToGame() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(null);
        }
    }
}
