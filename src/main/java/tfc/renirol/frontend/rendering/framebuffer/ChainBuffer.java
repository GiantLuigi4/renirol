package tfc.renirol.frontend.rendering.framebuffer;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.rendering.pass.RenderPass;
import tfc.renirol.frontend.rendering.resource.image.Image;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;

public class ChainBuffer {
    SwapChain chain;
    Image[] attachmentArray;

    public ChainBuffer(SwapChain chain, Image... attachments) {
        this.chain = chain;
        this.attachmentArray = attachments;
    }

    public void recreate(int width, int height) {
        chain.recreate(width, height);
        for (Image image : attachmentArray) {
            image.recreate(width, height);
        }
    }

    public long createFbo(int frameIndex, RenderPass pass) {
        VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc();
        LongBuffer attachments = MemoryUtil.memAllocLong(attachmentArray.length + 1);
        attachments.put(0, chain.getFbo(frameIndex).view);
        for (int i = 0; i < attachmentArray.length; i++) {
            attachments.put(i + 1, attachmentArray[i].getView());
        }
        framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
        framebufferInfo.renderPass(pass.handle);
        framebufferInfo.attachmentCount(1 + attachmentArray.length);
        framebufferInfo.width(chain.extents.width());
        framebufferInfo.height(chain.extents.height());
        framebufferInfo.layers(1);

        framebufferInfo.pAttachments(attachments);

        long fbo = (VkUtil.getCheckedLong((buf) ->
                VK10.nvkCreateFramebuffer(chain.device.getDirect(VkDevice.class), framebufferInfo.address(), 0, MemoryUtil.memAddress(buf))
        ));

        framebufferInfo.free();
        return fbo;
    }
}
