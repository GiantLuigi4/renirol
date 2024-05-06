package tfc.renirol.frontend.hardware.util;

import tfc.renirol.frontend.hardware.device.ReniHardwareDevice;
import tfc.renirol.frontend.hardware.device.Vendors;
import tfc.renirol.frontend.hardware.device.support.ReniDeviceType;
import tfc.renirol.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DeviceQuery {
    final ArrayList<DeviceFilter> filters = new ArrayList<>();
    final ArrayList<Pair<Predicate<ReniHardwareDevice>, Integer>> requests = new ArrayList<>();

    /**
     * Required capabilities are capabilities that your program absolutely cannot function without
     * If no GPUs match these filters, an error will be thrown
     *
     * @param capability the capability to require
     */
    public DeviceQuery require(ReniHardwareCapability capability) {
        filters.add(capability.supportQuery::test);
        return this;
    }

    /**
     * Require if possible checks if any device matches the filter, and if any does, it filters out all devices that do not
     * This is useful for capabilities that have a high performance impact but aren't explicitly required, such as multidraw
     *
     * @param capability a capability that you heavily desire for your program
     */
    public DeviceQuery requireIfPossible(ReniHardwareCapability capability) {
        filters.add((SoftRequisite) capability.supportQuery::test);
        return this;
    }

    /**
     * Allows for scoring a device
     * The device that gets the highest score from all request filters will be selected
     * These should be completely necessary for the program, but benefit it in different amounts; the highest priority should have the highest impact
     *
     * @param priority   the priority of the capability; if the capability passes, then this gets added to the score
     * @param capability the capability to look for
     */
    public DeviceQuery request(int priority, ReniHardwareCapability capability) {
        requests.add(Pair.of(capability.supportQuery, priority));
        return this;
    }

    /**
     * Fills the query with the recommended set of features
     */
    public DeviceQuery reniRecommended() {
        this.require(ReniHardwareCapability.SWAPCHAIN);
        this.requireIfPossible(ReniHardwareCapability.MULTIDRAW);
        this.requireIfPossible(ReniHardwareCapability.INSTANCING);
        this.requireIfPossible(ReniHardwareCapability.MESH_SHADER);
        this.requireIfPossible(ReniHardwareCapability.GEOMETRY_SHADER);
        this.prioritizeDedicated();
        return this;
    }

    DeviceFilter priority = (dev) -> true;

    // priority models

    /**
     * dGPUs tend to perform better, but produce more heat and use more battery
     * They can also perform worse for simple graphics tasks on laptops due to having to send information back to the iGPU for it to be presented
     * Usually, that fact is not worth worrying about though
     */
    public DeviceQuery prioritizeDedicated() {
        priority = (dev) -> dev.getType() == ReniDeviceType.DISCRETE_GPU;
        return this;
    }

    /**
     * iGPUs tend to have lower performance, but also tend to use less battery
     * in laptops, these can also cause the device to overheat if you use them too intensely, so be careful with this
     */
    public DeviceQuery prioritizeIntegrated() {
        priority = (dev) -> dev.getType() == ReniDeviceType.INTEGRATED_GPU;
        return this;
    }

    /**
     * Unsets priority model
     */
    public DeviceQuery noPriority() {
        priority = (dev) -> true;
        return this;
    }

    // custom
    public DeviceQuery require(DeviceFilter filter) {
        filters.add(filter);
        return this;
    }

    public DeviceQuery requireIfPossible(SoftRequisite filter) {
        filters.add(filter);
        return this;
    }

    public DeviceQuery request(int priority, Predicate<ReniHardwareDevice> filter) {
        requests.add(Pair.of(filter, priority));
        return this;
    }

    public DeviceQuery priorityModel(DeviceFilter filter) {
        priority = filter;
        return this;
    }

    DeviceFilter priorityB = (dev) -> true;

    public DeviceQuery favorBrands(Vendors... reniVendors) {
        priorityB = (SoftRequisite) (dev) -> {
            for (Vendors reniVendor : reniVendors) {
                if (reniVendor.equals(dev.information.getVendorEnum())) {
                    return true;
                }
            }
            return false;
        };
        return this;
    }

    public ReniHardwareDevice select(List<ReniHardwareDevice> devices) {
        Stream<ReniHardwareDevice> deviceStream = devices.stream();
        for (DeviceFilter filter : filters) deviceStream = filter.apply(deviceStream);

        deviceStream = ((SoftRequisite) device -> priority.check(device)).apply(deviceStream);
        deviceStream = ((SoftRequisite) device -> priorityB.check(device)).apply(deviceStream);

        if (!requests.isEmpty()) {
            ArrayList<Pair<Integer, ReniHardwareDevice>> scoring = new ArrayList<>();
            deviceStream.forEach(dev -> {
                int score = 0;
                for (Pair<Predicate<ReniHardwareDevice>, Integer> request : requests)
                    if (request.left().test(dev))
                        score += request.right();
                scoring.add(Pair.of(score, dev));
            });
            scoring.sort((o1, o2) -> -Integer.compare(o1.left(), o2.left()));
            deviceStream = Stream.of(scoring.get(0).right());
        }

        return deviceStream.findFirst().get();
    }

    public interface DeviceFilter {
        boolean check(ReniHardwareDevice device);

        default Stream<ReniHardwareDevice> apply(Stream<ReniHardwareDevice> devices) {
            return devices.filter(this::check);
        }
    }

    public interface SoftRequisite extends DeviceFilter {
        @Override
        default Stream<ReniHardwareDevice> apply(Stream<ReniHardwareDevice> devices) {
            List<ReniHardwareDevice> origi = devices.toList();
            List<ReniHardwareDevice> filtered = origi.stream().filter(this::check).toList();
            if (filtered.isEmpty()) return origi.stream();
            return filtered.stream();
        }
    }
}
