package tfc.renirol.frontend.rendering.fencing;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import tfc.renirol.frontend.hardware.util.ReniDestructable;

import java.nio.LongBuffer;

public class Fence implements ReniDestructable {
    public final long handle;
    final VkDevice device;

    public Fence(VkDevice device, long handle) {
        this.device = device;
        this.handle = handle;
    }

    public void await() {
        LongBuffer buffer = MemoryUtil.memAllocLong(1);
        buffer.put(0, handle);
        VK10.nvkWaitForFences(device, 1, MemoryUtil.memAddress(buffer), 1, Long.MAX_VALUE);
        MemoryUtil.memFree(buffer);
    }

    public void reset() {
        LongBuffer buffer = MemoryUtil.memAllocLong(1);
        buffer.put(0, handle);
        VK10.nvkResetFences(device, 1, MemoryUtil.memAddress(buffer));
        MemoryUtil.memFree(buffer);
    }

    public void destroy() {
        VK10.nvkDestroyFence(device, handle, 0);
    }
}
