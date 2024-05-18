package tfc.renirol.frontend.rendering.pass;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import tfc.renirol.itf.ReniDestructable;

public class RenderPass implements ReniDestructable {
    public final long handle;
    private final VkDevice device;

    public RenderPass(long handle, VkDevice device) {
        this.handle = handle;
        this.device = device;
    }

    public void destroy() {
        VK10.nvkDestroyRenderPass(device, handle, 0);
    }
}
