package tfc.renirol.frontend.rendering.resource.descriptor;

import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import tfc.renirol.frontend.rendering.resource.texture.Texture;

public class ImageInfo {
    VkDescriptorImageInfo info;

    public ImageInfo(Texture texture) {
        info = VkDescriptorImageInfo.calloc();
        info.imageLayout(VK13.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        info.imageView(texture.getView());
        info.sampler(texture.getSampler());
    }

    public VkDescriptorImageInfo getHandle() {
        return info;
    }
}
