package tfc.renirol.frontend.rendering.enums;

import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK13;

public enum ImageLayout {
    GENERAL(VK13.VK_IMAGE_LAYOUT_GENERAL),
    INITIALIZED(VK13.VK_IMAGE_LAYOUT_PREINITIALIZED),
    COLOR_ATTACHMENT_OPTIMAL(VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL),
    UNDEFINED(VK13.VK_IMAGE_LAYOUT_UNDEFINED),
    PRESENT(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR),
    ;

    public final int value;

    ImageLayout(int value) {
        this.value = value;
    }
}
