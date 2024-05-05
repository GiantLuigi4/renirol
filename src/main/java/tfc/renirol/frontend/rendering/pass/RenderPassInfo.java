package tfc.renirol.frontend.rendering.pass;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.device.support.image.ReniImageCapabilities;
import tfc.renirol.frontend.hardware.util.ReniDestructable;
import tfc.renirol.frontend.rendering.enums.ImageLayout;
import tfc.renirol.frontend.rendering.enums.Operation;
import tfc.renirol.frontend.rendering.selectors.FormatSelector;

public class RenderPassInfo implements ReniDestructable {
    VkDevice device;
    ReniImageCapabilities image;

    VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.calloc(1);
    VkAttachmentReference.Buffer colorAttachmentRef = VkAttachmentReference.calloc(1);
    VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1);
    VkRenderPassCreateInfo.Buffer renderPassInfo = VkRenderPassCreateInfo.calloc(1);
    VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1);

    public RenderPassInfo(ReniLogicalDevice device, long surface) {
        this.device = device.getDirect(VkDevice.class);
        image = device.hardware.features.image(surface);
    }

    public RenderPassInfo colorAttachment(
            Operation load, Operation store,
            ImageLayout initialLayout, ImageLayout targetLayout,
            FormatSelector format
    ) {
        // attachment
        int formatV = format.select(image).format();
        colorAttachment.format(formatV);
        colorAttachment.samples(VK10.VK_SAMPLE_COUNT_1_BIT);

        colorAttachment.loadOp(load.load);
        colorAttachment.storeOp(store.store);
        colorAttachment.stencilLoadOp(load.load);
        colorAttachment.stencilStoreOp(store.store);

        colorAttachment.initialLayout(initialLayout.value);
        colorAttachment.finalLayout(targetLayout.value);

        // ref
        colorAttachmentRef.attachment(0);
        colorAttachmentRef.layout(VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        return this;
    }

    public RenderPassInfo subpass() {
        subpass.pipelineBindPoint(VK10.VK_PIPELINE_BIND_POINT_GRAPHICS);
        subpass.colorAttachmentCount(1);
        subpass.pColorAttachments(colorAttachmentRef);
        return this;
    }

    public RenderPassInfo dependency() {
        dependency.srcSubpass(VK10.VK_SUBPASS_EXTERNAL);
        dependency.dstSubpass(0);

        dependency.srcStageMask(VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT);
        dependency.srcAccessMask(0);

        dependency.dstStageMask(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
        dependency.dstAccessMask(VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
        renderPassInfo.pDependencies(dependency);
        return this;
    }

    public RenderPass create() {
        renderPassInfo.sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
        renderPassInfo.pAttachments(colorAttachment);
        renderPassInfo.pSubpasses(subpass);

        return new RenderPass(VkUtil.getCheckedLong(
                (buf) -> VK10.nvkCreateRenderPass(device, renderPassInfo.address(), 0, MemoryUtil.memAddress(buf))
        ), device);
    }

    public void destroy() {
        renderPassInfo.free();
        dependency.free();
        colorAttachment.free();
        colorAttachmentRef.free();
        subpass.free();
        image.destroy();
    }
}
