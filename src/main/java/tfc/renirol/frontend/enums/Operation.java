package tfc.renirol.frontend.enums;

import org.lwjgl.vulkan.VK13;

public enum Operation {
    PERFORM(VK13.VK_ATTACHMENT_LOAD_OP_LOAD, VK13.VK_ATTACHMENT_STORE_OP_STORE),
    DONT_CARE(VK13.VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK13.VK_ATTACHMENT_STORE_OP_DONT_CARE),
    CLEAR(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR, VK13.VK_ATTACHMENT_STORE_OP_DONT_CARE),
    NONE(VK13.VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK13.VK_ATTACHMENT_STORE_OP_NONE)
    ;

    public final int load, store;

    Operation(int load, int store) {
        this.load = load;
        this.store = store;
    }
}
