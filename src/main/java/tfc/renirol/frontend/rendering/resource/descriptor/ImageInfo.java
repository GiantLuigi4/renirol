package tfc.renirol.frontend.rendering.resource.descriptor;

import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import tfc.renirol.frontend.hardware.util.ReniDestructable;
import tfc.renirol.frontend.rendering.resource.texture.Texture;
import tfc.renirol.frontend.rendering.resource.texture.TextureSampler;

public class ImageInfo implements ReniDestructable {
    VkDescriptorImageInfo info;

    public ImageInfo(Texture texture, TextureSampler sampler) {
        info = VkDescriptorImageInfo.calloc();
        info.imageLayout(VK13.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        info.imageView(texture.getView());
        info.sampler(sampler.handle);
    }

    public VkDescriptorImageInfo getHandle() {
        return info;
    }

    public void destroy() {
        info.free();
    }
}
