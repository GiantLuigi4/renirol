package tfc.renirol.frontend.rendering.framebuffer;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.util.ReniDestructable;
import tfc.renirol.frontend.rendering.enums.ImageLayout;
import tfc.renirol.frontend.rendering.pass.RenderPass;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;

public class FrameBuffer implements ReniDestructable {
    VkDevice logic;
    public final long image;
    long view;
    SwapChain chain;

    public FrameBuffer(ReniLogicalDevice device) {
        this.logic = device.getDirect(VkDevice.class);
        throw new RuntimeException("NYI");
    }

    public FrameBuffer(VkDevice device, long image, long view, SwapChain chain) {
        this.logic = device;
        this.image = image;
        this.view = view;
        this.chain = chain;
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
        framebufferInfo.width(chain.extents.width());
        framebufferInfo.height(chain.extents.height());
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
