package tfc.renirol.frontend.rendering.resource.image;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.device.ReniQueueType;
import tfc.renirol.frontend.hardware.util.ReniDestructable;
import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.enums.ImageLayout;
import tfc.renirol.frontend.rendering.enums.flags.SwapchainUsage;
import tfc.renirol.frontend.rendering.enums.format.TextureFormat;
import tfc.renirol.frontend.rendering.enums.masks.StageMask;
import tfc.renirol.frontend.rendering.framebuffer.SwapChain;
import tfc.renirol.frontend.rendering.selectors.FormatSelector;

import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.nvkCreateImage;

// TODO:
public class Image implements ReniDestructable {
    final ReniLogicalDevice logical;
    final VkDevice device;

    private long handle;
    private long memory;
    private long view;

    public long getView() {
        return view;
    }

    public Image(ReniLogicalDevice device) {
        this.logical = device;
        this.device = device.getDirect(VkDevice.class);
    }

    SwapchainUsage usage = SwapchainUsage.COLOR;

    public Image setUsage(SwapchainUsage usage) {
        this.usage = usage;
        return this;
    }

    int format;

    public void recreate(int width, int height) {
        destroy();
        create(width, height, format);
    }

    public void create(
            int width, int height,
            // TODO: selector
            int format
    ) {
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

//        imageInfo.usage(VK13.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK13.VK_IMAGE_USAGE_SAMPLED_BIT);
        imageInfo.usage(VK13.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
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
            viewInfo.subresourceRange().aspectMask(usage.aspect);
            viewInfo.subresourceRange().baseMipLevel(0);
            viewInfo.subresourceRange().levelCount(1);
            viewInfo.subresourceRange().baseArrayLayer(0);
            viewInfo.subresourceRange().layerCount(1);
            view = VkUtil.getCheckedLong(buf -> VK13.nvkCreateImageView(device, viewInfo.address(), 0, MemoryUtil.memAddress(buf)));
            viewInfo.free();
        }

        CommandBuffer cmd = CommandBuffer.create(
                logical, ReniQueueType.GRAPHICS,
                true, false
        );
        cmd.begin();
        switch (usage) {
            case COLOR -> {
                cmd.transition(
                        handle, StageMask.GRAPHICS, StageMask.GRAPHICS,
                        ImageLayout.UNDEFINED, ImageLayout.SHADER_READONLY
                );
            }
            case DEPTH -> {
                cmd.transition(
                        handle,
                        usage,
                        StageMask.TOP_OF_PIPE, StageMask.EARLY_FRAGMENT_TEST,
                        ImageLayout.UNDEFINED, ImageLayout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL
                );
            }
        }
        cmd.end();
        cmd.submit(logical.getStandardQueue(ReniQueueType.GRAPHICS));
        logical.waitForIdle();
        cmd.destroy();
    }

    @Override
    public void destroy() {
        VK13.nvkDestroyImage(device, handle, 0);
        VK13.nvkFreeMemory(device, memory, 0);
        VK13.nvkDestroyImageView(device, view, 0);
    }
}
