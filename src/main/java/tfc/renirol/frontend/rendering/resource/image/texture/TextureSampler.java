package tfc.renirol.frontend.rendering.resource.image.texture;

import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import tfc.renirol.itf.ReniDestructable;

public class TextureSampler implements ReniDestructable {
    public final long handle;
    private final VkDevice device;

    public TextureSampler(long handle, VkDevice device) {
        this.handle = handle;
        this.device = device;
    }

    @Override
    public void destroy() {
        VK13.nvkDestroySampler(device, handle, 0);
    }
}
