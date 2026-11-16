package areahint.compat;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class RendererRegistrationTest {
    @Test
    void registersLegacyRendererWithBothHandlerGenerations() throws Exception {
        LegacyRenderer renderer = new LegacyRenderer();
        LegacyHandler legacy = new LegacyHandler();
        ModernHandler modern = new ModernHandler();

        register(legacy, renderer);
        register(modern, renderer);

        assertSame(renderer, legacy.renderer);
        assertSame(renderer, modern.renderer);
    }

    @Test
    void selectsMostSpecificCompatibleOverload() throws Exception {
        OverloadedHandler handler = new OverloadedHandler();
        register(handler, new LegacyRenderer());
        assertTrue(handler.specificCalled);
    }

    @Test
    void rejectsIncompatibleHandlerWithoutCallingIt() {
        LegacyHandler handler = new LegacyHandler();
        assertThrows(NoSuchMethodException.class, () -> register(handler, new ModernRenderer()));
        assertNull(handler.renderer);
    }

    @Test
    void propagatesRegistrationFailureWithoutTryingOtherOverloads() {
        InvocationTargetException failure = assertThrows(InvocationTargetException.class,
            () -> register(new BrokenHandler(), new LegacyRenderer()));
        assertInstanceOf(IllegalStateException.class, failure.getCause());
    }

    private static void register(Object handler, Object renderer) throws Exception {
        RendererRegistration.register(handler, renderer);
    }

    public static class ModernRenderer { }
    public static class LegacyRenderer extends ModernRenderer { }

    public static class LegacyHandler {
        Object renderer;
        public void add(LegacyRenderer renderer) { this.renderer = renderer; }
    }

    public static class ModernHandler {
        Object renderer;
        public void add(ModernRenderer renderer) { this.renderer = renderer; }
    }

    public static class OverloadedHandler extends ModernHandler {
        boolean specificCalled;
        public void add(LegacyRenderer renderer) { specificCalled = true; }
    }

    public static class BrokenHandler {
        public void add(ModernRenderer renderer) { throw new IllegalStateException("registration failed"); }
    }
}
