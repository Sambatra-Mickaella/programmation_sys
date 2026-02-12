import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

final class VersionManager {
    private VersionManager() {}

    private static File versionsDir(String username, String filename) {
        return new File(StoragePaths.userDir(username), ".versions" + File.separator + filename);
    }

    static synchronized void archiveIfExists(String username, String filename) {
        if (!StoragePaths.isSafeFilename(filename)) return;
        File userDir = StoragePaths.userDir(username);
        File current = new File(userDir, filename);
        if (!current.exists() || !current.isFile()) return;

        long now = System.currentTimeMillis();
        File dir = versionsDir(username, filename);
        if (!dir.exists()) dir.mkdirs();

        File dst = new File(dir, now + "_" + filename);
        try {
            Files.move(current.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }

    static final class VersionRow {
        final String id;
        final long size;
        final long createdAt;

        VersionRow(String id, long size, long createdAt) {
            this.id = id;
            this.size = size;
            this.createdAt = createdAt;
        }
    }

    static synchronized List<VersionRow> list(String username, String filename) {
        List<VersionRow> out = new ArrayList<>();
        if (!StoragePaths.isSafeFilename(filename)) return out;

        File dir = versionsDir(username, filename);
        if (!dir.exists() || !dir.isDirectory()) return out;
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return out;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (File f : files) {
            if (f == null || !f.isFile()) continue;
            String id = f.getName();
            long createdAt = f.lastModified();
            out.add(new VersionRow(id, f.length(), createdAt));
        }
        return out;
    }

    static synchronized boolean restore(String username, String filename, String versionId) {
        if (!StoragePaths.isSafeFilename(filename)) return false;
        if (versionId == null || versionId.trim().isEmpty()) return false;

        File dir = versionsDir(username, filename);
        File versionFile = new File(dir, versionId);
        if (!versionFile.exists() || !versionFile.isFile()) return false;

        // Mettre la version courante en archive d'abord
        archiveIfExists(username, filename);

        File dst = new File(StoragePaths.userDir(username), filename);
        try {
            Files.copy(versionFile.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
