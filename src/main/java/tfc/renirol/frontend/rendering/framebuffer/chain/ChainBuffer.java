package tfc.renirol.frontend.rendering.framebuffer.chain;

import tfc.renirol.frontend.rendering.framebuffer.Framebuffer;
import tfc.renirol.frontend.rendering.resource.image.Image;

import java.nio.IntBuffer;

public class ChainBuffer extends Framebuffer {
    SwapChain chain;
    IntBuffer currentFrame;

    public ChainBuffer(IntBuffer currentFrame, SwapChain chain, Image... attachments) {
        super(attachments);
        this.chain = chain;
        this.currentFrame = currentFrame;
    }

    public void recreate(int width, int height) {
        chain.recreate(width, height);
        super.recreate(width, height);
    }

    public long getView(int index) {
        if (index == 0) return chain.getFbo(currentFrame.get(0)).view;
        return super.getView(index - 1);
    }
}
