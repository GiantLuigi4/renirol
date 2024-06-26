package tfc.renirol.frontend.rendering.command.pipeline;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.enums.modes.CullMode;
import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.shader.Shader;
import tfc.renirol.frontend.rendering.pass.RenderPassInfo;
import tfc.renirol.frontend.rendering.pass.ReniPassAttachment;
import tfc.renirol.itf.ReniDestructable;

import java.nio.IntBuffer;
import java.util.List;

public class GraphicsPipeline implements ReniDestructable {
    public final long handle;
    public final VkDevice device;
    public final PipelineLayout layout;
    public final CullMode cullFace;

    public GraphicsPipeline(long handle, VkDevice device, long layout, CullMode cullFace) {
        this.handle = handle;
        this.device = device;
        this.layout = new PipelineLayout(layout, device);
        this.cullFace = cullFace;
    }

    public GraphicsPipeline(RenderPassInfo info, PipelineState layout, Shader... shaders) {
        this.layout = layout.create();
        this.cullFace = layout.cullface;

        VkGraphicsPipelineCreateInfo pipelineInfo = VkGraphicsPipelineCreateInfo.calloc();

        VkPipelineShaderStageCreateInfo.Buffer buffer = VkPipelineShaderStageCreateInfo.malloc(shaders.length);
        for (int i = 0; i < shaders.length; i++) buffer.put(i, shaders[i].stage);

        pipelineInfo.sType(VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
        pipelineInfo.stageCount(2);
        pipelineInfo.pStages(buffer);

        pipelineInfo.pVertexInputState(layout.vertexInputInfo);
        pipelineInfo.pInputAssemblyState(layout.inputAssembly);
        pipelineInfo.pViewportState(layout.viewportState);
        pipelineInfo.pRasterizationState(layout.rasterizer);
        pipelineInfo.pMultisampleState(layout.multisampling);
        pipelineInfo.pDepthStencilState(null); // Optional
        pipelineInfo.pColorBlendState(layout.colorBlending);
        pipelineInfo.pDynamicState(layout.dynamic);
        pipelineInfo.pDepthStencilState(layout.depthStencil);
        pipelineInfo.pTessellationState(layout.tessellationState);

        pipelineInfo.layout(this.layout.handle);

        VkPipelineRenderingCreateInfo renderingCreateInfo;
        IntBuffer colorAttachments;
        {
            List<ReniPassAttachment> attachments = info.getColorAttachments();

            renderingCreateInfo = VkPipelineRenderingCreateInfo.calloc().sType$Default();

            renderingCreateInfo.colorAttachmentCount(attachments.size());
            colorAttachments = MemoryUtil.memAllocInt(renderingCreateInfo.colorAttachmentCount());

            for (int i = 0; i < attachments.size(); i++)
                colorAttachments.put(i, attachments.get(i).format);

            renderingCreateInfo.pColorAttachmentFormats(colorAttachments);

            for (ReniPassAttachment depthAttachment : info.getDepthAttachments()) {
                renderingCreateInfo.depthAttachmentFormat(depthAttachment.format);
            }

            pipelineInfo.pNext(renderingCreateInfo);
        }

        // TODO: likely important to expose!
        pipelineInfo.basePipelineHandle(0); // Optional
        pipelineInfo.basePipelineIndex(-1); // Optional

        this.device = layout.device;
        this.handle = VkUtil.getCheckedLong(
                (buf) -> VK10.nvkCreateGraphicsPipelines(device, 0, 1, pipelineInfo.address(), 0, MemoryUtil.memAddress(buf))
        );

        buffer.free();
        pipelineInfo.free();
        renderingCreateInfo.free();
        MemoryUtil.memFree(colorAttachments);
    }

    public void destroy() {
        VK10.nvkDestroyPipeline(device, handle, 0);
        layout.destroy();
    }

    public void bind(CommandBuffer buffer) {
        buffer.bindPipe(this);
    }
}
