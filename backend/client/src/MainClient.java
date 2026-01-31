import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import Service.ServeurService;
import model.Serveur;
import model.User;

public class MainClient {
    public static void main(String[] args) {
        try {
            Serveur serveur = new Serveur("127.0.0.1", 2121);
            serveur.connect();
            
            Socket socket = serveur.getSocket();
            PrintWriter out = serveur.getOutWriter();
            BufferedReader in = serveur.getInReader();
            Scanner sc = new Scanner(System.in);
            
            // Login
            System.out.print("Username: ");
            String username = sc.nextLine().trim();
            System.out.print("Password: ");
            String password = sc.nextLine().trim();

            User u = new User();
            u.setNom(username);
            u.setPassword(password);

            out.println("LOGIN;" + u.getNom() + ";" + u.getPassword());
            out.flush();

            String response = in.readLine();
            System.out.println("Server: " + response);

            if (response == null || !response.startsWith("Welcome")) {
                System.out.println("Échec de connexion");
                sc.close();
                serveur.close();
                return;
            }

            // Boucle de commandes
            while (true) {
                System.out.print("\nCommand (list / upload <fichier> / download <nom> / exit) : ");
                String input = sc.nextLine().trim();

                if (input.isEmpty()) continue;
                if (input.equalsIgnoreCase("exit")) break;

                if (input.equalsIgnoreCase("list")) {
                    ServeurService.handleList(in, out);
                }
                else if (input.toLowerCase().startsWith("upload ")) {
                    ServeurService.handleUpload(input, in, out, socket);
                }
                else if (input.toLowerCase().startsWith("download ")) {
                    ServeurService.handleDownload(input, in, out, socket);
                }
                else {
                    System.out.println("Commande inconnue");
                }
            }
            
            serveur.close();
            sc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}