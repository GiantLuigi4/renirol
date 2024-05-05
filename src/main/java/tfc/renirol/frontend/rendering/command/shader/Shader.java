package tfc.renirol.frontend.rendering.command.shader;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.util.CompilationResult;
import tfc.renirol.util.ShaderCompiler;

import java.nio.ByteBuffer;

public class Shader {
    public final VkDevice device;
    public final long module;
    public final int type;

    final CompilationResult result;
    public final VkPipelineShaderStageCreateInfo stage;
    final ByteBuffer nameBuf;

    public Shader(
            ShaderCompiler compiler,

            ReniLogicalDevice device,
            String code,
            int typesc, int typevk,
            String name, String entry
    ) {
        this.device = device.getDirect(VkDevice.class);
        result = compiler.compile(code, typesc, name, entry);

        result.printLog();

        VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc();
        createInfo.sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
        createInfo.pCode(result.getBytes());
        module = VkUtil.getCheckedLong(
                (buf) -> VK10.nvkCreateShaderModule(this.device, createInfo.address(), 0, MemoryUtil.memAddress(buf))
        );
        createInfo.free();

        {
            stage = VkPipelineShaderStageCreateInfo.calloc();
            stage.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            stage.stage(typevk);
            stage.module(module);
            nameBuf = MemoryUtil.memUTF8(entry);
            stage.pName(nameBuf);
        }

        this.type = typevk;
    }

    public Shader(VkPipelineShaderStageCreateInfo stage, VkDevice device, long module, int type) {
        this.device = device;
        this.module = module;
        this.type = type;
        this.stage = stage;
        this.result = null;
        this.nameBuf = null;
    }

    public void destroy() {
        stage.free();
        if (result != null)
            result.free();
        MemoryUtil.memFree(nameBuf);
        VK10.nvkDestroyShaderModule(device, module, 0);
    }
}
