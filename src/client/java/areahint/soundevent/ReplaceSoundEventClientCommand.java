package areahint.soundevent;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * 域名切换声音的纯客户端指令，不向服务端发送分类或选择请求。
 */
public final class ReplaceSoundEventClientCommand {
    private static boolean registered;

    private ReplaceSoundEventClientCommand() {
    }

    /**
     * 注册客户端 /areahint replacesoundevent 指令树。
     */
    public static void register() {
        if (registered) {
            return;
        }
        ClientCommandRegistrationCallback.EVENT.register(ReplaceSoundEventClientCommand::registerCommands);
        registered = true;
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                         net.minecraft.command.CommandRegistryAccess registryAccess) {
        dispatcher.register(ClientCommandManager.literal("areahint")
                .then(ClientCommandManager.literal("replacesoundevent")
                        .executes(context -> start(context))
                        .then(ClientCommandManager.literal("category")
                                .then(ClientCommandManager.argument("category", StringArgumentType.word())
                                        .executes(context -> showCategory(context, 0))
                                        .then(ClientCommandManager.argument("page", IntegerArgumentType.integer(0))
                                                .executes(context -> showCategory(context,
                                                        IntegerArgumentType.getInteger(context, "page"))))))
                        .then(ClientCommandManager.literal("instrument")
                                .then(ClientCommandManager.argument("soundId", IdentifierArgumentType.identifier())
                                        .executes(context -> showInstrument(context, 0))
                                        .then(ClientCommandManager.argument("page", IntegerArgumentType.integer(0))
                                                .executes(context -> showInstrument(context,
                                                        IntegerArgumentType.getInteger(context, "page"))))))
                        .then(ClientCommandManager.literal("select")
                                .then(ClientCommandManager.argument("soundId", IdentifierArgumentType.identifier())
                                        .then(ClientCommandManager.argument("pitch", FloatArgumentType.floatArg(0.5f, 2.0f))
                                                .executes(ReplaceSoundEventClientCommand::select))))
                        .then(ClientCommandManager.literal("none")
                                .executes(context -> selectNone()))
                        .then(ClientCommandManager.literal("cancel")
                                .executes(context -> cancel()))));
    }

    private static int start(CommandContext<FabricClientCommandSource> context) {
        SoundEventChatUI.showCategories();
        return 1;
    }

    private static int showCategory(CommandContext<FabricClientCommandSource> context, int page) {
        SoundEventChatUI.showCategory(StringArgumentType.getString(context, "category"), page);
        return 1;
    }

    private static int showInstrument(CommandContext<FabricClientCommandSource> context, int page) {
        SoundEventChatUI.showInstrument(
                context.getArgument("soundId", Identifier.class).toString(), page);
        return 1;
    }

    private static int select(CommandContext<FabricClientCommandSource> context) {
        String soundId = context.getArgument("soundId", Identifier.class).toString();
        float pitch = FloatArgumentType.getFloat(context, "pitch");
        Optional<SoundEventSelection> selection = SoundEventCatalog.createSelection(soundId, pitch);
        if (selection.isEmpty()) {
            SoundEventChatUI.showError("soundevent.error.invalid");
            return 0;
        }
        if (!SoundEventManager.applySelection(selection.get())) {
            SoundEventChatUI.showError("soundevent.error.invalid_sound", soundId);
            return 0;
        }
        SoundEventChatUI.showSelectionSuccess(selection.get());
        return 1;
    }

    private static int selectNone() {
        SoundEventSelection selection = SoundEventSelection.none();
        SoundEventManager.applySelection(selection);
        SoundEventChatUI.showSelectionSuccess(selection);
        return 1;
    }

    private static int cancel() {
        SoundEventChatUI.showCancelled();
        return 1;
    }
}
