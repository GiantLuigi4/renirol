package tfc.renirol.frontend.rendering.resource.texture;

import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.device.ReniQueueType;
import tfc.renirol.frontend.hardware.device.support.image.ReniImageCapabilities;
import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.enums.BufferUsage;
import tfc.renirol.frontend.rendering.enums.ImageLayout;
import tfc.renirol.frontend.rendering.enums.format.BitDepth;
import tfc.renirol.frontend.rendering.enums.format.TextureChannels;
import tfc.renirol.frontend.rendering.enums.format.TextureFormat;
import tfc.renirol.frontend.rendering.enums.masks.StageMask;
import tfc.renirol.frontend.rendering.resource.buffer.GPUBuffer;
import tfc.renirol.frontend.rendering.selectors.ChannelInfo;
import tfc.renirol.frontend.rendering.selectors.FormatSelector;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;

public class Texture {
    public Texture(long surface, ReniLogicalDevice device, TextureFormat format, TextureChannels channels, BitDepth depth, InputStream data) {
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
        create(surface, device);
    }

    private final GPUBuffer data;
    private int width, height, channelCount;
    public final BitDepth bitDepth;
    public final TextureChannels channels;

    private long handle;
    private long view;
    private long sampler;

    public Texture(long surface, ReniLogicalDevice device, TextureFormat format, TextureChannels channels, BitDepth depth, ByteBuffer data) {
        this.channels = channels;
        this.bitDepth = depth;
        switch (format) {
            case PNG, PNG_ALPHA, JPG -> {
                this.data = loadStb(device, format, channels, depth, data);
            }
            default -> throw new RuntimeException("Unsupported texture format " + format.name());
        }
        create(surface, device);
    }

    int[] validFormats() {
        return switch (channels) {
            case RGB -> new int[]{
                    VK13.VK_FORMAT_R8G8B8_SINT,
                    VK13.VK_FORMAT_R8G8B8_UINT,
                    VK13.VK_FORMAT_R8G8B8_SRGB,
                    VK13.VK_FORMAT_R8G8B8_UNORM,
                    VK13.VK_FORMAT_R8G8B8_SNORM,
                    VK13.VK_FORMAT_R8G8B8_USCALED,
                    VK13.VK_FORMAT_R8G8B8_SSCALED,

                    VK13.VK_FORMAT_R16G16B16_SINT,
                    VK13.VK_FORMAT_R16G16B16_UINT,
                    VK13.VK_FORMAT_R16G16B16_UNORM,
                    VK13.VK_FORMAT_R16G16B16_SNORM,
                    VK13.VK_FORMAT_R16G16B16_USCALED,
                    VK13.VK_FORMAT_R16G16B16_SSCALED,
            };
            case RGBA -> new int[]{
                    VK13.VK_FORMAT_R8G8B8A8_SINT,
                    VK13.VK_FORMAT_R8G8B8A8_UINT,
                    VK13.VK_FORMAT_R8G8B8A8_SRGB,
                    VK13.VK_FORMAT_R8G8B8A8_UNORM,
                    VK13.VK_FORMAT_R8G8B8A8_SNORM,
                    VK13.VK_FORMAT_R8G8B8A8_USCALED,
                    VK13.VK_FORMAT_R8G8B8A8_SSCALED,

                    VK13.VK_FORMAT_R16G16B16A16_SINT,
                    VK13.VK_FORMAT_R16G16B16A16_UINT,
                    VK13.VK_FORMAT_R16G16B16A16_UNORM,
                    VK13.VK_FORMAT_R16G16B16A16_SNORM,
                    VK13.VK_FORMAT_R16G16B16A16_USCALED,
                    VK13.VK_FORMAT_R16G16B16A16_SSCALED,
            };

            default -> throw new RuntimeException("NYI!");
        };
    }

    long memory = 0;

