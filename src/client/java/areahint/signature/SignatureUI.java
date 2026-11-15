package areahint.signature;

import areahint.util.TextEventCompat;

import areahint.data.AreaData;
import areahint.util.AreaDataConverter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import areahint.i18n.I18nManager;

import java.util.List;
import java.util.StringJoiner;

/**
 * Signature聊天按钮界面。
 *
 * <p>界面风格参考EasyAdd：每一步都用聊天提示和可点击按钮推进，
 * 同时保留命令输入方式，方便玩家手动补全。</p>
 */
public class SignatureUI {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    /**
     * 显示可修改签名的域名列表。
     */
    public static void showAreaSelectionScreen(SignatureManager.Operation operation, List<AreaData> areas, boolean admin) {
        if (client.player == null) {
            return;
        }

        String commandPrefix = getCommandPrefix(operation);
        String operationName = getOperationName(operation);

        client.player.sendMessage(Text.literal("§6=== " + I18nManager.translate("signature.ui.title.select", operationName) + " ==="), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.prompt.select", operationName)), false);
        if (admin) {
            client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.mode.admin")), false);
        } else {
            client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.mode.normal")), false);
        }

        for (AreaData area : areas) {
            String displayName = AreaDataConverter.getDisplayName(area);
            MutableText areaButton = Text.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint " + commandPrefix + " select " + quoteCommandArgument(area.getName())))
                    .withHoverEvent(TextEventCompat.showText(createAreaHoverText(area)))
                    .withColor(Formatting.GOLD));
            client.player.sendMessage(areaButton, false);
        }

        client.player.sendMessage(createCancelButton(commandPrefix), false);
    }

    /**
     * 显示玩家名输入界面。玩家直接在聊天栏输入名字，流程会拦截该聊天内容并推进到确认阶段。
     */
    public static void showPlayerNamePrompt(SignatureManager.Operation operation, AreaData area, List<String> removableSignatures) {
        if (client.player == null) {
            return;
        }

        String commandPrefix = getCommandPrefix(operation);

        client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.selected", AreaDataConverter.getDisplayName(area))), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.realname", nullToText(area.getName()))), false);

        if (operation == SignatureManager.Operation.DELETE) {
            client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.current.signatures", formatSignatures(removableSignatures))), false);
            client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.prompt.delete")), false);
        } else {
            client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.prompt.add")), false);
        }

        client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.cancel.hint")), false);
        client.player.sendMessage(createCancelButton(commandPrefix), false);
    }

    /**
     * 显示第一层确认界面。
     */
    public static void showConfirmScreen(SignatureManager.Operation operation, AreaData area, String targetPlayerName) {
        if (client.player == null) {
            return;
        }

        String commandPrefix = getCommandPrefix(operation);
        String operationName = getOperationName(operation);

        client.player.sendMessage(Text.literal("§6=== " + I18nManager.translate("signature.ui.title.confirm", operationName) + " ==="), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.realname", nullToText(area.getName()))), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.displayname", AreaDataConverter.getDisplayName(area))), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.basename", nullToText(area.getBaseName()))), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.creator", nullToText(area.getSignature()))), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.signatures", formatSignatures(area.getSignatures()))), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.target.player", targetPlayerName)), false);

        String hover = operation == SignatureManager.Operation.DELETE ? I18nManager.translate("signature.ui.hover.delete.confirm") : I18nManager.translate("signature.ui.hover.add.confirm");
        MutableText confirmButton = Text.literal(I18nManager.translate("signature.ui.button.confirm"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + commandPrefix + " confirm"))
                .withHoverEvent(TextEventCompat.showText(Text.literal(hover)))
                .withColor(Formatting.GREEN));

        client.player.sendMessage(Text.empty()
            .append(confirmButton)
            .append(Text.literal("  "))
            .append(createCancelButton(commandPrefix)), false);
    }

    /**
     * 显示删除签名的二次确认界面。
     */
    public static void showFinalDeleteConfirmScreen(AreaData area, String targetPlayerName) {
        if (client.player == null) {
            return;
        }

        client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.title.delete.second")), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.delete.warning", AreaDataConverter.getDisplayName(area), targetPlayerName)), false);
        client.player.sendMessage(Text.literal(I18nManager.translate("signature.ui.delete.file.warning")), false);

        MutableText finalConfirmButton = Text.literal(I18nManager.translate("signature.ui.button.delete.final"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint deletesignature confirm2"))
                .withHoverEvent(TextEventCompat.showText(
                    Text.literal(I18nManager.translate("signature.ui.hover.delete.final"))))
                .withColor(Formatting.RED));

        client.player.sendMessage(Text.empty()
            .append(finalConfirmButton)
            .append(Text.literal("  "))
            .append(createCancelButton("deletesignature")), false);
    }

    private static MutableText createCancelButton(String commandPrefix) {
        return Text.literal(I18nManager.translate("signature.ui.button.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + commandPrefix + " cancel"))
                .withHoverEvent(TextEventCompat.showText(
                    Text.literal(I18nManager.translate("signature.ui.hover.cancel"))))
                .withColor(Formatting.RED));
    }

    private static Text createAreaHoverText(AreaData area) {
        return Text.literal(
            I18nManager.translate("signature.ui.hover.realname") + nullToText(area.getName()) + "\n" +
            I18nManager.translate("signature.ui.hover.displayname") + AreaDataConverter.getDisplayName(area) + "\n" +
            I18nManager.translate("signature.ui.hover.level") + area.getLevel() + "\n" +
            I18nManager.translate("signature.ui.hover.basename") + nullToText(area.getBaseName()) + "\n" +
            I18nManager.translate("signature.ui.hover.creator") + nullToText(area.getSignature()) + "\n" +
            I18nManager.translate("signature.ui.hover.signatures") + formatSignatures(area.getSignatures())
        );
    }

    private static String getCommandPrefix(SignatureManager.Operation operation) {
        return operation == SignatureManager.Operation.ADD ? "addsignature" : "deletesignature";
    }

    private static String getOperationName(SignatureManager.Operation operation) {
        return operation == SignatureManager.Operation.ADD ? I18nManager.translate("signature.ui.operation.add") : I18nManager.translate("signature.ui.operation.delete");
    }

    private static String quoteCommandArgument(String value) {
        String escaped = value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    private static String formatSignatures(List<String> signatures) {
        if (signatures == null || signatures.isEmpty()) {
            return I18nManager.translate("signature.ui.none");
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (String signature : signatures) {
            joiner.add(signature);
        }
        return joiner.toString();
    }

    private static String nullToText(String value) {
        return value == null || value.trim().isEmpty() ? I18nManager.translate("signature.ui.none") : value;
    }
}
