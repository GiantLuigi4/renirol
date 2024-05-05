package tfc.renirol.frontend.rendering.command.pipeline;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import tfc.renirol.frontend.hardware.util.ReniDestructable;

public class PipelineLayout implements ReniDestructable {
    public final long handle;
    public final VkDevice device;

    public PipelineLayout(long handle, VkDevice device) {
        this.handle = handle;
        this.device = device;
    }

    public void destroy() {
        VK10.nvkDestroyPipelineLayout(device, handle, 0);
    }
}
