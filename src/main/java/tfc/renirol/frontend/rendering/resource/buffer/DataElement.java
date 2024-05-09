package tfc.renirol.frontend.rendering.resource.buffer;

import tfc.renirol.frontend.rendering.enums.prims.NumericPrimitive;

public class DataElement {
    public final int size;
    public final NumericPrimitive type;
    public final int arrayCount;

    public DataElement(NumericPrimitive type, int count) {
        this(type, count, 1);
    }

    public DataElement(NumericPrimitive type, int count, int arrayCount) {
        this.type = type;
        this.size = count;
        this.arrayCount = arrayCount;
    }

    public int bytes() {
        return size * type.size * arrayCount;
    }
}
