package tfc.renirol.frontend.rendering.enums.flags;

import org.lwjgl.vulkan.VK13;

public enum SwapchainUsage {
    COLOR(VK13.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT, VK13.VK_IMAGE_ASPECT_COLOR_BIT),
    DEPTH(VK13.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, VK13.VK_IMAGE_ASPECT_DEPTH_BIT),
    STENCIL(VK13.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, VK13.VK_IMAGE_ASPECT_STENCIL_BIT),
    DEPTH_STENCIL(VK13.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, VK13.VK_IMAGE_ASPECT_DEPTH_BIT | VK13.VK_IMAGE_ASPECT_STENCIL_BIT),
    ;

    public final int id;
    public final int aspect;

    SwapchainUsage(int id, int aspect) {
        this.id = id;
        this.aspect = aspect;
    }
}
