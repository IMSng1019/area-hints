package areahint.description;

import areahint.AreashintClient;
import areahint.data.AreaData;
import areahint.detection.AreaDetector;
import areahint.log.AreaChangeTracker;
import areahint.keyhandler.UnifiedKeyHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * 域名描述查询按键。
 */
public final class DescriptionKeyHandler {
    private static KeyBinding queryKeyBinding;
    private static boolean registered;
    private static boolean skipNextQueryKeyPress;
    private static boolean skipNextRecordKeyPress;

    enum BoundKeyAction {
        NONE,
        CLOSE_DESCRIPTION,
        SKIP_PRESS,
        QUERY
    }

    private DescriptionKeyHandler() {
    }

    public static void register() {
        queryKeyBinding = KeyBindingHelper.registerKeyBinding(areahint.keyhandler.KeyBindingCompat.create(
            "key.areahint.description.query",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN
        ));

        if (!registered) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (queryKeyBinding == null) {
                    return;
                }
                if (!queryKeyBinding.wasPressed()) {
                    if (skipNextQueryKeyPress) {
                        skipNextQueryKeyPress = false;
                    }
                    return;
                }

                switch (resolveBoundKeyAction(skipNextQueryKeyPress, true, isViewDescriptionBookScreen(client.currentScreen))) {
                    case CLOSE_DESCRIPTION -> {
                        closeDescriptionBookScreen(client.currentScreen);
                        skipNextQueryKeyPress = false;
                        skipNextRecordKeyPress = true;
                        drainQueryKeyPresses();
                    }
                    case SKIP_PRESS -> {
                        skipNextQueryKeyPress = false;
                        drainQueryKeyPresses();
                    }
                    case QUERY -> queryCurrentDescription();
                    case NONE -> {
                    }
                }
            });
            registered = true;
        }
    }

    public static boolean closeCurrentDescriptionBookScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return false;
        }
        boolean closed = closeDescriptionBookScreen(client.currentScreen);
        if (closed) {
            skipNextQueryKeyPress = false;
            drainQueryKeyPresses();
        }
        return closed;
    }

    public static boolean consumeSuppressedRecordKeyPress() {
        if (!skipNextRecordKeyPress) {
            return false;
        }
        skipNextRecordKeyPress = false;
        return true;
    }

    public static void clearSuppressedRecordKeyPress() {
        skipNextRecordKeyPress = false;
    }

    static BoundKeyAction resolveBoundKeyAction(boolean skipNextPress, boolean keyPressed, boolean descriptionBookScreenOpen) {
        if (!keyPressed) {
            return BoundKeyAction.NONE;
        }
        if (descriptionBookScreenOpen) {
            return BoundKeyAction.CLOSE_DESCRIPTION;
        }
        if (skipNextPress) {
            return BoundKeyAction.SKIP_PRESS;
        }
        return BoundKeyAction.QUERY;
    }

    /**
     * 只有查询描述时打开的只读书本属于“查看描述界面”。
     * 描述指令中用于录入内容的 DescriptionBookEditScreen 是编辑界面，
     * 绑定键不能把它当成查看界面关闭。
     */
    private static boolean isViewDescriptionBookScreen(Screen screen) {
        return BookDescriptionScreenUtil.isDescriptionBookScreen(screen);
    }

    /**
     * 统一绑定键没有被记录顶点等功能消费时，会默认查询当前域名描述。
     * 如果当前已经有任意界面打开，或者描述添加/删除流程正在进行，
     * 这里需要阻止默认查询，避免描述指令流程中按绑定键又打开“查看描述”书本。
     */
    public static boolean shouldSkipDefaultRecordKeyQuery() {
        MinecraftClient client = MinecraftClient.getInstance();
        return DescriptionManager.getInstance().isActive()
            || (client != null && client.currentScreen != null);
    }

    public static void queryCurrentDescription() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        if (client.currentScreen != null || isInteractionActive()) {
            return;
        }

        String dimensionType = areahint.network.Packets.convertDimensionPathToType(client.world.getRegistryKey().getValue().getPath());
        AreaData area = AreaChangeTracker.getCurrentAreaData();
        if (area == null) {
            AreaDetector detector = AreashintClient.getAreaDetector();
            if (detector != null) {
                area = detector.findAreaRaw(client.player.getX(), client.player.getY(), client.player.getZ());
            }
        }
        if (area != null && area.getName() != null) {
            // 绑定键与独立查询键共用客户端当前域名判断，避免误回退到维度域名描述。
            DescriptionClientNetworking.sendQuery("area", dimensionType, area.getName());
        } else {
            DescriptionClientNetworking.sendQuery("dimension", dimensionType, client.world.getRegistryKey().getValue().toString());
        }
    }

    private static boolean isInteractionActive() {
        return DescriptionManager.getInstance().isActive()
            || areahint.easyadd.EasyAddManager.getInstance().getCurrentState() != areahint.easyadd.EasyAddManager.EasyAddState.IDLE
            || areahint.expandarea.ExpandAreaManager.getInstance().isActive()
            || areahint.shrinkarea.ShrinkAreaManager.getInstance().isActive()
            || areahint.dividearea.DivideAreaManager.getInstance().isActive()
            || areahint.addhint.AddHintManager.getInstance().isActive()
            || areahint.deletehint.DeleteHintManager.getInstance().isActive()
            || areahint.delete.DeleteManager.getInstance().getCurrentState() != areahint.delete.DeleteManager.DeleteState.IDLE
            || areahint.dimensional.DimensionalNameUIManager.getInstance().getCurrentState() != areahint.dimensional.DimensionalNameUIManager.State.IDLE
            || areahint.rename.RenameManager.getInstance().getCurrentState() != areahint.rename.RenameManager.RenameState.IDLE
            || areahint.recolor.RecolorManager.getInstance().getCurrentState() != areahint.recolor.RecolorManager.RecolorState.IDLE
            || areahint.signature.SignatureManager.getInstance().isActive();
    }

    public static boolean consumeCloseDescriptionBookKey(int keyCode, int scanCode) {
        if (!shouldCloseOnBoundKey(keyCode, scanCode)) {
            return false;
        }
        if (matchesQueryKey(keyCode, scanCode)) {
            skipNextQueryKeyPress = true;
        }
        if (matchesRecordKey(keyCode, scanCode)) {
            skipNextRecordKeyPress = true;
        }
        drainQueryKeyPresses();
        return true;
    }

    public static boolean consumeCloseDescriptionBookMouse(int button) {
        if (!shouldCloseOnBoundMouse(button)) {
            return false;
        }
        if (matchesQueryMouse(button)) {
            skipNextQueryKeyPress = true;
        }
        if (matchesRecordMouse(button)) {
            skipNextRecordKeyPress = true;
        }
        drainQueryKeyPresses();
        return true;
    }

    static boolean shouldCloseOnBoundKey(int keyCode, int scanCode) {
        KeyBinding recordKeyBinding = UnifiedKeyHandler.getRecordKeyBinding();
        return shouldCloseOnAnyBoundInput(
            queryKeyBinding != null,
            queryKeyBinding == null || queryKeyBinding.isUnbound(),
            queryKeyBinding != null && areahint.keyhandler.KeyBindingCompat.matchesKey(queryKeyBinding, keyCode, scanCode),
            recordKeyBinding != null,
            recordKeyBinding == null || recordKeyBinding.isUnbound(),
            recordKeyBinding != null && areahint.keyhandler.KeyBindingCompat.matchesKey(recordKeyBinding, keyCode, scanCode)
        );
    }

    private static boolean matchesQueryKey(int keyCode, int scanCode) {
        return shouldCloseOnBoundInput(
            queryKeyBinding != null,
            queryKeyBinding == null || queryKeyBinding.isUnbound(),
            queryKeyBinding != null && areahint.keyhandler.KeyBindingCompat.matchesKey(queryKeyBinding, keyCode, scanCode)
        );
    }

    private static boolean matchesRecordKey(int keyCode, int scanCode) {
        KeyBinding recordKeyBinding = UnifiedKeyHandler.getRecordKeyBinding();
        return shouldCloseOnBoundInput(
            recordKeyBinding != null,
            recordKeyBinding == null || recordKeyBinding.isUnbound(),
            recordKeyBinding != null && areahint.keyhandler.KeyBindingCompat.matchesKey(recordKeyBinding, keyCode, scanCode)
        );
    }

    private static boolean matchesQueryMouse(int button) {
        return shouldCloseOnBoundInput(
            queryKeyBinding != null,
            queryKeyBinding == null || queryKeyBinding.isUnbound(),
            queryKeyBinding != null && areahint.keyhandler.KeyBindingCompat.matchesMouse(queryKeyBinding, button)
        );
    }

    private static boolean matchesRecordMouse(int button) {
        KeyBinding recordKeyBinding = UnifiedKeyHandler.getRecordKeyBinding();
        return shouldCloseOnBoundInput(
            recordKeyBinding != null,
            recordKeyBinding == null || recordKeyBinding.isUnbound(),
            recordKeyBinding != null && areahint.keyhandler.KeyBindingCompat.matchesMouse(recordKeyBinding, button)
        );
    }

    static boolean shouldCloseOnBoundMouse(int button) {
        KeyBinding recordKeyBinding = UnifiedKeyHandler.getRecordKeyBinding();
        return shouldCloseOnAnyBoundInput(
            queryKeyBinding != null,
            queryKeyBinding == null || queryKeyBinding.isUnbound(),
            queryKeyBinding != null && areahint.keyhandler.KeyBindingCompat.matchesMouse(queryKeyBinding, button),
            recordKeyBinding != null,
            recordKeyBinding == null || recordKeyBinding.isUnbound(),
            recordKeyBinding != null && areahint.keyhandler.KeyBindingCompat.matchesMouse(recordKeyBinding, button)
        );
    }

    static boolean shouldCloseOnBoundInput(boolean bindingRegistered, boolean bindingUnbound, boolean bindingMatches) {
        return bindingRegistered && !bindingUnbound && bindingMatches;
    }

    static boolean shouldCloseOnAnyBoundInput(
        boolean queryBindingRegistered,
        boolean queryBindingUnbound,
        boolean queryBindingMatches,
        boolean recordBindingRegistered,
        boolean recordBindingUnbound,
        boolean recordBindingMatches
    ) {
        return shouldCloseOnBoundInput(queryBindingRegistered, queryBindingUnbound, queryBindingMatches)
            || shouldCloseOnBoundInput(recordBindingRegistered, recordBindingUnbound, recordBindingMatches);
    }

    private static boolean closeDescriptionBookScreen(Screen screen) {
        if (isViewDescriptionBookScreen(screen)) {
            screen.close();
            return true;
        }
        return false;
    }

    private static void drainQueryKeyPresses() {
        if (queryKeyBinding == null) {
            return;
        }
        while (queryKeyBinding.wasPressed()) {
            // Drain queued presses so closing the book does not immediately query again.
        }
    }
}
