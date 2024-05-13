package tfc.renirol.frontend.enums.flags;

import org.lwjgl.vulkan.VK13;

public enum DescriptorSetLayoutFlags {
    AUTO(-1),
    UPDATE_AFTER_BIND_POOL(VK13.VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT),
    ;

    public final int bits;

    DescriptorSetLayoutFlags(int id) {
        this.bits = id;
    }
}
