package tfc.renirol.frontend.rendering.framebuffer;

import tfc.renirol.frontend.rendering.resource.image.Image;

public class Framebuffer extends AbstractFramebuffer {
    Image[] attachmentArray;

    public Framebuffer(Image... attachments) {
        this.attachmentArray = attachments;
    }

    @Override
    public void recreate(int width, int height) {
        for (Image image : attachmentArray) {
            image.recreate(width, height);
        }
    }

    @Override
    public long getView(int index) {
        return attachmentArray[index].getView();
    }
}
