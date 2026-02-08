import java.io.*;
import java.net.Socket;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;

public class ClientThread implements Runnable {
    private final Socket socket;
    private final FileManager fileManager;
    private String username;

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
            JSONObject users = (JSONObject) parser.parse(new FileReader("resources/user.json"));

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
                else if (line.equals("LIST")) {
                    handleList(writer);
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

        String userDirPath = "../shared_storage/users/" + username + "/";
        File userDir = new File(userDirPath);
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

        File targetFile = new File(userDirPath + filename);

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
                NotificationService.broadcastNewFile(username, filename, size);
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
        String userDirPath = "../shared_storage/users/" + username + "/";
        File file = new File(userDirPath + filename);

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
        String userDirPath = "../shared_storage/users/" + username + "/";
        File dir = new File(userDirPath);

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
}