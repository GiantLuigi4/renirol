package tfc.renirol.frontend.rendering.pass;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.device.support.image.ReniSwapchainCapabilities;
import tfc.renirol.frontend.hardware.util.ReniDestructable;
import tfc.renirol.frontend.rendering.enums.ImageLayout;
import tfc.renirol.frontend.rendering.enums.Operation;
import tfc.renirol.frontend.rendering.selectors.FormatSelector;
import tfc.renirol.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class RenderPassInfo implements ReniDestructable {
    VkDevice device;
    ReniSwapchainCapabilities image;

    List<Pair<VkAttachmentDescription, VkAttachmentReference>> attachments = new ArrayList<>();
    List<Pair<VkAttachmentDescription, VkAttachmentReference>> colorAttachments = new ArrayList<>();
    List<Pair<VkAttachmentDescription, VkAttachmentReference>> depthAttachments = new ArrayList<>();
    VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1);
    VkRenderPassCreateInfo.Buffer renderPassInfo = VkRenderPassCreateInfo.calloc(1);
    VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1);

    boolean depth = false;

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
        VkAttachmentDescription colorAttachment = VkAttachmentDescription.calloc();
        colorAttachment.format(formatV);
        colorAttachment.samples(VK10.VK_SAMPLE_COUNT_1_BIT);

        colorAttachment.loadOp(load.load);
        colorAttachment.storeOp(store.store);
        colorAttachment.stencilLoadOp(load.load);
        colorAttachment.stencilStoreOp(store.store);

        colorAttachment.initialLayout(initialLayout.value);
        colorAttachment.finalLayout(targetLayout.value);

        // ref
        VkAttachmentReference colorAttachmentRef = VkAttachmentReference.calloc();
        colorAttachmentRef.attachment(attachments.size());
        colorAttachmentRef.layout(VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        this.attachments.add(Pair.of(colorAttachment, colorAttachmentRef));
        this.colorAttachments.add(Pair.of(colorAttachment, colorAttachmentRef));
        return this;
    }

    public RenderPassInfo depthAttachment(
            Operation load, Operation store,
            ImageLayout initialLayout, ImageLayout targetLayout,
            int format
    ) {
        VkAttachmentDescription depthAttachment = VkAttachmentDescription.calloc();

        // attachment
        depthAttachment.format(format);
        depthAttachment.samples(VK10.VK_SAMPLE_COUNT_1_BIT);

        depthAttachment.loadOp(load.load);
        depthAttachment.storeOp(store.store);
        depthAttachment.stencilLoadOp(load.load);
        depthAttachment.stencilStoreOp(store.store);

        depthAttachment.initialLayout(initialLayout.value);
        depthAttachment.finalLayout(targetLayout.value);

        // ref
        VkAttachmentReference depthAttachmentRef = VkAttachmentReference.calloc();
        depthAttachmentRef.attachment(attachments.size());
        depthAttachmentRef.layout(VK13.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
        depth = true;

        this.attachments.add(Pair.of(depthAttachment, depthAttachmentRef));
        this.depthAttachments.add(Pair.of(depthAttachment, depthAttachmentRef));
        return this;
    }

    VkAttachmentReference.Buffer colorRefs;
    VkAttachmentReference.Buffer depthRefs;

    public RenderPassInfo subpass() {
        if (colorRefs != null) {
            colorRefs.free();
            colorRefs = null;
        }
        if (depthRefs != null) {
            depthRefs.free();
            depthRefs = null;
        }
        colorRefs = VkAttachmentReference.calloc(colorAttachments.size());
        depthRefs = VkAttachmentReference.calloc(depthAttachments.size());

        subpass.pipelineBindPoint(VK10.VK_PIPELINE_BIND_POINT_GRAPHICS);
        subpass.colorAttachmentCount(colorAttachments.size());
        for (int i = 0; i < colorAttachments.size(); i++) colorRefs.put(i, colorAttachments.get(i).right());
        for (int i = 0; i < depthAttachments.size(); i++) depthRefs.put(i, depthAttachments.get(i).right());
        subpass.pColorAttachments(colorRefs);
        if (depth) subpass.pDepthStencilAttachment(depthRefs.get(0));
        return this;
    }

    public RenderPassInfo dependency() {
        dependency.srcSubpass(VK10.VK_SUBPASS_EXTERNAL);
        dependency.dstSubpass(0);

        dependency.srcStageMask(
                VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT |
                        (depth ? VK13.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT : 0)
        );
        dependency.srcAccessMask(0);

        dependency.dstStageMask(
                VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT |
                        (depth ? VK13.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT : 0)
        );
        dependency.dstAccessMask(
                VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT |
                        (depth ? VK13.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT : 0)
        );
        renderPassInfo.pDependencies(dependency);
        return this;
    }

    public RenderPass create() {
        renderPassInfo.sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
        VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(this.attachments.size());
        for (int i = 0; i < this.attachments.size(); i++) {
            attachments.put(i, this.attachments.get(i).left());
        }

        renderPassInfo.pAttachments(attachments);
        renderPassInfo.pSubpasses(subpass);

        return new RenderPass(VkUtil.getCheckedLong(
                (buf) -> VK10.nvkCreateRenderPass(device, renderPassInfo.address(), 0, MemoryUtil.memAddress(buf))
        ), device);
    }

    public void destroy() {
        renderPassInfo.free();
        dependency.free();
        for (Pair<VkAttachmentDescription, VkAttachmentReference> attachment : attachments) {
            attachment.left().free();
            attachment.right().free();
        }
        subpass.free();
        image.destroy();
    }
}
