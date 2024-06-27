package tfc.renirol.frontend.hardware.util;

import tfc.renirol.frontend.hardware.device.ReniHardwareDevice;
import tfc.renirol.frontend.hardware.device.ReniQueueType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public abstract class QueueRequest {
    public abstract void prepare(
            ReniHardwareDevice hardware, Function<ReniQueueType, List<Integer>> queues
    );

    public record QueueInfo(ReniQueueType[] types, int count, int index) {
        @Override
        public String toString() {
            return "QueueInfo{" +
                    "types=" + Arrays.toString(types) +
                    ", count=" + count +
                    ", index=" + index +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QueueInfo queueInfo = (QueueInfo) o;
            return index == queueInfo.index;
        }

        @Override
        public int hashCode() {
            return index;
        }
    }

    public static class Split extends QueueRequest {
        QueueRequest[] requests;

        public Split(QueueRequest... requests) {
            this.requests = requests;
        }

        List<List<QueueInfo>> options = new ArrayList<>();

        @Override
        public void prepare(ReniHardwareDevice hardware, Function<ReniQueueType, List<Integer>> queues) {
            for (QueueRequest request : requests) request.prepare(hardware, queues);

            List<List<QueueInfo>> combos = new ArrayList<>();
            // TODO: improve algo?
            for (QueueRequest request : requests) {
                List<List<QueueInfo>> ints = new ArrayList<>(request.options());

                for (List<QueueInfo> anInt : ints) {
                    if (anInt.size() > 1)
                        throw new RuntimeException("Split indices only supports single indices (i.e. Shared indices)");
                }

                for (QueueRequest queueRequest : requests) {
                    List<List<QueueInfo>> infs = new ArrayList<>(ints);
                    if (queueRequest != request)
                        ints.removeAll(queueRequest.options());
                    if (ints.isEmpty())
                        ints = infs;
                }
                combos.addAll(ints);
            }

            if (combos.size() > 2)
                throw new RuntimeException("Split indices currently only supports having two sets");
            if (combos.size() < 2) {
                // no result
                options = new ArrayList<>();
                return;
            }

            // TODO: improve algo.
            List<QueueInfo> left1 = combos.get(0);
            List<QueueInfo> left = new ArrayList<>(left1);
            List<QueueInfo> right = new ArrayList<>(combos.get(1));
            left.removeAll(right);
            right.removeAll(left1);

            List<List<QueueInfo>> results = new ArrayList<>();
            for (int i = 0; i < left.size(); i++) {
                QueueInfo valL = left1.get(i);
                for (int i1 = 0; i1 < right.size(); i1++) {
                    QueueInfo valR = right.get(i1);
                    if (!valL.equals(valR)) {
                        List<QueueInfo> ints = new ArrayList<>();
                        ints.add(valL);
                        ints.add(valR);
                        results.add(ints);
                    }
                }
            }
            options = results;
        }

        @Override
        public List<List<QueueInfo>> options() {
            return options;
        }

        @Override
        public List<QueueInfo> select() {
            return options.get(0);
        }
    }

    public static class Either extends QueueRequest {
        QueueRequest[] queueRequests;

        public Either(QueueRequest... queueRequests) {
            this.queueRequests = queueRequests;
        }

        List<List<QueueInfo>> cache = new ArrayList<>();

        @Override
        public void prepare(ReniHardwareDevice hardware, Function<ReniQueueType, List<Integer>> queues) {
            for (QueueRequest queueRequest : queueRequests) queueRequest.prepare(hardware, queues);
            cache = new ArrayList<>();
            for (QueueRequest queueRequest : queueRequests)
                cache.addAll(queueRequest.options());
        }

        @Override
        public List<List<QueueInfo>> options() {
            return cache;
        }

        @Override
        public List<QueueInfo> select() {
            return cache.get(0);
        }
    }

    public static class Shared extends QueueRequest {
        ReniQueueType[] types;
        int count;

        public Shared(ReniQueueType... types) {
            this.types = types;
            this.count = 1;
        }

        public Shared(int count, ReniQueueType... types) {
            this.types = types;
            this.count = count;
        }

        List<QueueInfo> cache;

        @Override
        public void prepare(ReniHardwareDevice hardware, Function<ReniQueueType, List<Integer>> queues) {
            List<QueueInfo> ints = new ArrayList<>();
            for (Integer i : queues.apply(types[0])) {
                ints.add(new QueueInfo(
                        types,
                        count,
                        i
                ));
            }
            for (int i = 1; i < types.length; i++) {
                List<QueueInfo> ints1 = new ArrayList<>();
                for (Integer i1 : queues.apply(types[i])) {
                    ints1.add(new QueueInfo(
                            types,
                            count,
                            i1
                    ));
                }
                ints.retainAll(ints1);
            }
            this.cache = ints;
        }

        public List<List<QueueInfo>> options() {
            List<List<QueueInfo>> opt = new ArrayList<>();
            for (QueueInfo i : cache) opt.add(Arrays.asList(i));
            return opt;
        }

        @Override
        public List<QueueInfo> select() {
            return Arrays.asList(cache.get(0));
        }
    }

    public abstract List<List<QueueInfo>> options();

    public abstract List<QueueInfo> select();

    public static QueueRequest EITHER(QueueRequest... requests) {
        return new Either(requests);
    }

    public static QueueRequest SPLIT(QueueRequest... requests) {
        return new Split(requests);
    }

    public static QueueRequest SHARED(int count, ReniQueueType... types) {
        return new Shared(count, types);
    }

    public static QueueRequest SHARED(ReniQueueType... types) {
        return new Shared(types);
    }

    public static QueueRequest SINGLE(int count, ReniQueueType type) {
        return new Shared(count, type);
    }

    public static QueueRequest SINGLE(ReniQueueType type) {
        return new Shared(type);
    }
}
