
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.simple.*;
import org.json.simple.parser.*;

public class FileManager {
    private final Map<String, Long> quotas = new HashMap<>();
    private final Map<String, Set<String>> permissions = new HashMap<>(); // plus propre avec Set

    private static final String DEFAULT_QUOTAS_RELATIVE = "resources/quotas.json";
    private static final String DEFAULT_PERMS_RELATIVE = "resources/permissions.json";

    private final Path quotasPath;
    private final Path permsPath;

    private volatile long quotasLastModifiedMs = -1L;

    public FileManager() {
        this.quotasPath = resolveConfigPath("quotas.json", "SMARTDRIVE_QUOTAS_PATH", "smartdrive.quotasPath", DEFAULT_QUOTAS_RELATIVE);
        this.permsPath = resolveConfigPath("permissions.json", "SMARTDRIVE_PERMISSIONS_PATH", "smartdrive.permissionsPath", DEFAULT_PERMS_RELATIVE);
        reloadQuotas();
        reloadPermissions();
    }
    public synchronized Long getQuota(String user) {
        maybeReloadQuotas();
        return quotas.get(user);
    }

    private void maybeReloadQuotas() {
        try {
            File f = quotasPath.toFile();
            long lm = f.exists() ? f.lastModified() : -1L;
            if (lm > 0 && lm != quotasLastModifiedMs) {
                reloadQuotas();
            }
        } catch (Exception ignored) {
        }
    }

    public synchronized boolean setQuota(String user, long quotaBytes) {
        if (user == null || user.trim().isEmpty())
            return false;
        if (quotaBytes < 0)
            quotaBytes = 0;
        quotas.put(user.trim(), quotaBytes);
        saveQuotas();
        return true;
    }

    public synchronized boolean removeUserQuota(String user) {
        if (user == null || user.trim().isEmpty())
            return false;
        String u = user.trim();
        if (!quotas.containsKey(u))
            return false;
        quotas.remove(u);
        saveQuotas();
        return true;
    }

    private static Path resolveConfigPath(String filename, String envVar, String sysProp, String defaultRelative) {
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
        List<Path> candidates = List.of(
                cwd.resolve(defaultRelative),
                cwd.resolve("server").resolve("resources").resolve(filename),
                cwd.resolve("backend").resolve("server").resolve("resources").resolve(filename),
                cwd.resolve("..").resolve("server").resolve("resources").resolve(filename)
        );
        for (Path c : candidates) {
            if (Files.exists(c)) {
                return c.toAbsolutePath().normalize();
            }
        }

        return cwd.resolve(defaultRelative).toAbsolutePath().normalize();
    }
    

    // À appeler seulement au démarrage ou via admin (pas à chaque check !)
    public synchronized void reloadQuotas() {
        quotas.clear();
        try {
            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject) parser.parse(new FileReader(quotasPath.toFile()));
            for (Object key : obj.keySet()) {
                String user = (String) key;
                Object val = obj.get(user);
                long q = 0;
                if (val instanceof Number) {
                    q = ((Number) val).longValue();
                } else if (val instanceof String) {
                    try { q = Long.parseLong((String) val); } catch (Exception ignored) {}
                }
                quotas.put(user, q);
            }
            System.out.println("[FileManager] Quotas chargés : " + quotas);
            try {
                File f = quotasPath.toFile();
                quotasLastModifiedMs = f.exists() ? f.lastModified() : -1L;
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement quotas (" + quotasPath + ") : " + e);
        }
    }

    private synchronized void reloadPermissions() {
        permissions.clear();
        try {
            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject) parser.parse(new FileReader(permsPath.toFile()));
            for (Object key : obj.keySet()) {
                String user = (String) key;
                JSONArray arr = (JSONArray) obj.get(user);
                Set<String> perms = new HashSet<>();
                for (Object p : arr) perms.add(((String)p).toLowerCase());
                permissions.put(user, perms);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement permissions (" + permsPath + ") : " + e);
        }
    }

    public boolean canWrite(String user) {
        Set<String> perms = permissions.getOrDefault(user, Collections.emptySet());
        return perms.contains("write");
    }

    public synchronized boolean hasEnoughQuota(String user, long needed) {
        maybeReloadQuotas();
        Long current = quotas.get(user);
        if (current == null) return false;
        return current >= needed;
    }

    public synchronized void consumeQuota(String user, long size) {
        maybeReloadQuotas();
        Long current = quotas.get(user);
        if (current != null) {
            quotas.put(user, Math.max(0, current - size));
            saveQuotas();
        }
    }

    private synchronized void saveQuotas() {
        try {
            Path parent = quotasPath.getParent();
            if (parent != null) Files.createDirectories(parent);

            JSONObject obj = new JSONObject();
            obj.putAll(quotas);
            try (Writer w = Files.newBufferedWriter(quotasPath, StandardCharsets.UTF_8)) {
                w.write(obj.toJSONString());
                w.flush();
            }
        } catch (Exception e) {
            System.err.println("Erreur sauvegarde quotas (" + quotasPath + ") : " + e);
        }
    }

    public void forceReload() {
        reloadQuotas();
        reloadPermissions();
    }
}
