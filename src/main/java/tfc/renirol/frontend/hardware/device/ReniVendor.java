package tfc.renirol.frontend.hardware.device;

// reference: https://www.pcilookup.com/
// reference: https://pcisig.com/membership/member-companies
public enum ReniVendor {
    NVIDIA("NVidia Corporation", 4318),
    AMD("Advanced Micro Devices, Inc.", new int[]{4130, 4098}),
    APPLE("Apple Computer", 4203),
    INTEL("Intel Corporation", 32902),
    MICROSOFT("Microsoft", 5140),
    UNKNOWN("UNKNOWN", -1)
    ;

    public final String name;
    private final int[] ids;

    ReniVendor(String name, int[] ids) {
        this.name = name;
        this.ids = ids;
    }

    ReniVendor(String name, int ids) {
        this.name = name;
        this.ids = new int[]{ids};
    }

    public boolean matches(int id) {
        for (int i : ids) {
            if (i == id) return true;
        }
        return false;
    }
}
