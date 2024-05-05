package tfc.renirol.frontend.windowing.glfw;

import org.lwjgl.glfw.*;
import org.lwjgl.system.CallbackI;
import tfc.renirol.ReniContext;
import tfc.renirol.Renirol;
import tfc.renirol.frontend.windowing.GenericWindow;
import tfc.renirol.util.QuadConsumer;

import java.util.function.BiConsumer;

public class GLFWWindow extends GenericWindow {
    long handle;
    private final int[] width = new int[1];
    private final int[] height = new int[1];

    private ReniContext context;

    public GLFWWindow(int width, int height, String name) {
        super(GLFWWindowManager.INSTANCE);
        handle = GLFW.glfwCreateWindow(width, height, name, 0, 0);
    }

    public void center() {
        int[] width = new int[1];
        int[] height = new int[1];

        GLFW.glfwGetWindowSize(handle, width, height);

        GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());

        GLFW.glfwSetWindowPos(
                handle,
                (vidmode.width() - width[0]) / 2,
                (vidmode.height() - height[0]) / 2
        );
    }

    public static void poll() {
        GLFW.glfwPollEvents();
    }

    public void setSwapInterval(int interval) {
        context.swapInterval(interval);
    }

    public void initContext(ReniContext context) {
        if (this.context != null)
            throw new RuntimeException("Cannot setup multiple contexts to display to one window.");
        (this.context = context).setupSurface(this);
    }

    public void show() {
        GLFW.glfwShowWindow(handle);
    }

    public void hide() {
        GLFW.glfwHideWindow(handle);
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(handle);
    }

    public void swap() {
        context.swapBuffers(this);
    }

    public void pollSize() {
        GLFW.glfwGetWindowSize(handle, width, height);
    }

    public void swapAndPollSize() {
        context.swapBuffers(this);
        GLFW.glfwGetWindowSize(handle, width, height);
    }

    public void addMouseListener(BiConsumer<Double, Double> listener) {
        GLFW.glfwSetCursorPosCallback(handle, (h, x, y) -> listener.accept(x, y));
    }

    public void addKeyListener(QuadConsumer<Integer, Integer, Integer, Integer> listener) {
        GLFW.glfwSetKeyCallback(handle, (h, k, s, a, m) -> listener.accept(k, s, a, m));
    }

    public void dispose() {
        hide();
        GLFW.glfwDestroyWindow(handle);
    }

    public boolean isKeyPressed(int key) {
        return GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
    }

    public void grabContext() {
        Renirol.useContext(context, this);
    }

    public int getWidth() {
        return width[0];
    }

    public int getHeight() {
        return height[0];
    }

    public void registerListeners(CallbackI... callbacks) {
        for (CallbackI callback : callbacks) {
            if (callback instanceof GLFWMouseButtonCallbackI cb)
                GLFW.glfwSetMouseButtonCallback(handle, cb);
            if (callback instanceof GLFWScrollCallbackI cb)
                GLFW.glfwSetScrollCallback(handle, cb);
            if (callback instanceof GLFWCursorPosCallback cb)
                GLFW.glfwSetCursorPosCallback(handle, cb);
            if (callback instanceof GLFWCursorEnterCallbackI cb)
                GLFW.glfwSetCursorEnterCallback(handle, cb);

            if (callback instanceof GLFWKeyCallbackI cb)
                GLFW.glfwSetKeyCallback(handle, cb);
            if (callback instanceof GLFWCharCallbackI cb)
                GLFW.glfwSetCharCallback(handle, cb);
            if (callback instanceof GLFWCharModsCallbackI cb)
                GLFW.glfwSetCharModsCallback(handle, cb);

            if (callback instanceof GLFWWindowCloseCallbackI cb)
                GLFW.glfwSetWindowCloseCallback(handle, cb);
            if (callback instanceof GLFWWindowContentScaleCallbackI cb)
                GLFW.glfwSetWindowContentScaleCallback(handle, cb);
            if (callback instanceof GLFWWindowFocusCallbackI cb)
                GLFW.glfwSetWindowFocusCallback(handle, cb);

            if (callback instanceof GLFWWindowIconifyCallbackI cb)
                GLFW.glfwSetWindowIconifyCallback(handle, cb);
            if (callback instanceof GLFWWindowMaximizeCallbackI cb)
                GLFW.glfwSetWindowMaximizeCallback(handle, cb);

            if (callback instanceof GLFWWindowSizeCallbackI cb)
                GLFW.glfwSetWindowSizeCallback(handle, cb);

            if (callback instanceof GLFWWindowPosCallbackI cb)
                GLFW.glfwSetWindowPosCallback(handle, cb);
        }
    }

    public void captureMouse() {
        GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
    }

    public void freeMouse() {
        GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    }

    @Override
    public long handle() {
        return handle;
    }

    @Override
    public void setName(String name) {
        GLFW.glfwSetWindowTitle(handle, name);
    }
}