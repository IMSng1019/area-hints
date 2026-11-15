package areahint.language;

import areahint.util.TextEventCompat;

import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * 语言选择UI
 */
public class LanguageUI {

    public static void showLanguageSelectionScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String currentLang = I18nManager.getCurrentLanguage();
        String currentDisplayName = I18nManager.getLanguageDisplayName(currentLang);

        client.player.sendMessage(Text.of(I18nManager.translate("language.title.settings")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.language") + currentDisplayName + " (" + currentLang + ")"), false);
        client.player.sendMessage(Text.of(I18nManager.translate("language.prompt.select")), false);
        client.player.sendMessage(Text.of(""), false);

        List<String> languages = I18nManager.getAvailableLanguages();

        if (languages.isEmpty()) {
            client.player.sendMessage(Text.of(I18nManager.translate("gui.error.language")), false);
            client.player.sendMessage(Text.of(I18nManager.translate("language.error.no_files")), false);
            return;
        }

        // 为每个语言创建按钮
        for (String langCode : languages) {
            String displayName = I18nManager.getLanguageDisplayName(langCode);
            boolean isCurrent = langCode.equals(currentLang);

            String prefix = isCurrent ? "§a✓ " : "§b  ";
            MutableText button = Text.literal(prefix + "[" + displayName + " (" + langCode + ")]")
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint language select " + langCode))
                    .withHoverEvent(TextEventCompat.showText(
                        Text.of(I18nManager.translate("addhint.prompt.general") + displayName)))
                    .withColor(isCurrent ? Formatting.GREEN : Formatting.AQUA));

            client.player.sendMessage(button, false);
        }

        client.player.sendMessage(Text.of(""), false);

        // 取消按钮
        MutableText cancelButton = Text.literal(I18nManager.translate("language.button.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint language cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.prompt.cancel.language"))))
                .withColor(Formatting.RED));

        client.player.sendMessage(cancelButton, false);
    }
}
