package tfc.renirol.frontend.windowing.winnt;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.windowing.GenericWindow;
import tfc.renirol.frontend.windowing.WindowManager;

public class WinNTWindowManager extends WindowManager {
    public static final WindowManager INSTANCE = new WinNTWindowManager();

    @Override
    public PointerBuffer requiredVkExtensions(VkInstanceCreateInfo createInfo) {
//        return GLFWVulkan.glfwGetRequiredInstanceExtensions();
        return VkUtil.toPointerBuffer(
                new String[]{
                        KHRWin32Surface.VK_KHR_WIN32_SURFACE_EXTENSION_NAME,
                        KHRSurface.VK_KHR_SURFACE_EXTENSION_NAME,
                }
        );
    }

    @Override
    public void createVkSurface(VkInstance instance, GenericWindow window, int allocator, long buffer) {
        VkWin32SurfaceCreateInfoKHR createInfoKHR = VkWin32SurfaceCreateInfoKHR.calloc();
        createInfoKHR.sType(KHRWin32Surface.VK_STRUCTURE_TYPE_WIN32_SURFACE_CREATE_INFO_KHR);
        createInfoKHR.hinstance(WinNTWindow.hInst);
        createInfoKHR.hwnd(window.handle());
        KHRWin32Surface.nvkCreateWin32SurfaceKHR(
                instance,
                createInfoKHR.address(),
                allocator, buffer
        );
        createInfoKHR.free();
    }

    @Override
    public void freeExtensionBuffer(PointerBuffer pb) {
    }
}
