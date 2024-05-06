package tfc.renirol.frontend.rendering.enums.modes;

import org.lwjgl.vulkan.VK13;

public enum CompareOp {
    LESS(VK13.VK_COMPARE_OP_LESS),
    LEQUAL(VK13.VK_COMPARE_OP_LESS_OR_EQUAL),
    GREATER(VK13.VK_COMPARE_OP_GREATER),
    GEQUAL(VK13.VK_COMPARE_OP_GREATER_OR_EQUAL),
    EQUAL(VK13.VK_COMPARE_OP_EQUAL),
    NEQUAL(VK13.VK_COMPARE_OP_NOT_EQUAL),
    NEVER(VK13.VK_COMPARE_OP_NEVER),
    ALWAYS(VK13.VK_COMPARE_OP_ALWAYS),
    ;

    public final int id;

    CompareOp(int id) {
        this.id = id;
    }
}
