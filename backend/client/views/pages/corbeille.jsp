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
    List<String> trashRows = (ArrayList<String>) request.getAttribute("trash_rows");
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
    <title>SmartDrive - Corbeille</title>
</head>
<body class="bg-light">
    <nav class="navbar navbar-light bg-white border-bottom">
        <div class="container-fluid">
            <a class="navbar-brand fw-bold" href="#">Corbeille</a>
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
                    <h3 class="text-primary mb-0"><i class="bi bi-trash-fill me-2"></i>Fichiers supprimés</h3>
                    <a class="btn btn-outline-danger btn-sm"
                       href="<%= request.getContextPath() %>/show/corbeille_purge?id=ALL"
                       onclick="return confirm('Vider la corbeille définitivement ?');">
                        <i class="bi bi-x-circle me-1"></i>Vider
                    </a>
                </div>

                <div class="card shadow-sm">
                    <div class="card-body p-0">
                        <div class="table-responsive">
                            <table class="table table-striped mb-0">
                                <thead>
                                    <tr>
                                        <th>Fichier</th>
                                        <th>Taille</th>
                                        <th>Supprimé le</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                <% if (trashRows != null && !trashRows.isEmpty()) {
                                       for (String row : trashRows) {
                                           if (row == null) continue;
                                           String[] parts = row.split(";", 4);
                                           if (parts.length < 4) continue;
                                           String id = parts[0];
                                           String original = parts[1];
                                           String size = parts[2];
                                           String deletedAt = parts[3];
                                %>
                                    <tr>
                                        <td class="text-truncate" style="max-width: 360px" title="<%= original %>"><%= original %></td>
                                        <td><%= size %> o</td>
                                        <td><%= deletedAt %></td>
                                        <td>
                                            <div class="d-flex gap-2">
                                                <a class="btn btn-success btn-sm"
                                                   href="<%= request.getContextPath() %>/show/corbeille_restore?id=<%= java.net.URLEncoder.encode(id, "UTF-8") %>">
                                                    Restaurer
                                                </a>
                                                <a class="btn btn-outline-danger btn-sm"
                                                   href="<%= request.getContextPath() %>/show/corbeille_purge?id=<%= java.net.URLEncoder.encode(id, "UTF-8") %>"
                                                   onclick="return confirm('Supprimer définitivement ?');">
                                                    Supprimer
                                                </a>
                                            </div>
                                        </td>
                                    </tr>
                                <%     }
                                   } else { %>
                                    <tr>
                                        <td colspan="4" class="text-center text-muted py-4">Corbeille vide.</td>
                                    </tr>
                                <% } %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <div class="text-muted small mt-3">
                    La suppression depuis “Mes fichiers” déplace le fichier ici. La suppression définitive le supprime du disque.
                </div>
            </main>
        </div>
    </div>

    <script src="<%= request.getContextPath() %>/assets/bootstrap/js/bootstrap.bundle.min.js"></script>
</body>
</html>
