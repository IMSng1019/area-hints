package areahint.management.client;

import areahint.commandui.CommandVisualHandler;
import areahint.commandui.CommandVisualLaunchContext;
import areahint.commandui.CommandVisualRegistry;
import areahint.gui.CompatEntryListWidget;
import areahint.i18n.I18nManager;
import areahint.xaero.AreaOverlayRepository.OverlayArea;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 只展示服务端已授权操作的域名管理面板。
 */
final class AreaManagementScreen extends Screen {
    private static final Map<String, List<String>> GROUPS = createGroups();

    private final Screen parent;
    private final OverlayArea area;
    private final String dimensionId;
    private final long requestId;
    private final Set<String> allowed;
    private ManagementList list;

    AreaManagementScreen(Screen parent, OverlayArea area, String dimensionId,
                         long requestId, List<String> allowedOperations) {
        super(Component.literal(I18nManager.translate("xaero.areahint.management.title", area.displayName())));
        this.parent = parent;
        this.area = area;
        this.dimensionId = dimensionId;
        this.requestId = requestId;
        this.allowed = Set.copyOf(allowedOperations == null ? List.of() : allowedOperations);
    }

    @Override
    protected void init() {
        this.list = new ManagementList(this.minecraft, this.width, this.height, 48, this.height - 34);
        this.addRenderableWidget(this.list);
        for (Map.Entry<String, List<String>> group : GROUPS.entrySet()) {
            List<String> visible = group.getValue().stream().filter(allowed::contains).toList();
            if (visible.isEmpty()) {
                continue;
            }
            this.list.addGroup(group.getKey());
            for (String operation : visible) {
                this.list.addOperation(operation);
            }
        }
        int width = 90;
        this.addRenderableWidget(Button.builder(Component.literal(I18nManager.translate("commandui.button.close")),
            button -> onClose()).bounds((this.width - width) / 2, this.height - 28, width, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // 1.20.2 起 renderBackground 改为 4 参、1 参重载被移除，故按版本分支
        this.extractBackground(context, mouseX, mouseY, delta);
        /*this.extractBackground(context);*/
        context.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
        context.centeredText(this.font,
            Component.literal(I18nManager.translate("xaero.areahint.management.area", area.name(), area.level())),
            this.width / 2, 28, 0xFFAAAAAA);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        CommandVisualLaunchContext.clear();
        if (this.minecraft != null) {
            areahint.util.ClientScreenCompat.setScreen(this.minecraft, parent);
        }
    }

    private void launch(String operation) {
        CommandVisualHandler handler = CommandVisualRegistry.getById(operation);
        if (handler == null) {
            return;
        }
        if (CommandVisualLaunchContext.requiresAreaTarget(operation)) {
            CommandVisualLaunchContext.begin(operation, area.name(), dimensionId, requestId);
        } else {
            CommandVisualLaunchContext.clear();
        }
        handler.open(this);
    }

    private static Map<String, List<String>> createGroups() {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        groups.put("xaero.areahint.group.teleport", List.of("tcp", "udp"));
        groups.put("xaero.areahint.group.properties", List.of("rename", "recolor", "applyurbanarea", "sethigh"));
        groups.put("xaero.areahint.group.text", List.of("adddescription", "replacedescription", "deletedescription",
            "addsubtitle", "replacesubtitle", "deletesubtitle", "replacesubtitlecolor", "replacesubtitlesize",
            "addsignature", "deletesignature"));
        groups.put("xaero.areahint.group.geometry", List.of("expandarea", "shrinkarea", "dividearea", "addhint", "deletehint"));
        groups.put("xaero.areahint.group.danger", List.of("delete"));
        return groups;
    }

    private final class ManagementList extends CompatEntryListWidget<ManagementList.Entry> {
        private ManagementList(Minecraft client, int width, int height, int top, int bottom) {
            super(client, width, bottom - top, top, 26);
            // 1.20.5 起列表控件不再自绘背景、setRenderBackground 被移除，故只在 1.20.4 及以前调用
            /*this.setRenderBackground(false);
            */
        }

        private void addGroup(String key) {
            this.addEntry(new GroupEntry(key));
        }

        private void addOperation(String operation) {
            this.addEntry(new OperationEntry(operation));
        }

        @Override
        public int getRowWidth() {
            return Math.min(500, AreaManagementScreen.this.width - 36);
        }

        @Override
        // 1.20.5 起 getScrollbarPositionX 改名为 getScrollbarX，方法体共用
        /*protected int getScrollbarPositionX() {
        */
        protected int scrollBarX() {
            return this.width - 6;
        }

        private abstract class Entry extends ContainerObjectSelectionList.Entry<areahint.management.client.AreaManagementScreen.ManagementList.Entry> {
        }

        private final class GroupEntry extends areahint.management.client.AreaManagementScreen.ManagementList.Entry {
            private final String key;

            private GroupEntry(String key) {
                this.key = key;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX(), y = getContentY(), entryWidth = getWidth(), entryHeight = getContentHeight();
            /*public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
            */
                context.text(AreaManagementScreen.this.font,
                    Component.literal(I18nManager.translate(key)), x + 6, y + 8, 0xFFFFFF55);
            }

            @Override public List<? extends GuiEventListener> children() { return List.of(); }
            @Override public List<? extends NarratableEntry> narratables() { return List.of(); }
        }

        private final class OperationEntry extends areahint.management.client.AreaManagementScreen.ManagementList.Entry {
            private final String operation;
            private final Button button;

            private OperationEntry(String operation) {
                this.operation = operation;
                this.button = Button.builder(Component.literal(operationName(operation)),
                    ignored -> AreaManagementScreen.this.launch(operation)).size(190, 20).build();
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX(), y = getContentY(), entryWidth = getWidth(), entryHeight = getContentHeight();
            /*public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
            */
                this.button.setPosition(x + 6, y + 3);
                this.button.extractRenderState(context, mouseX, mouseY, tickDelta);
                CommandVisualHandler handler = CommandVisualRegistry.getById(operation);
                if (handler != null) {
                    String detail = handler.description().getString();
                    int detailX = x + 206;
                    context.text(AreaManagementScreen.this.font,
                        Component.literal(trim(detail, entryWidth - 212)), detailX, y + 9, 0xFFAAAAAA);
                }
            }
            @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent clickEvent, boolean doubled) {
                double mouseX = clickEvent.x(), mouseY = clickEvent.y(); int button = clickEvent.button();
            /*@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            */
                return this.button.mouseClicked(clickEvent, doubled);
                /*return this.button.mouseClicked(mouseX, mouseY, button);
                */
            }
            @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent clickEvent) {
                double mouseX = clickEvent.x(), mouseY = clickEvent.y(); int button = clickEvent.button();
            /*@Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
            */
                return this.button.mouseReleased(clickEvent);
                /*return this.button.mouseReleased(mouseX, mouseY, button);
                */
            }
            @Override public List<? extends GuiEventListener> children() { return List.of(this.button); }
            @Override public List<? extends NarratableEntry> narratables() { return List.of(this.button); }
        }
    }

    private String operationName(String operation) {
        String key = "commandui." + operation + ".title";
        String translated = I18nManager.translate(key);
        return key.equals(translated) ? "/areahint " + operation : translated;
    }

    private String trim(String text, int maxWidth) {
        if (maxWidth <= 0 || this.font.width(text) <= maxWidth) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, maxWidth - this.font.width("...")) + "...";
    }
}
