package Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServeurService {

    public static List<String> showhandleList(BufferedReader in, PrintWriter out) throws IOException {
        out.println("LIST");
        out.flush();

        List<String> list = new ArrayList<>();

        String countLine = in.readLine();
        int count = Integer.parseInt(countLine.trim());

        if (count == 0) {
            System.out.println("Aucun fichier");
        } else {
            System.out.println("Fichiers (" + count + ") :");
            for (int i = 0; i < count; i++) {
                String line = in.readLine();
                list.add(line);
            }
        }
        return list;
    }

    public static List<String> showNomDossier(List<String> list) {
        List<String> nom_dossier = new ArrayList<>();
        for(int i =0; i<list.size(); i++) {
            String line = list.get(i);
            String[] parts = line.split(";");
            if (parts.length >= 2) {
                nom_dossier.add(parts[0].trim());
            }
        }
        return nom_dossier;
    }

    public static int getStockageUtiliser(List<String> list) {
        long total = 0;
        for(int i=0; i<list.size(); i++) {
            String line = list.get(i);
            String[] parts = line.split(";");
            if (parts.length >= 2) {
                try {
                    total += Long.parseLong(parts[1].trim());
                } catch (NumberFormatException e) {
                    System.out.println("Format de stockage invalide: " + parts[1]);
                }
            }
        }
        if (total > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        return (int) total;
    }

    public static long getQuotaUser(BufferedReader in, PrintWriter out) throws IOException {
        out.println("QUOTA");
        out.flush();

        String response = in.readLine();
        if (response == null || response.startsWith("ERROR")) {
            System.out.println("Erreur lors de la récupération du quota: " + response);
            return -1;
        }

        try {
            long quota = Long.parseLong(response.trim());
            System.out.println("Quota utilisateur: " + formatSize(quota));
            return quota;
        } catch (NumberFormatException e) {
            System.out.println("Format de quota invalide: " + response);
            return -1;
        }
    }

    public static int getStoragePercentage(long used, long quota) {
        if (quota <= 0) return 0;
        return (int) Math.min(100, (used * 100) / quota);
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " o";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f Ko", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f Mo", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f Go", bytes / (1024.0 * 1024 * 1024));
        }
    }

    public static List<String> showhandleListUsers(BufferedReader in, PrintWriter out) throws IOException {
        out.println("USERS");
        out.flush();

        List<String> users = new ArrayList<>();

        String countLine = in.readLine();
        if (countLine == null) {
            System.out.println("Aucune réponse du serveur");
            return users;
        }

        String trimmed = countLine.trim();
        if (trimmed.startsWith("ERROR")) {
            throw new IOException("USERS failed: " + trimmed);
        }
        int count;
        try {
            count = Integer.parseInt(trimmed);
        } catch (Exception e) {
            throw new IOException("USERS invalid response: " + trimmed);
        }

        if (count == 0) {
            System.out.println("Aucun utilisateur trouvé");
        } else {
            System.out.println("Utilisateurs (" + count + ") :");
            for (int i = 0; i < count; i++) {
                String line = in.readLine();
                if (line != null) {
                    users.add(line);
                    System.out.println("  " + line);
                }
            }
        }
        return users;
    }

    public static List<String> listSharedFiles(BufferedReader in, PrintWriter out, String owner) throws IOException {
        out.println("LIST_SHARED;" + owner);
        out.flush();

        List<String> rows = new ArrayList<>();
        String countLine = in.readLine();
        if (countLine == null) return rows;
        int count = 0;
        try { count = Integer.parseInt(countLine.trim()); } catch (Exception ignored) {}
        for (int i = 0; i < count; i++) {
            String line = in.readLine();
            if (line != null) rows.add(line);
        }
        return rows;
    }

    public static String requestRead(BufferedReader in, PrintWriter out, String owner, String file) throws IOException {
        out.println("REQUEST_READ;" + owner + ";" + file);
        out.flush();
        String resp = in.readLine();
        return resp == null ? "ERROR" : resp;
    }

    public static List<String> listIncomingRequests(BufferedReader in, PrintWriter out) throws IOException {
        out.println("LIST_REQUESTS");
        out.flush();

        List<String> rows = new ArrayList<>();
        String countLine = in.readLine();
        if (countLine == null) return rows;
        int count = 0;
        try { count = Integer.parseInt(countLine.trim()); } catch (Exception ignored) {}
        for (int i = 0; i < count; i++) {
            String line = in.readLine();
            if (line != null) rows.add(line);
        }
        return rows;
    }

    public static String respondRequest(BufferedReader in, PrintWriter out, String requester, String file, String action) throws IOException {
        out.println("RESPOND_REQUEST;" + requester + ";" + file + ";" + action);
        out.flush();
        String resp = in.readLine();
        return resp == null ? "ERROR" : resp;
    }
    
    public static void handleList(BufferedReader in, PrintWriter out) throws IOException {
        out.println("LIST");
        out.flush();

        String countLine = in.readLine();
        int count = Integer.parseInt(countLine.trim());

        if (count == 0) {
            System.out.println("Aucun fichier");
        } else {
            System.out.println("Fichiers (" + count + ") :");
            for (int i = 0; i < count; i++) {
                String line = in.readLine();
                System.out.println("  " + line);
            }
        }
        String end = in.readLine(); // consume "END"

        // String end = in.readLine(); // "END"
    }

    public static void handleUpload(String input, BufferedReader in, PrintWriter out, Socket socket) throws IOException {
        String[] parts = input.split("\\s+", 2);
        if (parts.length < 2) {
            System.out.println("Usage: upload chemin_du_fichier");
            return;
        }

        File file = new File(parts[1]);
        if (!file.exists() || !file.isFile()) {
            System.out.println("Fichier introuvable : " + file);
            return;
        }

        String filename = file.getName();
        long size = file.length();

        out.println("UPLOAD;" + filename + ";" + size);
        out.flush();

        String response = in.readLine();
        System.out.println("Server: " + response);

        if (!"READY".equalsIgnoreCase(response.trim())) {
            return;
        }

        System.out.println("Envoi du fichier (" + size + " octets)...");

        try (FileInputStream fis = new FileInputStream(file)) {
            OutputStream os = socket.getOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
        }

        String result = in.readLine();
        System.out.println("Server: " + result);
    }

    public static void handleDownload(String input, BufferedReader in, PrintWriter out, Socket socket) throws IOException {
        String[] parts = input.split("\\s+", 2);
        if (parts.length < 2) {
            System.out.println("Usage: download nom_du_fichier");
            return;
        }

        String filename = parts[1].trim();
        out.println("DOWNLOAD;" + filename);
        out.flush();

        String header = in.readLine();
        if (header.startsWith("ERROR")) {
            System.out.println("Erreur : " + header);
            return;
        }

        if (!header.startsWith("FILE;")) {
            System.out.println("Protocole invalide : " + header);
            return;
        }

        long size = Long.parseLong(header.split(";", 2)[1].trim());

        System.out.println("Téléchargement : " + filename + " (" + size + " octets)");

        String saveName = "downloaded_" + filename;
        try (FileOutputStream fos = new FileOutputStream(saveName)) {
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

            System.out.println("Téléchargement terminé → " + saveName);
        } catch (Exception e) {
            System.out.println("Erreur pendant le téléchargement : " + e.getMessage());
        }
    }

    public static String deleteToTrash(BufferedReader in, PrintWriter out, String filename) throws IOException {
        out.println("DELETE;" + filename);
        out.flush();
        String resp = in.readLine();
        return resp == null ? "ERROR" : resp;
    }

    public static List<String> listTrash(BufferedReader in, PrintWriter out) throws IOException {
        out.println("TRASH_LIST");
        out.flush();
        List<String> rows = new ArrayList<>();
        String countLine = in.readLine();
        if (countLine == null)
            return rows;
        int count = 0;
        try {
            count = Integer.parseInt(countLine.trim());
        } catch (Exception ignored) {
        }
        for (int i = 0; i < count; i++) {
            String line = in.readLine();
            if (line != null)
                rows.add(line);
        }
        return rows;
    }

    public static String trashRestore(BufferedReader in, PrintWriter out, String id) throws IOException {
        out.println("TRASH_RESTORE;" + id);
        out.flush();
        String resp = in.readLine();
        return resp == null ? "ERROR" : resp;
    }

    public static String trashPurge(BufferedReader in, PrintWriter out, String idOrAll) throws IOException {
        out.println("TRASH_PURGE;" + idOrAll);
        out.flush();
        String resp = in.readLine();
        return resp == null ? "ERROR" : resp;
    }

    public static List<String> listVersions(BufferedReader in, PrintWriter out, String filename) throws IOException {
        out.println("VERSIONS;" + filename);
        out.flush();
        List<String> rows = new ArrayList<>();
        String countLine = in.readLine();
        if (countLine == null)
            return rows;
        int count = 0;
        try {
            count = Integer.parseInt(countLine.trim());
        } catch (Exception ignored) {
        }
        for (int i = 0; i < count; i++) {
            String line = in.readLine();
            if (line != null)
                rows.add(line);
        }
        return rows;
    }

    public static String restoreVersion(BufferedReader in, PrintWriter out, String filename, String versionId)
            throws IOException {
        out.println("RESTORE_VERSION;" + filename + ";" + versionId);
        out.flush();
        String resp = in.readLine();
        return resp == null ? "ERROR" : resp;
    }

    public static List<String> listNotifications(BufferedReader in, PrintWriter out) throws IOException {
        out.println("NOTIFS");
        out.flush();
        List<String> rows = new ArrayList<>();
        String countLine = in.readLine();
        if (countLine == null)
            return rows;
        int count = 0;
        try {
            count = Integer.parseInt(countLine.trim());
        } catch (Exception ignored) {
        }
        for (int i = 0; i < count; i++) {
            String line = in.readLine();
            if (line != null)
                rows.add(line);
        }
        return rows;
    }

    public static String clearNotifications(BufferedReader in, PrintWriter out) throws IOException {
        out.println("NOTIFS_CLEAR");
        out.flush();
        String resp = in.readLine();
        return resp == null ? "ERROR" : resp;
    }

    // ================= ADMIN (TCP sockets, no WebSocket) =================
    public static List<String> adminListUsers(BufferedReader in, PrintWriter out) throws IOException {
        out.println("ADMIN_USERS");
        out.flush();
        List<String> rows = new ArrayList<>();
        String countLine = in.readLine();
        if (countLine == null)
            return rows;
        String trimmed = countLine.trim();
        if (trimmed.startsWith("ERROR"))
            throw new IOException(trimmed);
        int count = 0;
        try {
            count = Integer.parseInt(trimmed);
        } catch (Exception ignored) {
        }
        for (int i = 0; i < count; i++) {
            String line = in.readLine();
            if (line != null)
                rows.add(line);
        }
        return rows;
    }

    public static String adminStorage(BufferedReader in, PrintWriter out) throws IOException {
        out.println("ADMIN_STORAGE");
        out.flush();
        String resp = in.readLine();
        return resp == null ? "ERROR" : resp;
    }

    public static List<String> adminLogs(BufferedReader in, PrintWriter out, int limit) throws IOException {
        out.println("ADMIN_LOGS;" + limit);
        out.flush();
        List<String> rows = new ArrayList<>();
        String countLine = in.readLine();
        if (countLine == null)
            return rows;
        String trimmed = countLine.trim();
        if (trimmed.startsWith("ERROR"))
            throw new IOException(trimmed);
        int count = 0;
        try {
            count = Integer.parseInt(trimmed);
        } catch (Exception ignored) {
        }
        for (int i = 0; i < count; i++) {
            String line = in.readLine();
            if (line != null)
                rows.add(line);
        }
        return rows;
    }

    public static String adminMonitor(BufferedReader in, PrintWriter out) throws IOException {
        out.println("ADMIN_MONITOR");
        out.flush();
        String resp = in.readLine();
        return resp == null ? "ERROR" : resp;
    }

    public static List<String> adminListUserFiles(BufferedReader in, PrintWriter out, String owner) throws IOException {
        out.println("ADMIN_LIST_FILES;" + owner);
        out.flush();

        List<String> rows = new ArrayList<>();
        String countLine = in.readLine();
        if (countLine == null)
            return rows;
        String trimmed = countLine.trim();
        if (trimmed.startsWith("ERROR"))
            throw new IOException(trimmed);

        int count = 0;
        try {
            count = Integer.parseInt(trimmed);
        } catch (Exception ignored) {
        }
        for (int i = 0; i < count; i++) {
            String line = in.readLine();
            if (line != null)
                rows.add(line);
        }

        return rows;
    }

}
