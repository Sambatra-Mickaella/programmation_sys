import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class StoragePaths {
    private StoragePaths() {}

    static boolean isSafeFilename(String name) {
        if (name == null) return false;
        String n = name.trim();
        if (n.isEmpty()) return false;
        if (n.contains("..")) return false;
        if (n.contains("/") || n.contains("\\") || n.contains("\u0000")) return false;
        return true;
    }

    static File findSharedStorageRoot() {
        String[] candidates = new String[] {
                "shared_storage",
                "../shared_storage",
                "../../shared_storage",
                "backend/shared_storage",
                "../backend/shared_storage",
                "../../backend/shared_storage"
        };
        for (String c : candidates) {
            try {
                File f = new File(c);
                if (f.exists() && f.isDirectory()) return f;
            } catch (Exception ignored) {}
        }
        return new File("shared_storage");
    }

    static File usersRootDir() {
        return new File(findSharedStorageRoot(), "users");
    }

    static File userDir(String username) {
        return new File(usersRootDir(), username);
    }

    static Path resolveServerResourcePath(String filename, String envVar, String sysProp, String defaultRelative) {
        String override = System.getProperty(sysProp);
        if (override == null || override.trim().isEmpty()) {
            override = System.getenv(envVar);
        }
        if (override != null && !override.trim().isEmpty()) {
            Path p = Paths.get(override.trim());
            if (Files.isDirectory(p)) {
                p = p.resolve(filename);
            }
            if (Files.exists(p)) {
                return p.toAbsolutePath().normalize();
            }
        }

        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path[] candidates = new Path[] {
                cwd.resolve(defaultRelative),
                cwd.resolve("resources").resolve(filename),
                cwd.resolve("server").resolve("resources").resolve(filename),
                cwd.resolve("backend").resolve("server").resolve("resources").resolve(filename),
                cwd.resolve("..").resolve("server").resolve("resources").resolve(filename),
                cwd.resolve("..").resolve("backend").resolve("server").resolve("resources").resolve(filename)
        };
        for (Path c : candidates) {
            if (Files.exists(c)) {
                return c.toAbsolutePath().normalize();
            }
        }
        return cwd.resolve(defaultRelative).toAbsolutePath().normalize();
    }
}
