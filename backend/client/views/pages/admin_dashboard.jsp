<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*" %>
<%@ page import="model.User" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null || !user.isAdmin()) {
        response.sendRedirect(request.getContextPath() + "/pages/home.jsp");
        return;
    }
    Map<String, String> storage = (Map<String, String>) request.getAttribute("storage_kv");
    Map<String, String> monitor = (Map<String, String>) request.getAttribute("monitor_kv");
    List<String> auditRows = (List<String>) request.getAttribute("audit_rows");
    String error = (String) request.getAttribute("error");

    Integer currentPage = (Integer) request.getAttribute("page");
    Integer totalPages = (Integer) request.getAttribute("totalPages");
    Integer totalLogs = (Integer) request.getAttribute("totalLogs");
    if (currentPage == null) currentPage = 1;
    if (totalPages == null) totalPages = 1;
    if (totalLogs == null) totalLogs = (auditRows != null ? auditRows.size() : 0);

    String totalUsedMo = "?";
    if (storage != null) {
        try {
            long bytes = Long.parseLong(storage.getOrDefault("totalUsedBytes", "0"));
            totalUsedMo = String.format(java.util.Locale.FRANCE, "%.2f", bytes / (1024.0 * 1024.0));
        } catch (Exception ignored) {
        }
    }

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
    <title>SmartDrive - Admin</title>
</head>
<body class="bg-light">
<nav class="navbar navbar-light bg-white border-bottom">
    <div class="container-fluid">
        <span class="navbar-brand fw-bold"><i class="bi bi-shield-lock-fill me-2"></i>Admin Panel</span>
        <div class="d-flex gap-2">
            <a class="btn btn-outline-primary btn-sm" href="<%= request.getContextPath() %>/admin/users">Utilisateurs</a>
            <a class="btn btn-outline-primary btn-sm" href="<%= request.getContextPath() %>/admin/storage">Stockage</a>
            <a class="btn btn-outline-primary btn-sm" href="<%= request.getContextPath() %>/admin/logs">Logs</a>
            <a class="btn btn-outline-primary btn-sm" href="<%= request.getContextPath() %>/admin/monitor">Monitoring</a>
        </div>
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
                <div class="col-12 col-lg-6">
                    <div class="card shadow-sm">
                        <div class="card-body">
                            <div class="d-flex align-items-center justify-content-between">
                                <div>
                                    <div class="text-muted small">Total storage utilisé</div>
                                    <div class="fs-4 fw-semibold"><%= totalUsedMo %> Mo</div>
                                </div>
                                <i class="bi bi-hdd-network fs-2 text-primary"></i>
                            </div>
                            <div class="text-muted small mt-2">Fichiers: <%= storage != null ? storage.getOrDefault("totalFiles", "?") : "?" %></div>
                            <div class="text-muted small">Slave: <%= storage != null ? storage.getOrDefault("slaveStatus", "?") : "?" %></div>
                        </div>
                    </div>
                </div>

                <div class="col-12 col-lg-6">
                    <div class="card shadow-sm">
                        <div class="card-body">
                            <div class="d-flex align-items-center justify-content-between">
                                <div>
                                    <div class="text-muted small">Monitoring système</div>
                                    <div class="fs-4 fw-semibold">CPU: <%= monitor != null ? monitor.getOrDefault("cpuPercent", "?") : "?" %>%</div>
                                </div>
                                <i class="bi bi-speedometer2 fs-2 text-success"></i>
                            </div>
                            <div class="text-muted small mt-2">RAM utilisée: <%= ramMo %> Mo</div>
                            <div class="text-muted small">Disque utilisé: <%= diskMo %> Mo</div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="card shadow-sm mt-4">
                <div class="card-header bg-white fw-semibold">Audit (dernières actions)</div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-striped mb-0">
                            <thead>
                            <tr>
                                <th>Timestamp</th>
                                <th>Actor</th>
                                <th>Action</th>
                                <th>Détails</th>
                            </tr>
                            </thead>
                            <tbody>
                            <% if (auditRows != null && !auditRows.isEmpty()) {
                                   for (String r : auditRows) {
                                       if (r == null) continue;
                                       String[] p = r.split(";", 4);
                                       if (p.length < 4) continue;
                            %>
                                <tr>
                                    <td class="text-muted"><%= p[0] %></td>
                                    <td><%= p[1] %></td>
                                    <td><span class="badge text-bg-secondary"><%= p[2] %></span></td>
                                    <td class="text-truncate" style="max-width: 520px" title="<%= p[3] %>"><%= p[3] %></td>
                                </tr>
                            <%     }
                               } else { %>
                                <tr><td colspan="4" class="text-center text-muted py-4">Aucun log pour le moment.</td></tr>
                            <% } %>
                            </tbody>
                        </table>
                    </div>
                </div>
                <div class="card-footer bg-white d-flex justify-content-between align-items-center">
                    <div class="text-muted small">
                        Page <%= currentPage %> / <%= totalPages %> — <%= totalLogs %> logs (derniers)
                    </div>
                    <div class="d-flex gap-2">
                        <div class="btn-group">
                            <a class="btn btn-outline-secondary btn-sm <%= (currentPage <= 1) ? "disabled" : "" %>"
                               href="<%= request.getContextPath() %>/admin/dashboard?page=<%= Math.max(1, currentPage - 1) %>">Précédent</a>
                            <a class="btn btn-outline-secondary btn-sm <%= (currentPage >= totalPages) ? "disabled" : "" %>"
                               href="<%= request.getContextPath() %>/admin/dashboard?page=<%= Math.min(totalPages, currentPage + 1) %>">Suivant</a>
                        </div>
                        <a class="btn btn-outline-primary btn-sm" href="<%= request.getContextPath() %>/admin/logs">Voir tout</a>
                    </div>
                </div>
            </div>
        </main>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/bootstrap/js/bootstrap.bundle.min.js"></script>
</body>
</html>
