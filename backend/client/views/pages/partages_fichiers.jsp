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
    String owner = (String) request.getAttribute("owner");
    List<String> list_shared = (ArrayList<String>) request.getAttribute("list_shared");
    String message = (String) request.getAttribute("message");
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/bootstrap/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css">
    <title>SmartDrive - Fichiers partagés</title>
</head>
<body class="bg-light">
    <nav class="navbar navbar-light bg-white border-bottom">
        <div class="container-fluid">
            <a class="navbar-brand fw-bold" href="<%= request.getContextPath() %>/show/partages">Partages</a>
            <span class="navbar-text text-muted">Utilisateur : <%= owner %></span>
        </div>
    </nav>

    <div class="container-fluid">
        <div class="row">
            <jsp:include page="sidebar.jsp" />

            <main class="col-12 col-md-9 col-lg-10 p-4">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h3 class="text-primary mb-0"><i class="bi bi-folder2-open me-2"></i>Fichiers de <%= owner %>
                    </h3>
                    <a class="btn btn-outline-secondary btn-sm" href="<%= request.getContextPath() %>/show/partages">
                        <i class="bi bi-arrow-left me-1"></i>Retour
                    </a>
                </div>

                <% if (message != null) { %>
                    <div class="alert alert-info"><%= message %></div>
                <% } %>

                <div class="row g-3">
                    <% if (list_shared != null && !list_shared.isEmpty()) {
                        for (String row : list_shared) {
                            if (row == null) continue;
                            String[] parts = row.split(";", 3);
                            if (parts.length < 3) continue;
                            String filename = parts[0];
                            String size = parts[1];
                            String status = parts[2];
                            String fileEncoded = java.net.URLEncoder.encode(filename, "UTF-8");
                            String ownerEncoded = java.net.URLEncoder.encode(owner, "UTF-8");
                    %>
                        <div class="col-12 col-sm-6 col-lg-4 col-xl-3">
                            <div class="card border h-100 shadow-sm">
                                <div class="card-body d-flex flex-column">
                                    <h6 class="card-title text-truncate mb-1" title="<%= filename %>"><%= filename %></h6>
                                    <div class="small text-muted mb-3"><%= size %> octets</div>

                                    <div class="mt-auto">
                                        <% if ("approved".equalsIgnoreCase(status)) { %>
                                            <div class="d-flex gap-2">
                                                <a class="btn btn-primary btn-sm flex-grow-1"
                                                   href="<%= request.getContextPath() %>/show/shared_view?owner=<%= ownerEncoded %>&file=<%= fileEncoded %>">
                                                    <i class="bi bi-eye me-1"></i>Voir
                                                </a>
                                                <a class="btn btn-outline-primary btn-sm"
                                                   href="<%= request.getContextPath() %>/show/shared_download?owner=<%= ownerEncoded %>&file=<%= fileEncoded %>">
                                                    <i class="bi bi-download"></i>
                                                </a>
                                            </div>
                                        <% } else if ("pending".equalsIgnoreCase(status)) { %>
                                            <button class="btn btn-outline-warning btn-sm w-100" disabled>
                                                <i class="bi bi-hourglass-split me-1"></i>Demande en attente
                                            </button>
                                        <% } else { %>
                                            <a class="btn btn-outline-info btn-sm w-100"
                                               href="<%= request.getContextPath() %>/show/partages_demander?owner=<%= ownerEncoded %>&file=<%= fileEncoded %>">
                                                <i class="bi bi-send me-1"></i>Demander lecture
                                            </a>
                                        <% } %>
                                    </div>
                                </div>
                            </div>
                        </div>
                    <%  }
                       } else { %>
                        <div class="col-12">
                            <div class="alert alert-secondary">Aucun fichier trouvé.</div>
                        </div>
                    <% } %>
                </div>
            </main>
        </div>
    </div>

    <script src="<%= request.getContextPath() %>/assets/bootstrap/js/bootstrap.bundle.min.js"></script>
</body>
</html>

