package areahint.urban;

import areahint.Areashint;
import areahint.approval.ApprovalManager;
import areahint.command.ServerCommands;
import areahint.config.ServerConfig;
import areahint.data.AreaData;
import areahint.i18n.ServerI18nManager;
import areahint.management.AreaManagementCapabilityService;
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
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

// 聊天列表与可视化向导共用服务端文件、授权和提交入口，客户端不能指定类型或替换正式文件。
public final class ApplyUrbanAreaCommand {
    private static final String ROOT = "areahint applyurbanarea";

    private ApplyUrbanAreaCommand() { }

    public static LiteralArgumentBuilder<ServerCommandSource> createCommand() {
        return literal("applyurbanarea")
            // 开关与节点共同控制命令树，关闭功能时聊天补全和可视化 GUI 都不会显示此入口。
            .requires(source -> ServerConfig.isSubmitUrbanArea()
                && PermissionService.hasCommandPermission(source, PermissionNodes.APPLY_URBAN_AREA, 0))
            .executes(context -> start(context.getSource(), false))
            .then(selectBranch(false))
            .then(literal("gui").executes(context -> start(context.getSource(), true)).then(selectBranch(true)));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> selectBranch(boolean visual) {
        return literal("select").then(argument("dimension", StringArgumentType.string())
            .then(argument("areaName", StringArgumentType.string()).executes(context -> apply(context, visual))));
    }

    private static int start(ServerCommandSource source, boolean visual) {
        if (!validate(source, null)) return 0;
        String dimension = currentDimension(source);
        try {
            List<JsonObject> available = new ArrayList<>();
            for (JsonObject area : namedAreas(readAreas(dimension))) {
                if (canUrbanize(source.getPlayer(), model(area))) available.add(area);
            }
            if (available.isEmpty()) {
                source.sendMessage(message(source, "commandui.common.no_areas"));
                return 0;
            }
            if (visual) {
                sendEditor(source.getPlayer(), "list", dimension, available);
            } else {
                source.sendMessage(message(source, "urban.message.select_area"));
                for (JsonObject area : available) {
                    String name = area.get("name").getAsString();
                    source.sendMessage(button(name, ROOT + " select " + quote(dimension) + " " + quote(name))
                        .append(Text.literal("  type: " + model(area).getType() + " → urban").formatted(Formatting.GRAY)));
                }
            }
            return 1;
        } catch (IOException | RuntimeException e) {
            return fileError(source, e);
        }
    }

    private static int apply(CommandContext<ServerCommandSource> context, boolean visual) {
        ServerCommandSource source = context.getSource();
        String dimension = StringArgumentType.getString(context, "dimension");
        String name = StringArgumentType.getString(context, "areaName");
        if (!validate(source, dimension)) return 0;
        try {
            // 点击时重新读取，确保旧按钮不能越过归属变化、重复城市化或维度变化检查。
            JsonArray document = readAreas(dimension);
            JsonObject target = findUnique(source, document, name);
            if (target == null) return 0;
            AreaData current = model(target);
            if (AreaData.URBAN_TYPE.equals(current.getType())) {
                source.sendError(message(source, "urban.error.already_urban", name));
                return 0;
            }
            if (!canUrbanize(source.getPlayer(), current)) {
                source.sendError(message(source, "message.error.permission"));
                return 0;
            }
            target.addProperty("type", AreaData.URBAN_TYPE);
            Path file = areaFile(dimension);
            boolean submitted = false;
            if (!ApprovalManager.isOperator(source.getPlayer())) {
                List<AreaData> proposed = new ArrayList<>();
                for (JsonElement element : document) proposed.add(model(element.getAsJsonObject()));
                submitted = ApprovalManager.submitAreasIfRequired(source.getPlayer(), "applyurbanarea", file, proposed);
                // 非管理员永远不能因配置状态或调用约定变化意外落入直接写入分支。
                if (!submitted) {
                    source.sendError(message(source, "urban.error.disabled"));
                    return 0;
                }
            } else {
                // 只更新选中对象的 type，保留该文件内其他属性以及未知扩展字段。
                ApprovalManager.writeDocument(file, document);
            }
            source.sendMessage(message(source, submitted ? "urban.message.submitted" : "urban.message.updated",
                name, AreaData.URBAN_TYPE));
            // 重载始终读取正式数据，普通玩家的临时申请不会因此提前生效。
            ServerCommands.reload(source);
            if (visual) sendEditor(source.getPlayer(), submitted ? "submitted" : "result", dimension, List.of(target));
            return 1;
        } catch (IOException | RuntimeException e) {
            return fileError(source, e);
        }
    }

    public static boolean canUrbanize(ServerPlayerEntity player, AreaData area) {
        return AreaManagementCapabilityService.canPerform(player, AreaManagementCapabilityService.APPLY_URBAN_AREA,
            area, List.of());
    }

    private static boolean validate(ServerCommandSource source, String expectedDimension) {
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.translatable("command.error.general_9"));
            return false;
        }
        if (!ServerConfig.isSubmitUrbanArea()) {
            source.sendError(message(source, "urban.error.disabled"));
            return false;
        }
        if (!PermissionService.hasCommandPermission(source, PermissionNodes.APPLY_URBAN_AREA, 0)) {
            source.sendError(message(source, "message.error.permission"));
            return false;
        }
        if (expectedDimension != null && !expectedDimension.equals(currentDimension(source))) {
            source.sendError(message(source, "urban.error.dimension_changed"));
            return false;
        }
        return true;
    }

    private static JsonArray readAreas(String dimension) throws IOException {
        Path file = areaFile(dimension);
        if (Files.notExists(file)) return new JsonArray();
        JsonElement document = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
        if (!document.isJsonArray()) throw new IOException("域名文件根节点必须是数组：" + file);
        return document.getAsJsonArray();
    }

    private static Path areaFile(String dimension) throws IOException {
        // 与现有服务端域名命令一致，维度到文件的映射固定，不能从客户端参数拼出任意路径。
        String fileName = switch (dimension) {
            case "minecraft:overworld" -> Packets.getFileNameForDimension(Packets.DIMENSION_OVERWORLD);
            case "minecraft:the_nether" -> Packets.getFileNameForDimension(Packets.DIMENSION_NETHER);
            case "minecraft:the_end" -> Packets.getFileNameForDimension(Packets.DIMENSION_END);
            default -> null;
        };
        if (fileName == null) throw new IOException("当前维度没有域名文件：" + dimension);
        return WorldFolderManager.getWorldDimensionFile(fileName);
    }

    private static List<JsonObject> namedAreas(JsonArray document) {
        List<JsonObject> result = new ArrayList<>();
        for (JsonElement element : document) {
            if (!element.isJsonObject()) continue;
            JsonObject area = element.getAsJsonObject();
            JsonElement name = area.get("name");
            if (name != null && name.isJsonPrimitive() && name.getAsJsonPrimitive().isString()
                && !name.getAsString().isBlank()) result.add(area);
        }
        return result;
    }

    private static JsonObject findUnique(ServerCommandSource source, JsonArray document, String name) {
        JsonObject found = null;
        for (JsonObject area : namedAreas(document)) {
            if (!name.equals(area.get("name").getAsString())) continue;
            if (found != null) {
                source.sendError(message(source, "urban.error.duplicate_name", name));
                return null;
            }
            found = area;
        }
        if (found == null) source.sendError(message(source, "urban.error.area_not_found", name));
        return found;
    }

    private static AreaData model(JsonObject area) {
        return ApprovalManager.GSON.fromJson(area, AreaData.class);
    }

    private static void sendEditor(ServerPlayerEntity player, String action, String dimension, List<JsonObject> areas) {
        NetworkBuf buffer = NetworkCompat.get().newBuffer();
        buffer.writeString(action);
        buffer.writeString(dimension);
        buffer.writeInt(areas.size());
        for (JsonObject area : areas) {
            buffer.writeString(area.get("name").getAsString());
            buffer.writeString(model(area).getType());
        }
        NetworkCompat.get().sendToPlayer(player, Channel.S2C_URBAN_AREA_EDITOR, buffer);
    }

    private static String currentDimension(ServerCommandSource source) {
        return source.getWorld().getRegistryKey().getValue().toString();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static MutableText button(String label, String command) {
        return Text.literal(label).formatted(Formatting.AQUA).styled(style -> style
            // 点击事件沿用已有版本分支，兼容记录类形式的新版 ClickEvent。
            //? if <=1.21.4 {
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + command))
            //?} else {
            /*.withClickEvent(new ClickEvent.RunCommand("/" + command))*/
            //?}
        );
    }

    private static Text message(ServerCommandSource source, String key, Object... args) {
        return Text.literal(ServerI18nManager.translateForPlayer(source.getPlayer().getUuid(), key, args));
    }

    private static int fileError(ServerCommandSource source, Exception e) {
        Areashint.LOGGER.error("提交或保存域名城市化失败", e);
        source.sendError(message(source, "urban.error.file"));
        return 0;
    }
}
