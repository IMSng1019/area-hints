package areahint.mixin.client;

import areahint.i18n.I18nManager;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguage.class)
public class TranslationStorageMixin {

    @Inject(method = "getOrDefault", at = @At("HEAD"), cancellable = true)
    private void areasHintOverrideTranslation(String key, String fallback, CallbackInfoReturnable<String> cir) {
        if (I18nManager.hasKey(key)) {
            cir.setReturnValue(I18nManager.translate(key));
        }
    }
}
