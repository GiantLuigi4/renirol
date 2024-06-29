package tfc.renirol.frontend.rendering.resource.image;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.enums.ImageLayout;
import tfc.renirol.frontend.enums.flags.ImageUsage;
import tfc.renirol.frontend.enums.masks.AccessMask;
import tfc.renirol.frontend.enums.masks.StageMask;
import tfc.renirol.frontend.enums.modes.image.FilterMode;
import tfc.renirol.frontend.enums.modes.image.MipmapMode;
import tfc.renirol.frontend.enums.modes.image.WrapMode;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.device.ReniQueueType;
import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.resource.image.itf.ImageBacked;
import tfc.renirol.frontend.rendering.resource.image.itf.RecreateableImage;
import tfc.renirol.frontend.rendering.resource.image.texture.TextureSampler;
import tfc.renirol.itf.ReniDestructable;
import tfc.renirol.itf.ReniTaggable;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.lwjgl.vulkan.VK10.*;

// TODO:
public class Image implements ReniDestructable, ImageBacked, ReniTaggable<Image>, RecreateableImage {
    final ReniLogicalDevice logical;
    final VkDevice device;

    private long handle;
    private long memory;
    private long view;
    private final VkExtent2D extents = VkExtent2D.calloc();

    public long getView() {
        return view;
    }

    public Image(ReniLogicalDevice device) {
        this.logical = device;
        this.device = device.getDirect(VkDevice.class);
    }

    ImageUsage[] usages = new ImageUsage[]{ImageUsage.COLOR};

    public Image setUsage(ImageUsage usage) {
        this.usages = new ImageUsage[]{usage};
        return this;
    }

    /**
     * Sets the image for a collection of usages
     * The first usage is the primary usage
     *
     * @param usages the collection of usages this image will be used for
     * @return self
     */
    public Image setUsage(ImageUsage... usages) {
        this.usages = Arrays.copyOf(usages, usages.length);
        return this;
    }

    int format;

    public int getFormat() {
        return format;
    }

    public void recreate(int width, int height) {
        destroy();
        create(width, height, format);
    }

