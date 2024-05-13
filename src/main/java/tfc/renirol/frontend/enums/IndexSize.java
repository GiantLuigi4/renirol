package tfc.renirol.frontend.enums;

import org.lwjgl.vulkan.VK13;
import tfc.renirol.frontend.enums.prims.NumericPrimitive;

public enum IndexSize {
    INDEX_16(VK13.VK_INDEX_TYPE_UINT16, NumericPrimitive.SHORT.size),
    INDEX_32(VK13.VK_INDEX_TYPE_UINT32, NumericPrimitive.INT.size),
    ;

    public final int id;
    public final int size;

    IndexSize(int id, int size) {
        this.id = id;
        this.size = size;
    }
}
