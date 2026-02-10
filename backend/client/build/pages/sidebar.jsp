<%@ page import="model.User" %>
<%
    User user = (User) session.getAttribute("user");
    
    // Déterminer la page actuelle pour le menu actif
    String currentUrl = request.getRequestURL().toString();
    String accueilActive = currentUrl.contains("/accueil") ? "active" : "";
    String fichiersActive = currentUrl.contains("/mes_fichiers") ? "active" : "";
    String partagesActive = currentUrl.contains("/partages") ? "active" : "";
    String stockageActive = currentUrl.contains("/stockage") ? "active" : "";
    String parametresActive = currentUrl.contains("/parametres") ? "active" : "";
%>

<!-- Sidebar for md+ -->
<aside class="col-12 col-md-3 col-lg-2 sidebar d-none d-md-block p-3">
    <div class="user-info mb-4">
        <div class="fw-bold">Bonjour, <%= user.getNom() %></div>
        <div class="user-email"><%= user.getNom() %>@example.com</div>
    </div>
    <nav class="nav flex-column">
        <a class="nav-link <%= accueilActive %>" href="/SmartDrive/show/accueil"><i class="bi bi-house-door-fill"></i>Accueil</a>
        <a class="nav-link <%= fichiersActive %>" href="/SmartDrive/show/mes_fichiers"><i class="bi bi-folder-fill"></i>Mes fichiers</a>
        <a class="nav-link <%= partagesActive %>" href="/SmartDrive/show/partages"><i class="bi bi-share-fill"></i>Partages</a>
        <a class="nav-link <%= stockageActive %>" href="/SmartDrive/show/stockage"><i class="bi bi-hdd-fill"></i>Stockage</a>
        <a class="nav-link <%= parametresActive %>" href="#"><i class="bi bi-gear-fill"></i>Parametres</a>
    </nav>
    <div class="mt-auto pt-3 border-top" style="border-top-color: var(--border-color) !important;">
        <a href="<%= request.getContextPath() %>/logout" class="btn btn-outline-danger btn-sm w-100">
            <i class="bi bi-box-arrow-right me-2"></i>Déconnexion
        </a>
    </div>
</aside>

<!-- Offcanvas sidebar for small screens -->
<div class="offcanvas offcanvas-start" tabindex="-1" id="offcanvasSidebar">
    <div class="offcanvas-header">
        <h5 class="offcanvas-title">SmartDrive</h5>
        <button type="button" class="btn-close" data-bs-dismiss="offcanvas"></button>
    </div>
    <div class="offcanvas-body d-flex flex-column">
        <nav class="nav flex-column">
            <a class="nav-link <%= accueilActive %>" href="/SmartDrive/show/accueil"><i class="bi bi-house-door-fill me-2"></i>Accueil</a>
            <a class="nav-link <%= fichiersActive %>" href="/SmartDrive/show/mes_fichiers"><i class="bi bi-folder-fill me-2"></i>Mes fichiers</a>
            <a class="nav-link <%= partagesActive %>" href="/SmartDrive/show/partages"><i class="bi bi-share-fill me-2"></i>Partages</a>
            <a class="nav-link <%= stockageActive %>" href="/SmartDrive/show/stockage"><i class="bi bi-hdd-fill me-2"></i>Stockage</a>
            <a class="nav-link <%= parametresActive %>" href="#"><i class="bi bi-gear-fill me-2"></i>Parametres</a>
        </nav>
        <div class="mt-auto pt-3 border-top" style="border-top-color: var(--border-color) !important;">
            <a href="<%= request.getContextPath() %>/logout" class="btn btn-outline-danger btn-sm w-100">
                <i class="bi bi-box-arrow-right me-2"></i>Déconnexion
            </a>
        </div>
    </div>
</div>
