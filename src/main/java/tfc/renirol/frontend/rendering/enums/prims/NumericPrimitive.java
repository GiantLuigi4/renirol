package tfc.renirol.frontend.rendering.enums.prims;

public enum NumericPrimitive {
    BYTE(1),
    SHORT(2),
    INT(4),
    LONG(8),
    FLOAT(4),
    DOUBLE(8),
    ;

    public final int size;

    NumericPrimitive(int size) {
        this.size = size;
    }
}
