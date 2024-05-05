package tfc.renirol.frontend.rendering.enums.masks;

import org.lwjgl.vulkan.VK13;

public enum StageMask {
    GRAPHICS(VK13.VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT),
    TRANSFER(VK13.VK_PIPELINE_STAGE_TRANSFER_BIT),
    ;
    public final int value;

    StageMask(int value) {
        this.value = value;
    }
}
