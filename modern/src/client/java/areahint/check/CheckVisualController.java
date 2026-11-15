package areahint.check;

import areahint.commandui.CommandUiActions;
import areahint.commandui.CommandUiData;
import areahint.commandui.WizardOptionScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.commandui.WizardTextInputScreen;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * check 图形流程控制器，只负责把联合域名选择转换为现有 /areahint check 指令。
 */
public final class CheckVisualController {
    private CheckVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        Map<String, List<AreaData>> unionGroups = buildUnionGroups();
        List<WizardOptionScreen.OptionSpec> options = new ArrayList<>();
        options.add(option("commandui.check.all", () -> runAndClose("areahint check")));
        if (!unionGroups.isEmpty()) {
            options.add(option("commandui.check.select", () -> openUnionSelect(parent, unionGroups)));
        }
        options.add(option("commandui.check.input", () -> openInput(parent, null)));

        setScreen(new WizardOptionScreen(parent,
            "commandui.check.title",
            "commandui.check.prompt",
            "commandui.check.detail",
            options,
            null));
    }

    private static void openUnionSelect(Screen parent, Map<String, List<AreaData>> unionGroups) {
        setScreen(new WizardSelectionListScreen<>(parent,
            "commandui.check.title",
            "commandui.check.prompt",
            unionItems(unionGroups),
            unionName -> runAndClose("areahint check " + CommandUiData.quote(unionName)),
            null));
    }

    private static void openInput(Screen parent, String errorKey) {
        setScreen(new WizardTextInputScreen(parent,
            "commandui.check.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.check.label",
                "commandui.check.placeholder", "", 120)),
            "commandui.check.input.prompt",
            "commandui.check.input.detail",
            errorKey,
            values -> {
                String unionName = values.isEmpty() ? "" : values.get(0).trim();
                if (unionName.isEmpty()) {
                    openInput(parent, "commandui.check.error.empty");
                    return;
                }
                runAndClose("areahint check " + CommandUiData.quote(unionName));
            },
            null));
    }

    private static Map<String, List<AreaData>> buildUnionGroups() {
        Map<String, List<AreaData>> groups = new LinkedHashMap<>();
        for (AreaData area : CommandUiData.loadCurrentDimensionAreas()) {
            String unionName = area.getSurfacename() == null || area.getSurfacename().trim().isEmpty()
                ? area.getName()
                : area.getSurfacename().trim();
            groups.computeIfAbsent(unionName, ignored -> new ArrayList<>()).add(area);
        }
        return groups;
    }

    private static List<WizardSelectionListScreen.SelectionItem<String>> unionItems(Map<String, List<AreaData>> unionGroups) {
        List<WizardSelectionListScreen.SelectionItem<String>> items = new ArrayList<>();
        for (Map.Entry<String, List<AreaData>> entry : unionGroups.entrySet()) {
            items.add(new WizardSelectionListScreen.SelectionItem<>(entry.getKey(), entry.getKey(),
                I18nManager.translate("commandui.check.item.detail", entry.getValue().size())));
        }
        return items;
    }

    private static WizardOptionScreen.OptionSpec option(String labelKey, Runnable action) {
        return new WizardOptionScreen.OptionSpec(labelKey, "", -1, action);
    }

    private static void runAndClose(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, null);
        }
        CommandUiActions.runCommand(command);
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, screen);
        }
    }
}
