package tfc.renirol.frontend.rendering.pass;

import tfc.renirol.frontend.enums.ImageLayout;
import tfc.renirol.frontend.enums.Operation;

public class ReniPassAttachment {
    public final int format;
    public final boolean isDepth;
    public final Operation load, store;
    public final ImageLayout initialLayout, targetLayout;

    public ReniPassAttachment(
            int format, boolean isDepth,
            Operation load, Operation store,
            ImageLayout initialLayout, ImageLayout targetLayout
    ) {
        this.format = format;
        this.isDepth = isDepth;
        this.load = load;
        this.store = store;
        this.initialLayout = initialLayout;
        this.targetLayout = targetLayout;
    }

    public void free() {
        // no-op for desktop
    }
}
