package tfc.renirol.frontend.rendering.resource.descriptor;

import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import tfc.renirol.frontend.enums.DescriptorType;
import tfc.renirol.frontend.enums.flags.DescriptorBindingFlags;
import tfc.renirol.frontend.enums.flags.ShaderStageFlags;
import tfc.renirol.itf.ReniDestructable;

public class DescriptorLayoutInfo implements ReniDestructable {
    VkDescriptorSetLayoutBinding binding = VkDescriptorSetLayoutBinding.calloc();

    public DescriptorLayoutInfo(
            int id,
            DescriptorType type,
            int count,
            ShaderStageFlags... stages
    ) {
        int flags = 0;
        for (ShaderStageFlags stage : stages) flags |= stage.bits;

        binding.binding(id)
                .descriptorCount(count)
                .descriptorType(type.id)
                .stageFlags(flags)
        ;
    }

    int flags = 0;

    public DescriptorLayoutInfo flags(DescriptorBindingFlags... flags) {
        this.flags = 0;
        for (DescriptorBindingFlags flag : flags) this.flags |= flag.bits;
        return this;
    }

    public void destroy() {
        binding.free();
    }
}
