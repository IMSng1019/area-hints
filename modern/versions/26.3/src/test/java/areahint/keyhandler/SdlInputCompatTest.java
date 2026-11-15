package areahint.keyhandler;

import areahint.util.GuiInputCompat;
import areahint.util.PlatformInputCompat;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.input.KeyEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SdlInputCompatTest {
    @Test
    void legacyDefaultAndUnsupportedBindingsUseTheRealMinecraftKeyDomain() {
        assertEquals(InputConstants.KEY_X, PlatformInputCompat.fromStoredCode(88));
        assertEquals(88, PlatformInputCompat.toStoredCode(InputConstants.KEY_X));
        assertEquals(InputConstants.Type.KEYBOARD, PlatformInputCompat.storedKey(88).getType());
        assertEquals(InputConstants.UNKNOWN, PlatformInputCompat.storedKey(314));
        assertEquals(LegacyKeyCodec.UNSUPPORTED, PlatformInputCompat.toStoredCode(220));
    }

    @Test
    void physicalBindingsAndLayoutShortcutsUseDifferentPartsOfTheSameEvent() {
        // 非美式布局中，同一颗物理键可以产生不同字符；绑定看物理键，复制看逻辑字符。
        KeyEvent event = new KeyEvent(InputConstants.KEY_X, InputConstants.KEYCODE_C, InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER);
        var binding = KeyBindingCompat.create("key.areahint.test.sdl_binding", PlatformInputCompat.keyboardType(),
            PlatformInputCompat.fromStoredCode(88));
        int secondary = PlatformInputCompat.secondaryCode(event);
        assertEquals(InputConstants.KEYCODE_C, secondary);
        assertTrue(KeyBindingCompat.matchesKey(binding, event.key(), secondary));
        assertFalse(KeyBindingCompat.matchesKey(binding, InputConstants.KEY_NUMPADENTER, InputConstants.KEYCODE_NUMPADENTER));
        assertTrue(GuiInputCompat.isCopy(event.key(), secondary, event.modifiers()));
        assertTrue(GuiInputCompat.isUndoKey(event.key(), InputConstants.KEYCODE_Z));
        assertTrue(GuiInputCompat.isRedoKey(event.key(), InputConstants.KEYCODE_Y));
    }

    @Test
    void rightShiftAndPlatformEditingModifierAreNotReadAsGlfwBits() {
        assertTrue(GuiInputCompat.hasShift(2), "SDL 右 Shift 位也必须约束拖动并触发 Shift+撤销");
        assertFalse(GuiInputCompat.hasShift(InputConstants.MOD_CAPS_LOCK));
        assertTrue(GuiInputCompat.hasControl(InputConstants.KEY_Z, InputConstants.KEYCODE_Z,
            InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER));
    }

    @Test
    void mouseBindingsKeepNativeSdlLeftAndRightButtonsDistinct() {
        var binding = KeyBindingCompat.create("key.areahint.test.sdl_mouse", InputConstants.Type.MOUSE,
            InputConstants.MOUSE_BUTTON_RIGHT);
        assertTrue(KeyBindingCompat.matchesMouse(binding, InputConstants.MOUSE_BUTTON_RIGHT));
        assertFalse(KeyBindingCompat.matchesMouse(binding, InputConstants.MOUSE_BUTTON_LEFT));
        assertEquals(1, InputConstants.MOUSE_BUTTON_LEFT);
        assertEquals(3, InputConstants.MOUSE_BUTTON_RIGHT);
    }
}
