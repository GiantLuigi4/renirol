package tfc.renirol.frontend.rendering.command.pipeline;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.rendering.command.shader.Shader;
import tfc.renirol.frontend.rendering.pass.RenderPass;

public class GraphicsPipeline {
    public GraphicsPipeline(PipelineState layout, RenderPass pass, Shader... shaders) {
        this(layout, pass.handle, shaders);
    }

    public final long handle;
    public final VkDevice device;
    public final PipelineLayout layout;

    public GraphicsPipeline(long handle, VkDevice device, long layout) {
        this.handle = handle;
        this.device = device;
        this.layout = new PipelineLayout(layout, device);
    }

    public GraphicsPipeline(PipelineState layout, long pass, Shader... shaders) {
        this.layout = layout.create();

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

        pipelineInfo.layout(this.layout.handle);

        pipelineInfo.renderPass(pass);
        pipelineInfo.subpass(0);

        // TODO: likely important to expose!
        pipelineInfo.basePipelineHandle(0); // Optional
        pipelineInfo.basePipelineIndex(-1); // Optional

        this.device = layout.device;
        this.handle = VkUtil.getCheckedLong(
                (buf) -> VK10.nvkCreateGraphicsPipelines(device, 0, 1, pipelineInfo.address(), 0, MemoryUtil.memAddress(buf))
        );

        buffer.free();
        pipelineInfo.free();
    }

    public void destroy() {
        VK10.nvkDestroyPipeline(device, handle, 0);
        layout.destroy();
    }
}
