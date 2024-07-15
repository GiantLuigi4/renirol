package tfc.renirol.frontend.rendering.command.barrier;

import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import tfc.renirol.frontend.enums.ImageLayout;
import tfc.renirol.frontend.enums.flags.ImageUsage;
import tfc.renirol.frontend.enums.masks.AccessMask;
import tfc.renirol.frontend.enums.masks.StageMask;
import tfc.renirol.frontend.hardware.device.queue.ReniQueueFamily;
import tfc.renirol.frontend.rendering.resource.image.itf.ImageBacked;
import tfc.renirol.itf.ReniDestructable;

public class ImageBarrier implements ReniDestructable {
    VkImageMemoryBarrier barrier = VkImageMemoryBarrier.calloc();
    ImageBacked img;
    protected StageMask from;
    protected StageMask to;

    public ImageBarrier(
            ImageBacked img,
            ImageUsage usage,
            StageMask stage,
            AccessMask accessMask,
            ImageLayout layout,
            ReniQueueFamily queueFamily
    ) {
        this.from = stage;
        this.img = img;
        barrier.image(img.getHandle()).sType$Default();
        int family = queueFamily == null ? VK13.VK_QUEUE_FAMILY_IGNORED : queueFamily.family;
        barrier.oldLayout(layout.value).newLayout(layout.value)
                .srcQueueFamilyIndex(family).dstQueueFamilyIndex(family)
                .srcAccessMask(accessMask.value).dstAccessMask(accessMask.value)
        ;
        barrier.subresourceRange()
                .aspectMask(usage.aspect)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1)
        ;
    }

    public void swap() {
        barrier.srcAccessMask(barrier.dstAccessMask());
        barrier.srcQueueFamilyIndex(barrier.dstQueueFamilyIndex());
        barrier.oldLayout(barrier.newLayout());
        setSrcStage(getDstSage());
    }

    public ImageBarrier dst(
            ImageUsage usage,
            StageMask stage,
            AccessMask accessMask,
            ImageLayout layout,
            ReniQueueFamily queueFamily
    ) {
        int family = queueFamily == null ? VK13.VK_QUEUE_FAMILY_IGNORED : queueFamily.family;
        barrier.newLayout(layout.value)
                .dstQueueFamilyIndex(family)
                .dstAccessMask(accessMask.value);
        barrier.subresourceRange().aspectMask(usage.aspect);
        to = stage;
        return this;
    }

    public ImageBarrier stage(StageMask stage) {
        to = stage;
        return this;
    }

    public ImageBarrier queueFamily(ReniQueueFamily queueFamily) {
        barrier.dstQueueFamilyIndex(queueFamily == null ? VK13.VK_QUEUE_FAMILY_IGNORED : queueFamily.family);
        return this;
    }

    public ImageBarrier usage(ImageUsage usage) {
        barrier.subresourceRange().aspectMask(usage.aspect);
        return this;
    }

    public ImageBarrier layout(ImageLayout layout) {
        barrier.newLayout(layout.value);
        return this;
    }

    public ImageBarrier access(AccessMask accessMask) {
        barrier.dstAccessMask(accessMask.value);
        return this;
    }

    public StageMask getSrcStage() {
        return from;
    }

    public ImageBarrier setSrcStage(StageMask from) {
        this.from = from;
        return this;
    }

    public StageMask getDstSage() {
        return to;
    }

    public ImageBarrier setDstStage(StageMask to) {
        this.to = to;
        return this;
    }

    @Override
    public void destroy() {
        barrier.free();
    }

    public <T> T getDirect(Class<T> clz) {
        return (T) barrier;
    }
}
