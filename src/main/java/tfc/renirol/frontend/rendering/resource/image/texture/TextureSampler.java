package tfc.renirol.frontend.rendering.resource.image.texture;

import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.enums.modes.image.FilterMode;
import tfc.renirol.frontend.enums.modes.image.MipmapMode;
import tfc.renirol.frontend.enums.modes.image.WrapMode;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.itf.ReniDestructable;

public class TextureSampler implements ReniDestructable {
    public final long handle;
    private final VkDevice device;

    @Deprecated(forRemoval = true)
    public TextureSampler(long handle, VkDevice device) {
        this.handle = handle;
        this.device = device;
    }

    @Override
    public void destroy() {
        VK13.nvkDestroySampler(device, handle, 0);
    }

    public static TextureSampler createSampler(
            ReniLogicalDevice device,
            WrapMode xWrap, WrapMode yWrap,
            FilterMode min, FilterMode mag,
            MipmapMode mips,
            boolean useAnisotropy, float anisotropy,
            float lodBias, float minLod, float maxLod
    ) {
        VkDevice direct = device.getDirect(VkDevice.class);

        VkSamplerCreateInfo info = VkSamplerCreateInfo.calloc();

        info.sType(VK13.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
        info.magFilter(mag.id);
        info.minFilter(min.id);

        info.addressModeU(xWrap.id);
        info.addressModeV(yWrap.id);
        info.addressModeW(xWrap.id);

        info.anisotropyEnable(useAnisotropy);
        info.maxAnisotropy(anisotropy);

        info.borderColor(VK13.VK_BORDER_COLOR_INT_OPAQUE_BLACK);

        info.unnormalizedCoordinates(false);

        info.compareEnable(false);
        info.compareOp(VK13.VK_COMPARE_OP_ALWAYS);

        info.mipmapMode(mips.id);

        info.mipLodBias(lodBias);
        info.minLod(minLod);
        info.maxLod(maxLod);

        long sampler = VkUtil.getCheckedLong(buf -> VK13.vkCreateSampler(direct, info, null, buf));

        info.free();

        return new TextureSampler(sampler, direct);
    }
}
