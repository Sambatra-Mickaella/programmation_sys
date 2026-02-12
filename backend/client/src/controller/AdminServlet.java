package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Service.ServeurService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Serveur;
import model.User;

public class AdminServlet extends HttpServlet {

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

    private static boolean looksLikeText(byte[] buf, int len) {
        if (buf == null || len <= 0) return false;
        int nonPrintable = 0;
        for (int i = 0; i < len; i++) {
            int b = buf[i] & 0xFF;
            if (b == 0) return false;
            if (b < 9 || (b > 13 && b < 32)) nonPrintable++;
        }
        double ratio = (double) nonPrintable / (double) len;
        return ratio < 0.05;
    }

    private static Map<String, String> parseKv(String line) {
        Map<String, String> map = new HashMap<>();
        if (line == null) return map;
        String[] parts = line.split(";");
        for (String p : parts) {
            int idx = p.indexOf('=');
            if (idx <= 0) continue;
            String k = p.substring(0, idx).trim();
            String v = p.substring(idx + 1).trim();
            if (!k.isEmpty()) map.put(k, v);
        }
        return map;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static boolean isAdminSession(User user) {
        return user != null && user.isAdmin();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
            return;
        }
        if (!isAdminSession(user)) {
            res.sendRedirect(req.getContextPath() + "/show/accueil?message=" + URLEncoder.encode("Accès admin refusé", StandardCharsets.UTF_8));
            return;
        }

        String path = req.getPathInfo();
        if (path == null || path.isBlank() || "/".equals(path)) path = "/dashboard";

        if (path.startsWith("/dashboard")) {
            renderDashboard(req, res, user);
            return;
        }
        if (path.startsWith("/users")) {
            renderUsers(req, res, user);
            return;
        }
        if (path.startsWith("/storage")) {
            renderStorage(req, res, user);
            return;
        }
        if (path.startsWith("/logs")) {
            renderLogs(req, res, user);
            return;
        }
        if (path.startsWith("/monitor")) {
            renderMonitor(req, res, user);
            return;
        }
        if (path.startsWith("/user_files")) {
            renderUserFiles(req, res, user);
            return;
        }
        if (path.startsWith("/user_view") || path.startsWith("/user_download")) {
            streamUserFile(req, res, user, path.startsWith("/user_download"));
            return;
        }

        res.sendRedirect(req.getContextPath() + "/admin/dashboard");
    }

