package tfc.renirol.frontend.rendering.enums.flags;

import org.lwjgl.vulkan.VK13;

public enum DescriptorPoolFlags {
    UPDATER_AFTER_BIND(VK13.VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT),
    FREE_DESCRIPTOR_SET(VK13.VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT),
    ;

    public final int bits;

    DescriptorPoolFlags(int bits) {
        this.bits = bits;
    }
}
