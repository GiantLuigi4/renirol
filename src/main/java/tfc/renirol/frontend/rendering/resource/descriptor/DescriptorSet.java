package tfc.renirol.frontend.rendering.resource.descriptor;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.rendering.enums.DescriptorType;
import tfc.renirol.frontend.rendering.resource.buffer.GPUBuffer;

import java.nio.LongBuffer;

public class DescriptorSet {
    private final VkDevice device;
    public final long handle;

    public DescriptorSet(ReniLogicalDevice device, DescriptorPool pool, DescriptorLayout... layouts) {
        this.device = device.getDirect(VkDevice.class);
        VkDescriptorSetAllocateInfo info = VkDescriptorSetAllocateInfo.calloc().sType(VK13.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
        info.descriptorPool(pool.handle);
        LongBuffer bbBuf = MemoryUtil.memAllocLong(layouts.length);
        for (int i = 0; i < layouts.length; i++) {
            bbBuf.put(i, layouts[i].handle);
        }
        info.pSetLayouts(bbBuf);
        handle = VkUtil.getCheckedLong((buf) -> VK13.nvkAllocateDescriptorSets(
                this.device,
                info.address(), MemoryUtil.memAddress(buf)
        ));
        info.free();
    }

    public void bind(int binding, int resourceId, DescriptorType type, GPUBuffer buffer) {
        VkWriteDescriptorSet.Buffer writeDescriptorSet = VkWriteDescriptorSet.calloc(1);
        writeDescriptorSet.sType(VK13.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
        VkDescriptorBufferInfo.Buffer info = VkDescriptorBufferInfo.calloc(1);
        info.buffer(buffer.getHandle()).offset(0).range(buffer.getSize());
        writeDescriptorSet.pBufferInfo(info)
                .descriptorCount(1)
                .descriptorType(type.id)
                .dstSet(handle)
                .dstBinding(binding)
                .dstArrayElement(resourceId);
        VK13.vkUpdateDescriptorSets(device, writeDescriptorSet, null);
        info.free();
        writeDescriptorSet.free();
    }

    public void bind(int binding, int resourceId, DescriptorType type, ImageInfo buffer) {
        VkWriteDescriptorSet.Buffer writeDescriptorSet = VkWriteDescriptorSet.calloc(1);
        writeDescriptorSet.sType(VK13.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
        VkDescriptorImageInfo.Buffer info = VkDescriptorImageInfo.calloc(1);
        info.put(0, buffer.getHandle());
        writeDescriptorSet.pImageInfo(info)
                .descriptorCount(1)
                .descriptorType(type.id)
                .dstSet(handle)
                .dstBinding(binding)
                .dstArrayElement(resourceId);
        VK13.vkUpdateDescriptorSets(device, writeDescriptorSet, null);
        info.free();
        writeDescriptorSet.free();
    }
}
