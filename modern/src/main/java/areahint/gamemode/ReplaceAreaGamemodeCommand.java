package areahint.gamemode;

import areahint.Areashint;
import areahint.approval.ApprovalManager;
import areahint.command.ServerCommands;
import areahint.config.ServerConfig;
import areahint.i18n.ServerI18nManager;
import areahint.network.Channel;
import areahint.network.NetworkBuf;
import areahint.network.NetworkCompat;
import areahint.network.Packets;
import areahint.permission.PermissionNodes;
import areahint.permission.PermissionService;
import areahint.world.WorldFolderManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

// 域名游戏模式只修改服务端当前世界文件，聊天按钮与图形界面共用同一套命令校验。
public final class ReplaceAreaGamemodeCommand {
    private static final String ROOT = "areahint replaceareagamemode";
    private static final List<String> MODES = List.of("survival", "creative", "adventure", "spectator");

    private ReplaceAreaGamemodeCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return literal("replaceareagamemode")
            .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.REPLACE_AREA_GAMEMODE, 2))
            .executes(context -> start(context.getSource(), false))
            .then(selectBranch(false))
            .then(modeBranch(false))
            .then(literal("gui")
                .executes(context -> start(context.getSource(), true))
                .then(selectBranch(true))
                .then(modeBranch(true)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> selectBranch(boolean visual) {
        return literal("select")
            // 完整维度标识含冒号，使用带引号字符串解析以匹配聊天按钮和图形界面生成的命令。
            .then(argument("dimension", StringArgumentType.string())
                .then(argument("areaName", StringArgumentType.string())
                    .executes(context -> select(context, visual))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> modeBranch(boolean visual) {
        return literal("mode")
            .then(argument("dimension", StringArgumentType.string())
                .then(argument("gamemode", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        for (String mode : MODES) {
                            if (mode.startsWith(builder.getRemaining())) {
                                builder.suggest(mode);
                            }
                        }
                        return builder.buildFuture();
                    })
                    .then(argument("areaName", StringArgumentType.string())
                        .executes(context -> apply(context, visual)))));
    }

    private static int start(CommandSourceStack source, boolean visual) {
        if (!validate(source, null)) {
            return 0;
        }
        String dimension = currentDimension(source);
        try {
            List<JsonObject> areas = listedAreas(readAreas(dimension));
            if (areas.isEmpty()) {
                source.sendSystemMessage(message(source, "commandui.common.no_areas"));
                return 0;
            }
            if (visual) {
                sendEditor(source.getPlayer(), "list", dimension, areas);
            } else {
                source.sendSystemMessage(message(source, "gamemode.message.select_area"));
                for (JsonObject area : areas) {
                    String name = area.get("name").getAsString();
                    source.sendSystemMessage(button(name, ROOT + " select " + quote(dimension) + " " + quote(name))
                        .append(Component.literal("  " + modeDescription(source, area)).withStyle(ChatFormatting.GRAY)));
                }
            }
            return 1;
        } catch (IOException | RuntimeException e) {
            return fileError(source, e);
        }
    }

    private static int select(CommandContext<CommandSourceStack> context, boolean visual) {
        CommandSourceStack source = context.getSource();
        String dimension = StringArgumentType.getString(context, "dimension");
        String areaName = StringArgumentType.getString(context, "areaName");
        if (!validate(source, dimension)) {
            return 0;
        }
        try {
            JsonObject target = findUnique(source, readAreas(dimension), areaName);
            if (target == null) {
                return 0;
            }
            if (visual) {
                sendEditor(source.getPlayer(), "modes", dimension, List.of(target));
            } else {
                source.sendSystemMessage(message(source, "gamemode.message.select_mode", areaName));
                MutableComponent buttons = Component.empty();
                for (String mode : MODES) {
                    buttons.append(button("[" + translate(source, "gamemode.mode." + mode) + "]",
                        ROOT + " mode " + quote(dimension) + " " + mode + " " + quote(areaName))).append("  ");
                }
                source.sendSystemMessage(buttons);
            }
            return 1;
        } catch (IOException | RuntimeException e) {
            return fileError(source, e);
        }
    }

    private static int apply(CommandContext<CommandSourceStack> context, boolean visual) {
        CommandSourceStack source = context.getSource();
        String dimension = StringArgumentType.getString(context, "dimension");
        String areaName = StringArgumentType.getString(context, "areaName");
        String mode = StringArgumentType.getString(context, "gamemode");
        if (!validate(source, dimension)) {
            return 0;
        }
        if (!MODES.contains(mode)) {
            source.sendFailure(message(source, "gamemode.error.invalid_mode", mode));
            return 0;
        }
        try {
            // 提交时重新读取服务端文件，保留未知字段并避免覆盖列表显示之后发生的其他属性修改。
            JsonArray document = readAreas(dimension);
            JsonObject target = findUnique(source, document, areaName);
            if (target == null) {
                return 0;
            }
            target.addProperty("gamemode", mode);
            ApprovalManager.writeDocument(areaFile(dimension), document);
            source.sendSystemMessage(message(source, "gamemode.message.updated", areaName, mode));
            // 此处已验证本指令权限，内部重载不再要求玩家另获 reload 权限节点。
            ServerCommands.reload(source);
            if (visual) {
                sendEditor(source.getPlayer(), "result", dimension, List.of(target));
            }
            return 1;
        } catch (IOException | RuntimeException e) {
            return fileError(source, e);
        }
    }

    private static boolean validate(CommandSourceStack source, String expectedDimension) {
        if (!source.isPlayer()) {
            source.sendFailure(Component.translatable("command.error.general_9"));
            return false;
        }
        // 每个按钮提交都重新检查开关和权限，避免旧界面在撤权之后继续修改文件。
        if (!PermissionService.hasCommandPermission(source, PermissionNodes.REPLACE_AREA_GAMEMODE, 2)) {
            source.sendFailure(message(source, "message.error.permission"));
            return false;
        }
        if (!ServerConfig.isForceUseAreahint()) {
            source.sendFailure(message(source, "gamemode.error.force_required"));
            return false;
        }
        if (expectedDimension != null && !expectedDimension.equals(currentDimension(source))) {
            source.sendFailure(message(source, "gamemode.error.dimension_changed"));
            return false;
        }
        return true;
    }

    private static JsonArray readAreas(String dimension) throws IOException {
        Path file = areaFile(dimension);
        if (Files.notExists(file)) {
            return new JsonArray();
        }
        JsonElement document = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
        if (!document.isJsonArray()) {
            throw new IOException("域名文件根节点必须是数组：" + file);
        }
        return document.getAsJsonArray();
    }

    private static Path areaFile(String dimension) throws IOException {
        // 只接受服务器支持的完整维度标识，客户端传入的字符串不会直接组成磁盘路径。
        String fileName = switch (dimension) {
            case "minecraft:overworld" -> Packets.getFileNameForDimension(Packets.DIMENSION_OVERWORLD);
            case "minecraft:the_nether" -> Packets.getFileNameForDimension(Packets.DIMENSION_NETHER);
            case "minecraft:the_end" -> Packets.getFileNameForDimension(Packets.DIMENSION_END);
            default -> null;
        };
        if (fileName == null) {
            throw new IOException("当前维度没有域名文件：" + dimension);
        }
        return WorldFolderManager.getWorldDimensionFile(fileName);
    }

    private static List<JsonObject> listedAreas(JsonArray document) {
        List<JsonObject> result = new ArrayList<>();
        for (JsonElement element : document) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject area = element.getAsJsonObject();
            JsonElement name = area.get("name");
            if (name != null && name.isJsonPrimitive() && name.getAsJsonPrimitive().isString()
                    && !name.getAsString().isBlank()) {
                result.add(area);
            }
        }
        return result;
    }

    private static JsonObject findUnique(CommandSourceStack source, JsonArray document, String areaName) {
        JsonObject target = null;
        for (JsonObject area : listedAreas(document)) {
            if (areaName.equals(area.get("name").getAsString())) {
                if (target != null) {
                    source.sendFailure(message(source, "gamemode.error.duplicate_name", areaName));
                    return null;
                }
                target = area;
            }
        }
        if (target == null) {
            source.sendFailure(message(source, "gamemode.error.area_not_found", areaName));
        }
        return target;
    }

    private static void sendEditor(ServerPlayer player, String action, String dimension, List<JsonObject> areas) {
        NetworkBuf buffer = NetworkCompat.get().newBuffer();
        buffer.writeString(action);
        buffer.writeString(dimension);
        buffer.writeString(ServerConfig.getDefaultGamemode().getSerializedName());
        buffer.writeInt(areas.size());
        for (JsonObject area : areas) {
            buffer.writeString(area.get("name").getAsString());
            buffer.writeString(explicitMode(area));
        }
        NetworkCompat.get().sendToPlayer(player, Channel.S2C_AREA_GAMEMODE_EDITOR, buffer);
    }

    private static String explicitMode(JsonObject area) {
        JsonElement mode = area.get("gamemode");
        if (mode != null && mode.isJsonPrimitive() && mode.getAsJsonPrimitive().isString()
                && MODES.contains(mode.getAsString())) {
            return mode.getAsString();
        }
        return "";
    }

    private static String modeDescription(CommandSourceStack source, JsonObject area) {
        String mode = explicitMode(area);
        if (mode.isEmpty()) {
            return translate(source, "gamemode.mode.default",
                translate(source, "gamemode.mode." + ServerConfig.getDefaultGamemode().getSerializedName()));
        }
        return translate(source, "gamemode.mode." + mode);
    }

    private static String currentDimension(CommandSourceStack source) {
        return source.getLevel().dimension().identifier().toString();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static MutableComponent button(String label, String command) {
        return Component.literal(label).withStyle(ChatFormatting.AQUA).withStyle(style -> style
            // 点击事件沿用工程已有的多版本写法，保持 1.21.5 之后的记录类接口兼容。
            /*.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + command))
            */
            .withClickEvent(new ClickEvent.RunCommand("/" + command))
        );
    }

    private static Component message(CommandSourceStack source, String key, Object... args) {
        return Component.literal(translate(source, key, args));
    }

    private static String translate(CommandSourceStack source, String key, Object... args) {
        return ServerI18nManager.translateForPlayer(source.getPlayer().getUUID(), key, args);
    }

    private static int fileError(CommandSourceStack source, Exception e) {
        Areashint.LOGGER.error("修改域名游戏模式失败", e);
        source.sendFailure(message(source, "gamemode.error.file"));
        return 0;
    }
}
