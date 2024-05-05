package tfc.renirol.frontend.rendering;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkQueue;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.util.ReniDestructable;

public class ReniQueue {
    private final ReniLogicalDevice device;
    private final VkQueue direct;

    public ReniQueue(ReniLogicalDevice device, VkQueue direct) {
        this.device = device;
        this.direct = direct;
    }

    public <T> T getDirect(Class<T> type) {
        return (T) direct;
    }

    public void await() {
        VK13.vkQueueWaitIdle(direct);
    }
}
