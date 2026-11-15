package areahint.signature;

import areahint.util.TextEventCompat;

import areahint.data.AreaData;
import areahint.util.AreaDataConverter;
import areahint.i18n.I18nManager;

import java.util.List;
import java.util.StringJoiner;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Signature聊天按钮界面。
 *
 * <p>界面风格参考EasyAdd：每一步都用聊天提示和可点击按钮推进，
 * 同时保留命令输入方式，方便玩家手动补全。</p>
 */
public class SignatureUI {
    private static final Minecraft client = Minecraft.getInstance();

    /**
     * 显示可修改签名的域名列表。
     */
    public static void showAreaSelectionScreen(SignatureManager.Operation operation, List<AreaData> areas, boolean admin) {
        if (client.player == null) {
            return;
        }

        String commandPrefix = getCommandPrefix(operation);
        String operationName = getOperationName(operation);

        client.player.sendSystemMessage(Component.literal("§6=== " + I18nManager.translate("signature.ui.title.select", operationName) + " ==="));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.prompt.select", operationName)));
        if (admin) {
            client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.mode.admin")));
        } else {
            client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.mode.normal")));
        }

        for (AreaData area : areas) {
            String displayName = AreaDataConverter.getDisplayName(area);
            MutableComponent areaButton = Component.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint " + commandPrefix + " select " + quoteCommandArgument(area.getName())))
                    .withHoverEvent(TextEventCompat.showText(createAreaHoverText(area)))
                    .withColor(ChatFormatting.GOLD));
            client.player.sendSystemMessage(areaButton);
        }

        client.player.sendSystemMessage(createCancelButton(commandPrefix));
    }

    /**
     * 显示玩家名输入界面。玩家直接在聊天栏输入名字，流程会拦截该聊天内容并推进到确认阶段。
     */
    public static void showPlayerNamePrompt(SignatureManager.Operation operation, AreaData area, List<String> removableSignatures) {
        if (client.player == null) {
            return;
        }

        String commandPrefix = getCommandPrefix(operation);

        client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.selected", AreaDataConverter.getDisplayName(area))));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.realname", nullToText(area.getName()))));

        if (operation == SignatureManager.Operation.DELETE) {
            client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.current.signatures", formatSignatures(removableSignatures))));
            client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.prompt.delete")));
        } else {
            client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.prompt.add")));
        }

        client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.cancel.hint")));
        client.player.sendSystemMessage(createCancelButton(commandPrefix));
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

        client.player.sendSystemMessage(Component.literal("§6=== " + I18nManager.translate("signature.ui.title.confirm", operationName) + " ==="));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.realname", nullToText(area.getName()))));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.displayname", AreaDataConverter.getDisplayName(area))));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.basename", nullToText(area.getBaseName()))));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.creator", nullToText(area.getSignature()))));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.signatures", formatSignatures(area.getSignatures()))));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.target.player", targetPlayerName)));

        String hover = operation == SignatureManager.Operation.DELETE ? I18nManager.translate("signature.ui.hover.delete.confirm") : I18nManager.translate("signature.ui.hover.add.confirm");
        MutableComponent confirmButton = Component.literal(I18nManager.translate("signature.ui.button.confirm"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + commandPrefix + " confirm"))
                .withHoverEvent(TextEventCompat.showText(Component.literal(hover)))
                .withColor(ChatFormatting.GREEN));

        client.player.sendSystemMessage(Component.empty()
            .append(confirmButton)
            .append(Component.literal("  "))
            .append(createCancelButton(commandPrefix)));
    }

    /**
     * 显示删除签名的二次确认界面。
     */
    public static void showFinalDeleteConfirmScreen(AreaData area, String targetPlayerName) {
        if (client.player == null) {
            return;
        }

        client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.title.delete.second")));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.delete.warning", AreaDataConverter.getDisplayName(area), targetPlayerName)));
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("signature.ui.delete.file.warning")));

        MutableComponent finalConfirmButton = Component.literal(I18nManager.translate("signature.ui.button.delete.final"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint deletesignature confirm2"))
                .withHoverEvent(TextEventCompat.showText(
                    Component.literal(I18nManager.translate("signature.ui.hover.delete.final"))))
                .withColor(ChatFormatting.RED));

        client.player.sendSystemMessage(Component.empty()
            .append(finalConfirmButton)
            .append(Component.literal("  "))
            .append(createCancelButton("deletesignature")));
    }

    private static MutableComponent createCancelButton(String commandPrefix) {
        return Component.literal(I18nManager.translate("signature.ui.button.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + commandPrefix + " cancel"))
                .withHoverEvent(TextEventCompat.showText(
                    Component.literal(I18nManager.translate("signature.ui.hover.cancel"))))
                .withColor(ChatFormatting.RED));
    }

    private static Component createAreaHoverText(AreaData area) {
        return Component.literal(
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
