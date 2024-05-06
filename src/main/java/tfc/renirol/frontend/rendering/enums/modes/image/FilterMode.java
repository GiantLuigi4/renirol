package tfc.renirol.frontend.rendering.enums.modes.image;

import org.lwjgl.vulkan.VK13;

public enum FilterMode {
    NEAREST(VK13.VK_FILTER_NEAREST),
    LINEAR(VK13.VK_FILTER_LINEAR),
    ;

    public final int id;

    FilterMode(int id) {
        this.id = id;
    }
}
