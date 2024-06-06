package tfc.renirol.frontend.rendering.framebuffer.chain;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import tfc.renirol.frontend.rendering.resource.image.ImageBacked;
import tfc.renirol.itf.ReniDestructable;

public class SwapchainImage implements ReniDestructable, ImageBacked {
    VkDevice logic;
    public final long image;
    long view;
    VkExtent2D extents;

    public SwapchainImage(VkDevice device, long image, long view, SwapChain chain) {
        this.logic = device;
        this.image = image;
        this.view = view;
        this.extents = chain.extents;
    }

    @Override
    public void destroy() {
        VK10.nvkDestroyImageView(logic, view, 0);
    }

    @Override
    public long getHandle() {
        return image;
    }
}
