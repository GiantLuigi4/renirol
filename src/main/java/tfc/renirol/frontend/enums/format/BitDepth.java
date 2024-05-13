package tfc.renirol.frontend.enums.format;

public enum BitDepth {
    DEPTH_8(1),
    DEPTH_16(2),
    ;

    public final int size;

    BitDepth(int size) {
        this.size = size;
    }
}
