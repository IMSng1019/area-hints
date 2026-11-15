package areahint.language;

import areahint.util.TextEventCompat;

import areahint.i18n.I18nManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import java.util.List;

/**
 * 语言选择UI
 */
public class LanguageUI {

    public static void showLanguageSelectionScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        String currentLang = I18nManager.getCurrentLanguage();
        String currentDisplayName = I18nManager.getLanguageDisplayName(currentLang);

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("language.title.settings")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.language") + currentDisplayName + " (" + currentLang + ")"));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("language.prompt.select")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        List<String> languages = I18nManager.getAvailableLanguages();

        if (languages.isEmpty()) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.error.language")));
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("language.error.no_files")));
            return;
        }

        // 为每个语言创建按钮
        for (String langCode : languages) {
            String displayName = I18nManager.getLanguageDisplayName(langCode);
            boolean isCurrent = langCode.equals(currentLang);

            String prefix = isCurrent ? "§a✓ " : "§b  ";
            MutableComponent button = Component.literal(prefix + "[" + displayName + " (" + langCode + ")]")
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint language select " + langCode))
                    .withHoverEvent(TextEventCompat.showText(
                        Component.nullToEmpty(I18nManager.translate("addhint.prompt.general") + displayName)))
                    .withColor(isCurrent ? ChatFormatting.GREEN : ChatFormatting.AQUA));

            client.player.sendSystemMessage(button);
        }

        client.player.sendSystemMessage(Component.nullToEmpty(""));

        // 取消按钮
        MutableComponent cancelButton = Component.literal(I18nManager.translate("language.button.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint language cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.prompt.cancel.language"))))
                .withColor(ChatFormatting.RED));

        client.player.sendSystemMessage(cancelButton);
    }
}
