package tfc.renirol.frontend.windowing.glfw;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import tfc.renirol.frontend.windowing.GenericWindow;
import tfc.renirol.frontend.windowing.WindowManager;

public class GLFWWindowManager extends WindowManager {
    public static final WindowManager INSTANCE = new GLFWWindowManager();

    @Override
    public PointerBuffer requiredVkExtensions(VkInstanceCreateInfo createInfo) {
        return GLFWVulkan.glfwGetRequiredInstanceExtensions();
    }

    @Override
    public void createVkSurface(VkInstance instance, GenericWindow window, int allocator, long buffer) {
        GLFWVulkan.nglfwCreateWindowSurface(instance.address(), window.handle(), allocator, buffer);
    }

    @Override
    public void freeExtensionBuffer(PointerBuffer pb) {
    }
}
