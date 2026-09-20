package areahint.soundevent;

import areahint.command.AreasHintCommandRoot;
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
     * 注册客户端 /areahintc soundlevel 指令树。
     * <p>
     * 必须挂在独立根指令下：Fabric 会把通过 ClientCommandRegistrationCallback 注册的整条根指令
     * 标记为客户端命令，如果挂在 /areahint 下，会导致 /areahint 的全部服务端子命令在客户端被拦截。
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
        dispatcher.register(ClientCommandManager.literal(AreasHintCommandRoot.CLIENT_ROOT)
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
