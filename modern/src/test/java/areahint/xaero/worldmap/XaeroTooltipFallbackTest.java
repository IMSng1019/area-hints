package areahint.xaero.worldmap;

import areahint.xaero.XaeroTooltipFallback;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class XaeroTooltipFallbackTest {
    @Test
    void keepsNativeTooltipWithoutInspectingOptionalClasses() {
        Object nativeTooltip = new Object();
        assertSame(nativeTooltip, XaeroTooltipFallback.resolve(nativeTooltip, new Object(), false));
    }

    @Test
    void invokesOldTypedCallbackWhenNewParentCallbackReturnsNull() {
        Area area = new Area();
        Context context = new Context();
        LegacyReader reader = new LegacyReader();
        Holder holder = new Holder(area, new Renderer(reader, context));
        assertSame(reader.tooltip, XaeroTooltipFallback.resolve(null, holder, true));
        assertSame(area, reader.area);
        assertSame(context, reader.context);
    }

    @Test
    void ignoresMissingHolderAndForeignReader() {
        assertNull(XaeroTooltipFallback.resolve(null, null, false));
        assertNull(XaeroTooltipFallback.resolve(null, new Holder(new Area(), new Renderer(new Object(), new Context())), false));
    }

    @Test
    void rejectsWrongElementAndCatchesCallbackFailure() {
        assertNull(XaeroTooltipFallback.resolve(null, new Holder(new Object(), new Renderer(new LegacyReader(), new Context())), false));
        assertNull(XaeroTooltipFallback.resolve(null, new Holder(new Area(), new Renderer(new ThrowingReader(), new Context())), false));
    }

    // 用不同返回类型模拟 Xaero 新父类与旧编译读者之间缺失的协变桥接方法。
    static class ParentReader {
        public Object getTooltip(Object area, Object context, boolean overMenu) { return null; }
    }

    static final class LegacyReader extends ParentReader {
        final Tooltip tooltip = new Tooltip();
        Area area;
        Context context;
        public Tooltip getTooltip(Area area, Context context, boolean overMenu) {
            if (!overMenu) throw new AssertionError("菜单标志必须传递给原回调");
            this.area = area;
            this.context = context;
            return tooltip;
        }
    }

    static final class ThrowingReader extends ParentReader {
        public Tooltip getTooltip(Area area, Context context, boolean overMenu) { throw new IllegalStateException("不可用的可选接口"); }
    }

    static final class Holder {
        final Object area;
        final Renderer renderer;
        Holder(Object area, Renderer renderer) { this.area = area; this.renderer = renderer; }
        public Object getElement() { return area; }
        public Renderer getRenderer() { return renderer; }
    }

    static final class Renderer {
        final Object reader;
        final Object context;
        Renderer(Object reader, Object context) { this.reader = reader; this.context = context; }
        public Object getReader() { return reader; }
        public Object getContext() { return context; }
    }

    static final class Area { }
    static final class Context { }
    static final class Tooltip { }
}
