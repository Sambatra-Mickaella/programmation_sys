import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class SlaveServer {
    private static final ServerConfig CONFIG = ServerConfig.load();

    public static void main(String[] args) {
        int port = CONFIG.getSlavePort();
        String storageRoot = CONFIG.getSlaveStorageRoot();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Slave server running on port " + port + " | storage: " + storageRoot);
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(new SlaveHandler(client, storageRoot)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class SlaveHandler implements Runnable {
        private final Socket socket;
        private final String storageRoot;

        SlaveHandler(Socket socket, String storageRoot) {
            this.socket = socket;
            this.storageRoot = storageRoot;
        }

        @Override
        public void run() {
            try (
                socket;
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(out, true)
            ) {
                String line = readLine(in);
                if (line == null || !line.startsWith("REPLICA;")) {
                    writer.println("ERROR Invalid command");
                    return;
                }

                String[] parts = line.split(";", 4);
                if (parts.length != 4) {
                    writer.println("ERROR Invalid REPLICA format");
                    return;
                }

                String user = parts[1].trim();
                String filename = parts[2].trim();
                long size;
                try {
                    size = Long.parseLong(parts[3].trim());
                } catch (NumberFormatException e) {
                    writer.println("ERROR Invalid file size");
                    return;
                }

                if (size <= 0) {
                    writer.println("ERROR Invalid file size");
                    return;
                }

                File userDir = new File(storageRoot, "users" + File.separator + user);
                if (!userDir.exists() && !userDir.mkdirs()) {
                    writer.println("ERROR Cannot create user directory");
                    return;
                }

                File targetFile = new File(userDir, filename);

                writer.println("READY");
                writer.flush();

                try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[8192];
                    long remaining = size;
                    int read;
                    while (remaining > 0) {
                        read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (read == -1) break;
                        fos.write(buffer, 0, read);
                        remaining -= read;
                    }

                    if (remaining > 0) {
                        writer.println("ERROR Replica incomplete");
                    } else {
                        writer.println("OK");
                        System.out.println("[SLAVE] Replica OK: " + user + " -> " + filename + " (" + size + ")");
                    }
                }
            } catch (Exception e) {
                System.out.println("[SLAVE] Error: " + e.getMessage());
            }
        }

        private String readLine(InputStream in) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int b;
            while ((b = in.read()) != -1) {
                if (b == '\n') break;
                if (b != '\r') baos.write(b);
            }
            if (baos.size() == 0 && b == -1) return null;
            return baos.toString("UTF-8");
        }
    }
}
