package tfc.renirol.frontend.rendering.framebuffer;

import tfc.renirol.frontend.enums.Operation;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.rendering.pass.RenderPassInfo;

import java.util.Iterator;
import java.util.function.Consumer;

public abstract class AbstractFramebuffer implements Iterable<Attachment> {
    public abstract void recreate(int width, int height);

    public abstract int attachmentCount();

    public abstract Attachment getAttachment(int index);

    public long getView(int index) {
        return getAttachment(index).image.getView();
    }

    public RenderPassInfo genericPass(ReniLogicalDevice device) {
        return genericPass(device, Operation.PERFORM, Operation.PERFORM);
    }

    public abstract RenderPassInfo genericPass(ReniLogicalDevice device, Operation load, Operation store);

    @Override
    public Iterator<Attachment> iterator() {
        return new Itr();
    }

    @Override
    public void forEach(Consumer<? super Attachment> action) {
        for (int i = 0; i < attachmentCount(); i++) {
            action.accept(getAttachment(i));
        }
    }

    public class Itr implements Iterator<Attachment> {
        int idx = 0;
        int trg = attachmentCount();

        @Override
        public boolean hasNext() {
            return idx < trg;
        }

        @Override
        public Attachment next() {
            return getAttachment(idx++);
        }

        @Override
        public void remove() {
            throw new RuntimeException();
        }

        @Override
        public void forEachRemaining(Consumer<? super Attachment> action) {
            while (idx < trg) {
                action.accept(getAttachment(idx));
                idx++;
            }
        }
    }
}
