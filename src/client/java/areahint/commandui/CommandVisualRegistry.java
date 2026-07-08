package areahint.commandui;

import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 指令可视化注册表，后续单个指令只替换对应 handler 即可扩展图形流程。
 */
public final class CommandVisualRegistry {
    private static final String CATEGORY_GENERAL = "commandui.category.general";
    private static final String CATEGORY_DISPLAY = "commandui.category.display";
    private static final String CATEGORY_AREA_EDIT = "commandui.category.area_edit";
    private static final String CATEGORY_GEOMETRY = "commandui.category.geometry";
    private static final String CATEGORY_TELEPORT = "commandui.category.teleport";
    private static final String CATEGORY_TEXT = "commandui.category.text";
    private static final String CATEGORY_DIMENSION = "commandui.category.dimension";
    private static final String CATEGORY_SYSTEM = "commandui.category.system";
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
        handlers.add(visual("help", "areahint help", "help.command.help", CATEGORY_GENERAL,
            HelpVisualController::openFromCommandUi));
        handlers.add(visual("boundviz", "areahint boundviz", "help.command.boundviz", CATEGORY_DISPLAY,
            areahint.boundviz.BoundVizVisualController::openFromCommandUi));
        handlers.add(visual("language", "areahint language", "help.command.language", CATEGORY_GENERAL,
            areahint.language.LanguageVisualController::openFromCommandUi));
        handlers.add(visual("on", "areahint on", "command.usage.on", CATEGORY_GENERAL,
            areahint.command.OnVisualController::openFromCommandUi));
        handlers.add(visual("off", "areahint off", "command.usage.off", CATEGORY_GENERAL,
            areahint.command.OffVisualController::openFromCommandUi));
        handlers.add(visual("reload", "areahint reload", "help.command.reload", CATEGORY_SYSTEM,
            areahint.command.ReloadVisualController::openFromCommandUi));
        handlers.add(visual("delete", "areahint delete", "help.command.delete", CATEGORY_AREA_EDIT,
            areahint.delete.DeleteVisualController::openFromCommandUi));
        handlers.add(visual("frequency", "areahint frequency", "help.command.frequency", CATEGORY_DISPLAY,
            areahint.frequency.FrequencyVisualController::openFromCommandUi));
        handlers.add(visual("hintrender", "areahint hintrender", "help.command.hintrender", CATEGORY_DISPLAY,
            areahint.render.HintRenderVisualController::openFromCommandUi));
        handlers.add(visual("titlestyle", "areahint titlestyle", "help.command.titlestyle", CATEGORY_DISPLAY,
            areahint.titlestyle.TitleStyleVisualController::openFromCommandUi));
        handlers.add(visual("titlesize", "areahint titlesize", "help.command.titlesize", CATEGORY_DISPLAY,
            areahint.titlesize.TitleSizeVisualController::openFromCommandUi));
        handlers.add(visual("addsubtitle", "areahint addsubtitle", "help.command.addsubtitle", CATEGORY_TEXT,
            areahint.subtitle.AddSubtitleVisualController::openFromCommandUi));
        handlers.add(visual("replacesubtitle", "areahint replacesubtitle", "help.command.replacesubtitle", CATEGORY_TEXT,
            areahint.subtitle.ReplaceSubtitleVisualController::openFromCommandUi));
        handlers.add(visual("deletesubtitle", "areahint deletesubtitle", "help.command.deletesubtitle", CATEGORY_TEXT,
            areahint.subtitle.DeleteSubtitleVisualController::openFromCommandUi));
        handlers.add(visual("replacesubtitlecolor", "areahint replacesubtitlecolor", "help.command.replacesubtitlecolor", CATEGORY_TEXT,
            areahint.subtitle.ReplaceSubtitleColorVisualController::openFromCommandUi));
        handlers.add(visual("replacesubtitlesize", "areahint replacesubtitlesize", "help.command.replacesubtitlesize", CATEGORY_TEXT,
            areahint.subtitle.ReplaceSubtitleSizeVisualController::openFromCommandUi));
        handlers.add(visual("add", "areahint add", "help.command.add", CATEGORY_AREA_EDIT,
            CommandVisualController::openAddJson));
        handlers.add(visual("easyadd", "areahint easyadd", "help.command.easyadd", CATEGORY_AREA_EDIT,
            parent -> areahint.easyadd.EasyAddVisualController.openFromCommandUi(parent, "areahint easyadd")));
        handlers.add(visual("addarea", "areahint addarea", "help.command.addarea", CATEGORY_AREA_EDIT,
            parent -> areahint.easyadd.EasyAddVisualController.openFromCommandUi(parent, "areahint addarea")));
        handlers.add(visual("recolor", "areahint recolor", "help.command.recolor", CATEGORY_AREA_EDIT,
            areahint.recolor.RecolorVisualController::openFromCommandUi));
        handlers.add(visual("rename", "areahint rename", "help.command.rename", CATEGORY_AREA_EDIT,
            areahint.rename.RenameVisualController::openFromCommandUi));
        handlers.add(visual("sethigh", "areahint sethigh", "help.command.sethigh", CATEGORY_AREA_EDIT,
            areahint.command.SetHighVisualController::openFromCommandUi));
        handlers.add(visual("tcp", "areahint tcp", "help.command.tcp", CATEGORY_TELEPORT,
            areahint.teleport.TcpVisualController::openFromCommandUi));
        handlers.add(visual("udp", "areahint udp", "help.command.udp", CATEGORY_TELEPORT,
            areahint.teleport.UdpVisualController::openFromCommandUi));
        handlers.add(visual("settp", "areahint settp", "help.command.settp", CATEGORY_TELEPORT,
            areahint.command.SetTpVisualController::openFromCommandUi));
        handlers.add(visual("replacebutton", "areahint replacebutton", "help.command.replacebutton", CATEGORY_GENERAL,
            areahint.replacebutton.ReplaceButtonVisualController::openFromCommandUi));
        handlers.add(visual("check", "areahint check", "help.command.check", CATEGORY_GENERAL,
            areahint.check.CheckVisualController::openFromCommandUi));
        handlers.add(visual("dimensionalityname", "areahint dimensionalityname", "help.command.dimensionalityname", CATEGORY_DIMENSION,
            areahint.dimensional.DimensionalityNameVisualController::openFromCommandUi));
        handlers.add(visual("dimensionalitycolor", "areahint dimensionalitycolor", "help.command.dimensionalitycolor", CATEGORY_DIMENSION,
            areahint.dimensional.DimensionalityColorVisualController::openFromCommandUi));
        handlers.add(visual("expandarea", "areahint expandarea", "help.command.expandarea", CATEGORY_GEOMETRY,
            areahint.expandarea.ExpandAreaVisualController::openFromCommandUi));
        handlers.add(visual("shrinkarea", "areahint shrinkarea", "help.command.shrinkarea", CATEGORY_GEOMETRY,
            areahint.shrinkarea.ShrinkAreaVisualController::openFromCommandUi));
        handlers.add(visual("dividearea", "areahint dividearea", "help.command.dividearea", CATEGORY_GEOMETRY,
            areahint.dividearea.DivideAreaVisualController::openFromCommandUi));
        handlers.add(visual("addhint", "areahint addhint", "help.command.addhint", CATEGORY_GEOMETRY,
            areahint.addhint.AddHintVisualController::openFromCommandUi));
        handlers.add(visual("deletehint", "areahint deletehint", "help.command.deletehint", CATEGORY_GEOMETRY,
            areahint.deletehint.DeleteHintVisualController::openFromCommandUi));
        handlers.add(visual("firstdimname", "areahint firstdimname", "help.command.firstdimname", CATEGORY_DIMENSION,
            areahint.dimensional.FirstDimNameVisualController::openFromCommandUi));
        handlers.add(visual("firstdimname_skip", "areahint firstdimname_skip", "help.command.firstdimname_skip", CATEGORY_DIMENSION,
            areahint.dimensional.FirstDimNameSkipVisualController::openFromCommandUi));
        handlers.add(visual("debug", "areahint debug", "help.command.debug", CATEGORY_SYSTEM,
            areahint.debug.DebugVisualController::openFromCommandUi));
        handlers.add(visual("adddescription", "areahint adddescription", "help.command.adddescription", CATEGORY_TEXT,
            areahint.description.AddDescriptionVisualController::openFromCommandUi));
        handlers.add(visual("replacedescription", "areahint replacedescription", "help.command.replacedescription", CATEGORY_TEXT,
            areahint.description.ReplaceDescriptionVisualController::openFromCommandUi));
        handlers.add(visual("deletedescription", "areahint deletedescription", "help.command.deletedescription", CATEGORY_TEXT,
            areahint.description.DeleteDescriptionVisualController::openFromCommandUi));
        handlers.add(visual("adddimensionalitydescription", "areahint adddimensionalitydescription", "help.command.adddimensionalitydescription", CATEGORY_DIMENSION,
            areahint.description.AddDimensionalityDescriptionVisualController::openFromCommandUi));
        handlers.add(visual("replacedimensionalitydescription", "areahint replacedimensionalitydescription", "help.command.replacedimensionalitydescription", CATEGORY_DIMENSION,
            areahint.description.ReplaceDimensionalityDescriptionVisualController::openFromCommandUi));
        handlers.add(visual("deletedimensionalitydescription", "areahint deletedimensionalitydescription", "help.command.deletedimensionalitydescription", CATEGORY_DIMENSION,
            areahint.description.DeleteDimensionalityDescriptionVisualController::openFromCommandUi));
        handlers.add(visual("addsignature", "areahint addsignature", "help.command.addsignature", CATEGORY_TEXT,
            areahint.signature.AddSignatureVisualController::openFromCommandUi));
        handlers.add(visual("deletesignature", "areahint deletesignature", "help.command.deletesignature", CATEGORY_TEXT,
            areahint.signature.DeleteSignatureVisualController::openFromCommandUi));
        handlers.add(visual("serverlanguage", "areahint serverlanguage", "help.command.serverlanguage", CATEGORY_SYSTEM,
            areahint.language.ServerLanguageVisualController::openFromCommandUi));
        return Collections.unmodifiableList(handlers);
    }

    private static CommandVisualHandler visualCommand(String id, String defaultCommand, String descriptionKey) {
        return visual(id, defaultCommand, descriptionKey, CATEGORY_GENERAL,
            parent -> CommandVisualController.openConfirmCommand(parent, id, defaultCommand));
    }

    private static CommandVisualHandler visual(String id, String defaultCommand, String descriptionKey,
                                               String categoryKey, Consumer<Screen> opener) {
        return new SimpleHandler(id, defaultCommand, descriptionKey, categoryKey, true, opener);
    }

    private record SimpleHandler(String id, String defaultCommand, String descriptionKey, String categoryKey,
                                 boolean hasVisualFlow,
                                 Consumer<Screen> opener) implements CommandVisualHandler {
        @Override
        public void open(Screen parent) {
            opener.accept(parent);
        }
    }
}
