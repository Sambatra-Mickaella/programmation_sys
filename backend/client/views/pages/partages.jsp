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
    List<String> list_users = (ArrayList<String>) request.getAttribute("list_users");
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
    <title>SmartDrive - Partages</title>
</head>
<body class="bg-light">
    <nav class="navbar navbar-light bg-white border-bottom">
        <div class="container-fluid">
            <a class="navbar-brand fw-bold" href="#">SmartDrive</a>
            <div class="d-flex gap-2">
                <a class="btn btn-outline-primary btn-sm" href="<%= request.getContextPath() %>/show/partages_demandes">
                    <i class="bi bi-inbox me-1"></i>Demandes reçues
                </a>
            </div>
        </div>
    </nav>

    <div class="container-fluid">
        <div class="row">
            <jsp:include page="sidebar.jsp" />

            <main class="col-12 col-md-9 col-lg-10 p-4">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h3 class="text-primary mb-0"><i class="bi bi-share-fill me-2"></i>Partages</h3>
                </div>

                <% if (message != null) { %>
                    <div class="alert alert-info"><%= message %></div>
                <% } %>
                <% if (error != null) { %>
                    <div class="alert alert-danger"><%= error %></div>
                <% } %>

                <div class="row g-3">
                    <% if (list_users != null && !list_users.isEmpty()) {
                        for (String u : list_users) {
                            if (u == null) continue;
                            if (u.equalsIgnoreCase(user.getNom())) continue;
                            String ownerUrl = request.getContextPath() + "/show/partages_fichiers?owner=" + java.net.URLEncoder.encode(u, "UTF-8");
                    %>
                        <div class="col-12 col-sm-6 col-lg-4 col-xl-3">
                            <div class="card border h-100 shadow-sm">
                                <div class="card-body d-flex flex-column">
                                    <h5 class="card-title text-primary mb-3">
                                        <i class="bi bi-person-circle me-2"></i><%= u %>
                                    </h5>
                                    <div class="mt-auto">
                                        <a class="btn btn-primary btn-sm w-100"
                                           href="<%= ownerUrl %>">
                                            Voir les fichiers
                                        </a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    <%  }
                       } else { %>
                        <div class="col-12">
                            <div class="alert alert-secondary">Aucun autre utilisateur (ou backend injoignable).</div>
                        </div>
                    <% } %>
                </div>
            </main>
        </div>
    </div>

    <script src="<%= request.getContextPath() %>/assets/bootstrap/js/bootstrap.bundle.min.js"></script>
</body>
</html>
