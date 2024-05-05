package tfc.renirol.frontend.rendering.selectors;

import org.lwjgl.vulkan.EXTSwapchainColorspace;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK13;

public enum ColorSpaces {
    SRGB_NONLINEAR(KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR),
    EXTENDED_SRGB_NONLINEAR(EXTSwapchainColorspace.VK_COLOR_SPACE_EXTENDED_SRGB_NONLINEAR_EXT),
    ADOBERGB_NONLINEAR(EXTSwapchainColorspace.VK_COLOR_SPACE_ADOBERGB_NONLINEAR_EXT),
    ;

    int vk;

    ColorSpaces(int vk) {
        this.vk = vk;
    }
}
