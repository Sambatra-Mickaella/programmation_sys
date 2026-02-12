<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/bootstrap/css/bootstrap.min.css">
    <title>SmartDrive</title>
</head>
<body class="bg-light">

    <div class="container min-vh-100 d-flex align-items-center justify-content-center py-5">
        <div class="card shadow-sm" style="max-width: 520px; width: 100%;">
            <div class="card-body p-4 p-md-5">
                <div class="text-center mb-4">
                    <h1 class="h3 mb-1">SmartDrive</h1>
                    <div class="text-muted">Connexion</div>
                </div>
<% if (error !=null) { %>
    <div class="alert alert-danger" role="alert">
        <%= error %>
    </div>
    <% } %>

                <form action="<%= request.getContextPath() %>/user" method="post">
                    <div class="mb-3">
                        <label class="form-label" for="nom">Nom</label>
                        <input class="form-control" type="text" name="nom" id="nom" placeholder="Votre nom..." autocomplete="username"
                            required>
                    </div>
                    <div class="mb-4">
                        <label class="form-label" for="mdp">Mot de passe</label>
                        <input class="form-control" type="password" name="password" id="mdp" placeholder="Mot de passe"
                            autocomplete="current-password" required>
                        </div>
                    <div class="d-grid">
                        <button class="btn btn-primary" type="submit">Connexion</button>
                    </div>
                    </form>
            </div>
        </div>
    </div>

    <script src="<%= request.getContextPath() %>/assets/bootstrap/js/bootstrap.bundle.min.js"></script>
</body>
</html>
