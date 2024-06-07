package tfc.renirol.frontend.rendering.framebuffer.chain;

import tfc.renirol.frontend.enums.ImageLayout;
import tfc.renirol.frontend.enums.Operation;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.rendering.framebuffer.Attachment;
import tfc.renirol.frontend.rendering.framebuffer.Framebuffer;
import tfc.renirol.frontend.rendering.pass.RenderPassInfo;

import java.nio.IntBuffer;

public class ChainBuffer extends Framebuffer {
    SwapChain chain;
    IntBuffer currentFrame;

    public ChainBuffer(IntBuffer currentFrame, SwapChain chain, Attachment... attachments) {
        super(attachments);
        this.chain = chain;
        this.currentFrame = currentFrame;
    }

    public void recreate(int width, int height) {
        chain.recreate(width, height);
        super.recreate(width, height);
    }

    public long getView(int index) {
        if (index == 0) return chain.getFbo(currentFrame.get(0)).getView();
        return super.getView(index - 1);
    }

    @Override
    public int attachmentCount() {
        return super.attachmentCount() + 1;
    }

    @Override
    public Attachment getAttachment(int index) {
        if (index == 0) return new Attachment(chain.getFbo(currentFrame.get(0)), false, false);
        return super.getAttachment(index - 1);
    }

    @Override
    public RenderPassInfo genericPass(ReniLogicalDevice device, Operation load, Operation store) {
        RenderPassInfo info = new RenderPassInfo(device);

        info.colorAttachment(
                load, store,
                ImageLayout.COLOR_ATTACHMENT_OPTIMAL,
                ImageLayout.COLOR_ATTACHMENT_OPTIMAL,
                chain.format.format()
        );

        for (Attachment attachment : attachmentArray) {
            if (attachment.isDepth) {
                info.depthAttachment(
                        load, store,
                        ImageLayout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                        ImageLayout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                        attachment.image.getFormat()
                );
            } else {
                info.colorAttachment(
                        load, store,
                        ImageLayout.COLOR_ATTACHMENT_OPTIMAL,
                        ImageLayout.COLOR_ATTACHMENT_OPTIMAL,
                        attachment.image.getFormat()
                );
            }
        }

        return info;
    }

    public RenderPassInfo genericPass(
            ReniLogicalDevice device,
            Operation load, Operation store,
            ImageLayout chainTarget
    ) {
        RenderPassInfo info = new RenderPassInfo(device);

        info.colorAttachment(
                load, store,
                ImageLayout.COLOR_ATTACHMENT_OPTIMAL,
                chainTarget,
                chain.format.format()
        );

        for (Attachment attachment : attachmentArray) {
            if (attachment.isDepth) {
                info.depthAttachment(
                        load, store,
                        ImageLayout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                        ImageLayout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                        attachment.image.getFormat()
                );
            } else {
                info.colorAttachment(
                        load, store,
                        ImageLayout.COLOR_ATTACHMENT_OPTIMAL,
                        ImageLayout.COLOR_ATTACHMENT_OPTIMAL,
                        attachment.image.getFormat()
                );
            }
        }

        return info;
    }
}
