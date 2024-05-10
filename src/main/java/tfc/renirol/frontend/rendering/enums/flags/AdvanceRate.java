package tfc.renirol.frontend.rendering.enums.flags;

import org.lwjgl.vulkan.VK13;

public enum AdvanceRate {
    PER_VERTEX(VK13.VK_VERTEX_INPUT_RATE_VERTEX),
    PER_INSTANCE(VK13.VK_VERTEX_INPUT_RATE_INSTANCE);

    public final int id;

    AdvanceRate(int id) {
        this.id = id;
    }
}
