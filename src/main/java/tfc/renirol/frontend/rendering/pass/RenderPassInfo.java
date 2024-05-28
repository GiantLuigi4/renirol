package tfc.renirol.frontend.rendering.pass;

import org.lwjgl.vulkan.VkDevice;
import tfc.renirol.frontend.enums.ImageLayout;
import tfc.renirol.frontend.enums.Operation;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.device.support.image.ReniSwapchainCapabilities;
import tfc.renirol.frontend.rendering.selectors.FormatSelector;
import tfc.renirol.itf.ReniDestructable;
import tfc.renirol.util.ReadOnlyList;

import java.util.ArrayList;
import java.util.List;

public class RenderPassInfo implements ReniDestructable {
    VkDevice device;
    ReniSwapchainCapabilities image;

    final List<ReniPassAttachment> attachments = new ArrayList<>();
    final List<ReniPassAttachment> colorAttachments = new ArrayList<>();
    final List<ReniPassAttachment> depthAttachments = new ArrayList<>();

    public RenderPassInfo(ReniLogicalDevice device, long surface) {
        this.device = device.getDirect(VkDevice.class);
        image = device.hardware.features.image(surface);
    }

    public RenderPassInfo(ReniLogicalDevice device) {
        this.device = device.getDirect(VkDevice.class);
    }

    public RenderPassInfo colorAttachment(
            Operation load, Operation store,
            ImageLayout initialLayout, ImageLayout targetLayout,
            FormatSelector format
    ) {
        ReniPassAttachment passAttachment = new ReniPassAttachment(
                format.select(image).format(),
                false, load, store,
                initialLayout, targetLayout
        );

        this.attachments.add(passAttachment);
        this.colorAttachments.add(passAttachment);
        return this;
    }

    public RenderPassInfo colorAttachment(
            Operation load, Operation store,
            ImageLayout initialLayout, ImageLayout targetLayout,
            int format
    ) {
        ReniPassAttachment passAttachment = new ReniPassAttachment(
                format,
                false, load, store,
                initialLayout, targetLayout
        );

        this.attachments.add(passAttachment);
        this.colorAttachments.add(passAttachment);
        return this;
    }

    public RenderPassInfo depthAttachment(
            Operation load, Operation store,
            ImageLayout initialLayout, ImageLayout targetLayout,
            int format
    ) {
        ReniPassAttachment passAttachment = new ReniPassAttachment(
                format,
                true, load, store,
                initialLayout, targetLayout
        );

        this.attachments.add(passAttachment);
        this.depthAttachments.add(passAttachment);
        return this;
    }

    public void destroy() {
        for (ReniPassAttachment attachment : attachments)
            attachment.free();
        if (image != null)
            image.destroy();
    }

    public List<ReniPassAttachment> getAttachments() {
        return new ReadOnlyList<>(attachments);
    }

    public List<ReniPassAttachment> getColorAttachments() {
        return new ReadOnlyList<>(colorAttachments);
    }

    public List<ReniPassAttachment> getDepthAttachments() {
        return new ReadOnlyList<>(depthAttachments);
    }
}
