package tfc.renirol.frontend.rendering.resource.image.texture;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.enums.BufferUsage;
import tfc.renirol.frontend.enums.ImageLayout;
import tfc.renirol.frontend.enums.format.BitDepth;
import tfc.renirol.frontend.enums.format.TextureChannels;
import tfc.renirol.frontend.enums.format.TextureFormat;
import tfc.renirol.frontend.enums.masks.AccessMask;
import tfc.renirol.frontend.enums.masks.StageMask;
import tfc.renirol.frontend.enums.modes.image.FilterMode;
import tfc.renirol.frontend.enums.modes.image.MipmapMode;
import tfc.renirol.frontend.enums.modes.image.WrapMode;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.device.ReniQueueType;
import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.resource.buffer.GPUBuffer;
import tfc.renirol.frontend.rendering.resource.image.itf.ImageBacked;
import tfc.renirol.itf.ReniDestructable;
import tfc.renirol.itf.ReniTaggable;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.vulkan.VK10.*;

// TODO: extend from image instead of using itf
public class Texture implements ReniDestructable, ImageBacked, ReniTaggable<Texture> {
    final VkDevice device;

    public Texture(ReniLogicalDevice device, TextureFormat format, TextureChannels channels, BitDepth depth, InputStream data) {
        this.device = device.getDirect(VkDevice.class);

        this.channels = channels;
        this.bitDepth = depth;
        ByteBuffer buffer = VkUtil.read(data);
        switch (format) {
            case PNG, PNG_ALPHA, JPG -> {
                this.data = loadStb(device, format, channels, depth, buffer);
            }
            default -> throw new RuntimeException("Unsupported texture format " + format.name());
        }
        MemoryUtil.memFree(buffer);
        create(device, null);
    }

    private final GPUBuffer data;
    private int width, height, channelCount;
    public final BitDepth bitDepth;
    public final TextureChannels channels;

    private long handle;
    private long view;

    public Texture(ReniLogicalDevice device, TextureFormat format, TextureChannels channels, BitDepth depth, ByteBuffer data) {
        this.device = device.getDirect(VkDevice.class);

        this.channels = channels;
        this.bitDepth = depth;
        switch (format) {
            case PNG, PNG_ALPHA, JPG -> {
                this.data = loadStb(device, format, channels, depth, data);
            }
            default -> throw new RuntimeException("Unsupported texture format " + format.name());
        }
        create(device, null);
    }

    public Texture(ReniLogicalDevice device, int width, int height, TextureChannels channels, BitDepth depth, ByteBuffer data) {
        this.device = device.getDirect(VkDevice.class);

        this.channels = channels;
        this.bitDepth = depth;
        this.data = loadRaw(device, width, height, channels, depth, data);
        create(device, null);
    }

    public Texture(ReniLogicalDevice device, int width, int height, TextureChannels channels, BitDepth depth, ByteBuffer data, ImageLayout layout) {
        this.device = device.getDirect(VkDevice.class);

        this.channels = channels;
        this.bitDepth = depth;
        this.data = loadRaw(device, width, height, channels, depth, data);
        create(device, layout);
    }

    long memory = 0;

    int format;

    @Override
    public int getFormat() {
        return format;
    }

