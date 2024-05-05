package tfc.renirol.frontend.windowing;

import tfc.renirol.ReniContext;
import tfc.renirol.Renirol;

public abstract class GenericWindow {
    public final WindowManager manager;

    public GenericWindow(WindowManager manager) {
        this.manager = manager;
    }

    public abstract void dispose();

    public abstract void show();

    public abstract void hide();

    public abstract void initContext(ReniContext graphicsContext);

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract void grabContext();

    public abstract boolean shouldClose();

    public abstract void swapAndPollSize();

    public abstract long handle();

    public abstract void setName(String name);

    public abstract ReniContext getContext();
}
