import tfc.renirol.util.accel.memory.MemoryManager;
import tfc.renirol.util.accel.memory.cpp.BestFitManager;
import tfc.renirol.util.accel.memory.java.BestFitAllocator;
import tfc.renirol.util.windows.PerformanceCounters;

public class MemoryTracker {
    public static void main(String[] args) {
//        MemoryManager manager = new BestFitAllocator(204800, 4);
        MemoryManager manager = MemoryManager.createBestFit(204800, 4);
        int addr0 = manager.allocate(16);
        int addr1 = manager.allocate(32);
        int addr2 = manager.allocate(16);
        int addr4 = manager.allocate(16);
        System.out.println(addr0);
        System.out.println(addr1);
        System.out.println(addr2);
        System.out.println(addr4);
        System.out.println("Free " + addr1 + " to " + (addr1 + 32));
        System.out.println(manager.release(addr1));

        addr1 = manager.allocate(16);
        int addr3 = manager.allocate(16);
        System.out.println(addr1);
        System.out.println(addr3);

        System.out.println("Filling memory");
        int v = 1;
        while (v != -1) {
            long pc = PerformanceCounters.QueryPerformanceCounter();
            System.out.println(v = manager.allocate(16));
            System.out.println("PC:" + (PerformanceCounters.QueryPerformanceCounter() - pc));
        }

        System.out.println("Freeing random memory");
        for (int i = 0; i < 1000; i++) {
            int i1 = 0;
            if (!manager.release(i1 = (((int) (Math.random() * 204800 / 4)) * 4))) {
                i--;
                manager.release(i1 + 16);
            }
        }
        v = 1;
        while (v != -1) {
            long pc = PerformanceCounters.QueryPerformanceCounter();
            System.out.println(v = manager.allocate(32));
            System.out.println("PC:" + (PerformanceCounters.QueryPerformanceCounter() - pc));
        }

        manager.destroy();
    }
}
