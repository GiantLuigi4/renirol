package tfc.renirol.frontend.rendering.command.pipeline;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.rendering.enums.masks.DynamicStateMasks;
import tfc.renirol.frontend.rendering.resource.buffer.BufferDescriptor;
import tfc.renirol.frontend.rendering.resource.descriptor.DescriptorLayout;

import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;

public class PipelineState {
    final VkDevice device;
    VkPipelineDynamicStateCreateInfo dynamic = VkPipelineDynamicStateCreateInfo.calloc();
    VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc();
    VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc();
    VkViewport.Buffer viewport = VkViewport.calloc(1);
    VkRect2D.Buffer scissor = VkRect2D.calloc(1);
    VkOffset2D offset2D = VkOffset2D.calloc();
    VkExtent2D extents = VkExtent2D.calloc();
    VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc();
    VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc();
    VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc();
    VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1);
    VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc();
    VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc();

    public PipelineState(ReniLogicalDevice device) {
        this.device = device.getDirect(VkDevice.class);
        dynamic.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);

//        {
//            IntBuffer buf = MemoryUtil.memAllocInt(dynamicState.length);
//            for (int i = 0; i < dynamicState.length; i++) {
//                buf.put(i, dynamicState[i]);
//            }
//            dynamic.pDynamicStates(buf);
//        }

        // vertex input
        vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
        vertexInputInfo.pVertexBindingDescriptions(null); // Optional
        vertexInputInfo.pVertexAttributeDescriptions(null); // Optional

        // input assembly
        inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
        inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
        inputAssembly.primitiveRestartEnable(false);

        // viewport
        extents.set(1, 1);
        viewport.x(0.0f);
        viewport.y(0.0f);
        viewport.width((float) extents.width());
        viewport.height((float) extents.height());
        viewport.minDepth(0.0f);
        viewport.maxDepth(1.0f);

        // scissor
        scissor.offset(offset2D);
        scissor.extent(extents);

        // viewport state
        viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
        viewportState.viewportCount(1);
        viewportState.pViewports(viewport);
        viewportState.scissorCount(1);
        viewportState.pScissors(scissor);

        // rasterization
        rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
        rasterizer.depthClampEnable(false);

        rasterizer.rasterizerDiscardEnable(false);
        rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
        rasterizer.lineWidth(1.0f);

        rasterizer.cullMode(VK_CULL_MODE_BACK_BIT);
        rasterizer.frontFace(VK_FRONT_FACE_CLOCKWISE);

        rasterizer.depthBiasEnable(false);
        rasterizer.depthBiasConstantFactor(0.0f); // Optional
        rasterizer.depthBiasClamp(0.0f); // Optional
        rasterizer.depthBiasSlopeFactor(0.0f); // Optional

        // msaa
        multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
        multisampling.sampleShadingEnable(false);
        multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);
        multisampling.minSampleShading(1.0f); // Optional
        multisampling.pSampleMask(null); // Optional
        multisampling.alphaToCoverageEnable(false); // Optional
        multisampling.alphaToOneEnable(false); // Optional

        // alpha blending
        colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
        colorBlendAttachment.blendEnable(false);
        colorBlendAttachment.srcColorBlendFactor(VK_BLEND_FACTOR_ONE); // Optional
        colorBlendAttachment.dstColorBlendFactor(VK_BLEND_FACTOR_ZERO); // Optional
        colorBlendAttachment.colorBlendOp(VK_BLEND_OP_ADD); // Optional
        colorBlendAttachment.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE); // Optional
        colorBlendAttachment.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO); // Optional
        colorBlendAttachment.alphaBlendOp(VK_BLEND_OP_ADD); // Optional

        // color blend
        colorBlending.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
        colorBlending.logicOpEnable(false);
        colorBlending.logicOp(VK10.VK_LOGIC_OP_COPY); // Optional
        colorBlending.attachmentCount(1);
        colorBlending.pAttachments(colorBlendAttachment);
        colorBlending.blendConstants().put(0, 0.0f); // Optional
        colorBlending.blendConstants().put(1, 0.0f); // Optional
        colorBlending.blendConstants().put(2, 0.0f); // Optional
        colorBlending.blendConstants().put(3, 0.0f); // Optional

        // https://vulkan-tutorial.com/Drawing_a_triangle/Graphics_pipeline_basics/Fixed_functions

        // layout info
        pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
        pipelineLayoutInfo.setLayoutCount(0); // Optional
        pipelineLayoutInfo.pSetLayouts(null); // Optional
        pipelineLayoutInfo.pPushConstantRanges(null); // Optional
    }

    LongBuffer layoutDescBuffer = MemoryUtil.memAllocLong(1);

    ArrayList<VkPushConstantRange> push_constant = new ArrayList<>();
    VkPushConstantRange.Buffer constBuf;

    public void constantBuffer(int stage, int size) {
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
    }

    public void setLayoutDesc(long desc) {
        layoutDescBuffer.put(0, desc);
        pipelineLayoutInfo.setLayoutCount(1);
        pipelineLayoutInfo.pSetLayouts(layoutDescBuffer);
    }

    public void removeLayoutDesc() {
        pipelineLayoutInfo.setLayoutCount(0);
        pipelineLayoutInfo.pSetLayouts(null);
    }

    public PipelineLayout create() {
        return new PipelineLayout(VkUtil.getCheckedLong(
                (buf) -> nvkCreatePipelineLayout(device, pipelineLayoutInfo.address(), 0, MemoryUtil.memAddress(buf))
        ), device);
    }

    public void dynamicState(DynamicStateMasks... states) {
        // TODO
    }

    VkVertexInputBindingDescription.Buffer bindings = VkVertexInputBindingDescription.malloc(0);
    VkVertexInputAttributeDescription.Buffer attribs = VkVertexInputAttributeDescription.malloc(0);

    public void vertexInput(BufferDescriptor... vbo) {
        MemoryUtil.memFree(bindings);
        bindings = VkVertexInputBindingDescription.malloc(vbo.length);
        int attribCount = 0;
        for (int i = 0; i < vbo.length; i++) {
            bindings.put(i, vbo[i].getBindingDescription());
            attribCount += vbo[i].getAttribs().size();
        }

        attribs = VkVertexInputAttributeDescription.malloc(attribCount);
        int attribIdx = 0;
        for (BufferDescriptor vbo1 : vbo)
            for (VkVertexInputAttributeDescription attrib : vbo1.getAttribs())
                attribs.put(attribIdx++, attrib);

        vertexInputInfo.pVertexBindingDescriptions(bindings);
        vertexInputInfo.pVertexAttributeDescriptions(attribs);
    }

    LongBuffer setBuffer;

    public void descriptorLayouts(DescriptorLayout... layouts) {
        if (setBuffer != null) MemoryUtil.memFree(setBuffer);

        setBuffer = MemoryUtil.memAllocLong(layouts.length);
        for (int i = 0; i < layouts.length; i++)
            setBuffer.put(i, layouts[i].handle);

        pipelineLayoutInfo.setLayoutCount(layouts.length);
        pipelineLayoutInfo.pSetLayouts(setBuffer);
    }
}
