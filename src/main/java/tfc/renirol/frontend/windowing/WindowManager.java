package tfc.renirol.frontend.windowing;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

/**
 * Deprecation notice: internal use only; none of the functions on this class are useful to a program, they are only for use by {@link tfc.renirol.ReniContext}
 */
@Deprecated(forRemoval = false)
public abstract class WindowManager {
    public abstract void createVkSurface(VkInstance instance, GenericWindow window, int allocator, long buffer);

    public abstract PointerBuffer requiredVkExtensions(VkInstanceCreateInfo createInfo);

    public abstract void freeExtensionBuffer(PointerBuffer pb);
}
