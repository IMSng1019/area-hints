package areahint.teleport;

import areahint.util.TextEventCompat;

import areahint.AreashintClient;
import areahint.config.ClientConfig;
import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.network.ClientNetworking;
import areahint.network.Packets;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import areahint.i18n.I18nManager;

import java.nio.file.Path;
import java.util.List;

public class TeleportClientManager {
    private enum State {
        IDLE,
        INPUT_FORMAT
    }

    private static TeleportClientManager instance;
    private static boolean sendListenerRegistered;

    private State state = State.IDLE;

    private TeleportClientManager() {
    }

    public static TeleportClientManager getInstance() {
        if (instance == null) {
            instance = new TeleportClientManager();
        }
        return instance;
    }

    public static void init() {
        if (sendListenerRegistered) {
            return;
        }
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> !getInstance().handleChatInput(message));
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> !getInstance().handleChatInput(command));
        sendListenerRegistered = true;
    }

    public void handleClientCommand(String action) {
        if (action.equals("tcp_start")) {
            showAreaList("tcp");
        } else if (action.equals("udp_start")) {
            showAreaList("udp");
        } else if (action.startsWith("tcp_select:")) {
            requestTeleport("tcp", action.substring("tcp_select:".length()));
        } else if (action.startsWith("udp_select:")) {
            requestTeleport("udp", action.substring("udp_select:".length()));
        } else if (action.equals("settp_start")) {
            startFormatInput();
        } else if (action.startsWith("settp_set:")) {
            setTeleportFormat(action.substring("settp_set:".length()));
        }
    }

    public void handleServerResponse(boolean success, String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        client.player.sendMessage(Text.literal((success ? "§a" : "§c") + message), false);
    }

    private void showAreaList(String mode) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        List<AreaData> areas = loadCurrentDimensionAreas(client);
        if (areas.isEmpty()) {
            client.player.sendMessage(Text.literal(I18nManager.translate("teleport.error.no_areas")), false);
            return;
        }

        client.player.sendMessage(Text.literal(I18nManager.translate("teleport.prompt.select", mode.toUpperCase())), false);
        for (int i = 0; i < areas.size(); i++) {
            AreaData area = areas.get(i);
            MutableText line = Text.literal(String.format("§a%d. §f%s", i + 1, area.getName()))
                    .setStyle(Style.EMPTY
                            .withClickEvent(TextEventCompat.runCommand("/areahint " + mode + " " + area.getName()))
                            .withHoverEvent(TextEventCompat.showText(Text.literal(I18nManager.translate("teleport.hover.teleport_to", area.getName()))))
                            .withColor(Formatting.AQUA));
            client.player.sendMessage(line, false);
        }
    }

    private List<AreaData> loadCurrentDimensionAreas(MinecraftClient client) {
        String dimension = Packets.convertDimensionPathToType(client.world.getRegistryKey().getValue().getPath());
        String fileName = Packets.getFileNameForDimension(dimension);
        if (fileName == null) {
            return List.of();
        }
        Path areaFile = areahint.world.ClientWorldFolderManager.getWorldDimensionFile(fileName);
        return FileManager.readAreaData(areaFile).stream()
                .filter(AreaData::isValid)
                .toList();
    }

    private void requestTeleport(String mode, String areaName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        if (areaName == null || areaName.trim().isEmpty()) {
            client.player.sendMessage(Text.literal(I18nManager.translate("teleport.error.empty_name")), false);
            return;
        }
        String teleportFormat = ClientConfig.getTeleportFormat();
        if (!isValidTeleportFormat(teleportFormat)) {
            client.player.sendMessage(Text.literal(I18nManager.translate("teleport.error.invalid_format")), false);
            return;
        }
        client.player.sendMessage(Text.literal(I18nManager.translate("teleport.message.requesting", areaName)), false);
        ClientNetworking.sendTeleportRequest(mode, stripQuotes(areaName), teleportFormat);
    }

    private void startFormatInput() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        if (state != State.IDLE) {
            client.player.sendMessage(Text.literal(I18nManager.translate("teleport.error.busy")), false);
            return;
        }
        state = State.INPUT_FORMAT;
        client.player.sendMessage(Text.literal(I18nManager.translate("teleport.prompt.format_input", ClientConfig.getTeleportFormat())), false);
    }

    private boolean handleChatInput(String input) {
        if (state != State.INPUT_FORMAT) {
            return false;
        }
        if (input == null) {
            return true;
        }
        String value = input.trim();
        if (value.equalsIgnoreCase("cancel") || value.equals(I18nManager.translate("teleport.cancel.keyword"))) {
            state = State.IDLE;
            sendLocalMessage(I18nManager.translate("teleport.message.cancelled"));
            return true;
        }
        if (setTeleportFormat(value)) {
            state = State.IDLE;
        }
        return true;
    }

    private boolean setTeleportFormat(String format) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }
        String value = stripQuotes(format);
        if (!isValidTeleportFormat(value)) {
            client.player.sendMessage(Text.literal(I18nManager.translate("teleport.error.invalid_format_detail")), false);
            return false;
        }
        ClientConfig.setTeleportFormat(value);
        client.player.sendMessage(Text.literal(I18nManager.translate("teleport.message.format_set", ClientConfig.getTeleportFormat())), false);
        return true;
    }

    private void sendLocalMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), false);
        }
    }

    private boolean isValidTeleportFormat(String teleportFormat) {
        return areahint.data.ConfigData.isValidTeleportFormat(teleportFormat);
    }

    private String stripQuotes(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() > 1) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
