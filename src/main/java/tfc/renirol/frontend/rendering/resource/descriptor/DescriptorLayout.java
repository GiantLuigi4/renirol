package tfc.renirol.frontend.rendering.resource.descriptor;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.rendering.enums.flags.DescriptorSetLayoutFlags;

import java.nio.IntBuffer;

public class DescriptorLayout {
    VkDevice device;
    public final long handle;

    public DescriptorLayout(ReniLogicalDevice device, long handle) {
        this.device = device.getDirect(VkDevice.class);
        this.handle = handle;
    }

    public DescriptorLayout(ReniLogicalDevice device, int bindingCount, DescriptorLayoutInfo... infos) {
        this(device, bindingCount, new DescriptorSetLayoutFlags[]{DescriptorSetLayoutFlags.AUTO}, infos);
    }

    public DescriptorLayout(ReniLogicalDevice device, int bindingCount, DescriptorSetLayoutFlags[] flags, DescriptorLayoutInfo... infos) {
        this.device = device.getDirect(VkDevice.class);
        VkDescriptorSetLayoutBinding.Buffer infoBuffer = VkDescriptorSetLayoutBinding.calloc(infos.length);
        VkDescriptorSetLayoutBindingFlagsCreateInfo flagsCI = VkDescriptorSetLayoutBindingFlagsCreateInfo.create().sType(VK13.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO);
        IntBuffer buffer = MemoryUtil.memAllocInt(infos.length);

        int autoFlags = 0;

        for (int i = 0; i < infos.length; i++) {
            infoBuffer.put(i, infos[i].binding);
            buffer.put(i, infos[i].flags);

            if ((infos[i].flags & VK13.VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT) == VK13.VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT) {
                autoFlags |= DescriptorSetLayoutFlags.UPDATE_AFTER_BIND_POOL.bits;
            }
        }
        flagsCI.bindingCount(bindingCount);
        flagsCI.pBindingFlags(buffer);

        int foreground = 0;
        for (DescriptorSetLayoutFlags flag : flags) {
            if (flag == DescriptorSetLayoutFlags.AUTO) {
                foreground |= autoFlags;
            } else foreground |= flag.bits;
        }

        VkDescriptorSetLayoutCreateInfo[] ci = new VkDescriptorSetLayoutCreateInfo[1];
        int finalForeground = foreground;
        handle = VkUtil.getCheckedLong((buf) -> VK13.nvkCreateDescriptorSetLayout(
                this.device, (
                        (ci[0] = VkDescriptorSetLayoutCreateInfo.calloc())
                                .sType(VK13.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                                .pBindings(infoBuffer)
                                .flags(finalForeground)
                                .pNext(flagsCI)
                ).address(), 0,
                MemoryUtil.memAddress(buf)
        ));
        ci[0].free();
        flagsCI.free();
        MemoryUtil.memFree(buffer);
        infoBuffer.free();
    }

    public void destroy() {
        VK13.nvkDestroyDescriptorSetLayout(device, handle, 0);
    }
}
