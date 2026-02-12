<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%@ page import="java.util.Map" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null || !user.isAdmin()) {
        response.sendRedirect(request.getContextPath() + "/pages/home.jsp");
        return;
    }
    Map<String, String> storage = (Map<String, String>) request.getAttribute("storage_kv");
    String error = (String) request.getAttribute("error");

    String totalUsedMo = "?";
    if (storage != null) {
        try {
            long bytes = Long.parseLong(storage.getOrDefault("totalUsedBytes", "0"));
            totalUsedMo = String.format(java.util.Locale.FRANCE, "%.2f", bytes / (1024.0 * 1024.0));
        } catch (Exception ignored) {
        }
    }
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/bootstrap/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css">
    <title>SmartDrive - Admin Stockage</title>
</head>
<body class="bg-light">
<nav class="navbar navbar-light bg-white border-bottom">
    <div class="container-fluid">
        <a class="navbar-brand fw-bold" href="<%= request.getContextPath() %>/admin/dashboard"><i class="bi bi-shield-lock-fill me-2"></i>Admin</a>
        <span class="text-muted">Gestion du stockage</span>
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
                <div class="col-12 col-lg-4">
                    <div class="card shadow-sm">
                        <div class="card-body">
                            <div class="text-muted small">Total utilisé</div>
                            <div class="fs-4 fw-semibold"><%= totalUsedMo %> Mo</div>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-lg-4">
                    <div class="card shadow-sm">
                        <div class="card-body">
                            <div class="text-muted small">Fichiers</div>
                            <div class="fs-4 fw-semibold"><%= storage != null ? storage.getOrDefault("totalFiles", "?") : "?" %></div>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-lg-4">
                    <div class="card shadow-sm">
                        <div class="card-body">
                            <div class="text-muted small">Réplication</div>
                            <div class="fs-4 fw-semibold"><%= storage != null ? storage.getOrDefault("replication", "?") : "?" %></div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="card shadow-sm mt-4">
                <div class="card-header bg-white fw-semibold">Slave status</div>
                <div class="card-body">
                    <div class="row g-3">
                        <div class="col-12 col-md-4">
                            <div class="text-muted small">Host</div>
                            <div class="fw-semibold"><%= storage != null ? storage.getOrDefault("slaveHost", "?") : "?" %></div>
                        </div>
                        <div class="col-12 col-md-4">
                            <div class="text-muted small">Port</div>
                            <div class="fw-semibold"><%= storage != null ? storage.getOrDefault("slavePort", "?") : "?" %></div>
                        </div>
                        <div class="col-12 col-md-4">
                            <div class="text-muted small">Status</div>
                            <div class="fw-semibold"><%= storage != null ? storage.getOrDefault("slaveStatus", "?") : "?" %></div>
                        </div>
                    </div>
                    <div class="text-muted small mt-3">
                        Note: la réplication peut être simulée (script) mais visible pour la sécurité cloud.
                    </div>
                </div>
            </div>
        </main>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/bootstrap/js/bootstrap.bundle.min.js"></script>
</body>
</html>
