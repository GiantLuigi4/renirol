package tfc.renirol.frontend.rendering.framebuffer;

import tfc.renirol.frontend.enums.ImageLayout;
import tfc.renirol.frontend.enums.Operation;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.rendering.pass.RenderPassInfo;
import tfc.renirol.frontend.rendering.resource.image.itf.RecreateableImage;

public class Framebuffer extends AbstractFramebuffer {
    protected final Attachment[] attachmentArray;

    public Framebuffer(Attachment... attachments) {
        this.attachmentArray = attachments;
    }

    @Override
    public void recreate(int width, int height) {
        for (Attachment image : attachmentArray) {
            if (image.recreateable)
                ((RecreateableImage) image.image).recreate(width, height);
        }
    }

    @Override
    public int attachmentCount() {
        return attachmentArray.length;
    }

    @Override
    public Attachment getAttachment(int index) {
        return attachmentArray[index];
    }

    @Override
    public long getView(int index) {
        return attachmentArray[index].image.getView();
    }

    @Override
    public RenderPassInfo genericPass(ReniLogicalDevice device, Operation load, Operation store) {
        RenderPassInfo info = new RenderPassInfo(device);
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
