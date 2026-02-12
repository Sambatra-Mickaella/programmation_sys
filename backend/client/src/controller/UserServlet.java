package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Serveur;
import model.User;

public class UserServlet extends HttpServlet {

    
    // === Bloc des méthodes POST ===
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        Serveur serveur = null;
        try {
            // Nouvelle connexion backend pour cette requête (évite socket partagé/stale)
            serveur = BackendConfig.newServeur();
            serveur.connect();

            PrintWriter srvOut = serveur.getOutWriter();
            BufferedReader in = serveur.getInReader();

            // == User Login ==
            String username = req.getParameter("nom");
            String password = req.getParameter("password");

            // Validation des paramètres
            if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
                req.setAttribute("error", "Veuillez saisir le nom d'utilisateur et le mot de passe.");
                req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
                return;
            }

            User u = new User();
            u.setNom(username.trim());
            u.setPassword(password.trim());

            srvOut.println("LOGIN;" + u.getNom() + ";" + u.getPassword());
            srvOut.flush();

            String response = in.readLine();

            if (response == null) {
                System.out.println("Connexion perdue (aucune réponse du backend) pour: " + username);
                req.setAttribute("error",
                        "Connexion au serveur interrompue. Vérifiez que le load balancer et les serveurs backend sont bien démarrés.");
                req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
                return;
            }

            if (response.startsWith("All servers busy")) {
                System.out.println("Backend occupé/indisponible pour: " + username + " | resp=" + response);
                req.setAttribute("error", response);
                req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
                return;
            }

            if (!response.startsWith("Welcome")) {
                System.out.println("Échec de connexion pour: " + username + " | resp=" + response);
                req.setAttribute("error", "Nom d'utilisateur ou mot de passe incorrect.");
                req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
                return;
            }

            // Extraire le flag admin depuis la réponse du backend: "Welcome
            // <user>;admin=true|false"
            u.setAdmin(parseAdminFlag(response));

            // Connexion réussie : stocker l'utilisateur en session
            HttpSession session = req.getSession();
            session.setAttribute("user", u);

            // Timeout en secondes (1800 = 30 minutes)
            session.setMaxInactiveInterval(1800);

            res.sendRedirect(req.getContextPath() + "/show/mes_fichiers");
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Impossible de se connecter au serveur.");
            req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
        } finally {
            if (serveur != null) {
                try {
                    serveur.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static boolean parseAdminFlag(String welcomeResponse) {
        if (welcomeResponse == null) return false;
        int idx = welcomeResponse.indexOf(";admin=");
        if (idx < 0) return false;
        String raw = welcomeResponse.substring(idx + ";admin=".length()).trim();
        int end = raw.indexOf(';');
        if (end >= 0) raw = raw.substring(0, end);
        return Boolean.parseBoolean(raw);
    }

}
