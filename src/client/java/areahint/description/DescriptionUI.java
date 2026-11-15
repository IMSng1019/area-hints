package areahint.description;

import areahint.util.TextEventCompat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import areahint.i18n.I18nManager;

import java.util.List;

/**
 * 域名描述聊天交互 UI。
 */
public final class DescriptionUI {
    private DescriptionUI() {
    }

    public static void showLoadingList(String commandPrefix, boolean dimensionTarget, boolean deleteOperation) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        String target = dimensionTarget ? I18nManager.translate("description.ui.target.dimension") : I18nManager.translate("description.ui.target.area");
        String operation = deleteOperation ? I18nManager.translate("description.ui.operation.delete") : I18nManager.translate("description.ui.operation.add");
        client.player.sendMessage(Text.literal(I18nManager.translate("description.ui.title.header", target, operation)).formatted(Formatting.AQUA), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("description.ui.loading", target)), false);
        client.player.sendMessage(cancelButton(commandPrefix), false);
    }

    public static void showSelection(String commandPrefix, List<DescriptionListEntry> entries) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendMessage(Text.literal(I18nManager.translate("description.ui.select.prompt")).formatted(Formatting.AQUA), false);
        for (DescriptionListEntry entry : entries) {
            MutableText button = Text.literal("[" + nullToText(entry.displayName()) + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint " + commandPrefix + " select " + quoteCommandArgument(entry.id())))
                    .withHoverEvent(TextEventCompat.showText(createHoverText(entry)))
                    .withColor(Formatting.GOLD));
            client.player.sendMessage(button, false);
        }
        client.player.sendMessage(cancelButton(commandPrefix), false);
    }

    public static void showDescriptionInput(String commandPrefix, DescriptionListEntry entry) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendMessage(Text.literal(I18nManager.translate("description.ui.selected", nullToText(entry.displayName()))).formatted(Formatting.GREEN), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("description.ui.book.instruction")), false);
        client.player.sendMessage(cancelButton(commandPrefix), false);
    }

    public static void showAddConfirm(String commandPrefix, DescriptionListEntry entry, String description) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendMessage(Text.literal(I18nManager.translate("description.ui.confirm.save.title")).formatted(Formatting.AQUA), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("description.ui.confirm.target", nullToText(entry.displayName()))), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("description.ui.confirm.length", (description == null ? 0 : description.length()))), false);
        String preview = description == null ? "" : description;
        if (preview.length() > 120) {
            preview = preview.substring(0, 120) + "...";
        }
        client.player.sendMessage(Text.literal(I18nManager.translate("description.ui.confirm.preview", preview)), false);

        MutableText confirm = Text.literal(I18nManager.translate("description.ui.button.confirm.save"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + commandPrefix + " confirm"))
                .withHoverEvent(TextEventCompat.showText(Text.literal(I18nManager.translate("description.ui.hover.save"))))
                .withColor(Formatting.GREEN));
        client.player.sendMessage(Text.empty().append(confirm).append(Text.literal(" ")).append(cancelButton(commandPrefix)), false);
    }

    public static void showDeleteConfirmFirst(String commandPrefix, DescriptionListEntry entry) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendMessage(Text.literal(I18nManager.translate("description.ui.confirm.delete.title")).formatted(Formatting.RED), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("description.ui.confirm.target", nullToText(entry.displayName()))), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("description.ui.confirm.delete.warning")), false);
        MutableText confirm = Text.literal(I18nManager.translate("description.ui.button.continue.delete"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + commandPrefix + " confirm"))
                .withHoverEvent(TextEventCompat.showText(Text.literal(I18nManager.translate("description.ui.hover.second.confirm"))))
                .withColor(Formatting.RED));
        client.player.sendMessage(Text.empty().append(confirm).append(Text.literal(" ")).append(cancelButton(commandPrefix)), false);
    }

    public static void showDeleteConfirmSecond(String commandPrefix, DescriptionListEntry entry) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendMessage(Text.literal(I18nManager.translate("description.ui.confirm.delete.second.warning")).formatted(Formatting.RED), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("description.ui.confirm.target", nullToText(entry.displayName()))), false);
        MutableText confirm = Text.literal(I18nManager.translate("description.ui.button.confirm.delete.only"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + commandPrefix + " confirm2"))
                .withHoverEvent(TextEventCompat.showText(Text.literal(I18nManager.translate("description.ui.hover.confirm.delete"))))
                .withColor(Formatting.RED)
                .withBold(true));
        client.player.sendMessage(Text.empty().append(confirm).append(Text.literal(" ")).append(cancelButton(commandPrefix)), false);
    }

    private static MutableText cancelButton(String commandPrefix) {
        return Text.literal(I18nManager.translate("description.ui.button.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + commandPrefix + " cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.literal(I18nManager.translate("description.ui.hover.cancel"))))
                .withColor(Formatting.RED));
    }

    private static Text createHoverText(DescriptionListEntry entry) {
        return Text.literal(
            I18nManager.translate("description.ui.hover.displayname") + nullToText(entry.displayName()) + "\n" +
            I18nManager.translate("description.ui.hover.realname") + nullToText(entry.id()) + "\n" +
            I18nManager.translate("description.ui.hover.level") + (entry.level() == 0 ? I18nManager.translate("description.ui.target.dimension") : entry.level()) + "\n" +
            I18nManager.translate("description.ui.hover.parent") + nullToText(entry.baseName()) + "\n" +
            I18nManager.translate("description.ui.hover.signature") + nullToText(entry.signature()) + "\n" +
            I18nManager.translate("description.ui.hover.dimension") + nullToText(entry.dimension())
        );
    }

    private static String quoteCommandArgument(String value) {
        String escaped = value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    private static String nullToText(String value) {
        return value == null || value.trim().isEmpty() ? I18nManager.translate("description.ui.none") : value;
    }
}
