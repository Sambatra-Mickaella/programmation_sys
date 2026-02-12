import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

final class UserStore {
    private UserStore() {}

    static final class UserRecord {
        final String password;
        final boolean admin;
        final boolean blocked;

        UserRecord(String password, boolean admin, boolean blocked) {
            this.password = password;
            this.admin = admin;
            this.blocked = blocked;
        }
    }

    static UserRecord parseUserRecord(Object entry) {
        if (entry == null) return null;

        // Backward-compatible: { "alice": "1234" }
        if (entry instanceof String) {
            return new UserRecord((String) entry, false, false);
        }

        // New format: { "alice": { "password": "1234", "admin": true, "blocked": false } }
        if (entry instanceof JSONObject) {
            JSONObject obj = (JSONObject) entry;
            Object pw = obj.get("password");
            if (pw == null) return null;

            boolean admin = parseBoolean(obj.get("admin"));
            boolean blocked = parseBoolean(obj.get("blocked"));
            return new UserRecord(String.valueOf(pw), admin, blocked);
        }

        return null;
    }

    private static boolean parseBoolean(Object v) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v == null) return false;
        return Boolean.parseBoolean(String.valueOf(v));
    }

    static Path resolveUsersJsonPath() {
        String override = System.getProperty("smartdrive.usersPath");
        if (override == null || override.trim().isEmpty()) {
            override = System.getenv("SMARTDRIVE_USERS_PATH");
        }
        if (override != null && !override.trim().isEmpty()) {
            Path p = Paths.get(override.trim());
            if (Files.isDirectory(p)) {
                p = p.resolve("user.json");
            }
            if (Files.exists(p)) {
                return p.toAbsolutePath().normalize();
            }
        }

        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path[] candidates = new Path[] {
                cwd.resolve("resources").resolve("user.json"),
                cwd.resolve("server").resolve("resources").resolve("user.json"),
                cwd.resolve("backend").resolve("server").resolve("resources").resolve("user.json"),
            cwd.resolve("..").resolve("server").resolve("resources").resolve("user.json"),
            cwd.resolve("..").resolve("backend").resolve("server").resolve("resources").resolve("user.json")
        };
        for (Path c : candidates) {
            if (Files.exists(c)) {
                return c.toAbsolutePath().normalize();
            }
        }
        return cwd.resolve("resources").resolve("user.json").toAbsolutePath().normalize();
    }

    static synchronized JSONObject loadAllUsers() throws Exception {
        Path usersJson = resolveUsersJsonPath();
        JSONParser parser = new JSONParser();
        Object parsed = parser.parse(new FileReader(usersJson.toFile()));
        if (parsed instanceof JSONObject) return (JSONObject) parsed;
        return new JSONObject();
    }

    static synchronized boolean saveAllUsers(JSONObject users) {
        try {
            Path usersJson = resolveUsersJsonPath();
            Path parent = usersJson.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (FileWriter fw = new FileWriter(usersJson.toFile())) {
                fw.write(users.toJSONString());
                fw.flush();
            }
            return true;
        } catch (Exception e) {
            System.out.println("[UserStore] save failed: " + e.getMessage());
            return false;
        }
    }

    static synchronized boolean setBlocked(String username, boolean blocked) {
        if (username == null || username.trim().isEmpty()) return false;
        String u = username.trim();
        try {
            JSONObject users = loadAllUsers();
            Object entry = users.get(u);
            UserRecord rec = parseUserRecord(entry);
            if (rec == null || rec.password == null) return false;

            JSONObject obj;
            if (entry instanceof JSONObject) {
                obj = (JSONObject) entry;
            } else {
                obj = new JSONObject();
                obj.put("password", rec.password);
                obj.put("admin", rec.admin);
            }
            obj.put("blocked", blocked);
            users.put(u, obj);
            return saveAllUsers(users);
        } catch (Exception e) {
            return false;
        }
    }

    static synchronized boolean deleteUser(String username) {
        if (username == null || username.trim().isEmpty()) return false;
        String u = username.trim();
        try {
            JSONObject users = loadAllUsers();
            if (!users.containsKey(u)) return false;
            users.remove(u);
            return saveAllUsers(users);
        } catch (Exception e) {
            return false;
        }
    }
}
