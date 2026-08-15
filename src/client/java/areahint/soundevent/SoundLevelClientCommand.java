package areahint.soundevent;

import areahint.data.ConfigData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

/**
 * 域名切换声音音量的纯客户端指令，不向服务端发送设置请求。
 */
public final class SoundLevelClientCommand {
    private static boolean registered;

    private SoundLevelClientCommand() {
    }

    /**
     * 注册客户端 /areahint soundlevel 指令树。
     */
    public static void register() {
        if (registered) {
            return;
        }
        ClientCommandRegistrationCallback.EVENT.register(SoundLevelClientCommand::registerCommands);
        registered = true;
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                         net.minecraft.command.CommandRegistryAccess registryAccess) {
        dispatcher.register(ClientCommandManager.literal("areahint")
                .then(ClientCommandManager.literal("soundlevel")
                        .executes(context -> showMenu())
                        .then(ClientCommandManager.argument("level",
                                        FloatArgumentType.floatArg(ConfigData.SOUND_LEVEL_MIN,
                                                ConfigData.SOUND_LEVEL_MAX))
                                .executes(SoundLevelClientCommand::setSoundLevel))));
    }

    private static int showMenu() {
        SoundLevelChatUI.showMenu();
        return 1;
    }

    private static int setSoundLevel(CommandContext<FabricClientCommandSource> context) {
        float soundLevel = FloatArgumentType.getFloat(context, "level");
        if (!SoundEventManager.applySoundLevel(soundLevel)) {
            return 0;
        }
        SoundLevelChatUI.showSelectionSuccess(soundLevel);
        return 1;
    }
}
