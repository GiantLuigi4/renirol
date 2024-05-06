package tfc.renirol.frontend.rendering.enums.modes.image;

import org.lwjgl.vulkan.VK13;

public enum MipmapMode {
    NEAREST(VK13.VK_SAMPLER_MIPMAP_MODE_NEAREST),
    LINEAR(VK13.VK_FILTER_LINEAR),
    ;

    public final int id;

    MipmapMode(int id) {
        this.id = id;
    }
}