    protected void create(ReniLogicalDevice device, ImageLayout layout) {
        VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc();
        imageInfo.sType(VK13.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
        imageInfo.imageType(VK13.VK_IMAGE_TYPE_2D);
        imageInfo.format(format = switch (channelCount) {
            case 1 -> VK13.VK_FORMAT_R8_SRGB;
            case 4 -> VK13.VK_FORMAT_R8G8B8A8_SRGB;
            default -> throw new RuntimeException("NYI");
        });
        imageInfo.extent().width(width);
        imageInfo.extent().height(height);
        imageInfo.extent().depth(1);
        imageInfo.mipLevels(1);
        imageInfo.arrayLayers(1);

        imageInfo.initialLayout(VK13.VK_IMAGE_LAYOUT_UNDEFINED);

        imageInfo.usage(
                VK13.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK13.VK_IMAGE_USAGE_SAMPLED_BIT |
                        (
                                layout == null ? 0 :
                                        switch (layout) {
                                            case TRANSFER_SRC_OPTIMAL -> VK13.VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
                                            default -> 0;
                                        }
                        )
        );
        imageInfo.sharingMode(VK13.VK_SHARING_MODE_EXCLUSIVE);

        imageInfo.samples(VK13.VK_SAMPLE_COUNT_1_BIT);
        imageInfo.flags(0); // Optional

        handle = VkUtil.getCheckedLong((buf) -> VK13.nvkCreateImage(device.getDirect(VkDevice.class), imageInfo.address(), 0, MemoryUtil.memAddress(buf)));
        imageInfo.free();

        VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc();
        VK13.vkGetImageMemoryRequirements(device.getDirect(VkDevice.class), handle, memRequirements);

        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc();
        allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
        allocInfo.allocationSize(memRequirements.size());
        allocInfo.memoryTypeIndex(device.findMemoryType(memRequirements.memoryTypeBits(), VK13.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT));

        memory = VkUtil.getCheckedLong((buf) -> VK13.nvkAllocateMemory(
                device.getDirect(VkDevice.class),
                allocInfo.address(), 0,
                MemoryUtil.memAddress(buf)
        ));
        allocInfo.free();

        CommandBuffer buffer = CommandBuffer.create(
                device, device.getQueueFamily(ReniQueueType.GRAPHICS),
                true, false,
                false
        );
        VK13.vkBindImageMemory(device.getDirect(VkDevice.class), handle, memory, 0);

        {
            VkBufferImageCopy.Buffer inf = VkBufferImageCopy.calloc(1);
            VkExtent3D extent2D = VkExtent3D.calloc();
            extent2D.set(width, height, 1);
            inf.bufferImageHeight(height);
            inf.bufferRowLength(width);
            inf.imageExtent(extent2D);
            inf.imageSubresource().layerCount(1);
            inf.imageSubresource().aspectMask(VK13.VK_IMAGE_ASPECT_COLOR_BIT);

            buffer.begin();
            buffer.startLabel("upload", 0, 0, 0.5f, 0.5f);
            buffer.transition(
                    this, StageMask.TOP_OF_PIPE, StageMask.TRANSFER,
                    ImageLayout.UNDEFINED, ImageLayout.TRANSFER_DST_OPTIMAL,
                    AccessMask.NONE, AccessMask.TRANSFER_WRITE
            );
            VK13.vkCmdCopyBufferToImage(
                    buffer.getDirect(VkCommandBuffer.class),
                    data.getHandle(),
                    handle, VK13.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    inf
            );
            buffer.transition(
                    this, StageMask.TRANSFER, StageMask.GRAPHICS, // TODO: more optimal stage?
                    ImageLayout.TRANSFER_DST_OPTIMAL, layout == null ? ImageLayout.SHADER_READONLY : layout,
                    AccessMask.TRANSFER_WRITE, AccessMask.COLOR_READ
            );
            buffer.endLabel();
            buffer.end();
            buffer.submitBlocking(
                    device.getStandardQueue(ReniQueueType.GRAPHICS),
                    StageMask.COLOR_ATTACHMENT_OUTPUT
            );

            extent2D.free();
            inf.free();

            // must wait for idle to destroy buffer
            device.await();
            buffer.destroy();

            data.destroy();
        }

        // image view
        {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc();
            viewInfo.sType(VK13.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewInfo.image(handle);
            viewInfo.viewType(VK13.VK_IMAGE_VIEW_TYPE_2D);
            viewInfo.format(format);
            viewInfo.subresourceRange().aspectMask(VK13.VK_IMAGE_ASPECT_COLOR_BIT);
            viewInfo.subresourceRange().baseMipLevel(0);
            viewInfo.subresourceRange().levelCount(1);
            viewInfo.subresourceRange().baseArrayLayer(0);
            viewInfo.subresourceRange().layerCount(1);
            view = VkUtil.getCheckedLong(buf -> VK13.nvkCreateImageView(device.getDirect(VkDevice.class), viewInfo.address(), 0, MemoryUtil.memAddress(buf)));
            viewInfo.free();
        }
    }

    public TextureSampler createSampler(
            WrapMode xWrap,
            WrapMode yWrap,
            FilterMode min,
            FilterMode mag,
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

    protected GPUBuffer loadRaw(ReniLogicalDevice device, int width, int height, TextureChannels channels, BitDepth depth, ByteBuffer data) {
        channelCount = channels.count;

        GPUBuffer buffer1 = new GPUBuffer(device, BufferUsage.TRANSFER_SRC, data.capacity());
        buffer1.allocate();
        buffer1.upload(0, data);
        this.width = width;
        this.height = height;

        return buffer1;
    }

    protected GPUBuffer loadStb(ReniLogicalDevice device, TextureFormat format, TextureChannels channels, BitDepth depth, ByteBuffer data) {
        IntBuffer width = MemoryUtil.memAllocInt(1);
        IntBuffer height = MemoryUtil.memAllocInt(1);

        IntBuffer pChannels = MemoryUtil.memAllocInt(1);
        int desiredChannels = channels.stbi;

        ByteBuffer buffer;
        switch (depth) {
            case DEPTH_8 -> buffer = STBImage.stbi_load_from_memory(
                    data, width, height, pChannels, desiredChannels
            );
            case DEPTH_16 -> {
                ShortBuffer shorts = STBImage.stbi_load_16_from_memory(
                        data, width, height, pChannels, desiredChannels
                );
                buffer = MemoryUtil.memByteBuffer(MemoryUtil.memAddress(shorts), shorts.capacity() * 2);
            }
            default -> throw new RuntimeException("Invalid bit depth for " + format.name());
        }
        channelCount = pChannels.get(0);

        int wV = width.get(0);
        int hV = height.get(0);
        this.width = wV;
        this.height = hV;
        MemoryUtil.memFree(width);
        MemoryUtil.memFree(height);
        MemoryUtil.memFree(pChannels);

        GPUBuffer buffer1 = new GPUBuffer(device, BufferUsage.TRANSFER_SRC, buffer.capacity());
        buffer1.allocate();
        buffer1.upload(0, buffer);

        MemoryUtil.memFree(buffer);

        return buffer1;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getHandle() {
        return handle;
    }

    public long getView() {
        return view;
    }

    @Override
    public void destroy() {
        VK13.nvkDestroyImageView(device, view, 0);
        VK13.nvkDestroyImage(device, handle, 0);
        VK13.nvkFreeMemory(device, memory, 0);
    }

    @Override
    public Texture setName(String name) {
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
        if (true) {
            buf = MemoryUtil.memUTF8(name + " (memory)");
            objectNameInfoEXT.pObjectName(buf);
            objectNameInfoEXT.objectHandle(memory);
            objectNameInfoEXT.objectType(VK_OBJECT_TYPE_DEVICE_MEMORY);
            EXTDebugUtils.vkSetDebugUtilsObjectNameEXT(device, objectNameInfoEXT);
            MemoryUtil.memFree(buf);
        }
        objectNameInfoEXT.free();
        return this;
    }
}
