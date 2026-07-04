package areahint.commandui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 指令可视化注册表，后续单个指令只替换对应 handler 即可扩展图形流程。
 */
public final class CommandVisualRegistry {
    private static final List<CommandVisualHandler> HANDLERS = createHandlers();

    private CommandVisualRegistry() {
    }

    public static List<CommandVisualHandler> getHandlers() {
        return HANDLERS;
    }

    public static CommandVisualHandler getById(String id) {
        for (CommandVisualHandler handler : HANDLERS) {
            if (handler.id().equals(id)) {
                return handler;
            }
        }
        return null;
    }

    private static List<CommandVisualHandler> createHandlers() {
        List<CommandVisualHandler> handlers = new ArrayList<>();
        handlers.add(visual("help", "areahint help", "help.command.help", HelpVisualController::openFromCommandUi));
        handlers.add(visual("boundviz", "areahint boundviz", "help.command.boundviz",
            areahint.boundviz.BoundVizVisualController::openFromCommandUi));
        handlers.add(visual("language", "areahint language", "help.command.language",
            areahint.language.LanguageVisualController::openFromCommandUi));
        handlers.add(visual("on", "areahint on", "command.usage.on",
            areahint.command.OnVisualController::openFromCommandUi));
        handlers.add(visual("off", "areahint off", "command.usage.off",
            areahint.command.OffVisualController::openFromCommandUi));
        handlers.add(visual("reload", "areahint reload", "help.command.reload",
            areahint.command.ReloadVisualController::openFromCommandUi));
        handlers.add(visual("delete", "areahint delete", "help.command.delete",
            areahint.delete.DeleteVisualController::openFromCommandUi));
        handlers.add(visual("frequency", "areahint frequency", "help.command.frequency",
            areahint.frequency.FrequencyVisualController::openFromCommandUi));
        handlers.add(visual("hintrender", "areahint hintrender", "help.command.hintrender",
            areahint.render.HintRenderVisualController::openFromCommandUi));
        handlers.add(visual("titlestyle", "areahint titlestyle", "help.command.titlestyle", CommandVisualController::openTitleStyle));
        handlers.add(visual("titlesize", "areahint titlesize", "help.command.titlesize", CommandVisualController::openTitleSize));
        handlers.add(visual("addsubtitle", "areahint addsubtitle", "help.command.addsubtitle",
            areahint.subtitle.AddSubtitleVisualController::openFromCommandUi));
        handlers.add(visual("replacesubtitle", "areahint replacesubtitle", "help.command.replacesubtitle",
            areahint.subtitle.ReplaceSubtitleVisualController::openFromCommandUi));
        handlers.add(visual("deletesubtitle", "areahint deletesubtitle", "help.command.deletesubtitle",
            areahint.subtitle.DeleteSubtitleVisualController::openFromCommandUi));
        handlers.add(visual("replacesubtitlecolor", "areahint replacesubtitlecolor", "help.command.replacesubtitlecolor",
            areahint.subtitle.ReplaceSubtitleColorVisualController::openFromCommandUi));
        handlers.add(visual("replacesubtitlesize", "areahint replacesubtitlesize", "help.command.replacesubtitlesize",
            areahint.subtitle.ReplaceSubtitleSizeVisualController::openFromCommandUi));
        handlers.add(visual("add", "areahint add", "help.command.add", CommandVisualController::openAddJson));
        handlers.add(visual("easyadd", "areahint easyadd", "help.command.easyadd",
            parent -> areahint.easyadd.EasyAddVisualController.openFromCommandUi(parent, "areahint easyadd")));
        handlers.add(visual("addarea", "areahint addarea", "help.command.addarea",
            parent -> areahint.easyadd.EasyAddVisualController.openFromCommandUi(parent, "areahint addarea")));
        handlers.add(visual("recolor", "areahint recolor", "help.command.recolor",
            areahint.recolor.RecolorVisualController::openFromCommandUi));
        handlers.add(visual("rename", "areahint rename", "help.command.rename",
            areahint.rename.RenameVisualController::openFromCommandUi));
        handlers.add(visual("sethigh", "areahint sethigh", "help.command.sethigh",
            areahint.command.SetHighVisualController::openFromCommandUi));
        handlers.add(visual("tcp", "areahint tcp", "help.command.tcp",
            areahint.teleport.TcpVisualController::openFromCommandUi));
        handlers.add(visual("udp", "areahint udp", "help.command.udp", parent -> CommandVisualController.openTeleport(parent, "udp")));
        handlers.add(visual("settp", "areahint settp", "help.command.settp",
            areahint.command.SetTpVisualController::openFromCommandUi));
        handlers.add(visual("replacebutton", "areahint replacebutton", "help.command.replacebutton",
            areahint.replacebutton.ReplaceButtonVisualController::openFromCommandUi));
        handlers.add(visual("check", "areahint check", "help.command.check",
            areahint.check.CheckVisualController::openFromCommandUi));
        handlers.add(visual("dimensionalityname", "areahint dimensionalityname", "help.command.dimensionalityname",
            areahint.dimensional.DimensionalityNameVisualController::openFromCommandUi));
        handlers.add(visual("dimensionalitycolor", "areahint dimensionalitycolor", "help.command.dimensionalitycolor",
            areahint.dimensional.DimensionalityColorVisualController::openFromCommandUi));
        handlers.add(visual("expandarea", "areahint expandarea", "help.command.expandarea",
            areahint.expandarea.ExpandAreaVisualController::openFromCommandUi));
        handlers.add(visual("shrinkarea", "areahint shrinkarea", "help.command.shrinkarea",
            areahint.shrinkarea.ShrinkAreaVisualController::openFromCommandUi));
        handlers.add(visual("dividearea", "areahint dividearea", "help.command.dividearea",
            areahint.dividearea.DivideAreaVisualController::openFromCommandUi));
        handlers.add(visual("addhint", "areahint addhint", "help.command.addhint",
            areahint.addhint.AddHintVisualController::openFromCommandUi));
        handlers.add(visual("deletehint", "areahint deletehint", "help.command.deletehint",
            areahint.deletehint.DeleteHintVisualController::openFromCommandUi));
        handlers.add(visual("firstdimname", "areahint firstdimname", "help.command.firstdimname",
            areahint.dimensional.FirstDimNameVisualController::openFromCommandUi));
        handlers.add(visual("firstdimname_skip", "areahint firstdimname_skip", "help.command.firstdimname_skip",
            areahint.dimensional.FirstDimNameSkipVisualController::openFromCommandUi));
        handlers.add(visual("debug", "areahint debug", "help.command.debug",
            areahint.debug.DebugVisualController::openFromCommandUi));
        handlers.add(visual("adddescription", "areahint adddescription", "help.command.adddescription",
            areahint.description.AddDescriptionVisualController::openFromCommandUi));
        handlers.add(visual("replacedescription", "areahint replacedescription", "help.command.replacedescription",
            areahint.description.ReplaceDescriptionVisualController::openFromCommandUi));
        handlers.add(visual("deletedescription", "areahint deletedescription", "help.command.deletedescription",
            areahint.description.DeleteDescriptionVisualController::openFromCommandUi));
        handlers.add(visual("adddimensionalitydescription", "areahint adddimensionalitydescription", "help.command.adddimensionalitydescription",
            areahint.description.AddDimensionalityDescriptionVisualController::openFromCommandUi));
        handlers.add(visual("replacedimensionalitydescription", "areahint replacedimensionalitydescription", "help.command.replacedimensionalitydescription",
            areahint.description.ReplaceDimensionalityDescriptionVisualController::openFromCommandUi));
        handlers.add(visual("deletedimensionalitydescription", "areahint deletedimensionalitydescription", "help.command.deletedimensionalitydescription",
            areahint.description.DeleteDimensionalityDescriptionVisualController::openFromCommandUi));
        handlers.add(visual("addsignature", "areahint addsignature", "help.command.addsignature",
            areahint.signature.AddSignatureVisualController::openFromCommandUi));
        handlers.add(visual("deletesignature", "areahint deletesignature", "help.command.deletesignature",
            areahint.signature.DeleteSignatureVisualController::openFromCommandUi));
        handlers.add(visual("serverlanguage", "areahint serverlanguage", "help.command.serverlanguage",
            areahint.language.ServerLanguageVisualController::openFromCommandUi));
        return Collections.unmodifiableList(handlers);
    }

    private static CommandVisualHandler visualCommand(String id, String defaultCommand, String descriptionKey) {
        return visual(id, defaultCommand, descriptionKey,
            parent -> CommandVisualController.openConfirmCommand(parent, id, defaultCommand));
    }

    private static CommandVisualHandler visual(String id, String defaultCommand, String descriptionKey, Consumer<Screen> opener) {
        return new SimpleHandler(id, defaultCommand, descriptionKey, true, opener);
    }

    private record SimpleHandler(String id, String defaultCommand, String descriptionKey, boolean hasVisualFlow,
                                 Consumer<Screen> opener) implements CommandVisualHandler {
        @Override
        public void open(Screen parent) {
            opener.accept(parent);
        }
    }
}
