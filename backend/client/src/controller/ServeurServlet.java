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

    private static boolean looksLikeText(byte[] buf, int len) {
        if (buf == null || len <= 0)
            return false;
        int nonPrintable = 0;
        for (int i = 0; i < len; i++) {
            int b = buf[i] & 0xFF;
            if (b == 0)
                return false;
            // allow: tab(9), lf(10), cr(13)
            if (b < 9 || (b > 13 && b < 32))
                nonPrintable++;
        }
        double ratio = (double) nonPrintable / (double) len;
        return ratio < 0.05;
    }

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

    private static long parseLongSafe(String s, long def) {
        if (s == null)
            return def;
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static boolean matchesType(String filename, String type) {
        if (type == null || type.isBlank() || "all".equalsIgnoreCase(type))
            return true;
        String f = filename.toLowerCase();
        String t = type.toLowerCase();
        return switch (t) {
            case "pdf" -> f.endsWith(".pdf");
            case "image" -> f.endsWith(".jpg") || f.endsWith(".jpeg") || f.endsWith(".png") || f.endsWith(".gif")
                    || f.endsWith(".webp");
            case "video" -> f.endsWith(".mp4") || f.endsWith(".avi") || f.endsWith(".mkv");
            case "audio" -> f.endsWith(".mp3") || f.endsWith(".wav");
            case "doc" -> f.endsWith(".doc") || f.endsWith(".docx") || f.endsWith(".odt");
            case "sheet" -> f.endsWith(".xls") || f.endsWith(".xlsx") || f.endsWith(".ods");
            case "txt" -> f.endsWith(".txt") || f.endsWith(".md");
            case "zip" -> f.endsWith(".zip") || f.endsWith(".rar") || f.endsWith(".7z");
            default -> true;
        };
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

        // ====== DELETE -> CORBEILLE ======
        if (path.startsWith("/delete")) {
            String filename = req.getParameter("file");
            if (!isSafeFilename(filename)) {
                res.sendRedirect(req.getContextPath() + "/show/mes_fichiers?message="
                        + URLEncoder.encode("Nom de fichier invalide", "UTF-8"));
                return;
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
                    res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
                    return;
                }

                String resp = ServeurService.deleteToTrash(in, srvOut, filename);
                String msg = (resp != null && resp.startsWith("OK"))
                        ? "Fichier déplacé dans la corbeille."
                        : ("Erreur suppression: " + resp);
                res.sendRedirect(
                        req.getContextPath() + "/show/mes_fichiers?message=" + URLEncoder.encode(msg, "UTF-8"));
                return;
            } catch (Exception e) {
                e.printStackTrace();
                res.sendRedirect(req.getContextPath() + "/show/mes_fichiers?message="
                        + URLEncoder.encode("Erreur: " + e.getMessage(), "UTF-8"));
                return;
            } finally {
                if (serveur != null) {
                    try {
                        serveur.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        if (path.startsWith("/corbeille_restore")) {
            String id = req.getParameter("id");
            if (id == null || id.isBlank()) {
                res.sendRedirect(req.getContextPath() + "/show/corbeille?message="
                        + URLEncoder.encode("Paramètres invalides", "UTF-8"));
                return;
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
                    res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
                    return;
                }
                String resp = ServeurService.trashRestore(in, srvOut, id);
                String msg = (resp != null && resp.startsWith("OK")) ? "Fichier restauré" : ("Erreur: " + resp);
                res.sendRedirect(req.getContextPath() + "/show/corbeille?message=" + URLEncoder.encode(msg, "UTF-8"));
                return;
            } catch (Exception e) {
                res.sendRedirect(req.getContextPath() + "/show/corbeille?message="
                        + URLEncoder.encode("Erreur: " + e.getMessage(), "UTF-8"));
                return;
            } finally {
                if (serveur != null) {
                    try {
                        serveur.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        if (path.startsWith("/corbeille_purge")) {
            String id = req.getParameter("id");
            if (id == null || id.isBlank())
                id = "ALL";

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
                    res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
                    return;
                }
                String resp = ServeurService.trashPurge(in, srvOut, id);
                String msg = (resp != null && resp.startsWith("OK")) ? "Suppression définitive effectuée"
                        : ("Erreur: " + resp);
                res.sendRedirect(req.getContextPath() + "/show/corbeille?message=" + URLEncoder.encode(msg, "UTF-8"));
                return;
            } catch (Exception e) {
                res.sendRedirect(req.getContextPath() + "/show/corbeille?message="
                        + URLEncoder.encode("Erreur: " + e.getMessage(), "UTF-8"));
                return;
            } finally {
                if (serveur != null) {
                    try {
                        serveur.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // ====== CORBEILLE ======
        // IMPORTANT: doit venir après /corbeille_restore et /corbeille_purge
        // sinon startsWith("/corbeille") capture aussi "/corbeille_restore" et
        // "/corbeille_purge".
        if (path.startsWith("/corbeille")) {
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
                    req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
                    return;
                }

                List<String> rows = ServeurService.listTrash(in, srvOut);
                req.setAttribute("trash_rows", rows);
                String msg = req.getParameter("message");
                if (msg != null)
                    req.setAttribute("message", msg);
                req.getRequestDispatcher("/pages/corbeille.jsp").forward(req, res);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                req.setAttribute("error", "Erreur: " + e.getMessage());
                req.getRequestDispatcher("/pages/corbeille.jsp").forward(req, res);
                return;
            } finally {
                if (serveur != null) {
                    try {
                        serveur.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // ====== VERSIONS ======
        if (path.startsWith("/versions")) {
            String filename = req.getParameter("file");
            if (!isSafeFilename(filename)) {
                res.sendRedirect(req.getContextPath() + "/show/mes_fichiers?message="
                        + URLEncoder.encode("Nom de fichier invalide", "UTF-8"));
                return;
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
                    req.getRequestDispatcher("/pages/home.jsp").forward(req, res);
                    return;
                }

                List<String> rows = ServeurService.listVersions(in, srvOut, filename);
                req.setAttribute("file", filename);
                req.setAttribute("version_rows", rows);
                String msg = req.getParameter("message");
                if (msg != null)
                    req.setAttribute("message", msg);
                req.getRequestDispatcher("/pages/versions.jsp").forward(req, res);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                req.setAttribute("file", filename);
                req.setAttribute("error", "Erreur: " + e.getMessage());
                req.getRequestDispatcher("/pages/versions.jsp").forward(req, res);
                return;
            } finally {
                if (serveur != null) {
                    try {
                        serveur.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        if (path.startsWith("/versions_restore")) {
            String filename = req.getParameter("file");
            String id = req.getParameter("id");
            if (!isSafeFilename(filename) || id == null || id.isBlank()) {
                res.sendRedirect(req.getContextPath() + "/show/mes_fichiers?message="
                        + URLEncoder.encode("Paramètres invalides", "UTF-8"));
                return;
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
                    res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
                    return;
                }

                String resp = ServeurService.restoreVersion(in, srvOut, filename, id);
                String msg = (resp != null && resp.startsWith("OK")) ? "Version restaurée" : ("Erreur: " + resp);
                res.sendRedirect(req.getContextPath() + "/show/versions?file=" + URLEncoder.encode(filename, "UTF-8")
                        + "&message=" + URLEncoder.encode(msg, "UTF-8"));
                return;
            } catch (Exception e) {
                res.sendRedirect(req.getContextPath() + "/show/versions?file=" + URLEncoder.encode(filename, "UTF-8")
                        + "&message=" + URLEncoder.encode("Erreur: " + e.getMessage(), "UTF-8"));
                return;
            } finally {
                if (serveur != null) {
                    try {
                        serveur.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // ====== VIEW/DOWNLOAD (streaming binaire, sans BufferedReader) ======
        if (path.startsWith("/view") || path.startsWith("/download")) {
            String filename = req.getParameter("file");
            if (!isSafeFilename(filename)) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Nom de fichier invalide");
                return;
            }

            Serveur serveur = null;
            try {
                serveur = BackendConfig.newServeur();
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

                // Si Tomcat ne reconnaît pas le type, forcer du texte pour les extensions
                // texte.
                // Objectif: "Voir" doit afficher le contenu au lieu de déclencher un
                // téléchargement.
                if (path.startsWith("/view") && "application/octet-stream".equalsIgnoreCase(mime)) {
                    String lower = filename.toLowerCase();
                    if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".log")
                            || lower.endsWith(".csv")
                            || lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".yml")
                            || lower.endsWith(".yaml")
                            || lower.endsWith(".java") || lower.endsWith(".js") || lower.endsWith(".css")
                            || lower.endsWith(".html")) {
                        mime = "text/plain; charset=UTF-8";
                        res.setCharacterEncoding("UTF-8");
                    }
                }

                // Si pas d'extension reconnue (ex: fichiers uploadés tmp.*), essayer de
                // détecter du texte.
                byte[] firstChunk = null;
                int firstLen = 0;
                if (path.startsWith("/view") && "application/octet-stream".equalsIgnoreCase(mime) && size > 0) {
                    int peek = (int) Math.min(512, size);
                    firstChunk = new byte[peek];
                    int off = 0;
                    while (off < peek) {
                        int r = srvIn.read(firstChunk, off, peek - off);
                        if (r == -1)
                            break;
                        off += r;
                    }
                    firstLen = off;
                    if (looksLikeText(firstChunk, firstLen)) {
                        mime = "text/plain; charset=UTF-8";
                        res.setCharacterEncoding("UTF-8");
                    }
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
                    if (firstLen > 0) {
                        clientOut.write(firstChunk, 0, firstLen);
                        remaining -= firstLen;
                    }
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
                serveur = BackendConfig.newServeur();
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
                    String lower = header.toLowerCase();
                    String msgText;
                    if (lower.contains("not approved") || lower.contains("non approuv")) {
                        msgText = "Accès non approuvé. Envoyez une demande de lecture.";
                    } else if (lower.contains("file not found") || lower.contains("introuvable")) {
                        msgText = "Fichier introuvable.";
                    } else {
                        String err = header.substring("ERROR".length()).trim();
                        msgText = err.isEmpty() ? "Erreur lors de l'accès au fichier." : ("Erreur: " + err);
                    }
                    String msg = URLEncoder.encode(msgText, "UTF-8");
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

                byte[] firstChunk = null;
                int firstLen = 0;
                if (path.startsWith("/shared_view") && "application/octet-stream".equalsIgnoreCase(mime) && size > 0) {
                    String lower = filename.toLowerCase();
                    if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".log")
                            || lower.endsWith(".csv")
                            || lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".yml")
                            || lower.endsWith(".yaml")
                            || lower.endsWith(".java") || lower.endsWith(".js") || lower.endsWith(".css")
                            || lower.endsWith(".html")) {
                        mime = "text/plain; charset=UTF-8";
                        res.setCharacterEncoding("UTF-8");
                    } else {
                        int peek = (int) Math.min(512, size);
                        firstChunk = new byte[peek];
                        int off = 0;
                        while (off < peek) {
                            int r = srvIn.read(firstChunk, off, peek - off);
                            if (r == -1)
                                break;
                            off += r;
                        }
                        firstLen = off;
                        if (looksLikeText(firstChunk, firstLen)) {
                            mime = "text/plain; charset=UTF-8";
                            res.setCharacterEncoding("UTF-8");
                        }
                    }
                }

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
                    if (firstLen > 0) {
                        clientOut.write(firstChunk, 0, firstLen);
                        remaining -= firstLen;
                    }
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
                serveur = BackendConfig.newServeur();
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

                // ====== Recherche (nom/type/date/taille) côté web ======
                String q = req.getParameter("q");
                String type = req.getParameter("type");
                long minSize = parseLongSafe(req.getParameter("min"), -1);
                long maxSize = parseLongSafe(req.getParameter("max"), -1);
                long from = parseLongSafe(req.getParameter("from"), -1);
                long to = parseLongSafe(req.getParameter("to"), -1);
                String qLower = (q == null) ? null : q.toLowerCase();

                List<String> filtered = new java.util.ArrayList<>();
                if (list != null) {
                    for (String row : list) {
                        if (row == null)
                            continue;
                        String[] p = row.split(";", 3);
                        if (p.length < 2)
                            continue;
                        String name = p[0].trim();
                        long size = parseLongSafe(p[1], -1);
                        long lm = (p.length >= 3) ? parseLongSafe(p[2], -1) : -1;

                        if (qLower != null && !qLower.isBlank() && !name.toLowerCase().contains(qLower))
                            continue;
                        if (!matchesType(name, type))
                            continue;
                        if (minSize >= 0 && size >= 0 && size < minSize)
                            continue;
                        if (maxSize >= 0 && size >= 0 && size > maxSize)
                            continue;
                        if (from >= 0 && lm >= 0 && lm < from)
                            continue;
                        if (to >= 0 && lm >= 0 && lm > to)
                            continue;

                        filtered.add(row);
                    }
                }

                List<String> nom_dossier = ServeurService.showNomDossier(filtered);
                // int stockage_utiliser = ServeurService.getStockageUtiliser(list);

                String msg = req.getParameter("message");
                if (msg != null)
                    req.setAttribute("message", msg);

                if (nom_dossier == null || nom_dossier.isEmpty()) {
                    req.setAttribute("message", "Aucun fichier trouvé.");
                } else {
                    // req.setAttribute("stockage", stockage_utiliser);
                    req.setAttribute("list_file", nom_dossier);
                }

                RequestDispatcher dispat = req.getRequestDispatcher("/pages/mes_fichiers.jsp");
                dispat.forward(req, res);

            } catch (IOException e) {
                e.printStackTrace();
                req.setAttribute("error",
                        "Impossible de se connecter au serveur backend. Vérifiez que le load balancer et les serveurs backend sont bien démarrés.");
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
                serveur = BackendConfig.newServeur();
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

                // Notifications : lire et vider (affichage sur Accueil)
                List<String> notifs = ServeurService.listNotifications(in, srvOut);
                ServeurService.clearNotifications(in, srvOut);
                req.setAttribute("notifications", notifs);

                if (list == null || list.isEmpty()) {
                    req.setAttribute("message", "Aucun fichier trouvé.");
                } else {
                    req.setAttribute("list_users", list);
                }

                RequestDispatcher dispat = req.getRequestDispatcher("/pages/accueil.jsp");
                dispat.forward(req, res);

            } catch (IOException e) {
                e.printStackTrace();
                req.setAttribute("error",
                        "Impossible de se connecter au serveur backend. Vérifiez que le load balancer et les serveurs backend sont bien démarrés.");
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
                serveur = BackendConfig.newServeur();
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
                serveur = BackendConfig.newServeur();
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
                serveur = BackendConfig.newServeur();
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
                serveur = BackendConfig.newServeur();
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
                serveur = BackendConfig.newServeur();
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
                serveur = BackendConfig.newServeur();
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

                // Quota renvoyé par le backend = quota restant (actuel)
                long quotaRestant = ServeurService.getQuotaUser(in, srvOut);

                // Récupérer la liste des fichiers pour calculer l'espace utilisé
                List<String> list = ServeurService.showhandleList(in, srvOut);
                long stockageUtilise = ServeurService.getStockageUtiliser(list);

                // Reconstituer un quota total cohérent pour l'UI
                long quotaTotal = (quotaRestant >= 0) ? (quotaRestant + stockageUtilise) : 0;

                // Calculer le pourcentage d'utilisation
                int pourcentage = ServeurService.getStoragePercentage(stockageUtilise, quotaTotal);

                // Formater les tailles pour l'affichage
                String stockageFormate = ServeurService.formatSize(stockageUtilise);
                String quotaFormate = ServeurService.formatSize(quotaTotal);
                long stockageRestant = quotaTotal - stockageUtilise;
                String stockageRestantFormate = ServeurService.formatSize(stockageRestant > 0 ? stockageRestant : 0);

                // Envoyer les attributs à la JSP
                req.setAttribute("quota", quotaTotal);
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
