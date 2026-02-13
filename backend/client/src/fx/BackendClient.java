package fx;

import controller.BackendConfig;
import model.Serveur;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BackendClient {

    @FunctionalInterface
    private interface ConnectionAction<T> {
        T run(Connection conn) throws Exception;
    }

    public record AuthResult(String username, boolean admin, String rawResponse) {}
    public record FileEntry(String name, long size, long modifiedAt) {}
    public record SharedFileEntry(String owner, String name, long size, String status) {}
    public record TrashEntry(String id, String originalName, long size, String deletedAt) {}
    public record VersionEntry(String id, long size, String createdAt) {}
    public record NotificationEntry(String ts, String type, String message) {}
    public record ShareRequestEntry(String requester, String file, String status, String createdAt) {}
    public record AdminUserEntry(String username, boolean admin, boolean blocked, long quota) {}

    public AuthResult login(String username, String password) throws Exception {
        return withRawConnection(conn -> {
            String resp = conn.login(username, password);
            if (resp == null) {
                throw new IOException("Aucune reponse du serveur");
            }
            if (!resp.startsWith("Welcome")) {
                throw new IOException(resp);
            }
            String normalizedUser = parseWelcomeUser(resp);
            boolean admin = parseAdminFlag(resp);
            return new AuthResult(normalizedUser, admin, resp);
        });
    }

    public List<FileEntry> listFiles(String username, String password) throws Exception {
        return withAuthenticatedConnection(username, password, conn -> {
            conn.writeLine("LIST");
            String countLine = conn.readLine();
            int count = parseCount(countLine);
            List<FileEntry> rows = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String line = conn.readLine();
                if (line == null) {
                    continue;
                }
                String[] parts = line.split(";", 3);
                String name = parts.length > 0 ? parts[0].trim() : "";
                long size = parts.length > 1 ? parseLong(parts[1], 0L) : 0L;
                long modified = parts.length > 2 ? parseLong(parts[2], 0L) : 0L;
                if (!name.isEmpty()) {
                    rows.add(new FileEntry(name, size, modified));
                }
            }
            String end = conn.readLine();
            if (end != null && !"END".equalsIgnoreCase(end.trim())) {
                // No-op: tolerate older/newer protocol variants.
            }
            return rows;
        });
    }

    public long getQuota(String username, String password) throws Exception {
        return withAuthenticatedConnection(username, password, conn -> {
            conn.writeLine("QUOTA");
            String resp = conn.readLine();
            if (resp == null || resp.startsWith("ERROR")) {
                throw new IOException(resp == null ? "Aucune reponse QUOTA" : resp);
            }
            return parseLong(resp, -1L);
        });
    }

    public String uploadFile(String username, String password, Path filePath) throws Exception {
        if (filePath == null || !Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new IOException("Fichier invalide");
        }
        long size = Files.size(filePath);
        if (size <= 0L) {
            throw new IOException("Fichier vide");
        }
        String original = filePath.getFileName().toString();
        String safeName = original.replace(";", "_");

        return withAuthenticatedConnection(username, password, conn -> {
            conn.writeLine("UPLOAD;" + safeName + ";" + size);
            String ready = conn.readLine();
            if (ready == null) {
                throw new IOException("Aucune reponse upload");
            }
            if (!"READY".equalsIgnoreCase(ready.trim())) {
                throw new IOException(ready);
            }
            try (InputStream fis = new FileInputStream(filePath.toFile())) {
                conn.sendBytes(fis, size);
            }
            String result = conn.readLine();
            if (result == null) {
                throw new IOException("Upload termine sans accusé");
            }
            return result;
        });
    }

    public Path downloadOwnFile(String username, String password, String filename, Path target) throws Exception {
        return downloadWithCommand(username, password, "DOWNLOAD;" + filename, target);
    }

    public Path downloadSharedFile(String username, String password, String owner, String filename, Path target)
            throws Exception {
        return downloadWithCommand(username, password, "DOWNLOAD_AS;" + owner + ";" + filename, target);
    }

    public Path adminDownloadAs(String username, String password, String owner, String filename, Path target)
            throws Exception {
        return downloadWithCommand(username, password, "ADMIN_DOWNLOAD_AS;" + owner + ";" + filename, target);
    }

    public String deleteToTrash(String username, String password, String filename) throws Exception {
        return simpleCommand(username, password, "DELETE;" + filename);
    }

    public List<TrashEntry> listTrash(String username, String password) throws Exception {
        return withAuthenticatedConnection(username, password, conn -> {
            conn.writeLine("TRASH_LIST");
            String countLine = conn.readLine();
            int count = parseCount(countLine);
            List<TrashEntry> rows = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String line = conn.readLine();
                if (line == null) {
                    continue;
                }
                String[] p = line.split(";", 4);
                String id = p.length > 0 ? p[0] : "";
                String original = p.length > 1 ? p[1] : "";
                long size = p.length > 2 ? parseLong(p[2], 0L) : 0L;
                String deletedAt = p.length > 3 ? p[3] : "";
                rows.add(new TrashEntry(id, original, size, deletedAt));
            }
            return rows;
        });
    }

    public String restoreTrash(String username, String password, String id) throws Exception {
        return simpleCommand(username, password, "TRASH_RESTORE;" + id);
    }

    public String purgeTrash(String username, String password, String idOrAll) throws Exception {
        return simpleCommand(username, password, "TRASH_PURGE;" + idOrAll);
    }

    public List<VersionEntry> listVersions(String username, String password, String filename) throws Exception {
        return withAuthenticatedConnection(username, password, conn -> {
            conn.writeLine("VERSIONS;" + filename);
            String countLine = conn.readLine();
            int count = parseCount(countLine);
            List<VersionEntry> rows = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String line = conn.readLine();
                if (line == null) {
                    continue;
                }
                String[] p = line.split(";", 3);
                String id = p.length > 0 ? p[0] : "";
                long size = p.length > 1 ? parseLong(p[1], 0L) : 0L;
                String createdAt = p.length > 2 ? p[2] : "";
                rows.add(new VersionEntry(id, size, createdAt));
            }
            return rows;
        });
    }

    public String restoreVersion(String username, String password, String filename, String versionId) throws Exception {
        return simpleCommand(username, password, "RESTORE_VERSION;" + filename + ";" + versionId);
    }

    public List<String> listUsers(String username, String password) throws Exception {
        return withAuthenticatedConnection(username, password, conn -> {
            conn.writeLine("USERS");
            String countLine = conn.readLine();
            int count = parseCount(countLine);
            List<String> rows = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String line = conn.readLine();
                if (line != null && !line.isBlank()) {
                    rows.add(line.trim());
                }
            }
            return rows;
        });
    }

    public List<SharedFileEntry> listSharedFiles(String username, String password, String owner) throws Exception {
        return withAuthenticatedConnection(username, password, conn -> {
            conn.writeLine("LIST_SHARED;" + owner);
            String countLine = conn.readLine();
            int count = parseCount(countLine);
            List<SharedFileEntry> rows = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String line = conn.readLine();
                if (line == null) {
                    continue;
                }
                String[] p = line.split(";", 3);
                String file = p.length > 0 ? p[0] : "";
                long size = p.length > 1 ? parseLong(p[1], 0L) : 0L;
                String status = p.length > 2 ? p[2] : "none";
                rows.add(new SharedFileEntry(owner, file, size, status));
            }
            return rows;
        });
    }

    public String requestRead(String username, String password, String owner, String filename) throws Exception {
        return simpleCommand(username, password, "REQUEST_READ;" + owner + ";" + filename);
    }

    public List<ShareRequestEntry> listIncomingRequests(String username, String password) throws Exception {
        return withAuthenticatedConnection(username, password, conn -> {
            conn.writeLine("LIST_REQUESTS");
            String countLine = conn.readLine();
            int count = parseCount(countLine);
            List<ShareRequestEntry> rows = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String line = conn.readLine();
                if (line == null) {
                    continue;
                }
                String[] p = line.split(";", 4);
                String requester = p.length > 0 ? p[0] : "";
                String file = p.length > 1 ? p[1] : "";
                String status = p.length > 2 ? p[2] : "";
                String createdAt = p.length > 3 ? p[3] : "";
                rows.add(new ShareRequestEntry(requester, file, status, createdAt));
            }
            return rows;
        });
    }

    public String respondRequest(String username, String password, String requester, String file, String action)
            throws Exception {
        return simpleCommand(username, password, "RESPOND_REQUEST;" + requester + ";" + file + ";" + action);
    }

    public List<NotificationEntry> listNotifications(String username, String password) throws Exception {
        return withAuthenticatedConnection(username, password, conn -> {
            conn.writeLine("NOTIFS");
            String countLine = conn.readLine();
            int count = parseCount(countLine);
            List<NotificationEntry> rows = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String line = conn.readLine();
                if (line == null) {
                    continue;
                }
                String[] p = line.split(";", 3);
                String ts = p.length > 0 ? p[0] : "";
                String type = p.length > 1 ? p[1] : "";
                String msg = p.length > 2 ? p[2] : "";
                rows.add(new NotificationEntry(ts, type, msg));
            }
            return rows;
        });
    }

    public String clearNotifications(String username, String password) throws Exception {
        return simpleCommand(username, password, "NOTIFS_CLEAR");
    }

    public List<AdminUserEntry> adminListUsers(String username, String password) throws Exception {
        return withAuthenticatedConnection(username, password, conn -> {
            conn.writeLine("ADMIN_USERS");
            String countLine = conn.readLine();
            if (countLine == null) {
                throw new IOException("ADMIN_USERS sans reponse");
            }
            if (countLine.startsWith("ERROR")) {
                throw new IOException(countLine);
            }
            int count = parseCount(countLine);
            List<AdminUserEntry> rows = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String line = conn.readLine();
                if (line == null) {
                    continue;
                }
                String[] p = line.split(";", 4);
                String user = p.length > 0 ? p[0] : "";
                boolean admin = p.length > 1 && Boolean.parseBoolean(p[1]);
                boolean blocked = p.length > 2 && Boolean.parseBoolean(p[2]);
                long quota = p.length > 3 ? parseLong(p[3], -1L) : -1L;
                rows.add(new AdminUserEntry(user, admin, blocked, quota));
            }
            return rows;
        });
    }

    public String adminSetBlocked(String username, String password, String targetUser, boolean blocked) throws Exception {
        return simpleCommand(username, password, "ADMIN_BLOCK;" + targetUser + ";" + blocked);
    }

    public String adminDeleteUser(String username, String password, String targetUser) throws Exception {
        return simpleCommand(username, password, "ADMIN_DELETE;" + targetUser);
    }

    public String adminSetQuota(String username, String password, String targetUser, long quota) throws Exception {
        return simpleCommand(username, password, "ADMIN_SET_QUOTA;" + targetUser + ";" + quota);
    }

    public Map<String, String> adminStorage(String username, String password) throws Exception {
        return kvCommand(username, password, "ADMIN_STORAGE");
    }

    public Map<String, String> adminMonitor(String username, String password) throws Exception {
        return kvCommand(username, password, "ADMIN_MONITOR");
    }

    public List<String> adminLogs(String username, String password, int limit) throws Exception {
        int sanitized = Math.max(1, Math.min(500, limit));
        return withAuthenticatedConnection(username, password, conn -> {
            conn.writeLine("ADMIN_LOGS;" + sanitized);
            String countLine = conn.readLine();
            if (countLine == null) {
                throw new IOException("ADMIN_LOGS sans reponse");
            }
            if (countLine.startsWith("ERROR")) {
                throw new IOException(countLine);
            }
            int count = parseCount(countLine);
            List<String> rows = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String line = conn.readLine();
                if (line != null) {
                    rows.add(line);
                }
            }
            return rows;
        });
    }

    public List<FileEntry> adminListUserFiles(String username, String password, String owner) throws Exception {
        return withAuthenticatedConnection(username, password, conn -> {
            conn.writeLine("ADMIN_LIST_FILES;" + owner);
            String countLine = conn.readLine();
            if (countLine == null) {
                throw new IOException("ADMIN_LIST_FILES sans reponse");
            }
            if (countLine.startsWith("ERROR")) {
                throw new IOException(countLine);
            }
            int count = parseCount(countLine);
            List<FileEntry> rows = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String line = conn.readLine();
                if (line == null) {
                    continue;
                }
                String[] p = line.split(";", 3);
                String file = p.length > 0 ? p[0] : "";
                long size = p.length > 1 ? parseLong(p[1], 0L) : 0L;
                long modified = p.length > 2 ? parseLong(p[2], 0L) : 0L;
                rows.add(new FileEntry(file, size, modified));
            }
            return rows;
        });
    }

    private String simpleCommand(String username, String password, String command) throws Exception {
        return withAuthenticatedConnection(username, password, conn -> {
            conn.writeLine(command);
            String resp = conn.readLine();
            if (resp == null) {
                throw new IOException("Aucune reponse pour " + command);
            }
            return resp;
        });
    }

    private Map<String, String> kvCommand(String username, String password, String command) throws Exception {
        return withAuthenticatedConnection(username, password, conn -> {
            conn.writeLine(command);
            String resp = conn.readLine();
            if (resp == null) {
                throw new IOException(command + " sans reponse");
            }
            if (resp.startsWith("ERROR")) {
                throw new IOException(resp);
            }
            return parseKvLine(resp);
        });
    }

    private Path downloadWithCommand(String username, String password, String command, Path target) throws Exception {
        if (target == null) {
            throw new IOException("Chemin cible invalide");
        }
        return withAuthenticatedConnection(username, password, conn -> {
            conn.writeLine(command);
            String header = conn.readLine();
            if (header == null) {
                throw new IOException("Aucune reponse DOWNLOAD");
            }
            if (header.startsWith("ERROR")) {
                throw new IOException(header);
            }
            if (!header.startsWith("FILE;")) {
                throw new IOException("Protocole invalide: " + header);
            }
            long size = parseLong(header.substring("FILE;".length()), -1L);
            if (size < 0L) {
                throw new IOException("Taille invalide: " + header);
            }
            try (OutputStream fos = new FileOutputStream(target.toFile())) {
                conn.receiveBytes(fos, size);
            }
            return target;
        });
    }

    private <T> T withAuthenticatedConnection(String username, String password, ConnectionAction<T> action)
            throws Exception {
        return withRawConnection(conn -> {
            String auth = conn.login(username, password);
            if (auth == null) {
                throw new IOException("Connexion fermee par le serveur");
            }
            if (!auth.startsWith("Welcome")) {
                throw new IOException(auth);
            }
            return action.run(conn);
        });
    }

    private <T> T withRawConnection(ConnectionAction<T> action) throws Exception {
        Serveur serveur = null;
        try {
            serveur = BackendConfig.newServeur();
            serveur.connect();
            Socket socket = serveur.getSocket();
            Connection conn = new Connection(socket);
            return action.run(conn);
        } finally {
            if (serveur != null) {
                try {
                    serveur.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static String parseWelcomeUser(String welcomeLine) {
        if (welcomeLine == null || !welcomeLine.startsWith("Welcome")) {
            return "";
        }
        String payload = welcomeLine.substring("Welcome".length()).trim();
        int sep = payload.indexOf(";admin=");
        if (sep >= 0) {
            return payload.substring(0, sep).trim();
        }
        return payload;
    }

    private static boolean parseAdminFlag(String welcomeLine) {
        if (welcomeLine == null) {
            return false;
        }
        int idx = welcomeLine.indexOf(";admin=");
        if (idx < 0) {
            return false;
        }
        String raw = welcomeLine.substring(idx + 7).trim();
        int sep = raw.indexOf(';');
        if (sep >= 0) {
            raw = raw.substring(0, sep);
        }
        return Boolean.parseBoolean(raw);
    }

    private static int parseCount(String line) {
        return (int) Math.max(0, parseLong(line, 0L));
    }

    private static long parseLong(String raw, long fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public static Map<String, String> parseKvLine(String line) {
        Map<String, String> map = new LinkedHashMap<>();
        if (line == null) {
            return map;
        }
        String[] parts = line.split(";");
        for (String p : parts) {
            int idx = p.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = p.substring(0, idx).trim();
            String value = p.substring(idx + 1).trim();
            if (!key.isEmpty()) {
                map.put(key, value);
            }
        }
        return map;
    }

    private static final class Connection {
        private final InputStream in;
        private final OutputStream out;

        private Connection(Socket socket) throws IOException {
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        }

        private String login(String username, String password) throws IOException {
            writeLine("LOGIN;" + username + ";" + password);
            return readLine();
        }

        private void writeLine(String line) throws IOException {
            String payload = (line == null ? "" : line) + "\n";
            out.write(payload.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

        private String readLine() throws IOException {
            int b;
            int cap = 256;
            byte[] buf = new byte[cap];
            int len = 0;
            while ((b = in.read()) != -1) {
                if (b == '\n') {
                    break;
                }
                if (b == '\r') {
                    continue;
                }
                if (len == cap) {
                    cap *= 2;
                    byte[] n = new byte[cap];
                    System.arraycopy(buf, 0, n, 0, len);
                    buf = n;
                }
                buf[len++] = (byte) b;
            }
            if (len == 0 && b == -1) {
                return null;
            }
            return new String(buf, 0, len, StandardCharsets.UTF_8);
        }

        private void sendBytes(InputStream source, long bytes) throws IOException {
            byte[] buffer = new byte[8192];
            long remaining = bytes;
            while (remaining > 0) {
                int read = source.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) {
                    break;
                }
                out.write(buffer, 0, read);
                remaining -= read;
            }
            out.flush();
            if (remaining > 0) {
                throw new IOException("Upload incomplet: " + remaining + " octets restants");
            }
        }

        private void receiveBytes(OutputStream target, long bytes) throws IOException {
            byte[] buffer = new byte[8192];
            long remaining = bytes;
            while (remaining > 0) {
                int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) {
                    throw new IOException("Flux coupe avant la fin du telechargement");
                }
                target.write(buffer, 0, read);
                remaining -= read;
            }
            target.flush();
        }
    }
}
