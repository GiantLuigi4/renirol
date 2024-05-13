package tfc.renirol.frontend.enums.masks;

import org.lwjgl.vulkan.VK13;

public enum StageMask {
    GRAPHICS(VK13.VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT),
    TRANSFER(VK13.VK_PIPELINE_STAGE_TRANSFER_BIT),
    TOP_OF_PIPE(VK13.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT),
    HOST(VK13.VK_PIPELINE_STAGE_HOST_BIT),
    EARLY_FRAGMENT_TEST(VK13.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT),
    ;
    public final int value;

    StageMask(int value) {
        this.value = value;
    }
}