package tfc.renirol.frontend.hardware.device.queue;

import tfc.renirol.frontend.hardware.device.ReniQueueType;

import java.util.Arrays;

public class FamilyName {
    private final ReniQueueType[] types;
    private final int hash;

    public FamilyName(ReniQueueType[] types) {
        this.types = Arrays.copyOf(types, types.length);
        this.hash = Arrays.hashCode(types);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FamilyName that = (FamilyName) o;
        return hash == that.hash && Arrays.equals(types, that.types);
    }
}
