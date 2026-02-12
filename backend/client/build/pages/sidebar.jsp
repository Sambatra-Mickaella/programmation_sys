<%@ page import="model.User" %>
<%
    User user = (User) session.getAttribute("user");
    
    // Déterminer la page actuelle pour le menu actif
    String currentUrl = request.getRequestURL().toString();
    String accueilActive = currentUrl.contains("/accueil") ? "active" : "";
    String fichiersActive = currentUrl.contains("/mes_fichiers") ? "active" : "";
    String corbeilleActive = currentUrl.contains("/corbeille") ? "active" : "";
    String partagesActive = currentUrl.contains("/partages") ? "active" : "";
    String stockageActive = currentUrl.contains("/stockage") ? "active" : "";
    String parametresActive = currentUrl.contains("/parametres") ? "active" : "";
    String adminActive = currentUrl.contains("/admin") ? "active" : "";
%>

<!-- Sidebar for md+ -->
<aside class="col-12 col-md-3 col-lg-2 d-none d-md-block border-end bg-body-tertiary min-vh-100">
    <div class="p-3 position-sticky top-0">
        <div class="card mb-3 shadow-sm">
            <div class="card-body py-3">
                <div class="fw-semibold">Bonjour, <%= user.getNom() %></div>
                <div class="text-muted small"><%= user.getNom() %>@example.com</div>
            </div>
        </div>

        <nav class="nav nav-pills flex-column gap-1">
            <a class="nav-link <%= accueilActive %>" href="<%= request.getContextPath() %>/show/accueil">
                <i class="bi bi-house-door-fill me-2"></i>Accueil
            </a>
            <a class="nav-link <%= fichiersActive %>" href="<%= request.getContextPath() %>/show/mes_fichiers">
                <i class="bi bi-folder-fill me-2"></i>Mes fichiers
            </a>
            <a class="nav-link <%= corbeilleActive %>" href="<%= request.getContextPath() %>/show/corbeille">
                <i class="bi bi-trash-fill me-2"></i>Corbeille
            </a>
            <a class="nav-link <%= partagesActive %>" href="<%= request.getContextPath() %>/show/partages">
                <i class="bi bi-share-fill me-2"></i>Partages
            </a>
            <a class="nav-link <%= stockageActive %>" href="<%= request.getContextPath() %>/show/stockage">
                <i class="bi bi-hdd-fill me-2"></i>Stockage
            </a>
            <% if (user != null && user.isAdmin()) { %>
            <a class="nav-link <%= adminActive %>" href="<%= request.getContextPath() %>/admin/dashboard">
                <i class="bi bi-shield-lock-fill me-2"></i>Admin
            </a>
            <% } %>
            <a class="nav-link <%= parametresActive %>" href="#" aria-disabled="true">
                <i class="bi bi-gear-fill me-2"></i>Parametres
            </a>
        </nav>

        <div class="mt-3 pt-3 border-top">
            <a href="<%= request.getContextPath() %>/logout" class="btn btn-outline-danger btn-sm w-100">
                <i class="bi bi-box-arrow-right me-2"></i>Déconnexion
            </a>
        </div>
    </div>
</aside>

<!-- Offcanvas sidebar for small screens -->
<div class="offcanvas offcanvas-start bg-body-tertiary" tabindex="-1" id="offcanvasSidebar">
    <div class="offcanvas-header">
        <h5 class="offcanvas-title">SmartDrive</h5>
        <button type="button" class="btn-close" data-bs-dismiss="offcanvas"></button>
    </div>
    <div class="offcanvas-body d-flex flex-column">
        <nav class="nav nav-pills flex-column gap-1">
            <a class="nav-link <%= accueilActive %>" href="<%= request.getContextPath() %>/show/accueil"><i class="bi bi-house-door-fill me-2"></i>Accueil</a>
            <a class="nav-link <%= fichiersActive %>" href="<%= request.getContextPath() %>/show/mes_fichiers"><i class="bi bi-folder-fill me-2"></i>Mes fichiers</a>
            <a class="nav-link <%= corbeilleActive %>" href="<%= request.getContextPath() %>/show/corbeille"><i class="bi bi-trash-fill me-2"></i>Corbeille</a>
            <a class="nav-link <%= partagesActive %>" href="<%= request.getContextPath() %>/show/partages"><i class="bi bi-share-fill me-2"></i>Partages</a>
            <a class="nav-link <%= stockageActive %>" href="<%= request.getContextPath() %>/show/stockage"><i class="bi bi-hdd-fill me-2"></i>Stockage</a>
            <% if (user != null && user.isAdmin()) { %>
            <a class="nav-link <%= adminActive %>" href="<%= request.getContextPath() %>/admin/dashboard"><i class="bi bi-shield-lock-fill me-2"></i>Admin</a>
            <% } %>
            <a class="nav-link <%= parametresActive %>" href="#" aria-disabled="true"><i class="bi bi-gear-fill me-2"></i>Parametres</a>
        </nav>
        <div class="mt-auto pt-3 border-top">
            <a href="<%= request.getContextPath() %>/logout" class="btn btn-outline-danger btn-sm w-100">
                <i class="bi bi-box-arrow-right me-2"></i>Déconnexion
            </a>
        </div>
    </div>
</div>
