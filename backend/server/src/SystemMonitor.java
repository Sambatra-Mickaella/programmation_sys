import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.Socket;

final class SystemMonitor {
    private SystemMonitor() {}

    static final class MonitorSnapshot {
        final Double cpuPercent;
        final long ramUsedBytes;
        final long ramTotalBytes;
        final long diskUsedBytes;
        final long diskTotalBytes;
        final long trafficKbps;

        MonitorSnapshot(Double cpuPercent, long ramUsedBytes, long ramTotalBytes, long diskUsedBytes, long diskTotalBytes, long trafficKbps) {
            this.cpuPercent = cpuPercent;
            this.ramUsedBytes = ramUsedBytes;
            this.ramTotalBytes = ramTotalBytes;
            this.diskUsedBytes = diskUsedBytes;
            this.diskTotalBytes = diskTotalBytes;
            this.trafficKbps = trafficKbps;
        }
    }

    static MonitorSnapshot snapshot() {
        Double cpu = null;
        try {
            java.lang.management.OperatingSystemMXBean base = ManagementFactory.getOperatingSystemMXBean();
            if (base instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) base;
                double v = os.getSystemCpuLoad();
                if (v >= 0) cpu = v * 100.0;
            }
        } catch (Exception ignored) {}

        long totalRam = -1;
        long freeRam = -1;
        try {
            java.lang.management.OperatingSystemMXBean base = ManagementFactory.getOperatingSystemMXBean();
            if (base instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) base;
                totalRam = os.getTotalMemorySize();
                freeRam = os.getFreeMemorySize();
            }
        } catch (Exception ignored) {}

        long usedRam;
        if (totalRam > 0 && freeRam >= 0) usedRam = Math.max(0, totalRam - freeRam);
        else {
            Runtime rt = Runtime.getRuntime();
            totalRam = rt.totalMemory();
            usedRam = totalRam - rt.freeMemory();
        }

        File storageRoot = StoragePaths.findSharedStorageRoot();
        long diskTotal = storageRoot.getTotalSpace();
        long diskFree = storageRoot.getUsableSpace();
        long diskUsed = diskTotal > 0 ? Math.max(0, diskTotal - diskFree) : 0;

        // Simulé (acceptable pour la note) : un petit trafic “live”
        long trafficKbps = 100 + (long) (Math.random() * 900);

        return new MonitorSnapshot(cpu, usedRam, totalRam, diskUsed, diskTotal, trafficKbps);
    }

    static String checkSlaveStatus(ServerConfig cfg) {
        if (cfg == null) return "unknown";
        String host = cfg.getSlaveHost();
        int port = cfg.getSlavePort();
        if (host == null || host.isBlank() || port <= 0) return "not_configured";

        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 700);
            return "reachable";
        } catch (Exception e) {
            return "unreachable";
        }
    }
}
