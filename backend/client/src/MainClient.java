import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class MainClient {

    public static void main(String[] args) {
        try (
            Socket socket = new Socket("127.0.0.1", 2100);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner sc = new Scanner(System.in)
        ) {
            System.out.println("[Client] Connecté au load balancer sur port 2100");

            // === Phase de login ===
            System.out.print("Username: ");
            String username = sc.nextLine().trim();

            System.out.print("Password: ");
            String password = sc.nextLine().trim();

            String loginCommand = "LOGIN;" + username + ";" + password;
            System.out.println("[DEBUG] Envoi login : " + loginCommand);

            out.println(loginCommand);
            out.flush();

            System.out.println("[DEBUG] Attente de la réponse du serveur...");

            String response = in.readLine();
            if (response == null) {
                System.out.println("Le serveur a fermé la connexion sans répondre");
                return;
            }

            System.out.println("Server: " + response);

            if (!response.startsWith("Welcome")) {
                System.out.println("Échec de connexion - " + response);
                return;
            }

            System.out.println("Connexion réussie ! Bienvenue " + username);

            // === Boucle de commandes ===
            while (true) {
                System.out.print("\nCommande (list / upload <fichier> / download <nom> / quota / exit) : ");
                String input = sc.nextLine().trim();

                if (input.isEmpty()) continue;

                if (input.equalsIgnoreCase("exit")) {
                    System.out.println("Déconnexion...");
                    break;
                }

                if (input.equalsIgnoreCase("list")) {
                    handleList(in, out);
                } else if (input.toLowerCase().startsWith("upload ")) {
                    handleUpload(input, in, out, socket);
                } else if (input.toLowerCase().startsWith("download ")) {
                    handleDownload(input, in, out, socket);
                } else if (input.equalsIgnoreCase("quota")) {
                    handleQuota(in, out);
                } else {
                    System.out.println("Commande inconnue. Essayez : list, upload <fichier>, download <nom>, quota, exit");
                }
            }

        } catch (IOException e) {
            System.err.println("Erreur de connexion ou d'I/O : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erreur inattendue : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleList(BufferedReader in, PrintWriter out) throws IOException {
        out.println("LIST");
        out.flush();

        String countLine = in.readLine();
        if (countLine == null) return;

        int count;
        try {
            count = Integer.parseInt(countLine.trim());
        } catch (NumberFormatException e) {
            System.out.println("Réponse invalide du serveur pour LIST");
            return;
        }

        if (count == 0) {
            System.out.println("Aucun fichier dans votre espace.");
        } else {
            System.out.println("Vos fichiers (" + count + ") :");
            for (int i = 0; i < count; i++) {
                String line = in.readLine();
                if (line != null) {
                    System.out.println("  " + line);
                }
            }
        }

        // Consomme "END"
        in.readLine();
    }

    private static void handleUpload(String input, BufferedReader in, PrintWriter out, Socket socket) throws IOException {
        String[] parts = input.split("\\s+", 2);
        if (parts.length < 2) {
            System.out.println("Usage: upload chemin_du_fichier");
            return;
        }

        File file = new File(parts[1]);
        if (!file.exists() || !file.isFile()) {
            System.out.println("Fichier introuvable : " + file.getAbsolutePath());
            return;
        }

        String filename = file.getName();
        long size = file.length();

        out.println("UPLOAD;" + filename + ";" + size);
        out.flush();

        String response = in.readLine();
        System.out.println("Server: " + response);

        if (!"READY".equalsIgnoreCase(response.trim())) {
            System.out.println("Le serveur n'est pas prêt pour l'upload");
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
        if (header == null) {
            System.out.println("Connexion fermée par le serveur");
            return;
        }

        if (header.startsWith("ERROR")) {
            System.out.println("Erreur : " + header);
            return;
        }

        if (!header.startsWith("FILE;")) {
            System.out.println("Réponse inattendue du serveur : " + header);
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
        }
    }

    private static void handleQuota(BufferedReader in, PrintWriter out) throws IOException {
        out.println("QUOTA");
        out.flush();

        String response = in.readLine();
        if (response != null) {
            System.out.println("Quota restant : " + response + " octets");
        } else {
            System.out.println("Pas de réponse pour QUOTA");
        }
    }
}