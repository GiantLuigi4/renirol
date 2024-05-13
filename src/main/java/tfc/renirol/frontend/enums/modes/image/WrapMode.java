package tfc.renirol.frontend.enums.modes.image;

import org.lwjgl.vulkan.VK13;

public enum WrapMode {
    REPEAT(VK13.VK_SAMPLER_ADDRESS_MODE_REPEAT),
    CLAMP(VK13.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE),
    BORDER(VK13.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER),
    ;

    public final int id;

    WrapMode(int id) {
        this.id = id;
    }
}
