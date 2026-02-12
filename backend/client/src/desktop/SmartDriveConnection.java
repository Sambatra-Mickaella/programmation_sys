package desktop;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import controller.BackendConfig;
import model.Serveur;

public final class SmartDriveConnection {

    public static final class RemoteFileRow {
        public final String name;
        public final long size;
        public final long lastModifiedMillis;

        public RemoteFileRow(String name, long size, long lastModifiedMillis) {
            this.name = name;
            this.size = size;
            this.lastModifiedMillis = lastModifiedMillis;
        }
    }

    private final Serveur serveur;
    private InputStream in;
    private OutputStream out;
    private PrintWriter writer;
    private boolean loggedIn;
    private String welcomeLine;
    private boolean admin;

    public SmartDriveConnection(Serveur serveur) {
        this.serveur = serveur;
    }

    public static SmartDriveConnection connectAndLogin(String username, String password) throws Exception {
        Serveur serveur = BackendConfig.newServeur();
        SmartDriveConnection conn = new SmartDriveConnection(serveur);
        conn.connect();
        conn.login(username, password);
        return conn;
    }

    public synchronized void connect() throws Exception {
        serveur.connect();
        Socket socket = serveur.getSocket();
        this.in = new BufferedInputStream(socket.getInputStream());
        this.out = new BufferedOutputStream(socket.getOutputStream());
        this.writer = serveur.getOutWriter();
        this.loggedIn = false;
        this.welcomeLine = null;
    }

    public synchronized void close() {
        try {
            serveur.close();
        } catch (Exception ignored) {
        }
    }

    public synchronized boolean isConnected() {
        return serveur.isConnected();
    }

    public synchronized boolean isLoggedIn() {
        return loggedIn;
    }

    public synchronized boolean isAdmin() {
        return admin;
    }

    public synchronized String getWelcomeLine() {
        return welcomeLine;
    }

    public synchronized void login(String username, String password) throws IOException {
        ensureConnected();
        writer.println("LOGIN;" + username + ";" + password);
        writer.flush();

        String resp = readLine(in);
        if (resp == null) {
            throw new IOException("Connexion fermée par le serveur");
        }
        this.welcomeLine = resp;
        if (!resp.startsWith("Welcome")) {
            throw new IOException(resp);
        }
        this.loggedIn = true;
        this.admin = parseAdminFlag(resp);
    }

    public synchronized List<String> listUsers() throws IOException {
        ensureLoggedIn();
        writer.println("USERS");
        writer.flush();
        String countLine = readLine(in);
        if (countLine == null) throw new IOException("Réponse USERS vide");
        if (countLine.trim().startsWith("ERROR")) throw new IOException(countLine.trim());
        int count;
        try {
            count = Integer.parseInt(countLine.trim());
        } catch (Exception e) {
            throw new IOException("Réponse USERS invalide: " + countLine);
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String row = readLine(in);
            if (row != null && !row.isBlank()) out.add(row.trim());
        }
        return out;
    }

    public synchronized List<RemoteFileRow> listFiles() throws IOException {
        ensureLoggedIn();

        writer.println("LIST");
        writer.flush();

        String countLine = readLine(in);
        if (countLine == null) {
            throw new IOException("Réponse LIST vide");
        }
        int count;
        try {
            count = Integer.parseInt(countLine.trim());
        } catch (NumberFormatException e) {
            throw new IOException("Réponse LIST invalide: " + countLine);
        }

        List<RemoteFileRow> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String row = readLine(in);
            if (row == null) {
                throw new IOException("LIST interrompu");
            }
            String[] parts = row.split(";", -1);
            String name = parts.length > 0 ? parts[0].trim() : "";
            long size = parts.length > 1 ? parseLongSafe(parts[1].trim(), 0) : 0;
            long last = parts.length > 2 ? parseLongSafe(parts[2].trim(), 0) : 0;
            if (!name.isEmpty()) {
                rows.add(new RemoteFileRow(name, size, last));
            }
        }

