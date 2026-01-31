package controller;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;

public class ServeurServlet extends HttpServlet {
    
    // === Bloc des méthodes GET ===
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String url = req.getRequestURL().toString();

        // Vérifier que l'utilisateur est connecté
        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
            return;
        }

        // Bloc "/list"
        if (url.contains("/list")) {
            RequestDispatcher dispat = req.getRequestDispatcher("/pages/list.jsp");
            dispat.forward(req, res);
        }
    }

}
