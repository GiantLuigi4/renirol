package tfc.renirol.frontend.windowing.glfw;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.Configuration;

public class Setup {
    public static void genericSetup() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit())
            throw new RuntimeException("Could not initialize GLFW");

        GLFW.glfwDefaultWindowHints();

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_SRGB_CAPABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_AUX_BUFFERS, 0);

//		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_NO_ERROR, GLFW.GLFW_TRUE);
    }

    public static void performanceSetup() {
        if (!GLFW.glfwInit())
            throw new RuntimeException("Could not initialize GLFW");

        GLFW.glfwDefaultWindowHints();

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_SRGB_CAPABLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 1);
        GLFW.glfwWindowHint(GLFW.GLFW_AUX_BUFFERS, 0);

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_NO_ERROR, GLFW.GLFW_TRUE);
    }

	public static void loadRenderdoc() {
        try {
            System.loadLibrary("renderdoc");
        } catch (Throwable err) {
            err.printStackTrace();
        }
	}

    public static void noAPI() {
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
    }

    public static void lwjglPerformanceSetup() {
        Configuration.DISABLE_CHECKS.set(true);
        Configuration.DISABLE_FUNCTION_CHECKS.set(true);
    }
}