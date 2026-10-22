package areahint.mixin.client;

import areahint.handshake.ModHandshake;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Unique
    private ButtonWidget areahint$downloadButton;

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void areahint$addDownloadButton(CallbackInfo info) {
        areahint$downloadButton = null;
        // 原版断开界面不处理文本链接，装有本功能的客户端额外提供可点击的下载网址。
        if (!getNarratedTitle().getString().contains(ModHandshake.DOWNLOAD_URL)) {
            return;
        }
        int buttonWidth = Math.max(1, Math.min(340, width - 20));
        areahint$downloadButton = addDrawableChild(ButtonWidget.builder(Text.literal(ModHandshake.DOWNLOAD_URL),
                        button -> Util.getOperatingSystem().open(ModHandshake.DOWNLOAD_URL))
                .dimensions((width - buttonWidth) / 2, height - 28, buttonWidth, 20)
                .build());
    }

    //? if <1.21.4 {
    @Inject(method = "initTabNavigation", at = @At("TAIL"))
    //?} else {
    /*@Inject(method = "refreshWidgetPositions", at = @At("TAIL"))*/
    //?}
    private void areahint$positionDownloadButton(CallbackInfo info) {
        // 调整窗口或 GUI 缩放时，较新版本只重新布局，因此同步刷新独立下载按钮的位置。
        if (areahint$downloadButton != null) {
            int buttonWidth = Math.max(1, Math.min(340, width - 20));
            areahint$downloadButton.setWidth(buttonWidth);
            areahint$downloadButton.setPosition((width - buttonWidth) / 2, height - 28);
        }
    }
}
