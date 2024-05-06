package tfc.renirol.frontend.rendering.framebuffer;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.device.ReniQueueType;
import tfc.renirol.frontend.hardware.device.support.image.ReniSwapchainCapabilities;
import tfc.renirol.frontend.hardware.util.ReniDestructable;
import tfc.renirol.frontend.rendering.enums.flags.SwapchainUsage;
import tfc.renirol.frontend.rendering.pass.RenderPass;
import tfc.renirol.frontend.rendering.selectors.FormatSelector;
import tfc.renirol.util.Pair;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

public class SwapChain implements ReniDestructable {
    long swapChain;

    public long getId() {
        return swapChain;
    }

    List<FrameBuffer> buffers = new ArrayList<>();

    ReniLogicalDevice device;
    long surface;
    int frameCount;
    int presentMode;

    public SwapChain(ReniLogicalDevice device, long surface) {
        this.device = device;
        this.surface = surface;
    }

    VkSurfaceFormatKHR format;
    VkExtent2D extents;
    boolean initialized = false;
    SwapchainUsage usage = SwapchainUsage.COLOR;

    public SwapChain setUsage(SwapchainUsage usage) {
        this.usage = usage;
        return this;
    }

    public void create(
            int width, int height,
            FormatSelector selector,
            int preferredFrameCount,
            int presentMode
    ) {
        if (initialized) destroy();

        ReniSwapchainCapabilities image = device.hardware.features.image(surface);

        // create swapchain object
        {
            Pair<Integer, Integer> min = image.getMinSize();
            Pair<Integer, Integer> max = image.getMaxSize();

            width = Math.clamp(width, min.left(), max.left());
            height = Math.clamp(height, min.right(), max.right());

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc();
            createInfo.sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(surface);
            createInfo.minImageCount(preferredFrameCount);

            VkExtent2D extent = VkExtent2D.calloc();
            VkSurfaceFormatKHR format = selector.select(image);
            extent.set(width, height);
            this.extents = extent;
            this.format = format;

            createInfo.imageFormat(format.format());
            createInfo.imageColorSpace(format.colorSpace());
            createInfo.imageExtent(extent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(usage.id);

            // sharing
            int graphics, transfer;
            graphics = device.getQueueFamily(ReniQueueType.GRAPHICS);
            transfer = device.getQueueFamily(ReniQueueType.TRANSFER);

            IntBuffer indicesBuf = null;

            if (graphics != transfer) {
                createInfo.imageSharingMode(VK13.VK_SHARING_MODE_CONCURRENT);
                createInfo.queueFamilyIndexCount(2);
                indicesBuf = MemoryUtil.memAllocInt(2);
                indicesBuf.put(0, graphics);
                indicesBuf.put(0, transfer);
                createInfo.pQueueFamilyIndices(indicesBuf);
            } else {
                createInfo.imageSharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);
                createInfo.queueFamilyIndexCount(0);
                createInfo.pQueueFamilyIndices(null);
            }

            // things
            createInfo.preTransform(image.surfaceCapabilities.currentTransform());
            createInfo.compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);
            createInfo.oldSwapchain(0);
            createInfo.surface(surface);

            try {
                swapChain = VkUtil.getCheckedLong( // complains at this point
                        (buf) -> KHRSwapchain.nvkCreateSwapchainKHR(device.getDirect(VkDevice.class), createInfo.address(), 0, MemoryUtil.memAddress(buf))
                );
            } catch (Throwable err) {
                err.printStackTrace();
                String name;

                name = VkUtil.find(VK13.class, "VK_FORMAT", format.format());
                if (name == null) name = String.valueOf(format.format());
                System.out.println("Selected format: " + name);

                name = VkUtil.find(KHRSurface.class, "VK_COLOR_SPACE", format.colorSpace());
                if (name == null) name = String.valueOf(format.colorSpace());
                System.out.println("Selected color space: " + name);

                name = VkUtil.find(KHRSurface.class, "VK_PRESENT_MODE", presentMode);
                if (name == null) name = String.valueOf(presentMode);
                System.out.println("Selected transfer mode: " + name);

                if (graphics != transfer)
                    MemoryUtil.memFree(indicesBuf);
                createInfo.free();
                image.destroy();

                throw err;
            }

            image.destroy();
            if (graphics != transfer)
                MemoryUtil.memFree(indicesBuf);
            createInfo.free();
        }

        // create image views
        {
            IntBuffer count = MemoryUtil.memAllocInt(1);
            KHRSwapchain.vkGetSwapchainImagesKHR(device.getDirect(VkDevice.class), swapChain, count, null);
            LongBuffer buffer = MemoryUtil.memAllocLong(count.get(0));
            KHRSwapchain.vkGetSwapchainImagesKHR(device.getDirect(VkDevice.class), swapChain, count, buffer);
            LongBuffer buf = MemoryUtil.memAllocLong(1);

            // image view info
            VkImageViewCreateInfo viewCI = VkImageViewCreateInfo.calloc();
            viewCI.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewCI.format(format.format());
            viewCI.viewType(VK10.VK_IMAGE_VIEW_TYPE_2D);
            // color components
            viewCI.components().r(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
            viewCI.components().g(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
            viewCI.components().b(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
            viewCI.components().a(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
            // subresource stuff
            viewCI.subresourceRange().aspectMask(usage.aspect);
            viewCI.subresourceRange().baseMipLevel(0);
            viewCI.subresourceRange().levelCount(1);
            viewCI.subresourceRange().baseArrayLayer(0);
            viewCI.subresourceRange().layerCount(1);

            long addr = MemoryUtil.memAddress(buf);

            for (int i1 = 0; i1 < count.get(0); i1++) {
                long img = buffer.get(i1);

                // create image view
                viewCI.image(img);
                VkUtil.check(VK10.nvkCreateImageView(device.getDirect(VkDevice.class), viewCI.address(), 0, addr));
                buffers.add(new FrameBuffer(device.getDirect(VkDevice.class), img, buf.get(0), this));
            }

            MemoryUtil.memFree(buffer);
            MemoryUtil.memFree(buf);
            MemoryUtil.memFree(count);
        }
        this.frameCount = preferredFrameCount;
        this.presentMode = presentMode;

        initialized = true;
    }

    @Override
    public void destroy() {
        if (initialized) {
            initialized = false;
            for (FrameBuffer buffer : buffers)
                buffer.destroy();
            buffers.clear();
            extents.free();
            KHRSwapchain.nvkDestroySwapchainKHR(device.getDirect(VkDevice.class), swapChain, 0);
        }
    }

    public int acquire(IntBuffer index, long semaphore) {
        return KHRSwapchain.nvkAcquireNextImageKHR(
                device.getDirect(VkDevice.class),
                swapChain, Long.MAX_VALUE,
                semaphore, 0,
                MemoryUtil.memAddress(index)
        );
    }

    public VkExtent2D getExtents() {
        return extents;
    }

    public long getHandle(int index, RenderPass pass) {
        return buffers.get(index).forPass(pass);
    }

    public void recreate(int width, int height) {
        destroy();
        create(
                width, height,
                new FormatSelector() {
                    @Override
                    public VkSurfaceFormatKHR select(ReniSwapchainCapabilities image) {
                        return format;
                    }
                }, frameCount,
                presentMode
        );
    }

    public FrameBuffer getFbo(int frameIndex) {
        return buffers.get(frameIndex);
    }
}
