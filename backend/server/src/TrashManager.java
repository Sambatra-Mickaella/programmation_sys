import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

final class TrashManager {
    private TrashManager() {}

    private static File trashDir(String username) {
        return new File(StoragePaths.userDir(username), ".trash");
    }

    private static File trashIndex(String username) {
        return new File(trashDir(username), "index.json");
    }

    private static void ensureIndex(String username) {
        File dir = trashDir(username);
        if (!dir.exists()) dir.mkdirs();
        File idx = trashIndex(username);
        if (!idx.exists()) {
            try (FileWriter fw = new FileWriter(idx)) {
                fw.write("[]");
            } catch (Exception ignored) {}
        }
    }

    private static JSONArray loadIndex(String username) {
        ensureIndex(username);
        try {
            JSONParser parser = new JSONParser();
            Object parsed = parser.parse(new FileReader(trashIndex(username)));
            if (parsed instanceof JSONArray) return (JSONArray) parsed;
        } catch (Exception ignored) {}
        return new JSONArray();
    }

    private static void saveIndex(String username, JSONArray arr) {
        ensureIndex(username);
        try (FileWriter fw = new FileWriter(trashIndex(username))) {
            fw.write(arr.toJSONString());
            fw.flush();
        } catch (Exception ignored) {}
    }

    static final class TrashRow {
        final String id;
        final String original;
        final long size;
        final long deletedAt;

        TrashRow(String id, String original, long size, long deletedAt) {
            this.id = id;
            this.original = original;
            this.size = size;
            this.deletedAt = deletedAt;
        }
    }

    static synchronized String moveToTrash(String username, String filename) throws Exception {
        if (!StoragePaths.isSafeFilename(filename)) {
            return null;
        }

        File userDir = StoragePaths.userDir(username);
        File src = new File(userDir, filename);
        if (!src.exists() || !src.isFile()) {
            return null;
        }

        long now = System.currentTimeMillis();
        String id = now + "_" + filename;
        String storedName = id;

        File dir = trashDir(username);
        if (!dir.exists()) dir.mkdirs();

        File dst = new File(dir, storedName);
        Files.move(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);

        JSONArray arr = loadIndex(username);
        JSONObject rec = new JSONObject();
        rec.put("id", id);
        rec.put("original", filename);
        rec.put("stored", storedName);
        rec.put("size", dst.length());
        rec.put("deleted_at", now);
        arr.add(rec);
        saveIndex(username, arr);

        return id;
    }

    static synchronized List<TrashRow> list(String username) {
        JSONArray arr = loadIndex(username);
        List<TrashRow> out = new ArrayList<>();
        for (Object it : arr) {
            if (!(it instanceof JSONObject)) continue;
            JSONObject o = (JSONObject) it;
            String id = String.valueOf(o.get("id"));
            String original = String.valueOf(o.get("original"));
            long size = 0L;
            Object s = o.get("size");
            if (s instanceof Number) size = ((Number) s).longValue();
            long deletedAt = 0L;
            Object d = o.get("deleted_at");
            if (d instanceof Number) deletedAt = ((Number) d).longValue();
            out.add(new TrashRow(id, original, size, deletedAt));
        }
        return out;
    }

    static synchronized boolean restore(String username, String id) throws Exception {
        if (id == null || id.trim().isEmpty()) return false;
        JSONArray arr = loadIndex(username);
        JSONObject found = null;
        for (Object it : arr) {
            if (!(it instanceof JSONObject)) continue;
            JSONObject o = (JSONObject) it;
            if (id.equals(String.valueOf(o.get("id")))) {
                found = o;
                break;
            }
        }
        if (found == null) return false;

        String original = String.valueOf(found.get("original"));
        String stored = String.valueOf(found.get("stored"));
        if (!StoragePaths.isSafeFilename(original)) return false;

        File src = new File(trashDir(username), stored);
        if (!src.exists() || !src.isFile()) return false;

        File dst = new File(StoragePaths.userDir(username), original);
        if (dst.exists()) {
            // Si le fichier existe déjà, on restaure sous un nom unique.
            String base = original;
            String alt = id + "_" + base;
            dst = new File(StoragePaths.userDir(username), alt);
        }
        Files.move(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        arr.remove(found);
        saveIndex(username, arr);
        return true;
    }

    static synchronized int purge(String username, String idOrAll) {
        JSONArray arr = loadIndex(username);
        if (idOrAll == null) return 0;
        String v = idOrAll.trim();
        int purged = 0;

        if ("ALL".equalsIgnoreCase(v)) {
            for (Object it : arr) {
                if (!(it instanceof JSONObject)) continue;
                JSONObject o = (JSONObject) it;
                String stored = String.valueOf(o.get("stored"));
                try {
                    Files.deleteIfExists(new File(trashDir(username), stored).toPath());
                    purged++;
                } catch (Exception ignored) {}
            }
            saveIndex(username, new JSONArray());
            return purged;
        }

        JSONObject found = null;
        for (Object it : arr) {
            if (!(it instanceof JSONObject)) continue;
            JSONObject o = (JSONObject) it;
            if (v.equals(String.valueOf(o.get("id")))) {
                found = o;
                break;
            }
        }
        if (found == null) return 0;
        String stored = String.valueOf(found.get("stored"));
        try {
            Files.deleteIfExists(new File(trashDir(username), stored).toPath());
            purged = 1;
        } catch (Exception ignored) {}
        arr.remove(found);
        saveIndex(username, arr);
        return purged;
    }
}