    public void create(
            int width, int height,
            // TODO: selector
            int format
    ) {
        extents.set(width, height);

        this.format = format;
        VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc();
        imageInfo.sType(VK13.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
        imageInfo.imageType(VK13.VK_IMAGE_TYPE_2D);
        imageInfo.format(format);
        imageInfo.extent().width(width);
        imageInfo.extent().height(height);
        imageInfo.extent().depth(1);
        imageInfo.mipLevels(1);
        imageInfo.arrayLayers(1);

        imageInfo.initialLayout(VK13.VK_IMAGE_LAYOUT_UNDEFINED);

        int usage = 0;
        int aspect = 0;
        for (ImageUsage swapchainUsage : usages) {
            usage |= swapchainUsage.id;
            aspect |= swapchainUsage.aspect;
        }
        imageInfo.usage(usage);
        imageInfo.sharingMode(VK13.VK_SHARING_MODE_EXCLUSIVE);

        imageInfo.samples(VK13.VK_SAMPLE_COUNT_1_BIT);
        imageInfo.flags(0); // Optional

        handle = VkUtil.getCheckedLong((buf) -> VK13.nvkCreateImage(device, imageInfo.address(), 0, MemoryUtil.memAddress(buf)));
        imageInfo.free();

        // create memory
        VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc();
        VK13.vkGetImageMemoryRequirements(device, handle, memRequirements);

        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc();
        allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
        allocInfo.allocationSize(memRequirements.size());
        allocInfo.memoryTypeIndex(logical.findMemoryType(memRequirements.memoryTypeBits(), VK13.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT));

        memory = VkUtil.getCheckedLong((buf) -> VK13.nvkAllocateMemory(
                device,
                allocInfo.address(), 0,
                MemoryUtil.memAddress(buf)
        ));
        allocInfo.free();
        VK13.vkBindImageMemory(device, handle, memory, 0);

        {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc();
            viewInfo.sType(VK13.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewInfo.image(handle);
            viewInfo.viewType(VK13.VK_IMAGE_VIEW_TYPE_2D);
            viewInfo.format(format);
            viewInfo.subresourceRange().aspectMask(aspect);
            viewInfo.subresourceRange().baseMipLevel(0);
            viewInfo.subresourceRange().levelCount(1);
            viewInfo.subresourceRange().baseArrayLayer(0);
            viewInfo.subresourceRange().layerCount(1);
            view = VkUtil.getCheckedLong(buf -> VK13.nvkCreateImageView(device, viewInfo.address(), 0, MemoryUtil.memAddress(buf)));
            viewInfo.free();
        }

        // TODO: is this necessary?
        CommandBuffer cmd = CommandBuffer.create(
                logical, logical.getQueueFamily(ReniQueueType.GRAPHICS),
                true, false, true
        );
        cmd.begin();
        switch (usages[0]) {
            case COLOR -> {
                cmd.transition(
                        this,
                        StageMask.TOP_OF_PIPE, StageMask.GRAPHICS,
                        ImageLayout.UNDEFINED, ImageLayout.COLOR_ATTACHMENT_OPTIMAL,
                        AccessMask.NONE, AccessMask.COLOR_READ
                );
            }
            case DEPTH -> {
                cmd.transition(
                        this,
                        usages[0],
                        StageMask.TOP_OF_PIPE, StageMask.FRAGMENT_TEST,
                        ImageLayout.UNDEFINED, ImageLayout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL
                );
            }
        }
        cmd.end();
        cmd.submitBlocking(
                logical.getStandardQueue(ReniQueueType.GRAPHICS),
                StageMask.COLOR_ATTACHMENT_OUTPUT
        );
        logical.await();
        cmd.destroy();
    }

    @Override
    public void destroy() {
        VK13.nvkDestroyImage(device, handle, 0);
        VK13.nvkFreeMemory(device, memory, 0);
        VK13.nvkDestroyImageView(device, view, 0);
    }

    public long getHandle() {
        return handle;
    }

    public VkExtent2D getExtents() {
        return extents;
    }

    public TextureSampler createSampler(
            WrapMode xWrap, WrapMode yWrap,
            FilterMode min, FilterMode mag,
            MipmapMode mips,
            boolean useAnisotropy, float anisotropy,
            float lodBias, float minLod, float maxLod
    ) {
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

        long sampler = VkUtil.getCheckedLong(buf -> VK13.vkCreateSampler(device, info, null, buf));

        info.free();

        return new TextureSampler(sampler, device);
    }

    @Override
    public Image setName(String name) {
        VkDebugUtilsObjectNameInfoEXT objectNameInfoEXT = VkDebugUtilsObjectNameInfoEXT.malloc();
        objectNameInfoEXT.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_OBJECT_NAME_INFO_EXT);
        objectNameInfoEXT.objectType(VK_OBJECT_TYPE_IMAGE);
        objectNameInfoEXT.objectHandle(handle);
        objectNameInfoEXT.pNext(0);
        ByteBuffer buf = MemoryUtil.memUTF8(name);
        objectNameInfoEXT.pObjectName(buf);
        EXTDebugUtils.vkSetDebugUtilsObjectNameEXT(device, objectNameInfoEXT);
        MemoryUtil.memFree(buf);
        if (true) {
            buf = MemoryUtil.memUTF8(name + " (view)");
            objectNameInfoEXT.pObjectName(buf);
            objectNameInfoEXT.objectHandle(view);
            objectNameInfoEXT.objectType(VK_OBJECT_TYPE_IMAGE_VIEW);
            EXTDebugUtils.vkSetDebugUtilsObjectNameEXT(device, objectNameInfoEXT);
            MemoryUtil.memFree(buf);
        }
        objectNameInfoEXT.free();
        return this;
    }
}
