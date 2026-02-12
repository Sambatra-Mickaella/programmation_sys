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
        List<String> notifications = (ArrayList<String>) request.getAttribute("notifications");
%>
<!DOCTYPE html>
<html lang="fr">
<head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/bootstrap/css/bootstrap.min.css">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css">
        <title>SmartDrive</title>
</head>
<body class="bg-light">

    <!-- Top navbar -->
    <nav class="navbar navbar-light bg-white shadow-sm">
        <div class="container-fluid">
            <a class="navbar-brand d-flex align-items-center" href="#">
                <i class="bi bi-cloud-fill text-primary me-2" style="font-size:1.25rem"></i>
                <span class="fw-bold">SmartDrive</span>
            </a>
            <div class="d-flex align-items-center gap-3">
                <button class="btn btn-outline-secondary btn-sm d-md-none" type="button" data-bs-toggle="offcanvas" data-bs-target="#offcanvasSidebar">Menu</button>
            </div>
        </div>
    </nav>

    <div class="container-fluid">
        <div class="row">
            <!-- Import du sidebar réutilisable -->
            <jsp:include page="sidebar.jsp" />

            <!-- Main content -->
            <main class="col-12 col-md-9 col-lg-10 p-4">
                    <% if (notifications !=null && !notifications.isEmpty()) { %>
                        <div class="alert alert-info">
                            <div class="fw-bold mb-2"><i class="bi bi-bell me-2"></i>Notifications</div>
                            <ul class="mb-0">
                                <%
                                    for (String n : notifications) {
                                        if (n == null) continue;
                                        // format serveur: ts;type;msg
                                        String[] parts = n.split(";", 3);
                                        String msg = (parts.length >= 3) ? parts[2] : n;
                                %>
                                    <li>
                                        <%= msg %>
                                    </li>
                                <%
                                    }
                                %>
                            </ul>
                        </div>
                        <% } %>

                <div class="d-flex justify-content-between align-items-center mb-4">
                    <h3 class="mb-0 text-primary">Utilisateurs</h3>
                    <div class="d-flex gap-2 align-items-center">
                        <form action="<%= request.getContextPath() %>/upload" method="post" enctype="multipart/form-data" class="m-0">
                            <label class="btn btn-primary btn-sm mb-0">
                                <i class="bi bi-upload me-1"></i>Upload
                                <input type="file" name="file" hidden onchange="this.form.submit()">
                            </label>
                        </form>
                    </div>
                </div>

                <!-- Simple responsive grid -->
                <section>
                    <div class="row g-3">
                        <% if (list_users != null) { 
                            for (String users : list_users) { 
                                if (users == null) continue;
                                // Même page pour tous: liste + statut (approved/pending/none) + bouton "Demander lecture"
                                String ownerUrl = request.getContextPath() + "/show/partages_fichiers?owner=" + java.net.URLEncoder.encode(users, "UTF-8");
                        %>
                                <div class="col-12 col-sm-6 col-lg-4">
                                    <div class="card shadow-sm">
                                        <div class="card-body">
                                            <h5 class="card-title text-primary"><%= users %></h5>
                                            <a href="<%= ownerUrl %>" class="btn btn-outline-primary btn-sm">Voir</a>
                                        </div>
                                    </div>
                                </div>
                            <% } 
                        } %>
                    </div>
                </section>
            </main>
        </div>
    </div>

    <script src="<%= request.getContextPath() %>/assets/bootstrap/js/bootstrap.bundle.min.js"></script>
</body>
</html>
