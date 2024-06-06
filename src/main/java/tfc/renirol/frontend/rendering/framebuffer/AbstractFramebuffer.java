package tfc.renirol.frontend.rendering.framebuffer;

public abstract class AbstractFramebuffer {
    public abstract void recreate(int width, int height);
    public abstract long getView(int index);
}
