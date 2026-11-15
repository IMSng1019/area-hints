package areahint.description;

import areahint.util.TextEventCompat;
import areahint.i18n.I18nManager;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * 域名描述聊天交互 UI。
 */
public final class DescriptionUI {
    private DescriptionUI() {
    }

    public static void showLoadingList(String commandPrefix, boolean dimensionTarget, boolean deleteOperation) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        String target = dimensionTarget ? I18nManager.translate("description.ui.target.dimension") : I18nManager.translate("description.ui.target.area");
        String operation = deleteOperation ? I18nManager.translate("description.ui.operation.delete") : I18nManager.translate("description.ui.operation.add");
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("description.ui.title.header", target, operation)).withStyle(ChatFormatting.AQUA));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("description.ui.loading", target)));
        client.player.sendSystemMessage(cancelButton(commandPrefix));
    }

    public static void showSelection(String commandPrefix, List<DescriptionListEntry> entries) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendSystemMessage(Component.literal(I18nManager.translate("description.ui.select.prompt")).withStyle(ChatFormatting.AQUA));
        for (DescriptionListEntry entry : entries) {
            MutableComponent button = Component.literal("[" + nullToText(entry.displayName()) + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint " + commandPrefix + " select " + quoteCommandArgument(entry.id())))
                    .withHoverEvent(TextEventCompat.showText(createHoverText(entry)))
                    .withColor(ChatFormatting.GOLD));
            client.player.sendSystemMessage(button);
        }
        client.player.sendSystemMessage(cancelButton(commandPrefix));
    }

    public static void showDescriptionInput(String commandPrefix, DescriptionListEntry entry) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendSystemMessage(Component.literal(I18nManager.translate("description.ui.selected", nullToText(entry.displayName()))).withStyle(ChatFormatting.GREEN));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("description.ui.book.instruction")));
        client.player.sendSystemMessage(cancelButton(commandPrefix));
    }

    public static void showAddConfirm(String commandPrefix, DescriptionListEntry entry, String description) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendSystemMessage(Component.literal(I18nManager.translate("description.ui.confirm.save.title")).withStyle(ChatFormatting.AQUA));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("description.ui.confirm.target", nullToText(entry.displayName()))));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("description.ui.confirm.length", (description == null ? 0 : description.length()))));
        String preview = description == null ? "" : description;
        if (preview.length() > 120) {
            preview = preview.substring(0, 120) + "...";
        }
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("description.ui.confirm.preview", preview)));

        MutableComponent confirm = Component.literal(I18nManager.translate("description.ui.button.confirm.save"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + commandPrefix + " confirm"))
                .withHoverEvent(TextEventCompat.showText(Component.literal(I18nManager.translate("description.ui.hover.save"))))
                .withColor(ChatFormatting.GREEN));
        client.player.sendSystemMessage(Component.empty().append(confirm).append(Component.literal(" ")).append(cancelButton(commandPrefix)));
    }

    public static void showDeleteConfirmFirst(String commandPrefix, DescriptionListEntry entry) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendSystemMessage(Component.literal(I18nManager.translate("description.ui.confirm.delete.title")).withStyle(ChatFormatting.RED));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("description.ui.confirm.target", nullToText(entry.displayName()))));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("description.ui.confirm.delete.warning")));
        MutableComponent confirm = Component.literal(I18nManager.translate("description.ui.button.continue.delete"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + commandPrefix + " confirm"))
                .withHoverEvent(TextEventCompat.showText(Component.literal(I18nManager.translate("description.ui.hover.second.confirm"))))
                .withColor(ChatFormatting.RED));
        client.player.sendSystemMessage(Component.empty().append(confirm).append(Component.literal(" ")).append(cancelButton(commandPrefix)));
    }

    public static void showDeleteConfirmSecond(String commandPrefix, DescriptionListEntry entry) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendSystemMessage(Component.literal(I18nManager.translate("description.ui.confirm.delete.second.warning")).withStyle(ChatFormatting.RED));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("description.ui.confirm.target", nullToText(entry.displayName()))));
        MutableComponent confirm = Component.literal(I18nManager.translate("description.ui.button.confirm.delete.only"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + commandPrefix + " confirm2"))
                .withHoverEvent(TextEventCompat.showText(Component.literal(I18nManager.translate("description.ui.hover.confirm.delete"))))
                .withColor(ChatFormatting.RED)
                .withBold(true));
        client.player.sendSystemMessage(Component.empty().append(confirm).append(Component.literal(" ")).append(cancelButton(commandPrefix)));
    }

    private static MutableComponent cancelButton(String commandPrefix) {
        return Component.literal(I18nManager.translate("description.ui.button.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + commandPrefix + " cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.literal(I18nManager.translate("description.ui.hover.cancel"))))
                .withColor(ChatFormatting.RED));
    }

    private static Component createHoverText(DescriptionListEntry entry) {
        return Component.literal(
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
