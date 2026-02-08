
import java.io.*;
import java.util.*;
import org.json.simple.*;
import org.json.simple.parser.*;

public class FileManager {
    private final Map<String, Long> quotas = new HashMap<>();
    private final Map<String, Set<String>> permissions = new HashMap<>(); // plus propre avec Set

    private static final String QUOTAS_PATH   = "resources/quotas.json";
    private static final String PERMS_PATH    = "resources/permissions.json";
    
    public FileManager() {
        reloadQuotas();
        reloadPermissions();
    }
        public synchronized Long getQuota(String user) {
        return quotas.get(user);
    }
    

    // À appeler seulement au démarrage ou via admin (pas à chaque check !)
    public synchronized void reloadQuotas() {
        quotas.clear();
        try {
            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject) parser.parse(new FileReader(QUOTAS_PATH));
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
        } catch (Exception e) {
            System.err.println("Erreur chargement quotas : " + e);
        }
    }

    private synchronized void reloadPermissions() {
        permissions.clear();
        try {
            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject) parser.parse(new FileReader(PERMS_PATH));
            for (Object key : obj.keySet()) {
                String user = (String) key;
                JSONArray arr = (JSONArray) obj.get(user);
                Set<String> perms = new HashSet<>();
                for (Object p : arr) perms.add(((String)p).toLowerCase());
                permissions.put(user, perms);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement permissions : " + e);
        }
    }

    public boolean canWrite(String user) {
        Set<String> perms = permissions.getOrDefault(user, Collections.emptySet());
        return perms.contains("write");
    }

    public synchronized boolean hasEnoughQuota(String user, long needed) {
        Long current = quotas.get(user);
        if (current == null) return false;
        return current >= needed;
    }

    public synchronized void consumeQuota(String user, long size) {
        Long current = quotas.get(user);
        if (current != null) {
            quotas.put(user, Math.max(0, current - size));
            saveQuotas();
        }
    }

    private synchronized void saveQuotas() {
        try (FileWriter fw = new FileWriter(QUOTAS_PATH)) {
            JSONObject obj = new JSONObject();
            obj.putAll(quotas);
            fw.write(obj.toJSONString());
            fw.flush();
        } catch (Exception e) {
            System.err.println("Erreur sauvegarde quotas : " + e);
        }
    }

    public void forceReload() {
        reloadQuotas();
        reloadPermissions();
    }
}