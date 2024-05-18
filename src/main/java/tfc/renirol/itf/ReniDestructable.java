package tfc.renirol.itf;

import java.io.Closeable;
import java.io.IOException;

public interface ReniDestructable extends Closeable {
    void destroy();

    default void free() {
        destroy();
    }

    @Override
    default void close() throws IOException {
        destroy();
    }
}
