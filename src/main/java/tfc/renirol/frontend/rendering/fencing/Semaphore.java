package tfc.renirol.frontend.rendering.fencing;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.util.ReniDestructable;

public class Semaphore implements ReniDestructable {
    public final long handle;
    private final VkDevice device;

    public Semaphore(ReniLogicalDevice device) {
        this.device = device.getDirect(VkDevice.class);

        VkSemaphoreCreateInfo ci = VkSemaphoreCreateInfo.calloc();
        ci.sType(VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
        this.handle = VkUtil.getCheckedLong((buf) -> VK10.nvkCreateSemaphore(this.device, ci.address(), 0, MemoryUtil.memAddress(buf)));
        ci.free();
    }

    public void destroy() {
        VK13.nvkDestroySemaphore(device, handle, 0);
    }

    public Fence createFence() {
        VkFenceCreateInfo ci = VkFenceCreateInfo.calloc();
        ci.sType(VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
        long handle = VkUtil.getCheckedLong((buf) -> VK10.nvkCreateFence(device, ci.address(), 0, MemoryUtil.memAddress(buf)));
        ci.free();
        return new Fence(device, handle);
    }
}
