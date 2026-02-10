package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import Service.ServeurService;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Serveur;
import model.User;

public class ServeurServlet extends HttpServlet {

    private static String readLine(InputStream in) throws IOException {
        int b;
        int cap = 256;
        byte[] out = new byte[cap];
        int len = 0;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            if (b == '\r') continue;
            if (len == cap) {
                cap *= 2;
                byte[] n = new byte[cap];
                System.arraycopy(out, 0, n, 0, len);
                out = n;
            }
            out[len++] = (byte) b;
        }
        if (len == 0 && b == -1) return null;
        return new String(out, 0, len, StandardCharsets.UTF_8);
    }

    private static boolean isSafeFilename(String name) {
        if (name == null) return false;
        String n = name.trim();
        if (n.isEmpty()) return false;
        if (n.contains("..")) return false;
        if (n.contains("/") || n.contains("\\") || n.contains("\u0000")) return false;
        return true;
    }

    private static String contentDispositionFilename(String filename) {
        return filename.replace("\\", "_").replace("\"", "");
    }
    
    // === Bloc des méthodes GET ===
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        // Vérifier que l'utilisateur est connecté (session)
        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
            return;
        }

        String path = req.getPathInfo();
        if (path == null) path = "";

        // ====== VIEW/DOWNLOAD (streaming binaire, sans BufferedReader) ======
        if (path.startsWith("/view") || path.startsWith("/download")) {
            String filename = req.getParameter("file");
            if (!isSafeFilename(filename)) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Nom de fichier invalide");
                return;
            }

            Serveur serveur = null;
            try {
                serveur = new Serveur("127.0.0.1", 2121);
                serveur.connect();

                PrintWriter srvOut = serveur.getOutWriter();
                InputStream srvIn = serveur.getSocket().getInputStream();

                srvOut.println("LOGIN;" + user.getNom() + ";" + user.getPassword());
                srvOut.flush();
                String authResponse = readLine(srvIn);
                if (authResponse == null || !authResponse.startsWith("Welcome")) {
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentification échouée");
                    return;
                }

                srvOut.println("DOWNLOAD;" + filename);
                srvOut.flush();

                String header = readLine(srvIn);
                if (header == null) {
                    res.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Aucune réponse du serveur");
                    return;
                }
                if (header.startsWith("ERROR")) {
                    res.sendError(HttpServletResponse.SC_NOT_FOUND, header);
                    return;
                }
                if (!header.startsWith("FILE;")) {
                    res.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Réponse serveur invalide");
                    return;
                }

                long size;
                try {
                    size = Long.parseLong(header.split(";", 2)[1].trim());
                } catch (Exception e) {
                    res.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Taille de fichier invalide");
                    return;
                }
                if (size < 0) {
                    res.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Taille de fichier invalide");
                    return;
                }

                String mime = getServletContext().getMimeType(filename);
                if (mime == null || mime.trim().isEmpty()) {
                    mime = "application/octet-stream";
                }

                res.setHeader("X-Content-Type-Options", "nosniff");
                res.setContentType(mime);
                res.setContentLengthLong(size);

                String dispoType = path.startsWith("/download") ? "attachment" : "inline";
                res.setHeader(
                        "Content-Disposition",
                        dispoType + "; filename=\"" + contentDispositionFilename(filename) + "\""
                );

                try (OutputStream clientOut = res.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    long remaining = size;
                    while (remaining > 0) {
                        int read = srvIn.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (read == -1) break;
                        clientOut.write(buffer, 0, read);
                        remaining -= read;
                    }
                    clientOut.flush();
                }
                return;
            } catch (Exception e) {
                e.printStackTrace();
                res.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Erreur lors du streaming: " + e.getMessage());
                return;
            } finally {
                if (serveur != null) {
                    try {
                        serveur.close();
                    } catch (Exception ignored) {}
                }
            }
        }

        // ====== SHARED VIEW/DOWNLOAD ======
        if (path.startsWith("/shared_view") || path.startsWith("/shared_download")) {
            String owner = req.getParameter("owner");
            String filename = req.getParameter("file");
            if (owner == null || owner.trim().isEmpty() || owner.contains("..") || owner.contains("/") || owner.contains("\\") || !isSafeFilename(filename)) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Paramètres invalides");
                return;
            }

            Serveur serveur = null;
            try {
                serveur = new Serveur("127.0.0.1", 2121);
                serveur.connect();

                PrintWriter srvOut = serveur.getOutWriter();
                InputStream srvIn = serveur.getSocket().getInputStream();

                srvOut.println("LOGIN;" + user.getNom() + ";" + user.getPassword());
                srvOut.flush();
                String authResponse = readLine(srvIn);
                if (authResponse == null || !authResponse.startsWith("Welcome")) {
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentification échouée");
                    return;
                }

                srvOut.println("DOWNLOAD_AS;" + owner + ";" + filename);
                srvOut.flush();

                String header = readLine(srvIn);
                if (header == null) {
                    res.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Aucune réponse du serveur");
                    return;
                }
                if (header.startsWith("ERROR")) {
                    String msg = URLEncoder.encode("Accès non approuvé. Envoyez une demande de lecture.", "UTF-8");
                    res.sendRedirect(req.getContextPath() + "/show/partages_fichiers?owner=" + URLEncoder.encode(owner, "UTF-8") + "&message=" + msg);
                    return;
                }
                if (!header.startsWith("FILE;")) {
                    res.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Réponse serveur invalide");
                    return;
                }

                long size;
                try {
                    size = Long.parseLong(header.split(";", 2)[1].trim());
                } catch (Exception e) {
                    res.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Taille de fichier invalide");
                    return;
                }

                String mime = getServletContext().getMimeType(filename);
                if (mime == null || mime.trim().isEmpty()) mime = "application/octet-stream";

                res.setHeader("X-Content-Type-Options", "nosniff");
                res.setContentType(mime);
                res.setContentLengthLong(size);

                String dispoType = path.startsWith("/shared_download") ? "attachment" : "inline";
                res.setHeader(
                        "Content-Disposition",
                        dispoType + "; filename=\"" + contentDispositionFilename(filename) + "\""
                );

                try (OutputStream clientOut = res.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    long remaining = size;
                    while (remaining > 0) {
                        int read = srvIn.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (read == -1) break;
                        clientOut.write(buffer, 0, read);
                        remaining -= read;
                    }
                    clientOut.flush();
                }
                return;
            } catch (Exception e) {
                e.printStackTrace();
                res.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Erreur lors du streaming: " + e.getMessage());
                return;
            } finally {
                if (serveur != null) {
                    try { serveur.close(); } catch (Exception ignored) {}
                }
            }
        }

        // Bloc "/mes_fichiers" - Récupérer la liste des fichiers de l'utilisateur
        if (path.contains("/mes_fichiers")) {
            Serveur serveur = null;
            try {
                // Créer une NOUVELLE connexion pour cette requête
                serveur = new Serveur("127.0.0.1", 2121);
                serveur.connect();

                PrintWriter srvOut = serveur.getOutWriter();
                BufferedReader in = serveur.getInReader();

                // Authentifier l'utilisateur auprès du serveur
                srvOut.println("LOGIN;" + user.getNom() + ";" + user.getPassword());
                srvOut.flush();

                String authResponse = in.readLine();
                if (authResponse == null || !authResponse.startsWith("Welcome")) {
                    req.setAttribute("error", "Authentification échouée. Veuillez vous reconnecter.");
                    req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
                    return;
                }

                // Récupérer la liste des fichiers
                List<String> list = ServeurService.showhandleList(in, srvOut);

                List<String> nom_dossier = ServeurService.showNomDossier(list);
                // int stockage_utiliser = ServeurService.getStockageUtiliser(list);

                if (list == null || list.isEmpty()) {
                    req.setAttribute("message", "Aucun fichier trouvé.");
                } else {
                    // req.setAttribute("stockage", stockage_utiliser);
                    req.setAttribute("list_file", nom_dossier);
                }

                RequestDispatcher dispat = req.getRequestDispatcher("/pages/mes_fichiers.jsp");
                dispat.forward(req, res);

            } catch (IOException e) {
                e.printStackTrace();
                req.setAttribute("error", "Impossible de se connecter au serveur backend. Vérifiez qu'il est démarré sur le port 2121.");
                req.getRequestDispatcher("/pages/mes_fichiers.jsp").forward(req, res);
            } catch (Exception e) {
                e.printStackTrace();
                req.setAttribute("error", "Erreur lors de la récupération des fichiers : " + e.getMessage());
                req.getRequestDispatcher("/pages/mes_fichiers.jsp").forward(req, res);
            } finally {
                // Fermer la connexion après chaque requête
                if (serveur != null) {
                    try {
                        serveur.close();
                    } catch (Exception e) {
                        // Ignorer les erreurs de fermeture
                    }
                }
            }
        }

        if (path.contains("/accueil")) {
            Serveur serveur = null;
            try {
                // Créer une NOUVELLE connexion pour cette requête
                serveur = new Serveur("127.0.0.1", 2121);
                serveur.connect();

                PrintWriter srvOut = serveur.getOutWriter();
                BufferedReader in = serveur.getInReader();

                // Authentifier l'utilisateur auprès du serveur
                srvOut.println("LOGIN;" + user.getNom() + ";" + user.getPassword());
                srvOut.flush();

                String authResponse = in.readLine();
                if (authResponse == null || !authResponse.startsWith("Welcome")) {
                    req.setAttribute("error", "Authentification échouée. Veuillez vous reconnecter.");
                    req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
                    return;
                }

                // Récupérer la liste des fichiers
                List<String> list = ServeurService.showhandleListUsers(in, srvOut);

                if (list == null || list.isEmpty()) {
                    req.setAttribute("message", "Aucun fichier trouvé.");
                } else {
                    req.setAttribute("list_users", list);
                }

                RequestDispatcher dispat = req.getRequestDispatcher("/pages/accueil.jsp");
                dispat.forward(req, res);

            } catch (IOException e) {
                e.printStackTrace();
                req.setAttribute("error", "Impossible de se connecter au serveur backend. Vérifiez qu'il est démarré sur le port 2121.");
                req.getRequestDispatcher("/pages/accueil.jsp").forward(req, res);
            } catch (Exception e) {
                e.printStackTrace();
                req.setAttribute("error", "Erreur lors de la récupération des fichiers : " + e.getMessage());
                req.getRequestDispatcher("/pages/accueil.jsp").forward(req, res);
            } finally {
                // Fermer la connexion après chaque requête
                if (serveur != null) {
                    try {
                        serveur.close();
                    } catch (Exception e) {
                        // Ignorer les erreurs de fermeture
                    }
                }
            }
        }

        if (path.contains("/partages_fichiers")) {
            String owner = req.getParameter("owner");
            if (owner == null || owner.trim().isEmpty() || owner.contains("..") || owner.contains("/") || owner.contains("\\")) {
                res.sendRedirect(req.getContextPath() + "/show/partages?message=" + URLEncoder.encode("Utilisateur invalide", "UTF-8"));
                return;
            }

            Serveur serveur = null;
            try {
                serveur = new Serveur("127.0.0.1", 2121);
                serveur.connect();

                PrintWriter srvOut = serveur.getOutWriter();
                BufferedReader in = serveur.getInReader();

                srvOut.println("LOGIN;" + user.getNom() + ";" + user.getPassword());
                srvOut.flush();

                String authResponse = in.readLine();
                if (authResponse == null || !authResponse.startsWith("Welcome")) {
                    req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
                    return;
                }

                List<String> list = ServeurService.listSharedFiles(in, srvOut, owner);
                req.setAttribute("owner", owner);
                req.setAttribute("list_shared", list);

                String msg = req.getParameter("message");
                if (msg != null) req.setAttribute("message", msg);

                req.getRequestDispatcher("/pages/partages_fichiers.jsp").forward(req, res);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                req.setAttribute("owner", owner);
                req.setAttribute("error", "Erreur: " + e.getMessage());
                req.getRequestDispatcher("/pages/partages_fichiers.jsp").forward(req, res);
                return;
            } finally {
                if (serveur != null) {
                    try { serveur.close(); } catch (Exception ignored) {}
                }
            }
        }

        if (path.contains("/partages_demander")) {
            String owner = req.getParameter("owner");
            String file = req.getParameter("file");
            if (owner == null || owner.trim().isEmpty() || owner.contains("..") || owner.contains("/") || owner.contains("\\") || !isSafeFilename(file)) {
                res.sendRedirect(req.getContextPath() + "/show/partages?message=" + URLEncoder.encode("Paramètres invalides", "UTF-8"));
                return;
            }

            Serveur serveur = null;
            try {
                serveur = new Serveur("127.0.0.1", 2121);
                serveur.connect();

                PrintWriter srvOut = serveur.getOutWriter();
                BufferedReader in = serveur.getInReader();

                srvOut.println("LOGIN;" + user.getNom() + ";" + user.getPassword());
                srvOut.flush();
                String authResponse = in.readLine();
                if (authResponse == null || !authResponse.startsWith("Welcome")) {
                    res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
                    return;
                }

                String resp = ServeurService.requestRead(in, srvOut, owner, file);
                String msg = "Demande envoyée";
                if (resp != null && resp.toLowerCase().contains("approved")) msg = "Accès déjà approuvé";
                else if (resp != null && resp.toLowerCase().contains("pending")) msg = "Demande en attente de validation";

                res.sendRedirect(req.getContextPath() + "/show/partages_fichiers?owner=" + URLEncoder.encode(owner, "UTF-8")
                        + "&message=" + URLEncoder.encode(msg, "UTF-8"));
                return;
            } catch (Exception e) {
                e.printStackTrace();
                res.sendRedirect(req.getContextPath() + "/show/partages_fichiers?owner=" + URLEncoder.encode(owner, "UTF-8")
                        + "&message=" + URLEncoder.encode("Erreur: " + e.getMessage(), "UTF-8"));
                return;
            } finally {
                if (serveur != null) {
                    try { serveur.close(); } catch (Exception ignored) {}
                }
            }
        }

        if (path.contains("/partages_demandes")) {
            Serveur serveur = null;
            try {
                serveur = new Serveur("127.0.0.1", 2121);
                serveur.connect();

                PrintWriter srvOut = serveur.getOutWriter();
                BufferedReader in = serveur.getInReader();

                srvOut.println("LOGIN;" + user.getNom() + ";" + user.getPassword());
                srvOut.flush();

                String authResponse = in.readLine();
                if (authResponse == null || !authResponse.startsWith("Welcome")) {
                    req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
                    return;
                }

                List<String> reqs = ServeurService.listIncomingRequests(in, srvOut);
                req.setAttribute("list_requests", reqs);

                String msg = req.getParameter("message");
                if (msg != null) req.setAttribute("message", msg);

                req.getRequestDispatcher("/pages/partages_demandes.jsp").forward(req, res);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                req.setAttribute("error", "Erreur: " + e.getMessage());
                req.getRequestDispatcher("/pages/partages_demandes.jsp").forward(req, res);
                return;
            } finally {
                if (serveur != null) {
                    try { serveur.close(); } catch (Exception ignored) {}
                }
            }
        }

        if (path.contains("/partages_repondre")) {
            String requester = req.getParameter("requester");
            String file = req.getParameter("file");
            String action = req.getParameter("action");
            if (requester == null || requester.trim().isEmpty() || action == null || action.trim().isEmpty() || !isSafeFilename(file)) {
                res.sendRedirect(req.getContextPath() + "/show/partages_demandes?message=" + URLEncoder.encode("Paramètres invalides", "UTF-8"));
                return;
            }

            Serveur serveur = null;
            try {
                serveur = new Serveur("127.0.0.1", 2121);
                serveur.connect();

                PrintWriter srvOut = serveur.getOutWriter();
                BufferedReader in = serveur.getInReader();

                srvOut.println("LOGIN;" + user.getNom() + ";" + user.getPassword());
                srvOut.flush();

                String authResponse = in.readLine();
                if (authResponse == null || !authResponse.startsWith("Welcome")) {
                    res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
                    return;
                }

                String resp = ServeurService.respondRequest(in, srvOut, requester, file, action);
                String msg = (resp != null && resp.startsWith("OK")) ? "Mise à jour: " + resp : "Erreur: " + resp;
                res.sendRedirect(req.getContextPath() + "/show/partages_demandes?message=" + URLEncoder.encode(msg, "UTF-8"));
                return;
            } catch (Exception e) {
                e.printStackTrace();
                res.sendRedirect(req.getContextPath() + "/show/partages_demandes?message=" + URLEncoder.encode("Erreur: " + e.getMessage(), "UTF-8"));
                return;
            } finally {
                if (serveur != null) {
                    try { serveur.close(); } catch (Exception ignored) {}
                }
            }
        }

        if (path.contains("/partages")) {
            Serveur serveur = null;
            try {
                serveur = new Serveur("127.0.0.1", 2121);
                serveur.connect();

                PrintWriter srvOut = serveur.getOutWriter();
                BufferedReader in = serveur.getInReader();

                srvOut.println("LOGIN;" + user.getNom() + ";" + user.getPassword());
                srvOut.flush();

                String authResponse = in.readLine();
                if (authResponse == null || !authResponse.startsWith("Welcome")) {
                    req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
                    return;
                }

                List<String> list = ServeurService.showhandleListUsers(in, srvOut);
                req.setAttribute("list_users", list);

                String msg = req.getParameter("message");
                if (msg != null) req.setAttribute("message", msg);

                req.getRequestDispatcher("/pages/partages.jsp").forward(req, res);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                req.setAttribute("error", "Erreur: " + e.getMessage());
                req.getRequestDispatcher("/pages/partages.jsp").forward(req, res);
                return;
            } finally {
                if (serveur != null) {
                    try { serveur.close(); } catch (Exception ignored) {}
                }
            }
        }

        if(path.contains("/stockage")) {
            Serveur serveur = null;
            try {
                // Créer une NOUVELLE connexion pour cette requête
                serveur = new Serveur("127.0.0.1", 2121);
                serveur.connect();

                PrintWriter srvOut = serveur.getOutWriter();
                BufferedReader in = serveur.getInReader();

                // Authentifier l'utilisateur auprès du serveur
                srvOut.println("LOGIN;" + user.getNom() + ";" + user.getPassword());
                srvOut.flush();

                String authResponse = in.readLine();
                if (authResponse == null || !authResponse.startsWith("Welcome")) {
                    req.setAttribute("error", "Authentification échouée. Veuillez vous reconnecter.");
                    req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
                    return;
                }

                // Récupérer le quota de l'utilisateur
                long quota = ServeurService.getQuotaUser(in, srvOut);

                // Récupérer la liste des fichiers pour calculer l'espace utilisé
                List<String> list = ServeurService.showhandleList(in, srvOut);
                long stockageUtilise = ServeurService.getStockageUtiliser(list);

                // Calculer le pourcentage d'utilisation
                int pourcentage = ServeurService.getStoragePercentage(stockageUtilise, quota);

                // Formater les tailles pour l'affichage
                String stockageFormate = ServeurService.formatSize(stockageUtilise);
                String quotaFormate = ServeurService.formatSize(quota);
                long stockageRestant = quota - stockageUtilise;
                String stockageRestantFormate = ServeurService.formatSize(stockageRestant > 0 ? stockageRestant : 0);

                // Envoyer les attributs à la JSP
                req.setAttribute("quota", quota);
                req.setAttribute("stockageUtilise", stockageUtilise);
                req.setAttribute("pourcentage", pourcentage);
                req.setAttribute("stockageFormate", stockageFormate);
                req.setAttribute("quotaFormate", quotaFormate);
                req.setAttribute("stockageRestantFormate", stockageRestantFormate);
                req.setAttribute("nombreFichiers", list != null ? list.size() : 0);

                RequestDispatcher dispat = req.getRequestDispatcher("/pages/stockage.jsp");
                dispat.forward(req, res);

            } catch (IOException e) {
                e.printStackTrace();
                req.setAttribute("error", "Impossible de se connecter au serveur backend.");
                req.getRequestDispatcher("/pages/stockage.jsp").forward(req, res);
            } catch (Exception e) {
                e.printStackTrace();
                req.setAttribute("error", "Erreur lors de la récupération du stockage : " + e.getMessage());
                req.getRequestDispatcher("/pages/stockage.jsp").forward(req, res);
            } finally {
                if (serveur != null) {
                    try {
                        serveur.close();
                    } catch (Exception e) {
                        // Ignorer les erreurs de fermeture
                    }
                }
            }
        }

    }

}
