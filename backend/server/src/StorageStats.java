import java.io.File;

final class StorageStats {
    private StorageStats() {}

    static final class Stats {
        final long totalBytes;
        final long fileCount;

        Stats(long totalBytes, long fileCount) {
            this.totalBytes = totalBytes;
            this.fileCount = fileCount;
        }
    }

    static Stats computeTotalUserStorage() {
        File usersRoot = StoragePaths.usersRootDir();
        return walk(usersRoot);
    }

    private static Stats walk(File root) {
        if (root == null || !root.exists()) return new Stats(0L, 0L);
        if (root.isFile()) {
            return new Stats(root.length(), 1L);
        }
        long bytes = 0L;
        long files = 0L;
        File[] children = root.listFiles();
        if (children == null) return new Stats(0L, 0L);
        for (File c : children) {
            Stats s = walk(c);
            bytes += s.totalBytes;
            files += s.fileCount;
        }
        return new Stats(bytes, files);
    }
}
