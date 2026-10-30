package areahint.signature;

import areahint.commandui.CommandUiActions;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.commandui.WizardTextInputScreen;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.util.AreaDataConverter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * addsignature 图形流程控制器，只把 Screen 输入转交给现有 SignatureManager 状态机。
 */
public final class AddSignatureVisualController {
    private static final int VISUAL_START_TIMEOUT_TICKS = 100;
    private static Screen parentScreen;
    private static boolean startingFromVisualCommand;
    private static int visualStartTicksRemaining;
    private static boolean registered;

    private AddSignatureVisualController() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!startingFromVisualCommand) {
                return;
            }
            visualStartTicksRemaining--;
            if (visualStartTicksRemaining <= 0) {
                clear();
            }
        });
        registered = true;
    }

    public static void openFromCommandUi(Screen parent) {
        parentScreen = parent;
        startingFromVisualCommand = true;
        visualStartTicksRemaining = VISUAL_START_TIMEOUT_TICKS;
        CommandUiActions.runCommandAndClose(parent, "areahint addsignature");
    }

    public static boolean consumeVisualStartRequest() {
        boolean requested = startingFromVisualCommand;
        startingFromVisualCommand = false;
        visualStartTicksRemaining = 0;
        return requested;
    }

    public static void clear() {
        startingFromVisualCommand = false;
        visualStartTicksRemaining = 0;
        parentScreen = null;
    }

    public static void showAreaSelection(List<AreaData> areas, boolean admin) {
        if (areas == null || areas.isEmpty()) {
            showInfo("signature.manager.error.no_areas");
            return;
        }

        setScreen(new WizardSelectionListScreen<>(parentScreen,
            "commandui.addsignature.title",
            "commandui.addsignature.prompt",
            areaItems(areas, admin),
            area -> SignatureManager.getInstance().selectArea(area.getName()),
            SignatureManager.getInstance()::cancel));
    }

    public static void showPlayerNamePrompt(AreaData area, String errorTextOrKey) {
        setScreen(new WizardTextInputScreen(parentScreen,
            "commandui.addsignature.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.signature.player.label",
                "commandui.signature.player.placeholder", "", 32)),
            "commandui.signature.player.prompt",
            detailForArea(area),
            errorTextOrKey,
            values -> SignatureManager.getInstance().setPlayerName(values.isEmpty() ? "" : values.get(0)),
            SignatureManager.getInstance()::cancel));
    }

    public static void showConfirmScreen(AreaData area, String targetPlayerName) {
        List<String> details = areaDetails(area);
        details.add(I18nManager.translate("signature.ui.signatures", formatSignatures(area.getSignatures())));
        details.add(I18nManager.translate("signature.ui.target.player", targetPlayerName));

        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.addsignature.title",
            I18nManager.translate("signature.ui.title.confirm",
                I18nManager.translate("signature.ui.operation.add")),
            details,
            "commandui.button.confirm",
            SignatureManager.getInstance()::confirm,
            SignatureManager.getInstance()::cancel));
    }

    public static void showInfo(String messageTextOrKey) {
        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.addsignature.title",
            I18nManager.translate(messageTextOrKey),
            List.of(),
            "commandui.button.close",
            null,
            null));
    }

    private static List<WizardSelectionListScreen.SelectionItem<AreaData>> areaItems(List<AreaData> areas, boolean admin) {
        List<WizardSelectionListScreen.SelectionItem<AreaData>> items = new ArrayList<>();
        // 图形列表隐藏管理员模式说明，普通玩家仍显示可选域名范围提示。
        String modeSuffix = admin ? "" : "  " + I18nManager.translate("signature.ui.mode.normal");
        for (AreaData area : areas) {
            String displayName = AreaDataConverter.getDisplayName(area);
            String detail = I18nManager.translate("signature.ui.hover.realname") + nullText(area.getName()) + "  "
                + I18nManager.translate("signature.ui.hover.level") + area.getLevel() + "  "
                + I18nManager.translate("signature.ui.hover.creator") + nullText(area.getSignature()) + modeSuffix;
            items.add(new WizardSelectionListScreen.SelectionItem<>(area, displayName, detail));
        }
        return items;
    }

    private static String detailForArea(AreaData area) {
        if (area == null) {
            return I18nManager.translate("commandui.signature.player.detail");
        }
        return I18nManager.translate("signature.ui.selected", AreaDataConverter.getDisplayName(area)) + "  "
            + I18nManager.translate("signature.ui.realname", nullText(area.getName()));
    }

    private static List<String> areaDetails(AreaData area) {
        if (area == null) {
            return List.of();
        }
        List<String> details = new ArrayList<>();
        details.add(I18nManager.translate("signature.ui.hover.realname") + nullText(area.getName()));
        details.add(I18nManager.translate("signature.ui.hover.displayname") + AreaDataConverter.getDisplayName(area));
        details.add(I18nManager.translate("signature.ui.hover.level") + area.getLevel());
        details.add(I18nManager.translate("signature.ui.hover.basename") + nullText(area.getBaseName()));
        details.add(I18nManager.translate("signature.ui.hover.creator") + nullText(area.getSignature()));
        return details;
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

    private static String nullText(String value) {
        return value == null || value.trim().isEmpty() ? I18nManager.translate("signature.ui.none") : value;
    }

    private static void setScreen(Screen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(screen);
        }
    }
}
