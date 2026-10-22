package areahint.gamemode;

import areahint.commandui.CommandUiActions;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardOptionScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.i18n.I18nManager;
import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import java.util.ArrayList;
import java.util.List;

// 图形向导仅展示服务端返回的域名，并把完整维度和域名交回同一套服务端命令处理。
public final class ReplaceAreaGamemodeVisualController {
    private static final String ROOT = "areahint replaceareagamemode gui";
    private static final String TITLE = "commandui.replaceareagamemode.title";
    private static Screen parentScreen;
    private static boolean registered;

    private record AreaEntry(String name, String mode) {
    }

    private ReplaceAreaGamemodeVisualController() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_AREA_GAMEMODE_EDITOR, (client, buffer) -> {
            String action = buffer.readString();
            String dimension = buffer.readString();
            String defaultMode = buffer.readString();
            int count = buffer.readInt();
            if (count < 0 || count > 65536) {
                return;
            }
            List<AreaEntry> areas = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                areas.add(new AreaEntry(buffer.readString(), buffer.readString()));
            }
            ClientPlayNetworkHandler handler = client.getNetworkHandler();
            client.execute(() -> {
                // 传输期间断线重连或切换维度时丢弃旧页面，后续提交仍会由服务端再次校验维度。
                if (handler == null || client.getNetworkHandler() != handler || client.world == null
                        || !dimension.equals(client.world.getRegistryKey().getValue().toString())) {
                    return;
                }
                if ("list".equals(action)) {
                    showAreas(dimension, defaultMode, areas);
                } else if ("modes".equals(action) && areas.size() == 1) {
                    showModes(dimension, defaultMode, areas.get(0));
                } else if ("result".equals(action) && areas.size() == 1) {
                    showResult(areas.get(0));
                }
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        registered = true;
    }

    public static void openFromCommandUi(Screen parent) {
        parentScreen = parent;
        CommandUiActions.runCommandAndClose(parent, ROOT);
    }

    private static void showAreas(String dimension, String defaultMode, List<AreaEntry> areas) {
        List<WizardSelectionListScreen.SelectionItem<AreaEntry>> items = new ArrayList<>();
        for (AreaEntry area : areas) {
            items.add(new WizardSelectionListScreen.SelectionItem<>(area, area.name(), modeDescription(area, defaultMode)));
        }
        setScreen(new WizardSelectionListScreen<>(parentScreen, TITLE, "commandui.replaceareagamemode.prompt", items,
            area -> CommandUiActions.runCommandAndClose(MinecraftClient.getInstance().currentScreen,
                ROOT + " select " + quote(dimension) + " " + quote(area.name())),
            ReplaceAreaGamemodeVisualController::clear));
    }

    private static void showModes(String dimension, String defaultMode, AreaEntry area) {
        List<WizardOptionScreen.OptionSpec> options = new ArrayList<>();
        for (String mode : List.of("survival", "creative", "adventure", "spectator")) {
            options.add(new WizardOptionScreen.OptionSpec("gamemode.mode." + mode, "", -1,
                () -> CommandUiActions.runCommandAndClose(MinecraftClient.getInstance().currentScreen,
                    ROOT + " mode " + quote(dimension) + " " + mode + " " + quote(area.name()))));
        }
        setScreen(new WizardOptionScreen(parentScreen, TITLE, "commandui.replaceareagamemode.mode.prompt",
            area.name() + " · " + modeDescription(area, defaultMode), options,
            ReplaceAreaGamemodeVisualController::clear));
    }

    private static void showResult(AreaEntry area) {
        setScreen(new WizardConfirmScreen(parentScreen, TITLE,
            I18nManager.translate("gamemode.message.updated", area.name(), area.mode()),
            List.of("gamemode: " + area.mode()), "commandui.button.close",
            ReplaceAreaGamemodeVisualController::clear, ReplaceAreaGamemodeVisualController::clear));
    }

    private static String modeDescription(AreaEntry area, String defaultMode) {
        if (area.mode().isEmpty()) {
            return I18nManager.translate("gamemode.mode.default", I18nManager.translate("gamemode.mode." + defaultMode));
        }
        return I18nManager.translate("gamemode.mode." + area.mode());
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static void clear() {
        parentScreen = null;
    }

    private static void setScreen(Screen screen) {
        MinecraftClient.getInstance().setScreen(screen);
    }
}
