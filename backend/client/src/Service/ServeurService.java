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

public class ServeurService {
    
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

}
