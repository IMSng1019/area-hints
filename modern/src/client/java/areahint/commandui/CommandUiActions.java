package areahint.commandui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * 指令可视化通用动作，所有按钮都从这里回到原指令流程。
 */
public final class CommandUiActions {
    private CommandUiActions() {
    }

    public static void runCommand(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.player.connection == null || command == null) {
            return;
        }

        String normalizedCommand = command.trim();
        if (normalizedCommand.startsWith("/")) {
            normalizedCommand = normalizedCommand.substring(1);
        }
        if (!normalizedCommand.isEmpty()) {
            client.player.connection.sendCommand(normalizedCommand);
        }
    }

    public static void runCommandAndClose(Screen screen, String command) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, null);
        }
        runCommand(command);
    }
}
