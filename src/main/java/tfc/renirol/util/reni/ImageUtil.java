package tfc.renirol.util.reni;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class ImageUtil {
    public enum ConvertMode {
        BLACK_OPAQUE(0xFF000000),
        TRANSPARENT(0x00000000),
        WHITE(0xFFFFFFFF),
        RRR(0xFFFFFFFF),
        ;

        public final int mask;

        ConvertMode(int mask) {
            this.mask = mask;
        }
    }

    public static ByteBuffer convertChannels(
            ByteBuffer from,
            final int numComp,
            final int targetChannels,
            final boolean freeOld,
            ConvertMode mode
    ) {
        if (numComp == targetChannels) return from;
        if (numComp > targetChannels) throw new RuntimeException("NYI: reducing channels");

        if (targetChannels == 1) return from;

        switch (numComp) {
            case 0, 4:
                // if it is bigger or smaller than either of these, I have bigger problems
                // eventually I plan to change this
                return from;
        }

        // swizzle conversion
        if (mode == ConvertMode.RRR) {
            // TODO: debug?
            final int len;
            final int C_NUM_COMP = targetChannels;
            final ByteBuffer to = MemoryUtil.memAlloc((len = (from.capacity() / C_NUM_COMP)) * targetChannels);
            for (int i = 0; i < len; i++) {
                final int indexTo = i * targetChannels;
                final int indexFrom = i * C_NUM_COMP;
                final byte v = from.get(indexFrom);
                for (int c = 0; c < Math.min(targetChannels, 3); c++)
                    to.put(indexTo, v);
                if (targetChannels == 4) // if it is larger, I have bigger problems
                    to.put(indexTo + 3, (byte) 0xFF);
            }

            if (freeOld)
                MemoryUtil.memFree(from);
            return to;
        }

        // standard conversions
        switch (numComp) {
            case 1 -> {
                final ByteBuffer to;
                final int len;

                // TODO: no idea if this works
                if (targetChannels == 4) {
                    final int mask = mode.mask;
                    to = MemoryUtil.memAlloc((len = (from.capacity())) * targetChannels);

                    final IntBuffer asInt = to.asIntBuffer();
                    for (int i = 0; i < asInt.capacity(); i++)
                        asInt.put(i, mask);
                    for (int i = 0; i < to.capacity() - (asInt.capacity() << 2); i++)
                        to.put(asInt.capacity() << 2 + i, (byte) 0x00);
                    to.put(to.capacity() - 1, (byte) 0xFF);
                } else {
                    to = MemoryUtil.memCalloc((len = (from.capacity() >> 1)) * targetChannels);
                }

                for (int i = 0; i < len; i++) {
                    final int indexTo = i * targetChannels;
                    to.putShort(indexTo, from.get(i));
                }

                if (freeOld)
                    MemoryUtil.memFree(from);
                return to;
            }
            case 2 -> {
                final ByteBuffer to;
                final int len;

                // TODO: no idea if this works
                final ShortBuffer asShort = from.asShortBuffer();
                if (targetChannels == 4) {
                    final int mask = mode.mask;
                    to = MemoryUtil.memAlloc((len = (from.capacity() >> 1)) * targetChannels);

                    final IntBuffer asInt = to.asIntBuffer();
                    for (int i = 0; i < asInt.capacity(); i++)
                        asInt.put(i, mask);
                    for (int i = 0; i < to.capacity() - (asInt.capacity() << 2); i++)
                        to.put(asInt.capacity() << 2 + i, (byte) 0x00);
                    to.put(to.capacity() - 1, (byte) 0xFF);
                } else {
                    to = MemoryUtil.memCalloc((len = (from.capacity() >> 1)) * targetChannels);
                }

                for (int i = 0; i < len; i++) {
                    final int indexTo = i * targetChannels;
                    to.putShort(indexTo, asShort.get(i));
                }

                if (freeOld)
                    MemoryUtil.memFree(from);
                return to;
            }
            case 3 -> {
                final ByteBuffer to;
                final int len;

                final int C_NUM_COMP = 3;

                final ShortBuffer asShort = from.asShortBuffer();
                if (targetChannels == 4) {
                    final int mask = mode.mask;
                    to = MemoryUtil.memAlloc((len = (from.capacity() / C_NUM_COMP)) * targetChannels);

                    final IntBuffer asInt = to.asIntBuffer();
                    for (int i = 0; i < asInt.capacity(); i++)
                        asInt.put(i, mask);
                    for (int i = 0; i < to.capacity() - (asInt.capacity() << 2); i++)
                        to.put(asInt.capacity() << 2 + i, (byte) 0x00);
                    to.put(to.capacity() - 1, (byte) 0xFF);
                } else {
                    to = MemoryUtil.memCalloc((len = (from.capacity() / C_NUM_COMP)) * targetChannels);
                }

                boolean alt = false;
                for (int i = 0; i < len; i++) {
                    final int indexTo = i * targetChannels;
                    final int indexFrom = i * C_NUM_COMP;
                    if (alt) {
                        for (int i1 = 0; i1 < C_NUM_COMP; i1++) to.put(indexTo + i1, from.get(indexFrom + i1));
                    } else {
                        to.putShort(indexTo, asShort.get((i * C_NUM_COMP) >> 1));
                        for (int i1 = 2; i1 < C_NUM_COMP; i1++) to.put(indexTo + i1, from.get(indexFrom + i1));
                    }
                    alt = !alt;
                }

                if (freeOld)
                    MemoryUtil.memFree(from);
                return to;
            }
            default -> throw new RuntimeException("Unknown channel count");
        }
    }
}