    private void renderUserFiles(HttpServletRequest req, HttpServletResponse res, User admin) throws ServletException, IOException {
        String owner = safe(req.getParameter("owner")).trim();
        if (owner.isEmpty() || owner.contains("..") || owner.contains("/") || owner.contains("\\")) {
            res.sendRedirect(req.getContextPath() + "/show/accueil?message=" + URLEncoder.encode("Utilisateur invalide", StandardCharsets.UTF_8));
            return;
        }

        Serveur serveur = null;
        try {
            serveur = BackendConfig.newServeur();
            serveur.connect();
            PrintWriter out = serveur.getOutWriter();
            BufferedReader in = serveur.getInReader();

            out.println("LOGIN;" + admin.getNom() + ";" + admin.getPassword());
            out.flush();
            String auth = in.readLine();
            if (auth == null || !auth.startsWith("Welcome")) {
                res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
                return;
            }

            List<String> rows = ServeurService.adminListUserFiles(in, out, owner);
            req.setAttribute("owner", owner);
            req.setAttribute("admin_user_files", rows);
            String msg = req.getParameter("message");
            if (msg != null) req.setAttribute("message", msg);
            req.getRequestDispatcher("/pages/admin_user_files.jsp").forward(req, res);
        } catch (Exception e) {
            req.setAttribute("owner", owner);
            req.setAttribute("error", "Erreur: " + e.getMessage());
            req.getRequestDispatcher("/pages/admin_user_files.jsp").forward(req, res);
        } finally {
            if (serveur != null) {
                try { serveur.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void streamUserFile(HttpServletRequest req, HttpServletResponse res, User admin, boolean asAttachment) throws IOException {
        String owner = safe(req.getParameter("owner")).trim();
        String filename = safe(req.getParameter("file")).trim();
        if (owner.isEmpty() || owner.contains("..") || owner.contains("/") || owner.contains("\\") || !isSafeFilename(filename)) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Paramètres invalides");
            return;
        }

        Serveur serveur = null;
        try {
            serveur = BackendConfig.newServeur();
            serveur.connect();
            PrintWriter out = serveur.getOutWriter();
            InputStream srvIn = serveur.getSocket().getInputStream();

            out.println("LOGIN;" + admin.getNom() + ";" + admin.getPassword());
            out.flush();
            String auth = readLine(srvIn);
            if (auth == null || !auth.startsWith("Welcome")) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentification échouée");
                return;
            }

            out.println("ADMIN_DOWNLOAD_AS;" + owner + ";" + filename);
            out.flush();

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

            String mime = getServletContext().getMimeType(filename);
            if (mime == null || mime.trim().isEmpty()) mime = "application/octet-stream";

            byte[] firstChunk = null;
            int firstLen = 0;
            if (!asAttachment && "application/octet-stream".equalsIgnoreCase(mime) && size > 0) {
                String lower = filename.toLowerCase();
                if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".log") || lower.endsWith(".csv")
                        || lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".yml") || lower.endsWith(".yaml")
                        || lower.endsWith(".java") || lower.endsWith(".js") || lower.endsWith(".css") || lower.endsWith(".html")) {
                    mime = "text/plain; charset=UTF-8";
                    res.setCharacterEncoding("UTF-8");
                } else {
                    int peek = (int) Math.min(512, size);
                    firstChunk = new byte[peek];
                    int off = 0;
                    while (off < peek) {
                        int r = srvIn.read(firstChunk, off, peek - off);
                        if (r == -1) break;
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

            String dispoType = asAttachment ? "attachment" : "inline";
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
        } catch (Exception e) {
            e.printStackTrace();
            res.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Erreur lors du streaming: " + e.getMessage());
        } finally {
            if (serveur != null) {
                try { serveur.close(); } catch (Exception ignored) {}
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
            return;
        }
        if (!isAdminSession(user)) {
            res.sendRedirect(req.getContextPath() + "/show/accueil?message=" + URLEncoder.encode("Accès admin refusé", StandardCharsets.UTF_8));
            return;
        }

        String path = req.getPathInfo();
        if (path == null) path = "";

        if (path.startsWith("/users_action")) {
            handleUsersAction(req, res, user);
            return;
        }

        res.sendRedirect(req.getContextPath() + "/admin/users");
    }

    private void handleUsersAction(HttpServletRequest req, HttpServletResponse res, User admin) throws IOException {
        String action = safe(req.getParameter("action")).trim();
        String target = safe(req.getParameter("user")).trim();

        Serveur serveur = null;
        try {
            serveur = BackendConfig.newServeur();
            serveur.connect();
            PrintWriter out = serveur.getOutWriter();
            BufferedReader in = serveur.getInReader();

            out.println("LOGIN;" + admin.getNom() + ";" + admin.getPassword());
            out.flush();
            String auth = in.readLine();
            if (auth == null || !auth.startsWith("Welcome")) {
                res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
                return;
            }

            String resp;
            if ("block".equalsIgnoreCase(action)) {
                boolean blocked = Boolean.parseBoolean(safe(req.getParameter("blocked")));
                out.println("ADMIN_BLOCK;" + target + ";" + blocked);
                out.flush();
                resp = in.readLine();
            } else if ("quota".equalsIgnoreCase(action)) {
                String quotaStr = safe(req.getParameter("quota")).trim();
                out.println("ADMIN_SET_QUOTA;" + target + ";" + quotaStr);
                out.flush();
                resp = in.readLine();
            } else if ("delete".equalsIgnoreCase(action)) {
                out.println("ADMIN_DELETE;" + target);
                out.flush();
                resp = in.readLine();
            } else {
                resp = "ERROR Invalid action";
            }

            String msg = (resp != null && resp.startsWith("OK")) ? "Action admin OK" : ("Erreur: " + resp);
            res.sendRedirect(req.getContextPath() + "/admin/users?message=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
        } catch (Exception e) {
            res.sendRedirect(req.getContextPath() + "/admin/users?message=" + URLEncoder.encode("Erreur: " + e.getMessage(), StandardCharsets.UTF_8));
        } finally {
            if (serveur != null) {
                try { serveur.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void renderUsers(HttpServletRequest req, HttpServletResponse res, User admin) throws ServletException, IOException {
        Serveur serveur = null;
        try {
            serveur = BackendConfig.newServeur();
            serveur.connect();
            PrintWriter out = serveur.getOutWriter();
            BufferedReader in = serveur.getInReader();

            out.println("LOGIN;" + admin.getNom() + ";" + admin.getPassword());
            out.flush();
            String auth = in.readLine();
            if (auth == null || !auth.startsWith("Welcome")) {
                res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
                return;
            }

            int page = 1;
            try {
                page = Integer.parseInt(safe(req.getParameter("page")).trim());
            } catch (Exception ignored) {
            }
            if (page < 1) page = 1;

            final int pageSize = 10;
            List<String> allRows = ServeurService.adminListUsers(in, out);
            int total = (allRows == null) ? 0 : allRows.size();
            int totalPages = Math.max(1, (total + pageSize - 1) / pageSize);
            if (page > totalPages) page = totalPages;
            int from = Math.max(0, (page - 1) * pageSize);
            int to = Math.min(total, from + pageSize);
            List<String> pageRows = (allRows == null) ? new ArrayList<>() : allRows.subList(from, to);

            req.setAttribute("admin_user_rows", pageRows);
            req.setAttribute("page", page);
            req.setAttribute("totalPages", totalPages);
            req.setAttribute("pageSize", pageSize);
            req.setAttribute("totalUsers", total);
            String msg = req.getParameter("message");
            if (msg != null) req.setAttribute("message", msg);
            req.getRequestDispatcher("/pages/admin_users.jsp").forward(req, res);
        } catch (Exception e) {
            req.setAttribute("error", "Erreur: " + e.getMessage());
            req.getRequestDispatcher("/pages/admin_users.jsp").forward(req, res);
        } finally {
            if (serveur != null) {
                try { serveur.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void renderStorage(HttpServletRequest req, HttpServletResponse res, User admin) throws ServletException, IOException {
        Serveur serveur = null;
        try {
            serveur = BackendConfig.newServeur();
            serveur.connect();
            PrintWriter out = serveur.getOutWriter();
            BufferedReader in = serveur.getInReader();

            out.println("LOGIN;" + admin.getNom() + ";" + admin.getPassword());
            out.flush();
            String auth = in.readLine();
            if (auth == null || !auth.startsWith("Welcome")) {
                res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
                return;
            }

            String line = ServeurService.adminStorage(in, out);
            req.setAttribute("storage_kv", parseKv(line));
            req.getRequestDispatcher("/pages/admin_storage.jsp").forward(req, res);
        } catch (Exception e) {
            req.setAttribute("error", "Erreur: " + e.getMessage());
            req.getRequestDispatcher("/pages/admin_storage.jsp").forward(req, res);
        } finally {
            if (serveur != null) {
                try { serveur.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void renderLogs(HttpServletRequest req, HttpServletResponse res, User admin) throws ServletException, IOException {
        Serveur serveur = null;
        try {
            serveur = BackendConfig.newServeur();
            serveur.connect();
            PrintWriter out = serveur.getOutWriter();
            BufferedReader in = serveur.getInReader();

            out.println("LOGIN;" + admin.getNom() + ";" + admin.getPassword());
            out.flush();
            String auth = in.readLine();
            if (auth == null || !auth.startsWith("Welcome")) {
                res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
                return;
            }

            int page = 1;
            try {
                page = Integer.parseInt(safe(req.getParameter("page")).trim());
            } catch (Exception ignored) {
            }
            if (page < 1) page = 1;

            final int pageSize = 10;
            final int fetchLimit = 1000; // paginate locally on the latest N logs
            List<String> allLogs = ServeurService.adminLogs(in, out, fetchLimit);

            int total = (allLogs == null) ? 0 : allLogs.size();
            int totalPages = Math.max(1, (total + pageSize - 1) / pageSize);
            if (page > totalPages) page = totalPages;

            int from = Math.max(0, (page - 1) * pageSize);
            int to = Math.min(total, from + pageSize);
            List<String> pageLogs = (allLogs == null) ? new ArrayList<>() : allLogs.subList(from, to);

            req.setAttribute("audit_rows", pageLogs);
            req.setAttribute("page", page);
            req.setAttribute("totalPages", totalPages);
            req.setAttribute("pageSize", pageSize);
            req.setAttribute("totalLogs", total);
            req.getRequestDispatcher("/pages/admin_logs.jsp").forward(req, res);
        } catch (Exception e) {
            req.setAttribute("error", "Erreur: " + e.getMessage());
            req.getRequestDispatcher("/pages/admin_logs.jsp").forward(req, res);
        } finally {
            if (serveur != null) {
                try { serveur.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void renderMonitor(HttpServletRequest req, HttpServletResponse res, User admin) throws ServletException, IOException {
        Serveur serveur = null;
        try {
            serveur = BackendConfig.newServeur();
            serveur.connect();
            PrintWriter out = serveur.getOutWriter();
            BufferedReader in = serveur.getInReader();

            out.println("LOGIN;" + admin.getNom() + ";" + admin.getPassword());
            out.flush();
            String auth = in.readLine();
            if (auth == null || !auth.startsWith("Welcome")) {
                res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
                return;
            }

            String line = ServeurService.adminMonitor(in, out);
            req.setAttribute("monitor_kv", parseKv(line));
            req.getRequestDispatcher("/pages/admin_monitor.jsp").forward(req, res);
        } catch (Exception e) {
            req.setAttribute("error", "Erreur: " + e.getMessage());
            req.getRequestDispatcher("/pages/admin_monitor.jsp").forward(req, res);
        } finally {
            if (serveur != null) {
                try { serveur.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void renderDashboard(HttpServletRequest req, HttpServletResponse res, User admin) throws ServletException, IOException {
        Serveur serveur = null;
        try {
            serveur = BackendConfig.newServeur();
            serveur.connect();
            PrintWriter out = serveur.getOutWriter();
            BufferedReader in = serveur.getInReader();

            out.println("LOGIN;" + admin.getNom() + ";" + admin.getPassword());
            out.flush();
            String auth = in.readLine();
            if (auth == null || !auth.startsWith("Welcome")) {
                res.sendRedirect(req.getContextPath() + "/pages/home.jsp");
                return;
            }

            String storage = ServeurService.adminStorage(in, out);
            String monitor = ServeurService.adminMonitor(in, out);

            int page = 1;
            try {
                page = Integer.parseInt(safe(req.getParameter("page")).trim());
            } catch (Exception ignored) {
            }
            if (page < 1) page = 1;

            final int pageSize = 10;
            final int fetchLimit = 1000; // paginate locally on the latest N logs
            List<String> allLogs = ServeurService.adminLogs(in, out, fetchLimit);
            int total = (allLogs == null) ? 0 : allLogs.size();
            int totalPages = Math.max(1, (total + pageSize - 1) / pageSize);
            if (page > totalPages) page = totalPages;
            int from = Math.max(0, (page - 1) * pageSize);
            int to = Math.min(total, from + pageSize);
            List<String> pageLogs = (allLogs == null) ? new ArrayList<>() : allLogs.subList(from, to);

            req.setAttribute("storage_kv", parseKv(storage));
            req.setAttribute("monitor_kv", parseKv(monitor));
            req.setAttribute("audit_rows", pageLogs);
            req.setAttribute("page", page);
            req.setAttribute("totalPages", totalPages);
            req.setAttribute("pageSize", pageSize);
            req.setAttribute("totalLogs", total);

            req.getRequestDispatcher("/pages/admin_dashboard.jsp").forward(req, res);
        } catch (Exception e) {
            req.setAttribute("error", "Erreur: " + e.getMessage());
            req.getRequestDispatcher("/pages/admin_dashboard.jsp").forward(req, res);
        } finally {
            if (serveur != null) {
                try { serveur.close(); } catch (Exception ignored) {}
            }
        }
    }
}
