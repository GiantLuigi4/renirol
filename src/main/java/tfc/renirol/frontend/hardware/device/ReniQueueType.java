package tfc.renirol.frontend.hardware.device;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK13;

public enum ReniQueueType {
    COMPUTE(VK13.VK_QUEUE_COMPUTE_BIT),
    GRAPHICS(VK13.VK_QUEUE_GRAPHICS_BIT),
    PROTECTED(VK13.VK_QUEUE_PROTECTED_BIT),
    TRANSFER(VK13.VK_QUEUE_TRANSFER_BIT), // vk name for transfer
    SPARSE_BINDING(VK13.VK_QUEUE_SPARSE_BINDING_BIT),
    ;

    final int vk;

    ReniQueueType(int vk) {
        this.vk = vk;
    }

    public boolean isApplicable(int flags) {
        return (flags & vk) != 0;
    }
}
