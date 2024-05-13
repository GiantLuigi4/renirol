package tfc.renirol.frontend.enums;

import org.lwjgl.vulkan.VK13;

public enum BindPoint {
    GRAPHICS(VK13.VK_PIPELINE_BIND_POINT_GRAPHICS),
    COMPUTE(VK13.VK_PIPELINE_BIND_POINT_COMPUTE),
    ;

    public final int id;

    BindPoint(int id) {
        this.id = id;
    }
}
