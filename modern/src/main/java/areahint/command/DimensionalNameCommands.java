package areahint.command;

import areahint.Areashint;
import areahint.approval.ApprovalManager;
import areahint.data.DimensionalNameData;
import areahint.dimensional.DimensionalNameManager;
import areahint.network.DimensionalNameNetworking;
import areahint.network.ServerNetworking;
import areahint.util.ColorUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import areahint.i18n.ServerI18nManager;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 维度域名命令处理器
 * 处理 /areahint dimensionalityname 和 /areahint dimensionalitycolor 命令
 */
public class DimensionalNameCommands {
    private static final Gson GSON = new Gson();

    // ===== dimensionalityname 交互式命令 =====

    public static int executeStart(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) {
            source.sendSystemMessage(Component.translatable("command.error.general_9"));
            return 0;
        }
        // 确保所有服务器维度都已注册
        syncServerDimensions(source.getServer());
        // 先将最新维度列表发送给该玩家
        DimensionalNameNetworking.sendDimensionalNamesToClient(source.getPlayer());
        sendClientCommand(source, "areahint:dimname_start");
        return Command.SINGLE_SUCCESS;
    }

    public static int executeSelect(CommandContext<CommandSourceStack> context, String dimensionId) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) {
            source.sendSystemMessage(Component.translatable("command.error.general_9"));
            return 0;
        }
        // 去除引号
        if (dimensionId.startsWith("\"") && dimensionId.endsWith("\"") && dimensionId.length() > 1) {
            dimensionId = dimensionId.substring(1, dimensionId.length() - 1);
        }
        sendClientCommand(source, "areahint:dimname_select:" + dimensionId);
        return Command.SINGLE_SUCCESS;
    }

    public static int executeName(CommandContext<CommandSourceStack> context, String newName) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) {
            source.sendSystemMessage(Component.translatable("command.error.general_9"));
            return 0;
        }

        if (newName == null || newName.trim().isEmpty()) {
            source.sendFailure(Component.translatable("command.message.dimension.name_2"));
            return 0;
        }
        final String finalNewName = newName.trim();
        if (finalNewName.length() > 50) {
            source.sendFailure(Component.translatable("command.message.dimension.name_3"));
            return 0;
        }

        sendClientCommand(source, "areahint:dimname_name:" + finalNewName);
        return Command.SINGLE_SUCCESS;
    }

    public static int executeConfirm(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) {
            source.sendSystemMessage(Component.translatable("command.error.general_9"));
            return 0;
        }
        sendClientCommand(source, "areahint:dimname_confirm");
        return Command.SINGLE_SUCCESS;
    }

    public static int executeCancel(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) {
            source.sendSystemMessage(Component.translatable("command.error.general_9"));
            return 0;
        }
        sendClientCommand(source, "areahint:dimname_cancel");
        return Command.SINGLE_SUCCESS;
    }

    // ===== dimensionalitycolor 交互式命令 =====

    public static int executeColorStart(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) {
            source.sendSystemMessage(Component.translatable("command.error.general_9"));
            return 0;
        }
        syncServerDimensions(source.getServer());
        DimensionalNameNetworking.sendDimensionalNamesToClient(source.getPlayer());
        sendClientCommand(source, "areahint:dimcolor_start");
        return Command.SINGLE_SUCCESS;
    }

    public static int executeColorSelect(CommandContext<CommandSourceStack> context, String dimensionId) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) {
            source.sendSystemMessage(Component.translatable("command.error.general_9"));
            return 0;
        }
        if (dimensionId.startsWith("\"") && dimensionId.endsWith("\"") && dimensionId.length() > 1) {
            dimensionId = dimensionId.substring(1, dimensionId.length() - 1);
        }
        sendClientCommand(source, "areahint:dimcolor_select:" + dimensionId);
        return Command.SINGLE_SUCCESS;
    }

    public static int executeColorColor(CommandContext<CommandSourceStack> context, String colorValue) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) {
            source.sendSystemMessage(Component.translatable("command.error.general_9"));
            return 0;
        }
        sendClientCommand(source, "areahint:dimcolor_color:" + colorValue);
        return Command.SINGLE_SUCCESS;
    }

    public static int executeColorConfirm(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) {
            source.sendSystemMessage(Component.translatable("command.error.general_9"));
            return 0;
        }
        sendClientCommand(source, "areahint:dimcolor_confirm");
        return Command.SINGLE_SUCCESS;
    }

    public static int executeColorCancel(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) {
            source.sendSystemMessage(Component.translatable("command.error.general_9"));
            return 0;
        }
        sendClientCommand(source, "areahint:dimcolor_cancel");
        return Command.SINGLE_SUCCESS;
    }

    // ===== 服务端处理方法（由客户端Manager调用） =====

    /**
     * 服务端处理维度域名更改
     */
    public static void handleDimNameChange(CommandSourceStack source, String dimensionId, String newName) {
        try {
            String oldName = DimensionalNameManager.getDimensionalName(dimensionId);
            // 静态维度名称映射本身会参与权限与同步，必须在修改映射之前提交审批。
            if (submitDimensionalChangeForApproval(source, "dimensionalityname", dimensionId, newName, null)) {
                return;
            }
            DimensionalNameManager.setDimensionalName(dimensionId, newName, source.getTextName());

            if (DimensionalNameManager.saveDimensionalNames()) {
                source.sendSuccess(() -> Component.translatable("command.message.area.dimension_2"), false);
                source.sendSuccess(() -> Component.translatable("command.message.dimension").append(Component.literal(dimensionId)), false);
                source.sendSuccess(() -> Component.translatable("command.message.name_3").append(Component.literal(oldName)), false);
                source.sendSuccess(() -> Component.translatable("command.message.name_2").append(Component.literal(newName)), false);

                // 广播到所有客户端
                DimensionalNameNetworking.sendDimensionalNamesToAllClients(source.getServer());
                // 通知客户端reload
                ServerNetworking.sendCommandToAllClients(source.getServer(), "areahint:reload");

                Areashint.LOGGER.info(ServerI18nManager.translate("command.message.dimension.name"),
                    source.getTextName(), dimensionId, oldName, newName);
            } else {
                source.sendFailure(Component.translatable("command.error.area.dimension.save"));
            }
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.error.dimension.name_2").append(Component.literal(e.getMessage())));
            Areashint.LOGGER.error(ServerI18nManager.translate("command.error.dimension.name"), e);
        }
    }

    /**
     * 服务端处理维度域名颜色更改
     */
    public static void handleDimColorChange(CommandSourceStack source, String dimensionId, String newColor) {
        try {
            String oldColor = DimensionalNameManager.getDimensionalColor(dimensionId);
            String oldColorDisplay = oldColor != null ? oldColor : ServerI18nManager.translate("command.message.general_6");
            // 只为申请构造候选集合，未审批的颜色不得提前写入服务端缓存。
            if (submitDimensionalChangeForApproval(source, "dimensionalitycolor", dimensionId, null, newColor)) {
                return;
            }
            DimensionalNameManager.setDimensionalColor(dimensionId, newColor);

            if (DimensionalNameManager.saveDimensionalNames()) {
                source.sendSuccess(() -> Component.translatable("command.message.area.color.dimension"), false);
                source.sendSuccess(() -> Component.translatable("command.message.dimension").append(Component.literal(dimensionId)), false);
                source.sendSuccess(() -> Component.translatable("command.message.color_5").append(Component.literal(oldColorDisplay)), false);
                source.sendSuccess(() -> Component.translatable("command.message.color_6").append(Component.literal(newColor)), false);

                DimensionalNameNetworking.sendDimensionalNamesToAllClients(source.getServer());
                ServerNetworking.sendCommandToAllClients(source.getServer(), "areahint:reload");

                Areashint.LOGGER.info(ServerI18nManager.translate("command.message.color.dimension"),
                    source.getTextName(), dimensionId, oldColorDisplay, newColor);
            } else {
                source.sendFailure(Component.translatable("command.error.area.dimension.save"));
            }
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.error.color.dimension_2").append(Component.literal(e.getMessage())));
            Areashint.LOGGER.error(ServerI18nManager.translate("command.error.color.dimension"), e);
        }
    }

    // 维度配置包含多个维度，复制现有映射后仅替换目标字段以保留其他维度及已有签名。
    private static boolean submitDimensionalChangeForApproval(CommandSourceStack source, String operation,
                                                             String dimensionId, String newName, String newColor) {
        if (!source.isPlayer() || !ApprovalManager.requiresApproval(source.getPlayer())) {
            return false;
        }
        Map<String, String> names = DimensionalNameManager.getAllDimensionalNames();
        Map<String, String> colors = DimensionalNameManager.getAllDimensionalColors();
        Map<String, String> signatures = DimensionalNameManager.getAllDimensionalSignatures();
        if ("dimensionalityname".equals(operation)) {
            names.put(dimensionId, newName);
            signatures.putIfAbsent(dimensionId, source.getTextName().trim());
        } else {
            colors.put(dimensionId, newColor);
        }
        JsonArray proposed = new JsonArray();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            proposed.add(GSON.toJsonTree(new DimensionalNameData(entry.getKey(), entry.getValue(),
                colors.get(entry.getKey()), signatures.get(entry.getKey()))));
        }
        try {
            if (!ApprovalManager.submitDocumentIfRequired(source.getPlayer(), operation,
                areahint.world.WorldFolderManager.getWorldDimensionalNamesFile(), proposed, dimensionId, List.of())) {
                return false;
            }
            source.sendSystemMessage(Component.translatable("approval.message.submitted"));
        } catch (java.io.IOException e) {
            Areashint.LOGGER.error("提交维度域名审批失败", e);
            source.sendFailure(Component.translatable("approval.error.storage"));
        }
        return true;
    }

    /**
     * 获取服务器所有维度ID
     */
    public static Set<String> getServerDimensions(CommandSourceStack source) {
        Set<String> dims = source.getServer()
            .levelKeys()
            .stream()
            .map(key -> key.identifier().toString())
            .collect(Collectors.toSet());
        dims.add("minecraft:overworld");
        dims.add("minecraft:the_nether");
        dims.add("minecraft:the_end");
        return dims;
    }

    /**
     * 将服务器所有维度同步到管理器中（确保未配置的维度也能显示）
     */
    public static void syncServerDimensions(MinecraftServer server) {
        Set<String> serverDims = server.levelKeys()
            .stream()
            .map(key -> key.identifier().toString())
            .collect(Collectors.toSet());
        for (String dimId : serverDims) {
            if (!DimensionalNameManager.hasDimensionalName(dimId)) {
                // 未配置的维度使用维度ID作为默认名称
                DimensionalNameManager.setDimensionalName(dimId, dimId);
            }
        }
        DimensionalNameManager.saveDimensionalNames();
    }

    // ===== 首次维度命名（权限由指令树和网络接收器统一校验） =====

    public static int executeFirstDimName(CommandContext<CommandSourceStack> context, String name) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) return 0;

        String dimId = areahint.util.ServerPlayerCompat.world(source.getPlayer()).dimension().identifier().toString();
        String currentName = DimensionalNameManager.getDimensionalName(dimId);

        // 仅当维度名称等于维度ID时（未被命名）才允许
        if (!currentName.equals(dimId)) {
            source.sendSystemMessage(Component.translatable("command.error.dimension_4").append(Component.literal(currentName)));
            return 0;
        }

        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 50) {
            source.sendFailure(Component.translatable("command.error.name"));
            return 0;
        }

        handleDimNameChange(source, dimId, trimmed);
        return Command.SINGLE_SUCCESS;
    }

    public static int executeFirstDimNameSkip(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) return 0;

        String dimId = areahint.util.ServerPlayerCompat.world(source.getPlayer()).dimension().identifier().toString();
        String currentName = DimensionalNameManager.getDimensionalName(dimId);

        if (currentName.equals(dimId)) {
            // 使用维度路径作为默认名称（如 overworld, the_nether）
            String defaultName = areahint.util.ServerPlayerCompat.world(source.getPlayer()).dimension().identifier().getPath();
            handleDimNameChange(source, dimId, defaultName);
            // 需要审批时处理方法已发送申请结果，避免再提示名称已经正式生效。
            if (!ApprovalManager.requiresApproval(source.getPlayer())) {
                source.sendSystemMessage(Component.translatable("command.message.name").append(Component.literal(defaultName)));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void sendClientCommand(CommandSourceStack source, String command) {
        try {
            if (source.getPlayer() != null) {
                ServerNetworking.sendCommandToClient(source.getPlayer(), command);
            }
        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("command.message.general_17"), e);
        }
    }
}
