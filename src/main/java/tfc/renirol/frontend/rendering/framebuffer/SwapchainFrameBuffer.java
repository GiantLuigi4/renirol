package tfc.renirol.frontend.rendering.framebuffer;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.rendering.resource.image.Image;
import tfc.renirol.itf.ReniDestructable;

public class SwapchainFrameBuffer implements ReniDestructable {
    VkDevice logic;
    public final long image;
    long view;
    VkExtent2D extents;

    public SwapchainFrameBuffer(VkDevice device, long image, long view, SwapChain chain) {
        this.logic = device;
        this.image = image;
        this.view = view;
        this.extents = chain.extents;
    }

    public SwapchainFrameBuffer(ReniLogicalDevice logic, Image image) {
        this.logic = logic.getDirect(VkDevice.class);
        this.image = image.getHandle();
        this.view = image.getView();
        this.extents = image.getExtents();
    }

    @Override
    public void destroy() {
        VK10.nvkDestroyImageView(logic, view, 0);
    }
}
