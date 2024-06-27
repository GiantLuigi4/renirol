package tfc.renirol.frontend.rendering.command.pipeline;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkComputePipelineCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.shader.Shader;
import tfc.renirol.itf.ReniDestructable;

public class ComputePipeline implements ReniDestructable {
    public final long handle;
    private final VkDevice device;
    private final PipelineLayout layout;

    public ComputePipeline(
            ComputePipelineState state,
            ReniLogicalDevice logical,
            Shader shader
    ) {
        layout = state.create();
        this.device = logical.hardware.getDirect(VkDevice.class);
        VkComputePipelineCreateInfo.Buffer pipelineInfo = VkComputePipelineCreateInfo.calloc(1);
        pipelineInfo.sType(VK13.VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO);
        pipelineInfo.layout(layout.handle);
        pipelineInfo.stage(shader.stage);

        handle = VkUtil.getCheckedLong(buf ->
                VK13.nvkCreateComputePipelines(
                        device, 0,
                        1, pipelineInfo.address(),
                        0, MemoryUtil.memAddress(buf)
                )
        );
        pipelineInfo.free();
    }

    @Override
    public void destroy() {
        layout.destroy();
        VK13.nvkDestroyPipeline(device, handle, 0);
    }

    public void bind(CommandBuffer buffer) {
        buffer.bindCompute(this);
    }
}
