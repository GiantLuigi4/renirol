package tfc.renirol.frontend.enums.flags;

import org.lwjgl.vulkan.VK13;

public enum DescriptorBindingFlags {
    PARTIALLY_BOUND(VK13.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT),
    UPDATE_AFTER_BIND(VK13.VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT),
    UNUSED_WHILE_PENDING(VK13.VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT),
    VARIABLE_DESC_COUNT(VK13.VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT),
    ;

    public final int bits;

    DescriptorBindingFlags(int bits) {
        this.bits = bits;
    }
}
