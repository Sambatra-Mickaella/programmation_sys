<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%@ page import="java.util.List" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null || !user.isAdmin()) {
        response.sendRedirect(request.getContextPath() + "/pages/home.jsp");
        return;
    }
    List<String> rows = (List<String>) request.getAttribute("admin_user_rows");
    String message = (String) request.getAttribute("message");
    String error = (String) request.getAttribute("error");

    Integer currentPage = (Integer) request.getAttribute("page");
    Integer totalPages = (Integer) request.getAttribute("totalPages");
    Integer totalUsers = (Integer) request.getAttribute("totalUsers");
    if (currentPage == null) currentPage = 1;
    if (totalPages == null) totalPages = 1;
    if (totalUsers == null) totalUsers = (rows != null ? rows.size() : 0);
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/bootstrap/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css">
    <title>SmartDrive - Admin Utilisateurs</title>
</head>
<body class="bg-light">
<nav class="navbar navbar-light bg-white border-bottom">
    <div class="container-fluid">
        <a class="navbar-brand fw-bold" href="<%= request.getContextPath() %>/admin/dashboard"><i class="bi bi-shield-lock-fill me-2"></i>Admin</a>
        <span class="text-muted">Gestion utilisateurs</span>
    </div>
</nav>

<div class="container-fluid">
    <div class="row">
        <jsp:include page="sidebar.jsp" />

        <main class="col-12 col-md-9 col-lg-10 p-4">
            <% if (message != null) { %>
                <div class="alert alert-info"><%= message %></div>
            <% } %>
            <% if (error != null) { %>
                <div class="alert alert-danger"><%= error %></div>
            <% } %>

            <div class="d-flex justify-content-between align-items-center mb-3">
                <h3 class="mb-0"><i class="bi bi-people-fill me-2"></i>Utilisateurs</h3>
                <div class="d-flex gap-2">
                    <a class="btn btn-outline-primary btn-sm" href="<%= request.getContextPath() %>/admin/storage">Stockage</a>
                    <a class="btn btn-outline-primary btn-sm" href="<%= request.getContextPath() %>/admin/logs">Logs</a>
                    <a class="btn btn-outline-primary btn-sm" href="<%= request.getContextPath() %>/admin/monitor">Monitoring</a>
                </div>
            </div>

            <div class="card shadow-sm">
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-striped align-middle mb-0">
                            <thead>
                            <tr>
                                <th>User</th>
                                <th>Admin</th>
                                <th>Blocked</th>
                                <th>Quota (bytes)</th>
                                <th style="width: 520px;">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <% if (rows != null && !rows.isEmpty()) {
                                   for (String r : rows) {
                                       if (r == null) continue;
                                       String[] p = r.split(";", 4);
                                       if (p.length < 4) continue;
                                       String u = p[0];
                                       boolean isAdmin = Boolean.parseBoolean(p[1]);
                                       boolean blocked = Boolean.parseBoolean(p[2]);
                                       String quota = p[3];
                            %>
                                <tr>
                                    <td class="fw-semibold"><%= u %></td>
                                    <td><%= isAdmin ? "Oui" : "Non" %></td>
                                    <td>
                                        <span class="badge <%= blocked ? "text-bg-danger" : "text-bg-success" %>">
                                            <%= blocked ? "Bloqué" : "Actif" %>
                                        </span>
                                    </td>
                                    <td class="text-muted"><%= quota %></td>
                                    <td>
                                        <div class="d-flex flex-wrap gap-2">
                                            <form method="post" action="<%= request.getContextPath() %>/admin/users_action" class="d-inline">
                                                <input type="hidden" name="action" value="block" />
                                                <input type="hidden" name="user" value="<%= u %>" />
                                                <input type="hidden" name="blocked" value="<%= !blocked %>" />
                                                <button type="submit" class="btn btn-sm <%= blocked ? "btn-success" : "btn-warning" %>" <%= u.equalsIgnoreCase(user.getNom()) ? "disabled" : "" %>>
                                                    <%= blocked ? "Débloquer" : "Bloquer" %>
                                                </button>
                                            </form>

                                            <form method="post" action="<%= request.getContextPath() %>/admin/users_action" class="d-inline">
                                                <input type="hidden" name="action" value="quota" />
                                                <input type="hidden" name="user" value="<%= u %>" />
                                                <div class="input-group input-group-sm" style="max-width: 260px;">
                                                    <input type="number" class="form-control" name="quota" value="<%= quota %>" min="0" step="1" />
                                                    <button class="btn btn-outline-primary" type="submit">Quota</button>
                                                </div>
                                            </form>

                                            <form method="post" action="<%= request.getContextPath() %>/admin/users_action" class="d-inline" onsubmit="return confirm('Supprimer définitivement ' + '<%= u %>' + ' ?');">
                                                <input type="hidden" name="action" value="delete" />
                                                <input type="hidden" name="user" value="<%= u %>" />
                                                <button type="submit" class="btn btn-sm btn-outline-danger" <%= u.equalsIgnoreCase(user.getNom()) ? "disabled" : "" %>>
                                                    Supprimer
                                                </button>
                                            </form>
                                        </div>
                                    </td>
                                </tr>
                            <%     }
                               } else { %>
                                <tr><td colspan="5" class="text-center text-muted py-4">Aucun utilisateur.</td></tr>
                            <% } %>
                            </tbody>
                        </table>
                    </div>
                </div>

                <div class="card-footer bg-white d-flex justify-content-between align-items-center">
                    <div class="text-muted small">
                        Page <%= currentPage %> / <%= totalPages %> — <%= totalUsers %> utilisateurs
                    </div>
                    <div class="btn-group">
                        <a class="btn btn-outline-secondary btn-sm <%= (currentPage <= 1) ? "disabled" : "" %>"
                           href="<%= request.getContextPath() %>/admin/users?page=<%= Math.max(1, currentPage - 1) %>">Précédent</a>
                        <a class="btn btn-outline-secondary btn-sm <%= (currentPage >= totalPages) ? "disabled" : "" %>"
                           href="<%= request.getContextPath() %>/admin/users?page=<%= Math.min(totalPages, currentPage + 1) %>">Suivant</a>
                    </div>
                </div>
            </div>
        </main>
    </div>
</div>

<script src="<%= request.getContextPath() %>/assets/bootstrap/js/bootstrap.bundle.min.js"></script>
</body>
</html>