        // consume END
        readLine(in);
        return rows;
    }

    public synchronized long quotaRemaining() throws IOException {
        ensureLoggedIn();
        writer.println("QUOTA");
        writer.flush();

        String resp = readLine(in);
        if (resp == null) {
            throw new IOException("Réponse QUOTA vide");
        }
        if (resp.startsWith("ERROR")) {
            throw new IOException(resp);
        }
        return parseLongSafe(resp.trim(), -1);
    }

    public synchronized String deleteToTrash(String filename) throws IOException {
        ensureLoggedIn();
        writer.println("DELETE;" + filename);
        writer.flush();
        String resp = readLine(in);
        return resp == null ? "ERROR" : resp;
    }

    public synchronized List<String> listTrash() throws IOException {
        ensureLoggedIn();
        writer.println("TRASH_LIST");
        writer.flush();
        String countLine = readLine(in);
        if (countLine == null) throw new IOException("Réponse TRASH_LIST vide");
        int count;
        try {
            count = Integer.parseInt(countLine.trim());
        } catch (Exception e) {
            throw new IOException("Réponse TRASH_LIST invalide: " + countLine);
        }
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String row = readLine(in);
            if (row != null) rows.add(row);
        }
        return rows;
    }

    public synchronized String trashRestore(String id) throws IOException {
        ensureLoggedIn();
        writer.println("TRASH_RESTORE;" + id);
        writer.flush();
        String resp = readLine(in);
        return resp == null ? "ERROR" : resp;
    }

    public synchronized String trashPurge(String idOrAll) throws IOException {
        ensureLoggedIn();
        writer.println("TRASH_PURGE;" + idOrAll);
        writer.flush();
        String resp = readLine(in);
        return resp == null ? "ERROR" : resp;
    }

    public synchronized List<String> listVersions(String filename) throws IOException {
        ensureLoggedIn();
        writer.println("VERSIONS;" + filename);
        writer.flush();
        String countLine = readLine(in);
        if (countLine == null) throw new IOException("Réponse VERSIONS vide");
        int count;
        try {
            count = Integer.parseInt(countLine.trim());
        } catch (Exception e) {
            throw new IOException("Réponse VERSIONS invalide: " + countLine);
        }
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String row = readLine(in);
            if (row != null) rows.add(row);
        }
        return rows;
    }

    public synchronized String restoreVersion(String filename, String versionId) throws IOException {
        ensureLoggedIn();
        writer.println("RESTORE_VERSION;" + filename + ";" + versionId);
        writer.flush();
        String resp = readLine(in);
        return resp == null ? "ERROR" : resp;
    }

    public synchronized List<String> listNotifications() throws IOException {
        ensureLoggedIn();
        writer.println("NOTIFS");
        writer.flush();
        String countLine = readLine(in);
        if (countLine == null) throw new IOException("Réponse NOTIFS vide");
        int count;
        try {
            count = Integer.parseInt(countLine.trim());
        } catch (Exception e) {
            throw new IOException("Réponse NOTIFS invalide: " + countLine);
        }
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String row = readLine(in);
            if (row != null) rows.add(row);
        }
        return rows;
    }

    public synchronized String clearNotifications() throws IOException {
        ensureLoggedIn();
        writer.println("NOTIFS_CLEAR");
        writer.flush();
        String resp = readLine(in);
        return resp == null ? "ERROR" : resp;
    }

    public synchronized List<String> listSharedFiles(String owner) throws IOException {
        ensureLoggedIn();
        writer.println("LIST_SHARED;" + owner);
        writer.flush();
        String countLine = readLine(in);
        if (countLine == null) throw new IOException("Réponse LIST_SHARED vide");
        int count;
        try {
            count = Integer.parseInt(countLine.trim());
        } catch (Exception e) {
            throw new IOException("Réponse LIST_SHARED invalide: " + countLine);
        }
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String row = readLine(in);
            if (row != null) rows.add(row);
        }
        return rows;
    }

    public synchronized String requestRead(String owner, String file) throws IOException {
        ensureLoggedIn();
        writer.println("REQUEST_READ;" + owner + ";" + file);
        writer.flush();
        String resp = readLine(in);
        return resp == null ? "ERROR" : resp;
    }

    public synchronized List<String> listIncomingRequests() throws IOException {
        ensureLoggedIn();
        writer.println("LIST_REQUESTS");
        writer.flush();
        String countLine = readLine(in);
        if (countLine == null) throw new IOException("Réponse LIST_REQUESTS vide");
        int count;
        try {
            count = Integer.parseInt(countLine.trim());
        } catch (Exception e) {
            throw new IOException("Réponse LIST_REQUESTS invalide: " + countLine);
        }
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String row = readLine(in);
            if (row != null) rows.add(row);
        }
        return rows;
    }

    public synchronized String respondRequest(String requester, String file, String action) throws IOException {
        ensureLoggedIn();
        writer.println("RESPOND_REQUEST;" + requester + ";" + file + ";" + action);
        writer.flush();
        String resp = readLine(in);
        return resp == null ? "ERROR" : resp;
    }

    public synchronized void downloadAs(String owner, String filename, File destination) throws IOException {
        ensureLoggedIn();
        if (destination == null) throw new IOException("Destination invalide");
        writer.println("DOWNLOAD_AS;" + owner + ";" + filename);
        writer.flush();

        String header = readLine(in);
        if (header == null) throw new IOException("Connexion fermée");
        if (header.startsWith("ERROR")) throw new IOException(header);
        if (!header.startsWith("FILE;")) throw new IOException("Réponse inattendue: " + header);

        long size = parseLongSafe(header.split(";", 2)[1].trim(), -1);
        if (size < 0) throw new IOException("Taille de fichier invalide");

        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Impossible de créer le dossier: " + parent);
        }

        try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(destination))) {
            byte[] buffer = new byte[8192];
            long remaining = size;
            while (remaining > 0) {
                int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) throw new IOException("Téléchargement incomplet");
                fos.write(buffer, 0, read);
                remaining -= read;
            }
            fos.flush();
        }
    }

    public synchronized List<String> adminUsers() throws IOException {
        ensureLoggedIn();
        if (!admin) throw new IOException("Not authorized");
        writer.println("ADMIN_USERS");
        writer.flush();
        String countLine = readLine(in);
        if (countLine == null) throw new IOException("Réponse ADMIN_USERS vide");
        String trimmed = countLine.trim();
        if (trimmed.startsWith("ERROR")) throw new IOException(trimmed);
        int count;
        try {
            count = Integer.parseInt(trimmed);
        } catch (Exception e) {
            throw new IOException("Réponse ADMIN_USERS invalide: " + trimmed);
        }
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String row = readLine(in);
            if (row != null) rows.add(row);
        }
        return rows;
    }

    public synchronized String adminBlock(String user, boolean blocked) throws IOException {
        ensureLoggedIn();
        if (!admin) throw new IOException("Not authorized");
        writer.println("ADMIN_BLOCK;" + user + ";" + blocked);
        writer.flush();
        String resp = readLine(in);
        return resp == null ? "ERROR" : resp;
    }

    public synchronized String adminDelete(String user) throws IOException {
        ensureLoggedIn();
        if (!admin) throw new IOException("Not authorized");
        writer.println("ADMIN_DELETE;" + user);
        writer.flush();
        String resp = readLine(in);
        return resp == null ? "ERROR" : resp;
    }

    public synchronized String adminSetQuota(String user, long quotaBytes) throws IOException {
        ensureLoggedIn();
        if (!admin) throw new IOException("Not authorized");
        writer.println("ADMIN_SET_QUOTA;" + user + ";" + quotaBytes);
        writer.flush();
        String resp = readLine(in);
        return resp == null ? "ERROR" : resp;
    }

    public synchronized String adminStorage() throws IOException {
        ensureLoggedIn();
        if (!admin) throw new IOException("Not authorized");
        writer.println("ADMIN_STORAGE");
        writer.flush();
        String resp = readLine(in);
        return resp == null ? "ERROR" : resp;
    }

    public synchronized List<String> adminLogs(int limit) throws IOException {
        ensureLoggedIn();
        if (!admin) throw new IOException("Not authorized");
        writer.println("ADMIN_LOGS;" + limit);
        writer.flush();
        String countLine = readLine(in);
        if (countLine == null) throw new IOException("Réponse ADMIN_LOGS vide");
        String trimmed = countLine.trim();
        if (trimmed.startsWith("ERROR")) throw new IOException(trimmed);
        int count;
        try {
            count = Integer.parseInt(trimmed);
        } catch (Exception e) {
            throw new IOException("Réponse ADMIN_LOGS invalide: " + trimmed);
        }
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String row = readLine(in);
            if (row != null) rows.add(row);
        }
        return rows;
    }

    public synchronized String adminMonitor() throws IOException {
        ensureLoggedIn();
        if (!admin) throw new IOException("Not authorized");
        writer.println("ADMIN_MONITOR");
        writer.flush();
        String resp = readLine(in);
        return resp == null ? "ERROR" : resp;
    }

    public synchronized List<String> adminListUserFiles(String owner) throws IOException {
        ensureLoggedIn();
        if (!admin) throw new IOException("Not authorized");
        writer.println("ADMIN_LIST_FILES;" + owner);
        writer.flush();
        String countLine = readLine(in);
        if (countLine == null) throw new IOException("Réponse ADMIN_LIST_FILES vide");
        String trimmed = countLine.trim();
        if (trimmed.startsWith("ERROR")) throw new IOException(trimmed);
        int count;
        try {
            count = Integer.parseInt(trimmed);
        } catch (Exception e) {
            throw new IOException("Réponse ADMIN_LIST_FILES invalide: " + trimmed);
        }
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String row = readLine(in);
            if (row != null) rows.add(row);
        }
        return rows;
    }

    public synchronized void adminDownloadAs(String owner, String filename, File destination) throws IOException {
        ensureLoggedIn();
        if (!admin) throw new IOException("Not authorized");
        if (destination == null) throw new IOException("Destination invalide");

        writer.println("ADMIN_DOWNLOAD_AS;" + owner + ";" + filename);
        writer.flush();

        String header = readLine(in);
        if (header == null) throw new IOException("Connexion fermée");
        if (header.startsWith("ERROR")) throw new IOException(header);
        if (!header.startsWith("FILE;")) throw new IOException("Réponse inattendue: " + header);

        long size = parseLongSafe(header.split(";", 2)[1].trim(), -1);
        if (size < 0) throw new IOException("Taille de fichier invalide");

        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Impossible de créer le dossier: " + parent);
        }

        try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(destination))) {
            byte[] buffer = new byte[8192];
            long remaining = size;
            while (remaining > 0) {
                int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) throw new IOException("Téléchargement incomplet");
                fos.write(buffer, 0, read);
                remaining -= read;
            }
            fos.flush();
        }
    }

    public synchronized String upload(File localFile) throws IOException {
        ensureLoggedIn();
        if (localFile == null || !localFile.exists() || !localFile.isFile()) {
            throw new IOException("Fichier local invalide");
        }

        String filename = localFile.getName();
        long size = localFile.length();

        writer.println("UPLOAD;" + filename + ";" + size);
        writer.flush();

        String ready = readLine(in);
        if (ready == null) {
            throw new IOException("Pas de réponse du serveur");
        }
        if (!"READY".equalsIgnoreCase(ready.trim())) {
            return ready;
        }

        try (InputStream fis = new BufferedInputStream(new FileInputStream(localFile))) {
            byte[] buffer = new byte[8192];
            int r;
            while ((r = fis.read(buffer)) != -1) {
                out.write(buffer, 0, r);
            }
            out.flush();
        }

        String result = readLine(in);
        return result == null ? "ERROR" : result;
    }

    public synchronized void download(String filename, File destination) throws IOException {
        ensureLoggedIn();
        if (destination == null) {
            throw new IOException("Destination invalide");
        }

        writer.println("DOWNLOAD;" + filename);
        writer.flush();

        String header = readLine(in);
        if (header == null) {
            throw new IOException("Connexion fermée");
        }
        if (header.startsWith("ERROR")) {
            throw new IOException(header);
        }
        if (!header.startsWith("FILE;")) {
            throw new IOException("Réponse inattendue: " + header);
        }

        long size = parseLongSafe(header.split(";", 2)[1].trim(), -1);
        if (size < 0) {
            throw new IOException("Taille de fichier invalide");
        }

        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Impossible de créer le dossier: " + parent);
        }

        try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(destination))) {
            byte[] buffer = new byte[8192];
            long remaining = size;
            while (remaining > 0) {
                int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) {
                    throw new IOException("Téléchargement incomplet");
                }
                fos.write(buffer, 0, read);
                remaining -= read;
            }
            fos.flush();
        }
    }

    private void ensureConnected() throws IOException {
        if (!isConnected() || in == null || out == null || writer == null) {
            throw new IOException("Non connecté au serveur");
        }
    }

    private void ensureLoggedIn() throws IOException {
        ensureConnected();
        if (!loggedIn) {
            throw new IOException("Non authentifié");
        }
    }

    private static long parseLongSafe(String s, long fallback) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean parseAdminFlag(String welcomeLine) {
        if (welcomeLine == null) return false;
        String[] parts = welcomeLine.split(";", -1);
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (t.startsWith("admin=")) {
                return "true".equalsIgnoreCase(t.substring("admin=".length()).trim());
            }
        }
        return false;
    }

    private static String readLine(InputStream in) throws IOException {
        if (in == null) return null;
        byte[] buf = new byte[256];
        int len = 0;
        while (true) {
            int b = in.read();
            if (b == -1) {
                return len == 0 ? null : new String(buf, 0, len, StandardCharsets.UTF_8);
            }
            if (b == '\n') {
                break;
            }
            if (b == '\r') {
                continue;
            }
            if (len == buf.length) {
                byte[] nb = new byte[buf.length * 2];
                System.arraycopy(buf, 0, nb, 0, buf.length);
                buf = nb;
            }
            buf[len++] = (byte) b;
        }
        return new String(buf, 0, len, StandardCharsets.UTF_8);
    }
}
