package tfc.renirol.frontend.rendering.command.pipeline;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.rendering.resource.descriptor.DescriptorLayout;
import tfc.renirol.itf.ReniDestructable;

import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.nvkCreatePipelineLayout;

public class ComputePipelineState implements ReniDestructable {
    final VkDevice device;
    VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc();

    // TODO: check
    @Override
    public void destroy() {
        pipelineLayoutInfo.free();
        MemoryUtil.memFree(setBuffer);
        for (VkPushConstantRange vkPushConstantRange : push_constant)
            vkPushConstantRange.free();
        if (constBuf != null)
            constBuf.free();
    }

    public ComputePipelineState(ReniLogicalDevice device) {
        this.device = device.getDirect(VkDevice.class);

        // layout info
        pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
        pipelineLayoutInfo.setLayoutCount(0);
        pipelineLayoutInfo.pSetLayouts(null);
        pipelineLayoutInfo.pPushConstantRanges(null);
    }

    ArrayList<VkPushConstantRange> push_constant = new ArrayList<>();
    VkPushConstantRange.Buffer constBuf;

    public ComputePipelineState constantBuffer(int stage, int size) {
        VkPushConstantRange push_constant;
        this.push_constant.add(push_constant = VkPushConstantRange.calloc());
        push_constant.offset(0);
        push_constant.size(size);
        push_constant.stageFlags(stage);

        if (constBuf != null) MemoryUtil.memFree(constBuf);
        constBuf = VkPushConstantRange.malloc(this.push_constant.size());
        for (int i = 0; i < this.push_constant.size(); i++) {
            constBuf.put(i, this.push_constant.get(i));
        }
        pipelineLayoutInfo.pPushConstantRanges(constBuf);
        return this;
    }

    LongBuffer setBuffer;

    public ComputePipelineState descriptorLayouts(DescriptorLayout... layouts) {
        if (setBuffer != null) MemoryUtil.memFree(setBuffer);

        setBuffer = MemoryUtil.memAllocLong(layouts.length);
        for (int i = 0; i < layouts.length; i++)
            setBuffer.put(i, layouts[i].handle);

        pipelineLayoutInfo.setLayoutCount(layouts.length);
        pipelineLayoutInfo.pSetLayouts(setBuffer);
        return this;
    }

    public PipelineLayout create() {
        return new PipelineLayout(VkUtil.getCheckedLong(
                (buf) -> nvkCreatePipelineLayout(device, pipelineLayoutInfo.address(), 0, MemoryUtil.memAddress(buf))
        ), device);
    }
}