    protected void create(long surface, ReniLogicalDevice device) {
        VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc();
        imageInfo.sType(VK13.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
        imageInfo.imageType(VK13.VK_IMAGE_TYPE_2D);
        FormatSelector selector = new FormatSelector();
        int depth = switch (bitDepth) {
            case DEPTH_8 -> 8;
            case DEPTH_16 -> 16;
        };
        switch (channels) {
            case R -> selector.channels(new ChannelInfo('r', depth));
            case RG -> selector.channels(
                    new ChannelInfo('r', depth),
                    new ChannelInfo('g', depth)
            );
            case RGB -> selector.channels(
                    new ChannelInfo('r', depth),
                    new ChannelInfo('g', depth),
                    new ChannelInfo('b', depth)
            );
            case RGBA -> selector.channels(
                    new ChannelInfo('r', depth),
                    new ChannelInfo('g', depth),
                    new ChannelInfo('b', depth),
                    new ChannelInfo('a', depth)
            );
        }
        selector.type("SRGB");
        ReniImageCapabilities capabilities = device.hardware.features.image(surface);
        imageInfo.format(selector.select(capabilities).format());
        capabilities.destroy();
        imageInfo.extent().width(width);
        imageInfo.extent().height(height);
        imageInfo.extent().depth(1);
        imageInfo.mipLevels(1);
        imageInfo.arrayLayers(1);

        imageInfo.initialLayout(VK13.VK_IMAGE_LAYOUT_UNDEFINED);

        imageInfo.usage(VK13.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK13.VK_IMAGE_USAGE_SAMPLED_BIT);
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

        CommandBuffer buffer = CommandBuffer.create(
                device, ReniQueueType.GRAPHICS,
                true, false
        );
        {
            buffer.begin();
            GPUBuffer buffer1 = new GPUBuffer(
                    device,
                    BufferUsage.TRANSFER_DST,
                    memory,
                    memRequirements.size()
            );
            buffer.copy(data, 0, buffer1, 0, data.getSize());
            buffer.end();
            buffer.submit(device.getStandardQueue(ReniQueueType.GRAPHICS));
            buffer1.destroyBuffer();
        }

        VK13.vkBindImageMemory(device.getDirect(VkDevice.class), handle, memory, 0);

        buffer.begin();
        buffer.transition(
                handle,
                StageMask.TRANSFER,
                StageMask.GRAPHICS,
                ImageLayout.UNDEFINED,
                ImageLayout.GENERAL
        );
        buffer.end();
        buffer.submit(device.getStandardQueue(ReniQueueType.GRAPHICS));

        device.waitForIdle();

        buffer.destroy();

        // image view&sampler
        {
            VkSamplerCreateInfo info = VkSamplerCreateInfo.calloc();

            info.sType(VK13.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
            info.magFilter(VK13.VK_FILTER_LINEAR);
            info.minFilter(VK13.VK_FILTER_LINEAR);

            info.addressModeU(VK13.VK_SAMPLER_ADDRESS_MODE_REPEAT);
            info.addressModeV(VK13.VK_SAMPLER_ADDRESS_MODE_REPEAT);
            info.addressModeW(VK13.VK_SAMPLER_ADDRESS_MODE_REPEAT);

            info.maxAnisotropy(1.0f);

            info.borderColor(VK13.VK_BORDER_COLOR_INT_OPAQUE_BLACK);

            info.unnormalizedCoordinates(false);

            info.compareEnable(false);
            info.compareOp(VK13.VK_COMPARE_OP_ALWAYS);

            info.mipmapMode(VK13.VK_SAMPLER_MIPMAP_MODE_NEAREST);

            info.mipLodBias(0.0f);
            info.minLod(0.0f);
            info.maxLod(0.0f);

            sampler = VkUtil.getCheckedLong(buf -> VK13.vkCreateSampler(device.getDirect(VkDevice.class), info, null, buf));

            info.free();
        }
    }

    public long getSampler() {
        return sampler;
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

//        int size = wV * hV;
//        GPUBuffer buffer1 = new GPUBuffer(device, BufferUsage.TRANSFER_SRC, channelCount * depth.size * size);

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
}
