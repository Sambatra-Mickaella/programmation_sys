import java.io.*;
import java.net.Socket;
import java.util.Scanner;

<<<<<<< HEAD
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import Service.ServeurService;
import model.Serveur;
import model.User;

public class MainClient {
    public static void main(String[] args) {
        try {
            String serverIp = "127.0.0.1";
            int serverPort = 2100;

            File cfgFile = new File("resources/config.json");
            if (cfgFile.exists()) {
                try (FileReader fr = new FileReader(cfgFile)) {
                    JSONObject cfg = (JSONObject) new JSONParser().parse(fr);
                    Object ipObj = cfg.get("server_ip");
                    Object portObj = cfg.get("server_port");
                    if (ipObj != null) {
                        serverIp = ipObj.toString();
                    }
                    if (portObj != null) {
                        serverPort = Integer.parseInt(portObj.toString());
                    }
                } catch (Exception e) {
                    System.err.println("Failed to read resources/config.json: " + e.getMessage());
                }
            }

            Serveur serveur = new Serveur(serverIp, serverPort);
            serveur.connect();
            
            Socket socket = serveur.getSocket();
            PrintWriter out = serveur.getOutWriter();
            BufferedReader in = serveur.getInReader();
            Scanner sc = new Scanner(System.in);
            
=======
public class MainClient {
    public static void main(String[] args) {
        try (
            Socket socket = new Socket("127.0.0.1", 2100); // Connexion au LoadBalancer
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner sc = new Scanner(System.in)
        ) {
>>>>>>> file-list-views
            // Login
            System.out.print("Username: ");
            String username = sc.nextLine().trim();
            System.out.print("Password: ");
            String password = sc.nextLine().trim();

            out.println("LOGIN;" + username + ";" + password);
            out.flush();

            String response = in.readLine();
            System.out.println("Server: " + response);

            if (response == null || !response.startsWith("Welcome")) {
                System.out.println("Échec de connexion");
                return;
            }

            // Boucle de commandes
            while (true) {
                System.out.print("\nCommand (list / upload <fichier> / download <nom> / exit) : ");
                String input = sc.nextLine().trim();

                if (input.isEmpty()) continue;
                if (input.equalsIgnoreCase("exit")) break;

                if (input.equalsIgnoreCase("list")) {
                    handleList(in, out);
                }
                else if (input.toLowerCase().startsWith("upload ")) {
                    handleUpload(input, in, out, socket);
                }
                else if (input.toLowerCase().startsWith("download ")) {
                    handleDownload(input, in, out, socket);
                }
                else {
                    System.out.println("Commande inconnue");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleList(BufferedReader in, PrintWriter out) throws IOException {
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

        String end = in.readLine(); // "END"
    }

    private static void handleUpload(String input, BufferedReader in, PrintWriter out, Socket socket) throws IOException {
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

    private static void handleDownload(String input, BufferedReader in, PrintWriter out, Socket socket) throws IOException {
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