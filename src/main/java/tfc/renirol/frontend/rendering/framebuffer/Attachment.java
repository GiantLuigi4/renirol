package tfc.renirol.frontend.rendering.framebuffer;

import tfc.renirol.frontend.rendering.resource.image.Image;
import tfc.renirol.frontend.rendering.resource.image.itf.ImageBacked;
import tfc.renirol.frontend.rendering.resource.image.itf.RecreateableImage;

public class Attachment {
    public final ImageBacked image;
    public final boolean isDepth;
    public final boolean recreateable;

    // helper method for automating a bit of setup
    public Attachment(ImageBacked image, boolean isDepth) {
        this.image = image;
        this.isDepth = isDepth;
        this.recreateable = image instanceof RecreateableImage;
    }

    public Attachment(ImageBacked image, boolean isDepth, boolean recreateable) {
        this.image = image;
        this.isDepth = isDepth;
        this.recreateable = recreateable;
    }

    public static Attachment attachment(Image img, boolean depth) {
        return new Attachment(img, depth, true);
    }

    public static Attachment color(Image img) {
        return new Attachment(img, false, true);
    }

    public static Attachment depth(Image img) {
        return new Attachment(img, true, true);
    }
}
