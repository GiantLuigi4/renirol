package tfc.renirol.frontend.rendering.framebuffer;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.util.ReniDestructable;
import tfc.renirol.frontend.rendering.pass.RenderPass;
import tfc.renirol.frontend.rendering.resource.image.Image;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;

public class FrameBuffer implements ReniDestructable {
    VkDevice logic;
    public final long image;
    long view;
    VkExtent2D extents;

    public FrameBuffer(VkDevice device, long image, long view, SwapChain chain) {
        this.logic = device;
        this.image = image;
        this.view = view;
        this.extents = chain.extents;
    }

    public FrameBuffer(ReniLogicalDevice logic, Image image) {
        this.logic = logic.getDirect(VkDevice.class);
        this.image = image.getHandle();
        this.view = image.getView();
        this.extents = image.getExtents();
    }

    @Override
    public void destroy() {
        VK10.nvkDestroyImageView(logic, view, 0);
    }

    public long forPass(RenderPass pass) {
        VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc();
        LongBuffer attachments = MemoryUtil.memAllocLong(1);
        framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
        framebufferInfo.renderPass(pass.handle);
        framebufferInfo.attachmentCount(1);
        framebufferInfo.width(extents.width());
        framebufferInfo.height(extents.height());
        framebufferInfo.layers(1);

        framebufferInfo.pAttachments(
                attachments.put(0, view).rewind()
        );

        long fbo = (VkUtil.getCheckedLong((buf) ->
                VK10.nvkCreateFramebuffer(logic, framebufferInfo.address(), 0, MemoryUtil.memAddress(buf))
        ));

        framebufferInfo.free();
        return fbo;
    }
}
