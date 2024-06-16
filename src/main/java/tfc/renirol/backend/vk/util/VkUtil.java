package tfc.renirol.backend.vk.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;
import org.lwjgl.vulkan.VK10;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class VkUtil {
    public static <T> T getChecked(Function<PointerBuffer, Integer> func, Function<Long, T> success) {
        PointerBuffer buf = MemoryUtil.memAllocPointer(1);
        int result = func.apply(buf);
        check(result);
        T t = success.apply(buf.get(0));
        MemoryUtil.memFree(buf);
        return t;
    }

    public static <T> T getCheckedLong(Function<LongBuffer, Integer> func, Function<Long, T> success) {
        LongBuffer buf = MemoryUtil.memAllocLong(1);
        int result = func.apply(buf);
        check(result);
        T t = success.apply(buf.get(0));
        MemoryUtil.memFree(buf);
        return t;
    }

    public static long getCheckedLong(Function<LongBuffer, Integer> func) {
        LongBuffer buf = MemoryUtil.memAllocLong(1);
        int result = func.apply(buf);
        check(result);
        long l = buf.get(0);
        MemoryUtil.memFree(buf);
        return l;
    }

    public static void check(int result) {
        if (result != VK10.VK_SUCCESS)
            throw new RuntimeException("Failed with error " + result);
    }

    public static <T> T get(Consumer<PointerBuffer> func, Function<Long, T> success) {
        PointerBuffer buf = MemoryUtil.memAllocPointer(1);
        func.accept(buf);
        T t = success.apply(buf.get(0));
        MemoryUtil.memFree(buf);
        return t;
    }

    public static <T> T getLong(Consumer<LongBuffer> func, Function<Long, T> success) {
        LongBuffer buf = MemoryUtil.memAllocLong(1);
        func.accept(buf);
        T t = success.apply(buf.get(0));
        MemoryUtil.memFree(buf);
        return t;
    }

    public static <T> void iterate(PointerBuffer buf, Function<Long, T> mapper, Consumer<T> action) {
        for (int i = 0; i < buf.capacity(); i++) action.accept(mapper.apply(buf.get(i)));
        buf.free();
    }

    public static <T extends StructBuffer<V, T>, V extends Struct<V>> void iterate(StructBuffer<V, T> buf, Consumer<V> action) {
        for (int i = 0; i < buf.capacity(); i++) action.accept(buf.get(i));
        buf.free();
    }

    public static <T extends StructBuffer<V, T>, V extends Struct<V>> void iterateCancelable(StructBuffer<V, T> buf, Predicate<V> action) {
        for (int i = 0; i < buf.capacity(); i++)
            if (action.test(buf.get(i))) return;
        buf.free();
    }

    public static PointerBuffer toPointerBuffer(String[] extensions) {
        PointerBuffer pb = MemoryUtil.memAllocPointer(extensions.length);
        for (String extension : extensions)
            pb.put(MemoryUtil.memUTF8(extension));
        return pb.rewind();
    }

    public static void freeStringPointerBuffer(PointerBuffer extBuf) {
        for (int i = 0; i < extBuf.capacity(); i++) {
            MemoryUtil.nmemFree(extBuf.get(i));
            extBuf.put(i, 0);
        }
        MemoryUtil.memFree(extBuf);
    }

    public static <V extends Struct<V>, T extends StructBuffer<V, T>> V select(StructBuffer<V, T> buffer, Function<V, Integer> scoringFunc) {
        ArrayList<pair<V>> devices = new ArrayList<>();
        for (V device : buffer) devices.add(new pair(device, scoringFunc.apply(device)));
        devices.sort((o1, o2) ->
                -Integer.compare(o1.v, o2.v)
        );
        return devices.get(0).t;
    }

    public static int select(IntBuffer buffer, Function<Integer, Integer> scoringFunc) {
        ArrayList<pair<Integer>> devices = new ArrayList<>();
        for (int i = 0; i < buffer.capacity(); i++) {
            int device = buffer.get(i);
            devices.add(new pair(device, scoringFunc.apply(device)));
        }
        devices.sort((o1, o2) ->
                -Integer.compare(o1.v, o2.v)
        );
        return devices.get(0).t;
    }

    public static int select(int[] buffer, Function<Integer, Integer> scoringFunc) {
        ArrayList<pair<Integer>> devices = new ArrayList<>();
        for (int i = 0; i < buffer.length; i++) {
            int device = buffer[i];
            devices.add(new pair(device, scoringFunc.apply(device)));
        }
        devices.sort((o1, o2) ->
                -Integer.compare(o1.v, o2.v)
        );
        return devices.get(0).t;
    }

    public static String find(Class<?> in, String prefix, int value) {
        for (Field declaredField : in.getFields()) {
            try {
                if (declaredField.getName().startsWith(prefix)) {
                    if (declaredField.getInt(null) == value)
                        return declaredField.getName();
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static ByteBuffer read(InputStream is) {
        try {
            byte[] dat = is.readAllBytes();
            try {
                is.close();
            } catch (Throwable ignored) {
            }
            ByteBuffer buffer = MemoryUtil.memAlloc(dat.length);
            buffer.put(0, dat);
            return buffer;
        } catch (Throwable err) {
            throw new RuntimeException(err);
        }
    }

    public static <T, V> V managed(
            T t,
            Function<T, V> resultProvider,
            Consumer<T> end
    ) {
        V v = resultProvider.apply(t);
        end.accept(t);
        return v;
    }

    static class pair<T> {
        T t;
        int v;

        public pair(T t, int v) {
            this.t = t;
            this.v = v;
        }
    }
}
