package areahint.commandui;

import areahint.config.ClientConfig;
import areahint.data.AreaData;
import areahint.data.ConfigData;
import areahint.i18n.I18nManager;
import areahint.network.ClientNetworking;
import areahint.signature.SignatureClientNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 指令可视化流程控制器，只负责把图形输入转换为现有指令或网络请求。
 */
public final class CommandVisualController {
    private static String visualRecordCommandId;

    private CommandVisualController() {
    }

    public static void beginVisualRecordMode(String id) {
        visualRecordCommandId = id;
    }

    public static boolean isVisualRecordMode(String id) {
        return id != null && id.equals(visualRecordCommandId);
    }

    public static void clearVisualRecordMode() {
        visualRecordCommandId = null;
    }

    public static void openConfirmCommand(Screen parent, String id, String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(new WizardConfirmScreen(parent,
                titleKey(id),
                I18nManager.translate("commandui.common.confirm.prompt"),
                List.of("/" + command),
                "commandui.button.execute",
                () -> CommandUiActions.runCommand(command),
                null));
        }
    }

    public static void openTitleStyle(Screen parent) {
        List<WizardOptionScreen.OptionSpec> options = List.of(
            option("commandui.titlestyle.full", () -> startThenRun("areahint titlestyle", "areahint titlestyle select full")),
            option("commandui.titlestyle.simple", () -> startThenRun("areahint titlestyle", "areahint titlestyle select simple")),
            option("commandui.titlestyle.mixed", () -> startThenRun("areahint titlestyle", "areahint titlestyle select mixed"))
        );
        setScreen(new WizardOptionScreen(parent, titleKey("titlestyle"),
            "commandui.titlestyle.prompt",
            I18nManager.translate("commandui.titlestyle.detail", ClientConfig.getTitleStyle()),
            options,
            () -> CommandUiActions.runCommand("areahint titlestyle cancel")));
    }

    public static void openTitleSize(Screen parent) {
        List<WizardOptionScreen.OptionSpec> options = sizePresetOptions(size ->
            startThenRun("areahint titlesize", "areahint titlesize select " + size));
        setScreen(new WizardOptionScreen(parent, titleKey("titlesize"),
            "commandui.titlesize.prompt",
            I18nManager.translate("commandui.titlesize.detail", ClientConfig.getTitleSize()),
            options,
            () -> CommandUiActions.runCommand("areahint titlesize cancel")));
    }

    public static void openAddJson(Screen parent) {
        AddCommandVisualController.open(parent);
    }

    public static void openServerLanguage(Screen parent) {
        List<WizardSelectionListScreen.SelectionItem<String>> items = new ArrayList<>();
        for (String language : List.of("zh_cn", "zh_tw", "en_us", "en_pt", "ja_jp", "ko_kr", "fr_fr", "de_de", "es_es", "ru_ru", "zh_cn_neko")) {
            items.add(new WizardSelectionListScreen.SelectionItem<>(language, language,
                I18nManager.translate("commandui.serverlanguage.item.detail", language)));
        }
        setScreen(new WizardSelectionListScreen<>(parent, titleKey("serverlanguage"),
            "commandui.serverlanguage.prompt",
            items,
            language -> openConfirmSend(parent, "serverlanguage",
                "areahint serverlanguage " + language,
                List.of(I18nManager.translate("commandui.serverlanguage.confirm", language))),
            null));
    }

    public static void openTeleport(Screen parent, String mode) {
        List<AreaData> areas = CommandUiData.validAreas(CommandUiData.loadCurrentDimensionAreas());
        if (areas.isEmpty()) {
            openInfo(parent, mode, "commandui.teleport.empty", null);
            return;
        }
        setScreen(new WizardSelectionListScreen<>(parent, titleKey(mode),
            I18nManager.translate("commandui.teleport.prompt", mode.toUpperCase(Locale.ROOT)),
            CommandUiData.areaItems(areas),
            area -> openConfirmAction(parent, mode,
                I18nManager.translate("commandui.teleport.confirm", mode.toUpperCase(Locale.ROOT), area.getName()),
                List.of(I18nManager.translate("commandui.teleport.format", ClientConfig.getTeleportFormat())),
                () -> {
                    closeToGame();
                    ClientNetworking.sendTeleportRequest(mode, area.getName(), ClientConfig.getTeleportFormat());
                }),
            null));
    }

    public static void openSetTp(Screen parent, String errorKey) {
        openSingleField(parent, "settp",
            "commandui.settp.label",
            "commandui.settp.placeholder",
            ClientConfig.getTeleportFormat(),
            "commandui.settp.prompt",
            I18nManager.translate("commandui.settp.detail", ClientConfig.getTeleportFormat()),
            errorKey,
            32,
            value -> {
                String format = value.trim();
                if (!ConfigData.isValidTeleportFormat(format)) {
                    openSetTp(parent, "commandui.settp.error.invalid");
                    return;
                }
                runAndClose("areahint settp " + format);
            });
    }

    public static void openAreaSelectThenCommand(Screen parent, String id, String command, String cancelCommand) {
        List<AreaData> areas = CommandUiData.loadCurrentDimensionAreas();
        if (areas.isEmpty()) {
            openInfo(parent, id, "commandui.common.no_areas", cancelCommand);
            return;
        }
        setScreen(new WizardSelectionListScreen<>(parent, titleKey(id),
            promptKey(id),
            CommandUiData.areaItems(areas),
            area -> {
                closeToGame();
                CommandUiActions.runCommand(command + " " + CommandUiData.quote(area.getName()));
            },
            cancelCommand == null ? null : () -> CommandUiActions.runCommand(cancelCommand)));
    }

    public static void openRecordCommand(Screen parent, String id, String startCommand, String cancelCommand) {
        if ("shrinkarea".equals(id)) {
            openShrinkAreaVisual(parent);
        } else {
            setScreen(new WizardConfirmScreen(parent,
                titleKey(id),
                I18nManager.translate("commandui.record.prompt"),
                List.of(I18nManager.translate("commandui.record.detail")),
                "commandui.button.start",
                () -> CommandUiActions.runCommand(startCommand),
                () -> {
                    if (cancelCommand != null) {
                        CommandUiActions.runCommand(cancelCommand);
                    }
            }));
        }
    }

    private static void openShrinkAreaVisual(Screen parent) {
        areahint.shrinkarea.ShrinkAreaManager manager = areahint.shrinkarea.ShrinkAreaManager.getInstance();
        List<AreaData> areas = manager.beginVisualSelection();
        if (areas.isEmpty()) {
            openInfo(parent, "shrinkarea", "commandui.common.no_areas", null);
            return;
        }
        setScreen(new WizardSelectionListScreen<>(parent, titleKey("shrinkarea"),
            "commandui.shrinkarea.prompt",
            CommandUiData.areaItems(areas),
            area -> {
                closeToGame();
                beginVisualRecordMode("shrinkarea");
                manager.selectAreaByName(area.getName());
            },
            () -> {
                manager.stop();
                clearVisualRecordMode();
            }));
    }

    public static void openSetHigh(Screen parent) {
        List<AreaData> areas = CommandUiData.loadCurrentDimensionAreas();
        if (areas.isEmpty()) {
            openInfo(parent, "sethigh", "commandui.common.no_areas", "areahint sethigh cancel");
            return;
        }
        setScreen(new WizardSelectionListScreen<>(parent, titleKey("sethigh"),
            "commandui.sethigh.prompt",
            CommandUiData.areaItems(areas),
            area -> openSetHighMode(parent, area),
            () -> CommandUiActions.runCommand("areahint sethigh cancel")));
    }

    private static void openSetHighMode(Screen parent, AreaData area) {
        List<WizardOptionScreen.OptionSpec> options = List.of(
            option("commandui.sethigh.unlimited", () -> openConfirmAction(parent, "sethigh",
                I18nManager.translate("commandui.sethigh.confirm.unlimited", area.getName()),
                List.of(),
                () -> {
                    closeToGame();
                    ClientNetworking.sendSetHighRequest(area.getName(), false, null, null);
                })),
            option("commandui.sethigh.custom", () -> openSetHighCustom(parent, area, null))
        );
        setScreen(new WizardOptionScreen(parent, titleKey("sethigh"),
            "commandui.sethigh.mode.prompt",
            "commandui.sethigh.mode.detail",
            options,
            () -> CommandUiActions.runCommand("areahint sethigh cancel")));
    }

    private static void openSetHighCustom(Screen parent, AreaData area, String errorKey) {
        setScreen(new WizardTextInputScreen(parent, titleKey("sethigh"),
            List.of(
                new WizardTextInputScreen.FieldSpec("commandui.sethigh.min.label", "commandui.sethigh.min.placeholder", "", 12),
                new WizardTextInputScreen.FieldSpec("commandui.sethigh.max.label", "commandui.sethigh.max.placeholder", "", 12)
            ),
            "commandui.sethigh.custom.prompt",
            "commandui.sethigh.custom.detail",
            errorKey,
            values -> {
                try {
                    double min = Double.parseDouble(values.get(0).trim());
                    double max = Double.parseDouble(values.get(1).trim());
                    if (max <= min) {
                        openSetHighCustom(parent, area, "commandui.sethigh.error.order");
                        return;
                    }
                    openConfirmAction(parent, "sethigh",
                        I18nManager.translate("commandui.sethigh.confirm.custom", area.getName(), min, max),
                        List.of(),
                        () -> {
                            closeToGame();
                            ClientNetworking.sendSetHighRequest(area.getName(), true, max, min);
                        });
                } catch (NumberFormatException e) {
                    openSetHighCustom(parent, area, "commandui.sethigh.error.number");
                }
            },
            () -> CommandUiActions.runCommand("areahint sethigh cancel")));
    }

    public static void openDescriptionStart(Screen parent, String id) {
        openConfirmCommand(parent, id, "areahint " + id);
    }

    public static void openSignature(Screen parent, String id) {
        List<AreaData> areas = CommandUiData.loadCurrentDimensionAreas();
        if (areas.isEmpty()) {
            openInfo(parent, id, "commandui.common.no_areas", "areahint " + id + " cancel");
            return;
        }
        setScreen(new WizardSelectionListScreen<>(parent, titleKey(id),
            promptKey(id),
            CommandUiData.areaItems(areas),
            area -> openSignaturePlayer(parent, id, area, null),
            () -> CommandUiActions.runCommand("areahint " + id + " cancel")));
    }

    private static void openSignaturePlayer(Screen parent, String id, AreaData area, String errorKey) {
        openSingleField(parent, id,
            "commandui.signature.player.label",
            "commandui.signature.player.placeholder",
            "",
            "commandui.signature.player.prompt",
            "commandui.signature.player.detail",
            errorKey,
            32,
            value -> {
                String playerName = value.trim();
                if (playerName.isEmpty()) {
                    openSignaturePlayer(parent, id, area, "commandui.common.error.empty");
                    return;
                }
                List<String> details = new ArrayList<>(areaDetails(area));
                details.add(I18nManager.translate("commandui.signature.target", playerName));
                openConfirmAction(parent, id,
                    I18nManager.translate("commandui.signature.confirm", area.getName()),
                    details,
                    () -> {
                        String dimension = currentDimensionId();
                        if (dimension == null) {
                            sendLocalError("commandui.common.error.dimension");
                            return;
                        }
                        closeToGame();
                        SignatureClientNetworking.sendToServer("addsignature".equals(id) ? "add" : "delete",
                            area.getName(), dimension, playerName);
                    });
            },
            () -> CommandUiActions.runCommand("areahint " + id + " cancel"));
    }

    private static void openConfirmSend(Screen parent, String id, String command, List<String> details) {
        setScreen(new WizardConfirmScreen(parent, titleKey(id),
            I18nManager.translate("commandui.common.confirm.prompt"),
            details == null || details.isEmpty() ? List.of("/" + command) : details,
            "commandui.button.execute",
            () -> CommandUiActions.runCommand(command),
            null));
    }

    private static void openConfirmAction(Screen parent, String id, String prompt, List<String> details, Runnable action) {
        setScreen(new WizardConfirmScreen(parent, titleKey(id),
            prompt,
            details,
            "commandui.button.confirm",
            action,
            null));
    }

    private static void openInfo(Screen parent, String id, String messageKey, String cancelCommand) {
        setScreen(new WizardConfirmScreen(parent, titleKey(id),
            I18nManager.translate(messageKey),
            List.of(),
            "commandui.button.close",
            () -> {
                if (cancelCommand != null) {
                    CommandUiActions.runCommand(cancelCommand);
                }
            },
            cancelCommand == null ? null : () -> CommandUiActions.runCommand(cancelCommand)));
    }

    private static void openSingleField(Screen parent, String id, String labelKey, String placeholderKey,
                                        String initialValue, String promptKey, String detailTextOrKey,
                                        String errorKey, int maxLength,
                                        java.util.function.Consumer<String> submitAction) {
        openSingleField(parent, id, labelKey, placeholderKey, initialValue, promptKey, detailTextOrKey,
            errorKey, maxLength, submitAction, null);
    }

    private static void openSingleField(Screen parent, String id, String labelKey, String placeholderKey,
                                        String initialValue, String promptKey, String detailTextOrKey,
                                        String errorKey, int maxLength,
                                        java.util.function.Consumer<String> submitAction, Runnable cancelAction) {
        setScreen(new WizardTextInputScreen(parent, titleKey(id),
            List.of(new WizardTextInputScreen.FieldSpec(labelKey, placeholderKey, initialValue, maxLength)),
            promptKey,
            detailTextOrKey,
            errorKey,
            values -> submitAction.accept(values.isEmpty() ? "" : values.get(0)),
            cancelAction));
    }

    private static void runAndClose(String command) {
        closeToGame();
        CommandUiActions.runCommand(command);
    }

    private static void startThenRun(String startCommand, String actionCommand) {
        closeToGame();
        CommandUiActions.runCommand(startCommand);
        CommandUiActions.runCommand(actionCommand);
    }

    private static List<WizardOptionScreen.OptionSpec> sizePresetOptions(java.util.function.Consumer<String> action) {
        return List.of(
            option("commandui.common.size.extra_large", () -> action.accept("extra_large")),
            option("commandui.common.size.large", () -> action.accept("large")),
            option("commandui.common.size.medium_large", () -> action.accept("medium_large")),
            option("commandui.common.size.medium", () -> action.accept("medium")),
            option("commandui.common.size.medium_small", () -> action.accept("medium_small")),
            option("commandui.common.size.small", () -> action.accept("small")),
            option("commandui.common.size.extra_small", () -> action.accept("extra_small"))
        );
    }

    private static WizardOptionScreen.OptionSpec option(String labelKey, Runnable action) {
        return new WizardOptionScreen.OptionSpec(labelKey, "", -1, action);
    }

    private static void setScreen(Screen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(screen);
        }
    }

    private static void closeToGame() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(null);
        }
    }

    private static String titleKey(String id) {
        return "commandui." + id + ".title";
    }

    private static String promptKey(String id) {
        return "commandui." + id + ".prompt";
    }

    private static String currentDimensionId() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.world != null ? client.world.getRegistryKey().getValue().toString() : null;
    }

    private static List<String> areaDetails(AreaData area) {
        List<String> details = new ArrayList<>();
        details.add(I18nManager.translate("commandui.common.area.name", area.getName()));
        details.add(I18nManager.translate("commandui.common.area.level", area.getLevel()));
        details.add(I18nManager.translate("commandui.common.area.surface", nullText(area.getSurfacename())));
        details.add(I18nManager.translate("commandui.common.area.base", nullText(area.getBaseName())));
        details.add(I18nManager.translate("commandui.common.area.signature", nullText(area.getSignature())));
        return details;
    }

    private static String nullText(String value) {
        return value == null || value.trim().isEmpty() ? I18nManager.translate("commandui.common.none") : value;
    }

    private static void sendLocalError(String key) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal(I18nManager.translate(key)).formatted(Formatting.RED), false);
        }
    }

}
