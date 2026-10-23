package areahint.urban;

import areahint.commandui.CommandUiActions;
import areahint.commandui.CommandUiData;
import areahint.commandui.WizardConfirmScreen;
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

// 城市化向导只展示服务端筛选的域名，并通过同一套服务端命令执行申请或管理员直接修改。
public final class ApplyUrbanAreaVisualController {
    private static final String ROOT = "areahint applyurbanarea gui";
    private static final String TITLE = "commandui.applyurbanarea.title";
    private static Screen parentScreen;
    private static boolean registered;

    public record AreaEntry(String name, String type) {
    }

    private ApplyUrbanAreaVisualController() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_URBAN_AREA_EDITOR, (client, buffer) -> {
            String action = buffer.readString();
            String dimension = buffer.readString();
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
                // 断线重连、切换维度或服务端撤销指令权限后，不允许旧响应重新打开过期向导。
                if (handler == null || client.getNetworkHandler() != handler || client.world == null
                        || !dimension.equals(client.world.getRegistryKey().getValue().toString()) || !isAvailable()) {
                    return;
                }
                if ("list".equals(action)) {
                    showAreas(dimension, areas);
                } else if (("result".equals(action) || "submitted".equals(action)) && areas.size() == 1) {
                    showResult(dimension, areas.get(0), "submitted".equals(action));
                }
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        registered = true;
    }

    public static boolean isAvailable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) {
            return false;
        }
        // 服务端命令树已按配置开关和 LuckPerms 权限过滤，直接读取它可避免额外缓存造成显示状态滞后。
        var root = client.getNetworkHandler().getCommandDispatcher().getRoot().getChild("areahint");
        return root != null && root.getChild("applyurbanarea") != null;
    }

    public static void openFromCommandUi(Screen parent) {
        if (!isAvailable()) {
            return;
        }
        parentScreen = parent;
        CommandUiActions.runCommandAndClose(parent, ROOT);
    }

    private static void showAreas(String dimension, List<AreaEntry> areas) {
        List<WizardSelectionListScreen.SelectionItem<AreaEntry>> items = new ArrayList<>();
        for (AreaEntry area : areas) {
            String type = area.type().isBlank() ? "rural" : area.type();
            String detail = "rural".equals(type) || "urban".equals(type)
                ? I18nManager.translate("urban.type." + type) + " (type: " + type + ")"
                : "type: " + type;
            items.add(new WizardSelectionListScreen.SelectionItem<>(area, area.name(), detail));
        }
        MinecraftClient.getInstance().setScreen(new WizardSelectionListScreen<>(parentScreen, TITLE,
            "commandui.applyurbanarea.prompt", items,
            area -> CommandUiActions.runCommandAndClose(MinecraftClient.getInstance().currentScreen,
                ROOT + " select " + CommandUiData.quote(dimension) + " " + CommandUiData.quote(area.name())),
            ApplyUrbanAreaVisualController::clear));
    }

    private static void showResult(String dimension, AreaEntry area, boolean submitted) {
        // 申请仅展示待审批参数，正式文件的写入与 reload 始终交给服务端成功分支处理。
        String messageKey = submitted ? "urban.message.submitted" : "urban.message.updated";
        String details = I18nManager.translate("commandui.applyurbanarea.result", area.name(), dimension, area.type());
        MinecraftClient.getInstance().setScreen(new WizardConfirmScreen(parentScreen, TITLE,
            I18nManager.translate(messageKey, area.name(), area.type()), List.of(details.split("\\n")),
            "commandui.button.close", ApplyUrbanAreaVisualController::clear, ApplyUrbanAreaVisualController::clear));
    }

    private static void clear() {
        parentScreen = null;
    }
}
