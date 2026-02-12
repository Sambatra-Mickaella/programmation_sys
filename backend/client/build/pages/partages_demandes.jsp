<%@ page import="model.User" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/pages/home.jsp");
        return;
    }
    List<String> list_requests = (ArrayList<String>) request.getAttribute("list_requests");
    String message = (String) request.getAttribute("message");
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/bootstrap/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css">
    <title>SmartDrive - Demandes</title>
</head>
<body class="bg-light">
    <nav class="navbar navbar-light bg-white border-bottom">
        <div class="container-fluid">
            <a class="navbar-brand fw-bold" href="<%= request.getContextPath() %>/show/partages">Partages</a>
            <span class="navbar-text text-muted"><i class="bi bi-inbox me-1"></i>Demandes reçues</span>
        </div>
    </nav>

    <div class="container-fluid">
        <div class="row">
            <jsp:include page="sidebar.jsp" />

            <main class="col-12 col-md-9 col-lg-10 p-4">
                <% if (message != null) { %>
                    <div class="alert alert-info"><%= message %></div>
                <% } %>

                <div class="card shadow-sm">
                    <div class="card-header fw-bold">Demandes d'accès à vos fichiers</div>
                    <div class="card-body p-0">
                        <div class="table-responsive">
                            <table class="table table-striped mb-0">
                                <thead>
                                    <tr>
                                        <th>Demandeur</th>
                                        <th>Fichier</th>
                                        <th>Statut</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                <% if (list_requests != null && !list_requests.isEmpty()) {
                                    for (String row : list_requests) {
                                        if (row == null) continue;
                                        String[] parts = row.split(";", 4);
                                        if (parts.length < 3) continue;
                                        String requester = parts[0];
                                        String file = parts[1];
                                        String status = parts[2];
                                        String requesterEnc = java.net.URLEncoder.encode(requester, "UTF-8");
                                        String fileEnc = java.net.URLEncoder.encode(file, "UTF-8");
                                %>
                                    <tr>
                                        <td><%= requester %></td>
                                        <td class="text-truncate" style="max-width: 320px" title="<%= file %>"><%= file %></td>
                                        <td>
                                            <% if ("approved".equalsIgnoreCase(status)) { %>
                                                <span class="badge bg-success">approuvé</span>
                                            <% } else if ("denied".equalsIgnoreCase(status)) { %>
                                                <span class="badge bg-danger">refusé</span>
                                            <% } else { %>
                                                <span class="badge bg-warning text-dark">en attente</span>
                                            <% } %>
                                        </td>
                                        <td>
                                            <div class="d-flex gap-2">
                                                <a class="btn btn-success btn-sm"
                                                   href="<%= request.getContextPath() %>/show/partages_repondre?requester=<%= requesterEnc %>&file=<%= fileEnc %>&action=approve">
                                                    Approuver
                                                </a>
                                                <a class="btn btn-outline-danger btn-sm"
                                                   href="<%= request.getContextPath() %>/show/partages_repondre?requester=<%= requesterEnc %>&file=<%= fileEnc %>&action=deny">
                                                    Refuser
                                                </a>
                                            </div>
                                        </td>
                                    </tr>
                                <%  }
                                   } else { %>
                                    <tr>
                                        <td colspan="4" class="text-center text-muted py-4">Aucune demande.</td>
                                    </tr>
                                <% } %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    </div>

    <script src="<%= request.getContextPath() %>/assets/bootstrap/js/bootstrap.bundle.min.js"></script>
</body>
</html>

