package tfc.renirol.frontend.rendering.enums.masks;

import org.lwjgl.vulkan.VK13;

public enum DynamicStateMasks {
    SCISSOR(VK13.VK_DYNAMIC_STATE_SCISSOR),
    VIEWPORT(VK13.VK_DYNAMIC_STATE_VIEWPORT),
    BLEND(VK13.VK_DYNAMIC_STATE_BLEND_CONSTANTS),
    FRONT_FACE(VK13.VK_DYNAMIC_STATE_FRONT_FACE),
    ;
    public final int bits;

    DynamicStateMasks(int bits) {
        this.bits = bits;
    }
}
