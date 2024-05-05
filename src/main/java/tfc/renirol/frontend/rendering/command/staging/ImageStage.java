package tfc.renirol.frontend.rendering.command.staging;

import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK13;

public enum ImageStage {
    ALL_GRAPHICS(VK13.VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT),
    TRANSFER(VK13.VK_PIPELINE_STAGE_TRANSFER_BIT),
    ;

    int value;

    ImageStage(int value) {
        this.value = value;
    }
}
