package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Serveur;
import model.User;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class UserServlet extends HttpServlet {
    private Serveur serveur;

    @Override
    public void init() throws ServletException {
        super.init();
        String serverIp = "127.0.0.1";
        int serverPort = 2100;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.json")) {
            if (is != null) {
                JSONObject cfg = (JSONObject) new JSONParser().parse(new InputStreamReader(is));
                Object ipObj = cfg.get("server_ip");
                Object portObj = cfg.get("server_port");
                if (ipObj != null) {
                    serverIp = ipObj.toString();
                }
                if (portObj != null) {
                    serverPort = Integer.parseInt(portObj.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        serveur = new Serveur(serverIp, serverPort);
    }
    
    // === Bloc des méthodes POST ===
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        
        // Connexion au serveur backend
        try {
            if (serveur == null || !serveur.isConnected() || serveur.getOutWriter() == null || serveur.getInReader() == null) {
                serveur.connect();
            }
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Impossible de se connecter au serveur.");
            req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
            return;
        }

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
            try {
                serveur.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            req.setAttribute("error", "Connexion au serveur interrompue. Vérifiez que le load balancer et les serveurs backend sont bien démarrés.");
            req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
            return;
        }

        if (response.startsWith("All servers busy")) {
            System.out.println("Backend occupé/indisponible pour: " + username + " | resp=" + response);
            try {
                serveur.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            req.setAttribute("error", response);
            req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
            return;
        }

        if (!response.startsWith("Welcome")) {
            System.out.println("Échec de connexion pour: " + username + " | resp=" + response);
            try {
                serveur.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            req.setAttribute("error", "Nom d'utilisateur ou mot de passe incorrect.");
            req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
            return;
        }

        // Connexion réussie : stocker l'utilisateur en session
        HttpSession session = req.getSession();
        session.setAttribute("user", u);
        
        // Configuration du timeout de session
        // Timeout en secondes (1800 = 30 minutes)
        session.setMaxInactiveInterval(1800);
        
        // Alternative : logout automatique après 1 heure
        // session.setMaxInactiveInterval(3600);

        res.sendRedirect(req.getContextPath() + "/show/mes_fichiers");
    }

}
