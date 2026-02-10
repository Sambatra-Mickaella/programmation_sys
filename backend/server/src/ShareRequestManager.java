import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ShareRequestManager {
    private static final String REQUESTS_PATH = "server/resources/share_requests.json";

    private static void ensureFileExists() {
        File f = new File(REQUESTS_PATH);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        if (!f.exists()) {
            try (FileWriter fw = new FileWriter(f)) {
                fw.write("[]");
            } catch (Exception ignored) {}
        }
    }

    private static JSONArray loadAll() {
        ensureFileExists();
        try {
            JSONParser parser = new JSONParser();
            Object parsed = parser.parse(new FileReader(REQUESTS_PATH));
            if (parsed instanceof JSONArray) return (JSONArray) parsed;
        } catch (Exception ignored) {}
        return new JSONArray();
    }

    private static void saveAll(JSONArray arr) {
        ensureFileExists();
        try (FileWriter fw = new FileWriter(REQUESTS_PATH)) {
            fw.write(arr.toJSONString());
            fw.flush();
        } catch (Exception ignored) {}
    }

    private static boolean matches(JSONObject o, String owner, String requester, String file) {
        return owner.equalsIgnoreCase(String.valueOf(o.get("owner")))
                && requester.equalsIgnoreCase(String.valueOf(o.get("requester")))
                && file.equals(String.valueOf(o.get("file")));
    }

    public static class RequestRow {
        public final String owner;
        public final String requester;
        public final String file;
        public final String status;
        public final long createdAt;

        RequestRow(String owner, String requester, String file, String status, long createdAt) {
            this.owner = owner;
            this.requester = requester;
            this.file = file;
            this.status = status;
            this.createdAt = createdAt;
        }
    }

    public static synchronized String getStatus(String owner, String requester, String file) {
        if (owner == null || requester == null || file == null) return "none";
        if (owner.equalsIgnoreCase(requester)) return "approved";
        JSONArray arr = loadAll();
        for (Object it : arr) {
            if (!(it instanceof JSONObject)) continue;
            JSONObject o = (JSONObject) it;
            if (matches(o, owner, requester, file)) {
                Object st = o.get("status");
                if (st == null) return "none";
                return String.valueOf(st).toLowerCase();
            }
        }
        return "none";
    }

    public static synchronized String requestRead(String owner, String requester, String file) {
        if (owner == null || requester == null || file == null) return "ERROR Invalid params";
        if (owner.equalsIgnoreCase(requester)) return "OK approved";

        JSONArray arr = loadAll();
        long now = System.currentTimeMillis();

        for (Object it : arr) {
            if (!(it instanceof JSONObject)) continue;
            JSONObject o = (JSONObject) it;
            if (matches(o, owner, requester, file)) {
                String status = String.valueOf(o.get("status")).toLowerCase();
                if ("approved".equals(status)) return "OK approved";
                // pending/denied -> repasser en pending
                o.put("status", "pending");
                o.put("created_at", now);
                saveAll(arr);
                return "OK pending";
            }
        }

        JSONObject rec = new JSONObject();
        rec.put("owner", owner);
        rec.put("requester", requester);
        rec.put("file", file);
        rec.put("status", "pending");
        rec.put("created_at", now);
        arr.add(rec);
        saveAll(arr);
        return "OK pending";
    }

    public static synchronized String respond(String owner, String requester, String file, String action) {
        if (owner == null || requester == null || file == null || action == null) return "ERROR Invalid params";
        String status;
        String a = action.trim().toLowerCase();
        if ("approve".equals(a) || "approved".equals(a)) status = "approved";
        else if ("deny".equals(a) || "denied".equals(a) || "reject".equals(a)) status = "denied";
        else return "ERROR Invalid action";

        JSONArray arr = loadAll();
        long now = System.currentTimeMillis();
        for (Object it : arr) {
            if (!(it instanceof JSONObject)) continue;
            JSONObject o = (JSONObject) it;
            if (matches(o, owner, requester, file)) {
                o.put("status", status);
                o.put("updated_at", now);
                saveAll(arr);
                return "OK " + status;
            }
        }
        // Si pas trouvé, on crée directement (cas edge)
        JSONObject rec = new JSONObject();
        rec.put("owner", owner);
        rec.put("requester", requester);
        rec.put("file", file);
        rec.put("status", status);
        rec.put("created_at", now);
        rec.put("updated_at", now);
        arr.add(rec);
        saveAll(arr);
        return "OK " + status;
    }

    public static synchronized List<RequestRow> listIncoming(String owner) {
        List<RequestRow> out = new ArrayList<>();
        if (owner == null) return out;
        JSONArray arr = loadAll();
        for (Object it : arr) {
            if (!(it instanceof JSONObject)) continue;
            JSONObject o = (JSONObject) it;
            if (!owner.equalsIgnoreCase(String.valueOf(o.get("owner")))) continue;
            String requester = String.valueOf(o.get("requester"));
            String file = String.valueOf(o.get("file"));
            String status = String.valueOf(o.get("status")).toLowerCase();
            long createdAt = 0L;
            Object ca = o.get("created_at");
            if (ca instanceof Number) createdAt = ((Number) ca).longValue();
            out.add(new RequestRow(owner, requester, file, status, createdAt));
        }
        return out;
    }
}

