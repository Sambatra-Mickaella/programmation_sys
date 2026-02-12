import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;

public class ClientThread implements Runnable {
    private final Socket socket;
    private final FileManager fileManager;
    private String username;
    private boolean isAdmin;

    // users.json parsing / writes are centralized in UserStore

    private static File usersRootDir() {
        return StoragePaths.usersRootDir();
    }

    private static File userDir(String username) {
        return StoragePaths.userDir(username);
    }

    public ClientThread(Socket s, FileManager fm) {
        this.socket = s;
        this.fileManager = fm;
    }

    @Override
    public void run() {
        try (
            socket;  // auto-close
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // ================= LOGIN =================
            String line = reader.readLine();
            if (line == null || !line.startsWith("LOGIN;")) {
                writer.println("ERROR Invalid command - LOGIN required");
                return;
            }

            String[] parts = line.split(";", 3);
            if (parts.length != 3) {
                writer.println("ERROR Invalid LOGIN format");
                return;
            }

            username = parts[1].trim();
            String password = parts[2].trim();

            JSONObject users = UserStore.loadAllUsers();
            Object entry = users.get(username);
            // Tolérance: usernames en JSON sont sensibles à la casse, mais côté UI les
            // utilisateurs tapent souvent sans respecter.
            // On fait donc une recherche insensible à la casse et on normalise username sur
            // la clé trouvée.
            if (entry == null) {
                for (Object k : users.keySet()) {
                    if (k == null)
                        continue;
                    String key = String.valueOf(k);
                    if (key.equalsIgnoreCase(username)) {
                        username = key;
                        entry = users.get(k);
                        break;
                    }
                }
            }
            UserStore.UserRecord record = UserStore.parseUserRecord(entry);
            if (record == null || record.password == null || !record.password.equals(password)) {
                writer.println("ERROR Invalid username or password");
                return;
            }
            if (record.blocked) {
                writer.println("ERROR User blocked");
                return;
            }

            isAdmin = record.admin && "Tsoa".equalsIgnoreCase(username);
            writer.println("Welcome " + username + ";admin=" + isAdmin);
            System.out.println("User logged in: " + username);

            // ============== COMMAND LOOP ==============
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("UPLOAD;")) {
                    handleUpload(line, writer);
                }
                else if (line.startsWith("DOWNLOAD;")) {
                    handleDownload(line, writer);
                }
                else if (line.startsWith("DOWNLOAD_AS;")) {
                    handleDownloadAs(line, writer);
                }
                else if (line.equals("LIST")) {
                    handleList(writer);
                }
                else if (line.equals("USERS")) {
                    handleUsers(writer);
                }
                else if (line.startsWith("DELETE;")) {
                    handleDelete(line, writer);
                } else if (line.equals("TRASH_LIST")) {
                    handleTrashList(writer);
                } else if (line.startsWith("TRASH_RESTORE;")) {
                    handleTrashRestore(line, writer);
                } else if (line.startsWith("TRASH_PURGE;")) {
                    handleTrashPurge(line, writer);
                } else if (line.startsWith("VERSIONS;")) {
                    handleVersions(line, writer);
                } else if (line.startsWith("RESTORE_VERSION;")) {
                    handleRestoreVersion(line, writer);
                } else if (line.equals("NOTIFS")) {
                    handleNotifs(writer);
                } else if (line.equals("NOTIFS_CLEAR")) {
                    handleNotifsClear(writer);
                }
                else if (line.startsWith("LIST_SHARED;")) {
                    handleListShared(line, writer);
                }
                else if (line.startsWith("REQUEST_READ;")) {
                    handleRequestRead(line, writer);
                }
                else if (line.equals("LIST_REQUESTS")) {
                    handleListRequests(writer);
                }
                else if (line.startsWith("RESPOND_REQUEST;")) {
                    handleRespondRequest(line, writer);
                }
                else if (line.equals("QUOTA")) {
                    handleQuota(writer);
                }
                else if (line.equals("ADMIN_USERS")) {
                    handleAdminUsers(writer);
                } else if (line.startsWith("ADMIN_BLOCK;")) {
                    handleAdminBlock(line, writer);
                } else if (line.startsWith("ADMIN_DELETE;")) {
                    handleAdminDelete(line, writer);
                } else if (line.startsWith("ADMIN_SET_QUOTA;")) {
                    handleAdminSetQuota(line, writer);
                } else if (line.equals("ADMIN_STORAGE")) {
                    handleAdminStorage(writer);
                } else if (line.startsWith("ADMIN_LOGS")) {
                    handleAdminLogs(line, writer);
                } else if (line.equals("ADMIN_MONITOR")) {
                    handleAdminMonitor(writer);
                } else if (line.startsWith("ADMIN_LIST_FILES;")) {
                    handleAdminListFiles(line, writer);
                } else if (line.startsWith("ADMIN_DOWNLOAD_AS;")) {
                    handleAdminDownloadAs(line, writer);
                }
                else {
                    writer.println("ERROR Unknown command");
                }
            }

        } catch (Exception e) {
            System.out.println("Client " + username + " error: " + e.getMessage());
        }
    }

    private void handleUpload(String line, PrintWriter writer) throws IOException {
        String[] parts = line.split(";", 3);
        if (parts.length != 3) {
            writer.println("ERROR Invalid UPLOAD format");
            return;
        }

        String filename = parts[1].trim();
        if (!StoragePaths.isSafeFilename(filename)) {
            writer.println("ERROR Invalid filename");
            return;
        }
        long size;
        try {
            size = Long.parseLong(parts[2].trim());
        } catch (NumberFormatException e) {
            writer.println("ERROR Invalid file size");
            return;
        }

        if (size <= 0) {
            writer.println("ERROR Invalid file size");
            return;
        }

        File userDir = userDir(username);
        if (!userDir.exists() && !userDir.mkdirs()) {
            writer.println("ERROR Cannot create user directory");
            return;
        }

        if (!fileManager.canWrite(username)) {
            writer.println("ERROR No write permission");
            return;
        }

        if (!fileManager.hasEnoughQuota(username, size)) {
            writer.println("ERROR Quota exceeded (remaining: " + fileManager.getQuota(username) + " bytes)");
            return;
        }

        File targetFile = new File(userDir, filename);

        // Versioning: si le fichier existe déjà, on l'archive avant d'écraser.
        if (targetFile.exists() && targetFile.isFile()) {
            VersionManager.archiveIfExists(username, filename);
        }

        writer.println("READY");
        writer.flush();

        // Passage en mode binaire - on n'utilise plus le BufferedReader ici
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            InputStream is = socket.getInputStream();
            byte[] buffer = new byte[8192];
            long remaining = size;
            int read;

            while (remaining > 0) {
                read = is.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) break;
                fos.write(buffer, 0, read);
                remaining -= read;
            }

            if (remaining > 0) {
                System.out.println("[UPLOAD] Incomplet : " + remaining + " octets manquants pour " + filename);
                writer.println("ERROR Upload incomplete");
            } else {
                fileManager.consumeQuota(username, size);
                AuditLogger.log(username, "upload", filename + " (" + size + " bytes)");
                writer.println("OK Upload successful");
                System.out.println("[UPLOAD] Succès : " + username + " → " + filename + " (" + size + " octets)");
                NotificationService.broadcastNewFile(username, filename, size);

                Long remainingQuota = fileManager.getQuota(username);
                if (remainingQuota != null && remainingQuota > 0 && remainingQuota < (5L * 1024L * 1024L)) {
                    NotificationStore.push(username, "quota",
                            "Quota presque plein: reste " + remainingQuota + " octets");
                }
            }
        } catch (IOException e) {
            System.out.println("[UPLOAD] Erreur : " + e.getMessage());
            writer.println("ERROR Upload failed: " + e.getMessage());
        }
    }

    private void handleDownload(String line, PrintWriter writer) throws IOException {
        String[] parts = line.split(";", 2);
        if (parts.length != 2) {
            writer.println("ERROR Invalid DOWNLOAD format");
            return;
        }

        String filename = parts[1].trim();
        if (!StoragePaths.isSafeFilename(filename)) {
            writer.println("ERROR Invalid filename");
            return;
        }
        File file = new File(userDir(username), filename);

        if (!file.exists() || !file.isFile()) {
            writer.println("ERROR File not found");
            return;
        }

        long size = file.length();
        writer.println("FILE;" + size);
        writer.flush();

        try (FileInputStream fis = new FileInputStream(file)) {
            OutputStream os = socket.getOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
            AuditLogger.log(username, "download", filename + " (" + size + " bytes)");
        } catch (IOException e) {
            writer.println("ERROR Download failed");
        }
    }

    private void handleDownloadAs(String line, PrintWriter writer) throws IOException {
        String[] parts = line.split(";", 3);
        if (parts.length != 3) {
            writer.println("ERROR Invalid DOWNLOAD_AS format");
            return;
        }

        String owner = parts[1].trim();
        String filename = parts[2].trim();
        if (owner.isEmpty() || filename.isEmpty() || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            writer.println("ERROR Invalid params");
            return;
        }

        String status = ShareRequestManager.getStatus(owner, username, filename);
        if (!"approved".equalsIgnoreCase(status)) {
            writer.println("ERROR Not approved");
            return;
        }

        File file = new File(userDir(owner), filename);
        if (!file.exists() || !file.isFile()) {
            writer.println("ERROR File not found");
            return;
        }

        long size = file.length();
        writer.println("FILE;" + size);
        writer.flush();

        try (FileInputStream fis = new FileInputStream(file)) {
            OutputStream os = socket.getOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
            AuditLogger.log(username, "download_as", owner + "/" + filename + " (" + size + " bytes)");
        } catch (IOException e) {
            writer.println("ERROR Download failed");
        }
    }

    private void handleList(PrintWriter writer) {
        File dir = userDir(username);

        if (!dir.exists() || !dir.isDirectory()) {
            writer.println("0");
            writer.println("END");
            return;
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            writer.println("0");
            writer.println("END");
            return;
        }

        // Retourne des lignes du type: name;size;lastModifiedMillis
        int count = 0;
        for (File f : files) {
            if (f != null && f.isFile())
                count++;
        }

        writer.println(count);
        for (File f : files) {
            if (f == null || !f.isFile())
                continue;
            writer.println(f.getName() + ";" + f.length() + ";" + f.lastModified());
        }
        writer.println("END");
    }

    private void handleUsers(PrintWriter writer) {
        File root = usersRootDir();
        System.out.println("[USERS] root=" + root.getAbsolutePath());
        if (!root.exists() || !root.isDirectory()) {
            writer.println("0");
            return;
        }

        File[] dirs = root.listFiles();
        if (dirs == null || dirs.length == 0) {
            writer.println("0");
            return;
        }

        List<String> out = new ArrayList<>();
        for (File f : dirs) {
            if (f == null) continue;
            if (!f.isDirectory()) continue;
            String name = f.getName();
            if (name == null || name.trim().isEmpty()) continue;
            out.add(name);
        }

        writer.println(out.size());
        for (String u : out) writer.println(u);
    }

    private void handleListShared(String line, PrintWriter writer) {
        String[] parts = line.split(";", 2);
        if (parts.length != 2) {
            writer.println("0");
            return;
        }

        String owner = parts[1].trim();
        if (owner.isEmpty()) {
            writer.println("0");
            return;
        }

        File dir = new File(usersRootDir(), owner);
        System.out.println("[LIST_SHARED] owner=" + owner + " dir=" + dir.getAbsolutePath());
        if (!dir.exists() || !dir.isDirectory()) {
            writer.println("0");
            return;
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            writer.println("0");
            return;
        }

        int count = 0;
        for (File f : files) {
            if (f != null && f.isFile())
                count++;
        }

        writer.println(count);
        for (File f : files) {
            if (f == null || !f.isFile())
                continue;
            String status = ShareRequestManager.getStatus(owner, username, f.getName());
            writer.println(f.getName() + ";" + f.length() + ";" + status);
        }
    }

    private void handleAdminListFiles(String line, PrintWriter writer) {
        if (!isAdmin) {
            writer.println("ERROR Unauthorized");
            return;
        }
        String[] parts = line.split(";", 2);
        if (parts.length != 2) {
            writer.println("ERROR Missing owner");
            return;
        }

        String owner = parts[1].trim();
        if (owner.isEmpty() || owner.contains("..") || owner.contains("/") || owner.contains("\\")) {
            writer.println("ERROR Invalid owner");
            return;
        }

        File dir = userDir(owner);
        if (!dir.exists() || !dir.isDirectory()) {
            writer.println("0");
            return;
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            writer.println("0");
            return;
        }

        int count = 0;
        for (File f : files) {
            if (f != null && f.isFile())
                count++;
        }

        writer.println(count);
        for (File f : files) {
            if (f == null || !f.isFile())
                continue;
            writer.println(f.getName() + ";" + f.length() + ";" + f.lastModified());
        }
    }

    private void handleAdminDownloadAs(String line, PrintWriter writer) throws IOException {
        if (!isAdmin) {
            writer.println("ERROR Unauthorized");
            return;
        }

        String[] parts = line.split(";", 3);
        if (parts.length != 3) {
            writer.println("ERROR Invalid ADMIN_DOWNLOAD_AS format");
            return;
        }

        String owner = parts[1].trim();
        String filename = parts[2].trim();
        if (owner.isEmpty() || owner.contains("..") || owner.contains("/") || owner.contains("\\")) {
            writer.println("ERROR Invalid owner");
            return;
        }
        if (!StoragePaths.isSafeFilename(filename)) {
            writer.println("ERROR Invalid filename");
            return;
        }

        File file = new File(userDir(owner), filename);
        if (!file.exists() || !file.isFile()) {
            writer.println("ERROR File not found");
            return;
        }

        long size = file.length();
        writer.println("FILE;" + size);
        writer.flush();

        try (FileInputStream fis = new FileInputStream(file)) {
            OutputStream os = socket.getOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
            AuditLogger.log(username, "admin_download_as", owner + "/" + filename + " (" + size + " bytes)");
        } catch (IOException e) {
            writer.println("ERROR Download failed");
        }
    }

    private void handleRequestRead(String line, PrintWriter writer) {
        String[] parts = line.split(";", 3);
        if (parts.length != 3) {
            writer.println("ERROR Invalid REQUEST_READ format");
            return;
        }
        String owner = parts[1].trim();
        String filename = parts[2].trim();
        if (owner.isEmpty() || filename.isEmpty() || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            writer.println("ERROR Invalid params");
            return;
        }
        String resp = ShareRequestManager.requestRead(owner, username, filename);
        writer.println(resp);

        AuditLogger.log(username, "share_request", "owner=" + owner + " file=" + filename + " resp=" + resp);

        // Notification côté owner
        if (resp != null && resp.toLowerCase().contains("pending")) {
            NotificationStore.push(owner, "share", "Nouvelle demande d'accès: " + username + " → " + filename);
        }
    }

    private void handleListRequests(PrintWriter writer) {
        List<ShareRequestManager.RequestRow> reqs = ShareRequestManager.listIncoming(username);
        writer.println(reqs.size());
        for (ShareRequestManager.RequestRow r : reqs) {
            writer.println(r.requester + ";" + r.file + ";" + r.status + ";" + r.createdAt);
        }
    }

    private void handleRespondRequest(String line, PrintWriter writer) {
        String[] parts = line.split(";", 4);
        if (parts.length != 4) {
            writer.println("ERROR Invalid RESPOND_REQUEST format");
            return;
        }
        String requester = parts[1].trim();
        String filename = parts[2].trim();
        String action = parts[3].trim();
        if (requester.isEmpty() || filename.isEmpty()) {
            writer.println("ERROR Invalid params");
            return;
        }
        String resp = ShareRequestManager.respond(username, requester, filename, action);
        writer.println(resp);

        AuditLogger.log(username, "share_respond",
                "requester=" + requester + " file=" + filename + " action=" + action + " resp=" + resp);

        // Notification côté requester
        if (resp != null && resp.startsWith("OK")) {
            String status = resp.substring(2).trim();
            NotificationStore.push(requester, "share",
                    "Votre demande sur " + filename + " a été mise à jour: " + status);
        }
    }
    private void handleQuota(PrintWriter writer) {
        Long quota = fileManager.getQuota(username);
        if (quota == null) {
            writer.println("ERROR No quota found for user");
            return;
        }
        writer.println(quota);
        System.out.println("[QUOTA] " + username + " → " + quota + " octets restants");
    }

    private void handleDelete(String line, PrintWriter writer) {
        String[] parts = line.split(";", 2);
        if (parts.length != 2) {
            writer.println("ERROR Invalid DELETE format");
            return;
        }
        String filename = parts[1].trim();
        if (!StoragePaths.isSafeFilename(filename)) {
            writer.println("ERROR Invalid filename");
            return;
        }

        try {
            String id = TrashManager.moveToTrash(username, filename);
            if (id == null) {
                writer.println("ERROR File not found");
                return;
            }
            AuditLogger.log(username, "delete", filename + " -> trashId=" + id);
            writer.println("OK TRASHED;" + id);
        } catch (Exception e) {
            writer.println("ERROR Delete failed");
        }
    }

    private void handleTrashList(PrintWriter writer) {
        List<TrashManager.TrashRow> rows = TrashManager.list(username);
        writer.println(rows.size());
        for (TrashManager.TrashRow r : rows) {
            writer.println(r.id + ";" + r.original + ";" + r.size + ";" + r.deletedAt);
        }
    }

    private void handleTrashRestore(String line, PrintWriter writer) {
        String[] parts = line.split(";", 2);
        if (parts.length != 2) {
            writer.println("ERROR Invalid TRASH_RESTORE format");
            return;
        }
        String id = parts[1].trim();
        try {
            boolean ok = TrashManager.restore(username, id);
            if (ok)
                AuditLogger.log(username, "trash_restore", "id=" + id);
            writer.println(ok ? "OK restored" : "ERROR restore failed");
        } catch (Exception e) {
            writer.println("ERROR restore failed");
        }
    }

    private void handleTrashPurge(String line, PrintWriter writer) {
        String[] parts = line.split(";", 2);
        if (parts.length != 2) {
            writer.println("ERROR Invalid TRASH_PURGE format");
            return;
        }
        String id = parts[1].trim();
        int purged = TrashManager.purge(username, id);
        AuditLogger.log(username, "trash_purge", id + " purged=" + purged);
        writer.println("OK purged;" + purged);
    }

    private void handleVersions(String line, PrintWriter writer) {
        String[] parts = line.split(";", 2);
        if (parts.length != 2) {
            writer.println("0");
            return;
        }
        String filename = parts[1].trim();
        if (!StoragePaths.isSafeFilename(filename)) {
            writer.println("0");
            return;
        }
        List<VersionManager.VersionRow> rows = VersionManager.list(username, filename);
        writer.println(rows.size());
        for (VersionManager.VersionRow r : rows) {
            writer.println(r.id + ";" + r.size + ";" + r.createdAt);
        }
    }

    private void handleRestoreVersion(String line, PrintWriter writer) {
        String[] parts = line.split(";", 3);
        if (parts.length != 3) {
            writer.println("ERROR Invalid RESTORE_VERSION format");
            return;
        }
        String filename = parts[1].trim();
        String versionId = parts[2].trim();
        if (!StoragePaths.isSafeFilename(filename)) {
            writer.println("ERROR Invalid filename");
            return;
        }
        boolean ok = VersionManager.restore(username, filename, versionId);
        writer.println(ok ? "OK restored" : "ERROR restore failed");
    }

    private void handleNotifs(PrintWriter writer) {
        List<NotificationStore.NotifRow> rows = NotificationStore.list(username);
        writer.println(rows.size());
        for (NotificationStore.NotifRow r : rows) {
            writer.println(r.ts + ";" + r.type + ";" + r.msg);
        }
    }

    private void handleNotifsClear(PrintWriter writer) {
        NotificationStore.clear(username);
        writer.println("OK");
    }

    // ================= ADMIN =================
    private boolean ensureAdmin(PrintWriter writer) {
        if (!isAdmin) {
            writer.println("ERROR Not authorized");
            return false;
        }
        return true;
    }

    private void handleAdminUsers(PrintWriter writer) {
        if (!ensureAdmin(writer))
            return;
        try {
            JSONObject users = UserStore.loadAllUsers();
            List<String> keys = new ArrayList<>();
            for (Object k : users.keySet()) {
                if (k != null)
                    keys.add(String.valueOf(k));
            }

            writer.println(keys.size());
            for (String u : keys) {
                UserStore.UserRecord rec = UserStore.parseUserRecord(users.get(u));
                boolean admin = rec != null && rec.admin && "Tsoa".equalsIgnoreCase(u);
                boolean blocked = rec != null && rec.blocked;
                Long q = fileManager.getQuota(u);
                long quota = q == null ? -1L : q;
                writer.println(u + ";" + admin + ";" + blocked + ";" + quota);
            }
        } catch (Exception e) {
            writer.println("ERROR Admin users failed");
        }
    }

    private void handleAdminBlock(String line, PrintWriter writer) {
        if (!ensureAdmin(writer))
            return;
        String[] parts = line.split(";", 3);
        if (parts.length != 3) {
            writer.println("ERROR Invalid ADMIN_BLOCK format");
            return;
        }
        String target = parts[1].trim();
        boolean blocked = Boolean.parseBoolean(parts[2].trim());
        if (target.isEmpty()) {
            writer.println("ERROR Invalid user");
            return;
        }
        if (target.equalsIgnoreCase(username)) {
            writer.println("ERROR Cannot block self");
            return;
        }
        boolean ok = UserStore.setBlocked(target, blocked);
        if (ok) {
            AuditLogger.log(username, blocked ? "admin_block" : "admin_unblock", target);
            writer.println("OK");
        } else {
            writer.println("ERROR Block failed");
        }
    }

    private void handleAdminDelete(String line, PrintWriter writer) {
        if (!ensureAdmin(writer))
            return;
        String[] parts = line.split(";", 2);
        if (parts.length != 2) {
            writer.println("ERROR Invalid ADMIN_DELETE format");
            return;
        }
        String target = parts[1].trim();
        if (target.isEmpty()) {
            writer.println("ERROR Invalid user");
            return;
        }
        if (target.equalsIgnoreCase(username)) {
            writer.println("ERROR Cannot delete self");
            return;
        }

        boolean ok = UserStore.deleteUser(target);
        try {
            fileManager.removeUserQuota(target);
        } catch (Exception ignored) {
        }
        try {
            // suppression des données utilisateur
            File dir = userDir(target);
            deleteRecursively(dir);
        } catch (Exception ignored) {
        }

        if (ok) {
            AuditLogger.log(username, "admin_delete", target);
            writer.println("OK");
        } else {
            writer.println("ERROR Delete failed");
        }
    }

    private static void deleteRecursively(File f) {
        if (f == null || !f.exists())
            return;
        if (f.isFile()) {
            try {
                f.delete();
            } catch (Exception ignored) {
            }
            return;
        }
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children)
                deleteRecursively(c);
        }
        try {
            f.delete();
        } catch (Exception ignored) {
        }
    }

    private void handleAdminSetQuota(String line, PrintWriter writer) {
        if (!ensureAdmin(writer))
            return;
        String[] parts = line.split(";", 3);
        if (parts.length != 3) {
            writer.println("ERROR Invalid ADMIN_SET_QUOTA format");
            return;
        }
        String target = parts[1].trim();
        long quota;
        try {
            quota = Long.parseLong(parts[2].trim());
        } catch (Exception e) {
            writer.println("ERROR Invalid quota");
            return;
        }
        boolean ok = fileManager.setQuota(target, quota);
        if (ok) {
            AuditLogger.log(username, "admin_set_quota", target + "=" + quota);
            writer.println("OK");
        } else {
            writer.println("ERROR Set quota failed");
        }
    }

    private void handleAdminStorage(PrintWriter writer) {
        if (!ensureAdmin(writer))
            return;
        try {
            StorageStats.Stats s = StorageStats.computeTotalUserStorage();
            ServerConfig cfg = ServerConfig.load();
            String slaveStatus = SystemMonitor.checkSlaveStatus(cfg);
            writer.println(
                    "OK" +
                            ";totalUsedBytes=" + s.totalBytes +
                            ";totalFiles=" + s.fileCount +
                            ";replication=script" +
                            ";slaveHost=" + cfg.getSlaveHost() +
                            ";slavePort=" + cfg.getSlavePort() +
                            ";slaveStatus=" + slaveStatus);
        } catch (Exception e) {
            writer.println("ERROR Admin storage failed");
        }
    }

    private void handleAdminLogs(String line, PrintWriter writer) {
        if (!ensureAdmin(writer))
            return;
        int limit = 50;
        try {
            String[] parts = line.split(";", 2);
            if (parts.length == 2) {
                limit = Integer.parseInt(parts[1].trim());
            }
        } catch (Exception ignored) {
        }
        limit = Math.max(1, Math.min(500, limit));

        try {
            Path p = StoragePaths.resolveServerResourcePath(
                    "audit.log",
                    "SMARTDRIVE_AUDIT_PATH",
                    "smartdrive.auditPath",
                    "resources/audit.log");
            if (!Files.exists(p)) {
                writer.println("0");
                return;
            }
            List<String> lines = Files.readAllLines(p);
            int start = Math.max(0, lines.size() - limit);
            List<String> tail = lines.subList(start, lines.size());
            writer.println(tail.size());
            for (String l : tail)
                writer.println(l);
        } catch (Exception e) {
            writer.println("ERROR Admin logs failed");
        }
    }

    private void handleAdminMonitor(PrintWriter writer) {
        if (!ensureAdmin(writer))
            return;
        try {
            SystemMonitor.MonitorSnapshot m = SystemMonitor.snapshot();
            String cpu = (m.cpuPercent == null) ? "-1" : String.valueOf(Math.round(m.cpuPercent * 10.0) / 10.0);
            writer.println(
                    "OK" +
                            ";cpuPercent=" + cpu +
                            ";ramUsedBytes=" + m.ramUsedBytes +
                            ";ramTotalBytes=" + m.ramTotalBytes +
                            ";diskUsedBytes=" + m.diskUsedBytes +
                            ";diskTotalBytes=" + m.diskTotalBytes +
                            ";trafficKbps=" + m.trafficKbps);
        } catch (Exception e) {
            writer.println("ERROR Admin monitor failed");
        }
    }
}
