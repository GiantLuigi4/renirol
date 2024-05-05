package tfc.renirol;

import tfc.renirol.frontend.hardware.util.ReniDestructable;
import tfc.renirol.frontend.windowing.GenericWindow;

import java.util.function.Supplier;

public class Renirol {
    // prevent compile time inlining&IDE warnings about constant expressions
    private static String getBackend() {
        return "VULKAN";
    }

    public static final String BACKEND = getBackend();
    private static final ThreadLocal<ReniContext> CURRENT_CONTEXT = ThreadLocal.withInitial(() -> null);

    public static ReniContext activeContext() {
        return CURRENT_CONTEXT.get();
    }

    public static void useContext(ReniContext context) {
        CURRENT_CONTEXT.set(context);
        context.onActivate(null);
    }

    public static void useContext(ReniContext context, GenericWindow handle) {
        CURRENT_CONTEXT.set(context);
        context.onActivate(handle);
    }

    public static <T> T managed(Supplier<T> func, ReniDestructable... freeables) {
        T t = func.get();
        for (ReniDestructable freeable : freeables)
            freeable.destroy();
        return t;
    }
}
