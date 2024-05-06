package tfc.renirol.frontend.rendering.enums.modes;

import org.lwjgl.vulkan.VK13;

public enum FrontFace {
    CLOCKWISE(VK13.VK_FRONT_FACE_CLOCKWISE),
    COUNTER_CLOCKWISE(VK13.VK_FRONT_FACE_COUNTER_CLOCKWISE),
    ;

    public final int id;

    FrontFace(int id) {
        this.id = id;
    }
}
