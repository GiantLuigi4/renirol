package tfc.renirol.frontend.rendering.enums.modes;

import org.lwjgl.vulkan.VK13;

public enum CullMode {
    NONE(VK13.VK_CULL_MODE_NONE),
    FRONT(VK13.VK_CULL_MODE_FRONT_BIT),
    BACK(VK13.VK_CULL_MODE_BACK_BIT),
    FRONT_BACK(VK13.VK_CULL_MODE_FRONT_AND_BACK),
    ;
    public final int id;

    CullMode(int id) {
        this.id = id;
    }
}
