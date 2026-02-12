package controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class LogoutServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        
        // Récupérer la session sans la créer si elle n'existe pas
        HttpSession session = req.getSession(false);
        
        if (session != null) {
            // Invalider la session (détruit tous les attributs)
            session.invalidate();
        }
        
        // Rediriger vers la page de login
        res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
    }
}
