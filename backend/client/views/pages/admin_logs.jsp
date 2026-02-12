<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%@ page import="java.util.List" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null || !user.isAdmin()) {
        response.sendRedirect(request.getContextPath() + "/pages/home.jsp");
        return;
    }
    List<String> auditRows = (List<String>) request.getAttribute("audit_rows");
    String error = (String) request.getAttribute("error");

    Integer currentPage = (Integer) request.getAttribute("page");
    Integer totalPages = (Integer) request.getAttribute("totalPages");
    Integer totalLogs = (Integer) request.getAttribute("totalLogs");
    if (currentPage == null) currentPage = 1;
    if (totalPages == null) totalPages = 1;
    if (totalLogs == null) totalLogs = (auditRows != null ? auditRows.size() : 0);
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/bootstrap/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css">
    <title>SmartDrive - Admin Logs</title>
</head>
<body class="bg-light">
<nav class="navbar navbar-light bg-white border-bottom">
    <div class="container-fluid">
        <a class="navbar-brand fw-bold" href="<%= request.getContextPath() %>/admin/dashboard"><i class="bi bi-shield-lock-fill me-2"></i>Admin</a>
        <span class="text-muted">Logs & Audit</span>
    </div>
</nav>

<div class="container-fluid">
    <div class="row">
        <jsp:include page="sidebar.jsp" />

        <main class="col-12 col-md-9 col-lg-10 p-4">
            <% if (error != null) { %>
                <div class="alert alert-danger"><%= error %></div>
            <% } %>

            <div class="card shadow-sm">
                <div class="card-header bg-white fw-semibold">Historique (upload / download / delete / share)</div>
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
                                    <td class="text-truncate" style="max-width: 640px" title="<%= p[3] %>"><%= p[3] %></td>
                                </tr>
                            <%     }
                               } else { %>
                                <tr><td colspan="4" class="text-center text-muted py-4">Aucun log.</td></tr>
                            <% } %>
                            </tbody>
                        </table>
                    </div>
                </div>
                <div class="card-footer bg-white d-flex justify-content-between align-items-center">
                    <div class="text-muted small">
                            Page <%= currentPage %> / <%= totalPages %> — <%= totalLogs %> logs (derniers)
                    </div>
                    <div class="btn-group">
                            <a class="btn btn-outline-secondary btn-sm <%= (currentPage <= 1) ? "disabled" : "" %>"
                                href="<%= request.getContextPath() %>/admin/logs?page=<%= Math.max(1, currentPage - 1) %>">Précédent</a>
                            <a class="btn btn-outline-secondary btn-sm <%= (currentPage >= totalPages) ? "disabled" : "" %>"
                                href="<%= request.getContextPath() %>/admin/logs?page=<%= Math.min(totalPages, currentPage + 1) %>">Suivant</a>
                    </div>
                </div>
            </div>
        </main>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/bootstrap/js/bootstrap.bundle.min.js"></script>
</body>
</html>
