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
    private Serveur serveur = new Serveur("127.0.0.1", 2121);    
    
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

        if (response == null || !response.startsWith("Welcome")) {
            System.out.println("Échec de connexion pour: " + username);
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

        res.sendRedirect(req.getContextPath() + "/show/list");
    }

}
