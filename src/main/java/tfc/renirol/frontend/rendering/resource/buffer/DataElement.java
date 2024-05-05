package tfc.renirol.frontend.rendering.resource.buffer;

import tfc.renirol.frontend.rendering.enums.prims.NumericPrimitive;

public class DataElement {
    public final int size;
    public final NumericPrimitive type;

    public DataElement(NumericPrimitive type, int count) {
        this.type = type;
        this.size = count;
    }

    public int bytes() {
        return size * type.size;
    }
}
