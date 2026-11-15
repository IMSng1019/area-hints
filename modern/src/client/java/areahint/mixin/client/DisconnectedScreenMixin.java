package areahint.mixin.client;

import areahint.handshake.ModHandshake;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Unique
    private Button areahint$downloadButton;

    protected DisconnectedScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void areahint$addDownloadButton(CallbackInfo info) {
        areahint$downloadButton = null;
        // 原版断开界面不处理文本链接，装有本功能的客户端额外提供可点击的下载网址。
        if (!getNarrationMessage().getString().contains(ModHandshake.DOWNLOAD_URL)) {
            return;
        }
        int buttonWidth = Math.max(1, Math.min(340, width - 20));
        areahint$downloadButton = addRenderableWidget(Button.builder(Component.literal(ModHandshake.DOWNLOAD_URL),
                        button -> {
                            // SDL 版本由 Blaze3D 提供浏览器入口，旧版本仍使用原平台实现。
                            //? if >=26.3 {
                            /*com.mojang.blaze3d.Blaze3D.openUri(java.net.URI.create(ModHandshake.DOWNLOAD_URL));*/
                            //?} else {
                            Util.getPlatform().openUri(ModHandshake.DOWNLOAD_URL);
                            //?}
                        })
                .bounds((width - buttonWidth) / 2, height - 28, buttonWidth, 20)
                .build());
    }
    /*@Inject(method = "initTabNavigation", at = @At("TAIL"))
    */
    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void areahint$positionDownloadButton(CallbackInfo info) {
        // 调整窗口或 GUI 缩放时，较新版本只重新布局，因此同步刷新独立下载按钮的位置。
        if (areahint$downloadButton != null) {
            int buttonWidth = Math.max(1, Math.min(340, width - 20));
            areahint$downloadButton.setWidth(buttonWidth);
            areahint$downloadButton.setPosition((width - buttonWidth) / 2, height - 28);
        }
    }
}
