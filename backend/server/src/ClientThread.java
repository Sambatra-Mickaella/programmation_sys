import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;

public class ClientThread implements Runnable {
    private final Socket socket;
    private final FileManager fileManager;
    private String username;

    private static File findSharedStorageRoot() {
        // Le repo a `backend/shared_storage/` mais le serveur peut être lancé depuis différents dossiers.
        String[] candidates = new String[] {
                "shared_storage",
                "../shared_storage",
                "../../shared_storage",
                "backend/shared_storage",
                "../backend/shared_storage",
                "../../backend/shared_storage"
        };
        for (String c : candidates) {
            try {
                File f = new File(c);
                if (f.exists() && f.isDirectory()) return f;
            } catch (Exception ignored) {}
        }
        return new File("shared_storage");
    }

    private static File usersRootDir() {
        return new File(findSharedStorageRoot(), "users");
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

            // Charger users.json
            JSONParser parser = new JSONParser();
            JSONObject users = (JSONObject) parser.parse(new FileReader("server/resources/user.json"));

            if (!users.containsKey(username) || !users.get(username).equals(password)) {
                writer.println("ERROR Invalid username or password");
                return;
            }

            writer.println("Welcome " + username);
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

        File userDir = new File(usersRootDir(), username);
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
                writer.println("OK Upload successful");
                System.out.println("[UPLOAD] Succès : " + username + " → " + filename + " (" + size + " octets)");
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
        File file = new File(new File(usersRootDir(), username), filename);

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
        if (!"approved".equals(status)) {
            writer.println("ERROR Access not approved");
            return;
        }

        File file = new File(new File(usersRootDir(), owner), filename);

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
        } catch (IOException e) {
            writer.println("ERROR Download failed");
        }
    }

    private void handleList(PrintWriter writer) {
        File dir = new File(usersRootDir(), username);

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

        writer.println(files.length);
        for (File f : files) {
            if (f.isFile()) {
                writer.println(f.getName() + ";" + f.length());
            }
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

        writer.println(files.length);
        for (File f : files) {
            if (!f.isFile()) continue;
            String status = ShareRequestManager.getStatus(owner, username, f.getName());
            writer.println(f.getName() + ";" + f.length() + ";" + status);
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
}
