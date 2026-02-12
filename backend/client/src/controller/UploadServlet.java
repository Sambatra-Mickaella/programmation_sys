package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import Service.ServeurService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import model.Serveur;
import model.User;

@MultipartConfig
public class UploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
            return;
        }

        Part filePart = req.getPart("file");
        if (filePart == null || filePart.getSize() == 0) {
            res.sendRedirect(req.getContextPath() + "/show/mes_fichiers");
            return;
        }

        String submittedName = filePart.getSubmittedFileName();
        String safeName = (submittedName != null && !submittedName.isBlank())
                ? Path.of(submittedName).getFileName().toString()
                : "upload.bin";

        Path tempFile = Files.createTempFile("smartdrive_upload_", "_" + safeName);

        try (InputStream inStream = filePart.getInputStream()) {
            Files.copy(inStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        Serveur serveur = null;
        try {
            serveur = BackendConfig.newServeur();
            serveur.connect();

            PrintWriter srvOut = serveur.getOutWriter();
            BufferedReader in = serveur.getInReader();

            srvOut.println("LOGIN;" + user.getNom() + ";" + user.getPassword());
            srvOut.flush();

            String authResponse = in.readLine();
            if (authResponse == null || !authResponse.startsWith("Welcome")) {
                req.setAttribute("error", "Authentification échouée. Veuillez vous reconnecter.");
                req.getRequestDispatcher("/pages/mes_fichiers.jsp").forward(req, res);
                return;
            }

            ServeurService.handleUpload("upload " + tempFile.toString(), in, srvOut, serveur.getSocket());
            res.sendRedirect(req.getContextPath() + "/show/mes_fichiers");
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Erreur lors de l'upload : " + e.getMessage());
            req.getRequestDispatcher("/pages/mes_fichiers.jsp").forward(req, res);
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // Ignorer la suppression échouée du fichier temporaire
            }
            if (serveur != null) {
                try {
                    serveur.close();
                } catch (Exception ignored) {
                    // Ignorer les erreurs de fermeture
                }
            }
        }
    }
}
