<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/pages/home.jsp");
        return;
    }
    String filename = (String) request.getAttribute("file");
    List<String> versionRows = (ArrayList<String>) request.getAttribute("version_rows");
    String message = (String) request.getAttribute("message");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/bootstrap/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css">
    <title>SmartDrive - Versions</title>
</head>
<body class="bg-light">
    <nav class="navbar navbar-light bg-white border-bottom">
        <div class="container-fluid">
            <a class="navbar-brand fw-bold" href="<%= request.getContextPath() %>/show/mes_fichiers">Mes fichiers</a>
            <span class="navbar-text text-muted"><i class="bi bi-clock-history me-1"></i>Versions</span>
        </div>
    </nav>

    <div class="container-fluid">
        <div class="row">
            <jsp:include page="sidebar.jsp" />

            <main class="col-12 col-md-9 col-lg-10 p-4">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h3 class="text-primary mb-0">Historique: <span class="text-body"><%= filename %></span></h3>
                    <a class="btn btn-outline-secondary btn-sm" href="<%= request.getContextPath() %>/show/mes_fichiers">
                        <i class="bi bi-arrow-left me-1"></i>Retour
                    </a>
                </div>

                <% if (message != null) { %>
                    <div class="alert alert-info"><%= message %></div>
                <% } %>
                <% if (error != null) { %>
                    <div class="alert alert-danger"><%= error %></div>
                <% } %>

                <div class="card shadow-sm">
                    <div class="card-body p-0">
                        <div class="table-responsive">
                            <table class="table table-striped mb-0">
                                <thead>
                                    <tr>
                                        <th>Version</th>
                                        <th>Taille</th>
                                        <th>Date</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                <% if (versionRows != null && !versionRows.isEmpty()) {
                                       for (String row : versionRows) {
                                           if (row == null) continue;
                                           String[] parts = row.split(";", 3);
                                           if (parts.length < 3) continue;
                                           String id = parts[0];
                                           String size = parts[1];
                                           String createdAt = parts[2];
                                %>
                                    <tr>
                                        <td class="text-truncate" style="max-width: 420px" title="<%= id %>"><%= id %></td>
                                        <td><%= size %> o</td>
                                        <td><%= createdAt %></td>
                                        <td>
                                            <a class="btn btn-success btn-sm"
                                               href="<%= request.getContextPath() %>/show/versions_restore?file=<%= java.net.URLEncoder.encode(filename, "UTF-8") %>&id=<%= java.net.URLEncoder.encode(id, "UTF-8") %>"
                                               onclick="return confirm('Restaurer cette version ?');">
                                                Restaurer
                                            </a>
                                        </td>
                                    </tr>
                                <%     }
                                   } else { %>
                                    <tr>
                                        <td colspan="4" class="text-center text-muted py-4">Aucune version enregistrée.</td>
                                    </tr>
                                <% } %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <div class="text-muted small mt-3">
                    Une nouvelle version est créée automatiquement si tu re-upload un fichier qui existe déjà.
                </div>
            </main>
        </div>
    </div>

    <script src="<%= request.getContextPath() %>/assets/bootstrap/js/bootstrap.bundle.min.js"></script>
</body>
</html>
