package tfc.renirol.frontend.rendering.framebuffer.chain;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import tfc.renirol.frontend.rendering.resource.image.itf.ImageBacked;
import tfc.renirol.itf.ReniDestructable;

public class SwapchainImage implements ReniDestructable, ImageBacked {
    private final VkDevice logic;
    public final long image;
    private final long view;
    final VkExtent2D extents;
    private final int format;

    @Override
    public int getFormat() {
        return format;
    }

    @Override
    public long getView() {
        return view;
    }

    public SwapchainImage(VkDevice device, long image, long view, SwapChain chain) {
        this.logic = device;
        this.image = image;
        this.view = view;
        this.extents = chain.extents;
        this.format = chain.format.format();
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
