<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%@ page import="java.util.Map" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null || !user.isAdmin()) {
        response.sendRedirect(request.getContextPath() + "/pages/home.jsp");
        return;
    }
    Map<String, String> monitor = (Map<String, String>) request.getAttribute("monitor_kv");
    String error = (String) request.getAttribute("error");

    String ramMo = "?";
    String diskMo = "?";
    if (monitor != null) {
        try {
            long b = Long.parseLong(monitor.getOrDefault("ramUsedBytes", "0"));
            ramMo = String.format(java.util.Locale.FRANCE, "%.2f", b / (1024.0 * 1024.0));
        } catch (Exception ignored) {}
        try {
            long b = Long.parseLong(monitor.getOrDefault("diskUsedBytes", "0"));
            diskMo = String.format(java.util.Locale.FRANCE, "%.2f", b / (1024.0 * 1024.0));
        } catch (Exception ignored) {}
    }
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/bootstrap/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css">
    <title>SmartDrive - Admin Monitoring</title>
</head>
<body class="bg-light">
<nav class="navbar navbar-light bg-white border-bottom">
    <div class="container-fluid">
        <a class="navbar-brand fw-bold" href="<%= request.getContextPath() %>/admin/dashboard"><i class="bi bi-shield-lock-fill me-2"></i>Admin</a>
        <span class="text-muted">Monitoring système</span>
    </div>
</nav>

<div class="container-fluid">
    <div class="row">
        <jsp:include page="sidebar.jsp" />

        <main class="col-12 col-md-9 col-lg-10 p-4">
            <% if (error != null) { %>
                <div class="alert alert-danger"><%= error %></div>
            <% } %>

            <div class="row g-3">
                <div class="col-12 col-lg-3">
                    <div class="card shadow-sm">
                        <div class="card-body">
                            <div class="text-muted small">CPU</div>
                            <div class="fs-4 fw-semibold"><%= monitor != null ? monitor.getOrDefault("cpuPercent", "?") : "?" %>%</div>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-lg-3">
                    <div class="card shadow-sm">
                        <div class="card-body">
                            <div class="text-muted small">RAM utilisée</div>
                            <div class="fs-6 fw-semibold"><%= ramMo %> Mo</div>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-lg-3">
                    <div class="card shadow-sm">
                        <div class="card-body">
                            <div class="text-muted small">Disque utilisé</div>
                            <div class="fs-6 fw-semibold"><%= diskMo %> Mo</div>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-lg-3">
                    <div class="card shadow-sm">
                        <div class="card-body">
                            <div class="text-muted small">Trafic (simulé)</div>
                            <div class="fs-4 fw-semibold"><%= monitor != null ? monitor.getOrDefault("trafficKbps", "?") : "?" %> Kbps</div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="text-muted small mt-3">
                CPU peut être -1 si non disponible (fallback). Trafic est simulé (accepté pour la démo).
            </div>
        </main>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/bootstrap/js/bootstrap.bundle.min.js"></script>
</body>
</html>
