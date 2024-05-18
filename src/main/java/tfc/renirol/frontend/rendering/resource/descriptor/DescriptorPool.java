package tfc.renirol.frontend.rendering.resource.descriptor;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDevice;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.enums.DescriptorType;
import tfc.renirol.frontend.enums.flags.DescriptorPoolFlags;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.itf.ReniDestructable;

public class DescriptorPool implements ReniDestructable {
    private final VkDevice device;
    public final long handle;

    public DescriptorPool(
            ReniLogicalDevice device,
            int maxSets,
            DescriptorPoolFlags[] flags,
            PoolInfo... info
    ) {
        this.device = device.getDirect(VkDevice.class);
        VkDescriptorPoolCreateInfo ci = VkDescriptorPoolCreateInfo.calloc().sType(VK13.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
        VkDescriptorPoolSize.Buffer buffer = VkDescriptorPoolSize.calloc(info.length);
        for (int i = 0; i < info.length; i++) buffer.get(i).type(info[i].type.id).descriptorCount(info[i].count);
        ci.pPoolSizes(buffer);
        ci.maxSets(maxSets);
        int fg = 0;
        for (DescriptorPoolFlags flag : flags) fg |= flag.bits;
        ci.flags(fg);
        handle = VkUtil.getCheckedLong((buf) -> VK13.nvkCreateDescriptorPool(
                this.device, ci.address(),
                0, MemoryUtil.memAddress(buf)
        ));
        buffer.free();
    }

    @Override
    public void destroy() {
        VK13.nvkDestroyDescriptorPool(device, handle, 0);
    }

    public static class PoolInfo {
        final DescriptorType type;
        final int count;

        public PoolInfo(DescriptorType type, int count) {
            this.type = type;
            this.count = count;
        }

        public static PoolInfo of(DescriptorType type, int count) {
            return new PoolInfo(type, count);
        }
    }
}
