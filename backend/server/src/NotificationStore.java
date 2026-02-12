import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

final class NotificationStore {
    private NotificationStore() {}

    private static Path notificationsDirPath() {
        return StoragePaths.resolveServerResourcePath(
                "notifications",
                "SMARTDRIVE_NOTIFS_PATH",
                "smartdrive.notifsPath",
                "resources/notifications"
        );
    }

    private static File fileForUser(String username) {
        Path dir = notificationsDirPath();
        try { Files.createDirectories(dir); } catch (Exception ignored) {}
        String safe = (username == null) ? "unknown" : username.replaceAll("[^a-zA-Z0-9._-]", "_");
        return dir.resolve(safe + ".json").toFile();
    }

    private static JSONArray load(String username) {
        File f = fileForUser(username);
        if (!f.exists()) {
            return new JSONArray();
        }
        try {
            JSONParser parser = new JSONParser();
            Object parsed = parser.parse(new FileReader(f));
            if (parsed instanceof JSONArray) return (JSONArray) parsed;
        } catch (Exception ignored) {}
        return new JSONArray();
    }

    private static void save(String username, JSONArray arr) {
        File f = fileForUser(username);
        try (FileWriter fw = new FileWriter(f)) {
            fw.write(arr.toJSONString());
            fw.flush();
        } catch (Exception ignored) {}
    }

    static synchronized void push(String username, String type, String message) {
        if (username == null || username.trim().isEmpty()) return;
        long now = System.currentTimeMillis();
        JSONArray arr = load(username);
        JSONObject o = new JSONObject();
        o.put("ts", now);
        o.put("type", type == null ? "info" : type);
        o.put("msg", message == null ? "" : message);
        arr.add(o);

        // Garder une taille raisonnable
        while (arr.size() > 200) {
            arr.remove(0);
        }
        save(username, arr);
    }

    static final class NotifRow {
        final long ts;
        final String type;
        final String msg;

        NotifRow(long ts, String type, String msg) {
            this.ts = ts;
            this.type = type;
            this.msg = msg;
        }
    }

    static synchronized List<NotifRow> list(String username) {
        JSONArray arr = load(username);
        List<NotifRow> out = new ArrayList<>();
        for (Object it : arr) {
            if (!(it instanceof JSONObject)) continue;
            JSONObject o = (JSONObject) it;
            long ts = 0L;
            Object t = o.get("ts");
            if (t instanceof Number) ts = ((Number) t).longValue();
            String type = String.valueOf(o.get("type"));
            String msg = String.valueOf(o.get("msg"));
            out.add(new NotifRow(ts, type, msg));
        }
        return out;
    }

    static synchronized void clear(String username) {
        save(username, new JSONArray());
    }
}
