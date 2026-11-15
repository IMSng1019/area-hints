package areahint.approval;

import areahint.network.ServerNetworking;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

// 特殊分支附着在原 tcp/addhint/deletehint 指令下，沿用原命令权限且额外校验申请归属。
public final class ApprovalCommands {
    private ApprovalCommands() { }

    public static LiteralArgumentBuilder<CommandSourceStack> branch(String operation) {
        return literal("approval").then(argument("requestId", StringArgumentType.word())
            .executes(context -> execute(context.getSource(), operation, StringArgumentType.getString(context, "requestId"), 0))
            .then(argument("areaIndex", IntegerArgumentType.integer(0))
                .executes(context -> execute(context.getSource(), operation, StringArgumentType.getString(context, "requestId"),
                    IntegerArgumentType.getInteger(context, "areaIndex")))));
    }

    private static int execute(CommandSourceStack source, String operation, String id, int index) {
        if (!source.isPlayer()) {
            source.sendFailure(Component.translatable("command.error.general_10"));
            return 0;
        }
        var player = source.getPlayer();
        try {
            if ("tcp".equals(operation)) {
                return areahint.teleport.TeleportService.handleApprovalTeleport(player, id, index) ? 1 : 0;
            }
            ApprovalManager.previewArea(player, id, index, true);
            ServerNetworking.sendCommandToClient(player, "areahint:approval_" + operation + ":" + id + ":" + index);
            return 1;
        } catch (ApprovalManager.ApprovalException e) {
            source.sendFailure(Component.translatable(e.getMessage()));
            return 0;
        }
    }
}
