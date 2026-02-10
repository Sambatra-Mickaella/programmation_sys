<%@ page import="model.User" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/pages/home.jsp");
        return;
    }
    
    // Récupérer les attributs de stockage
    Long quota = (Long) request.getAttribute("quota");
    Long stockageUtilise = (Long) request.getAttribute("stockageUtilise");
    Integer pourcentage = (Integer) request.getAttribute("pourcentage");
    String stockageFormate = (String) request.getAttribute("stockageFormate");
    String quotaFormate = (String) request.getAttribute("quotaFormate");
    String stockageRestantFormate = (String) request.getAttribute("stockageRestantFormate");
    Integer nombreFichiers = (Integer) request.getAttribute("nombreFichiers");
    
    // Valeurs par défaut si null
    if (quota == null) quota = 0L;
    if (stockageUtilise == null) stockageUtilise = 0L;
    if (pourcentage == null) pourcentage = 0;
    if (stockageFormate == null) stockageFormate = "0 o";
    if (quotaFormate == null) quotaFormate = "0 o";
    if (stockageRestantFormate == null) stockageRestantFormate = "0 o";
    if (nombreFichiers == null) nombreFichiers = 0;
    
    // Déterminer la couleur de la barre de progression
    String progressColor = "bg-success";
    if (pourcentage > 80) {
        progressColor = "bg-danger";
    } else if (pourcentage > 60) {
        progressColor = "bg-warning";
    } else if (pourcentage > 40) {
        progressColor = "bg-info";
    }
%>
<!DOCTYPE html>
<html lang="fr">
<head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="stylesheet" href="../assets/bootstrap/css/bootstrap.min.css">
        <link rel="stylesheet" href="css/normalize.css">
        <link rel="stylesheet" href="css/vendor.css">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css">
        <!-- Chart.js pour les graphiques -->
        <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
        <script src="https://cdn.jsdelivr.net/npm/chartjs-adapter-date-fns"></script>
        <title>SmartDrive</title>
        <style>
            :root {
                --primary-blue: #0d6efd;
                --dark-blue: #0a58ca;
                --light-blue: #6ea8fe;
                --dark-bg: #1a1a1a;
                --darker-bg: #0f0f0f;
                --card-bg: #2a2a2a;
                --text-light: #e9ecef;
                --text-muted: #adb5bd;
                --border-color: #3a3a3a;
            }

            body {
                background-color: var(--darker-bg);
                color: var(--text-light);
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            }

            /* === Navbar === */
            .navbar {
                background: linear-gradient(135deg, var(--dark-bg) 0%, #000 100%) !important;
                border-bottom: 2px solid var(--primary-blue);
                box-shadow: 0 4px 12px rgba(13, 110, 253, 0.15);
            }

            .navbar-brand {
                color: var(--text-light) !important;
                font-weight: 700;
                font-size: 1.5rem;
                letter-spacing: -0.5px;
            }

            .navbar-brand:hover {
                color: var(--light-blue) !important;
            }

            /* === Sidebar === */
            .sidebar {
                background: linear-gradient(180deg, var(--dark-bg) 0%, var(--darker-bg) 100%);
                border-right: 2px solid var(--primary-blue);
                min-height: 100vh;
                position: sticky;
                top: 0;
                padding: 2rem 1rem !important;
                box-shadow: 4px 0 12px rgba(13, 110, 253, 0.1);
            }

            .sidebar .user-info {
                background: linear-gradient(135deg, var(--primary-blue) 0%, var(--dark-blue) 100%);
                padding: 1.5rem;
                border-radius: 12px;
                margin-bottom: 2rem;
                box-shadow: 0 4px 12px rgba(13, 110, 253, 0.3);
            }

            .sidebar .user-info .fw-bold {
                font-size: 1.1rem;
                color: #fff;
                margin-bottom: 0.25rem;
            }

            .sidebar .user-email {
                color: rgba(255, 255, 255, 0.8);
                font-size: 0.85rem;
            }

            .sidebar .nav-link {
                color: var(--text-light);
                padding: 0.875rem 1.25rem;
                margin-bottom: 0.5rem;
                border-radius: 10px;
                transition: all 0.3s ease;
                font-weight: 500;
                border-left: 3px solid transparent;
            }

            .sidebar .nav-link:hover {
                background: linear-gradient(90deg, rgba(13, 110, 253, 0.15) 0%, transparent 100%);
                border-left-color: var(--primary-blue);
                color: var(--light-blue);
                transform: translateX(5px);
            }

            .sidebar .nav-link.active {
                background: linear-gradient(90deg, rgba(13, 110, 253, 0.25) 0%, rgba(13, 110, 253, 0.1) 100%);
                border-left-color: var(--primary-blue);
                color: #fff;
            }

            .sidebar .nav-link i {
                margin-right: 0.75rem;
                font-size: 1.1rem;
            }

            /* === Offcanvas === */
            .offcanvas {
                background: linear-gradient(180deg, var(--dark-bg) 0%, var(--darker-bg) 100%);
                color: var(--text-light);
                border-right: 2px solid var(--primary-blue);
            }

            .offcanvas-header {
                border-bottom: 1px solid var(--border-color);
            }

            .offcanvas-title {
                color: var(--text-light);
                font-weight: 700;
            }

            .offcanvas .btn-close {
                filter: invert(1);
            }

            .offcanvas .nav-link {
                color: var(--text-light);
                padding: 0.875rem 1.25rem;
                margin-bottom: 0.5rem;
                border-radius: 10px;
                transition: all 0.3s ease;
            }

            .offcanvas .nav-link:hover {
                background: rgba(13, 110, 253, 0.15);
                color: var(--light-blue);
            }

            /* === Main content === */
            main {
                background: var(--darker-bg);
                min-height: 100vh;
                padding: 2rem !important;
            }

            main h3 {
                color: var(--light-blue);
                font-weight: 700;
                font-size: 2rem;
                margin-bottom: 0;
            }

            /* === Cards === */
            .card {
                background: linear-gradient(135deg, var(--card-bg) 0%, var(--dark-bg) 100%);
                border: 1px solid var(--border-color);
                border-radius: 16px;
                transition: all 0.3s ease;
                overflow: hidden;
                position: relative;
            }

            .card::before {
                content: '';
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                height: 4px;
                background: linear-gradient(90deg, var(--primary-blue) 0%, var(--light-blue) 100%);
                opacity: 0;
                transition: opacity 0.3s ease;
            }

            .card:hover {
                transform: translateY(-8px);
                box-shadow: 0 12px 24px rgba(13, 110, 253, 0.3);
                border-color: var(--primary-blue);
            }

            .card:hover::before {
                opacity: 1;
            }

            .card-body {
                padding: 1.75rem;
            }

            .card-title {
                color: var(--light-blue);
                font-size: 1.25rem;
                font-weight: 600;
                margin-bottom: 0.75rem;
            }

            .card-text {
                color: var(--text-muted);
                font-size: 0.9rem;
                margin-bottom: 1.25rem;
            }

            /* === Buttons === */
            .btn-primary {
                background: linear-gradient(135deg, var(--primary-blue) 0%, var(--dark-blue) 100%);
                border: none;
                padding: 0.625rem 1.5rem;
                font-weight: 600;
                border-radius: 10px;
                transition: all 0.3s ease;
                box-shadow: 0 4px 12px rgba(13, 110, 253, 0.3);
            }

            .btn-primary:hover {
                background: linear-gradient(135deg, var(--dark-blue) 0%, var(--primary-blue) 100%);
                transform: translateY(-2px);
                box-shadow: 0 6px 16px rgba(13, 110, 253, 0.4);
            }

            .btn-outline-primary {
                color: var(--light-blue);
                border: 2px solid var(--primary-blue);
                border-radius: 10px;
                font-weight: 600;
                padding: 0.5rem 1.25rem;
                transition: all 0.3s ease;
            }

            .btn-outline-primary:hover {
                background: var(--primary-blue);
                border-color: var(--primary-blue);
                color: #fff;
                transform: translateY(-2px);
                box-shadow: 0 4px 12px rgba(13, 110, 253, 0.3);
            }

            .btn-outline-secondary {
                color: var(--text-light);
                border: 2px solid var(--border-color);
                border-radius: 10px;
            }

            .btn-outline-secondary:hover {
                background: var(--card-bg);
                border-color: var(--primary-blue);
                color: var(--light-blue);
            }

            /* === Form controls === */
            .form-control {
                background: var(--card-bg);
                border: 1px solid var(--border-color);
                color: var(--text-light);
                border-radius: 10px;
            }

            .form-control:focus {
                background: var(--card-bg);
                border-color: var(--primary-blue);
                color: var(--text-light);
                box-shadow: 0 0 0 0.25rem rgba(13, 110, 253, 0.25);
            }

            /* === Icons === */
            .bi-cloud-fill {
                background: linear-gradient(135deg, var(--primary-blue), var(--light-blue));
                -webkit-background-clip: text;
                -webkit-text-fill-color: transparent;
                background-clip: text;
            }

            /* === Responsive === */
            @media (max-width: 767.98px) {
                main {
                    padding: 1.5rem !important;
                }

                main h3 {
                    font-size: 1.5rem;
                }

                .card-body {
                    padding: 1.25rem;
                }
            }

            /* === Scrollbar === */
            ::-webkit-scrollbar {
                width: 10px;
            }

            ::-webkit-scrollbar-track {
                background: var(--darker-bg);
            }

            ::-webkit-scrollbar-thumb {
                background: linear-gradient(180deg, var(--primary-blue), var(--dark-blue));
                border-radius: 10px;
            }

            ::-webkit-scrollbar-thumb:hover {
                background: var(--light-blue);
            }

            /* === Donut Chart === */
            .donut-chart {
                position: relative;
                width: 200px;
                height: 200px;
                margin: 0 auto;
            }

            .donut-chart svg {
                width: 100%;
                height: 100%;
                transform: rotate(-90deg);
            }

            .donut-chart circle {
                fill: none;
                stroke-width: 25;
            }

            .donut-bg {
                stroke: var(--border-color);
            }

            .donut-progress {
                stroke-linecap: round;
                transition: stroke-dasharray 1s ease-in-out;
            }

            .donut-center {
                position: absolute;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                text-align: center;
            }

            .donut-center .percentage {
                font-size: 2.5rem;
                font-weight: 700;
                color: var(--light-blue);
            }

            .donut-center .label {
                font-size: 0.9rem;
                color: var(--text-muted);
            }

            .legend-item {
                display: flex;
                align-items: center;
                margin-bottom: 0.5rem;
            }

            .legend-color {
                width: 16px;
                height: 16px;
                border-radius: 4px;
                margin-right: 10px;
            }

            .legend-used {
                background: linear-gradient(135deg, var(--primary-blue), var(--light-blue));
            }

            .legend-free {
                background: linear-gradient(135deg, #198754, #20c997);
            }

            /* === Line Chart === */
            .chart-container {
                position: relative;
                height: 300px;
                width: 100%;
            }
        </style>
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
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <h3 class="mb-0 text-primary">
                        <i class="bi bi-hdd-fill me-2"></i>Mon Stockage
                    </h3>
                </div>

                <!-- Carte principale de stockage -->
                <section>
                    <div class="row g-4">
                        <!-- Graphique d'évolution de la taille des fichiers -->
                        <div class="col-12">
                            <div class="card shadow-sm">
                                <div class="card-body">
                                    <h5 class="card-title mb-2">
                                        <i class="bi bi-graph-up text-primary me-2"></i>Évolution du stockage - <%= user.getNom() %>
                                    </h5>
                                    <p class="text-muted small mb-3">
                                        <i class="bi bi-info-circle me-1"></i>
                                        Utilisation actuelle: <strong class="text-primary"><%= stockageFormate %></strong> sur <strong><%= quotaFormate %></strong> (<%= pourcentage %>%)
                                    </p>
                                    
                                    <!-- Sélecteur de période -->
                                    <div class="d-flex gap-2 mb-3">
                                        <button class="btn btn-sm btn-outline-primary active" onclick="updateChart('minute')" id="btnMinute">Minutes</button>
                                        <button class="btn btn-sm btn-outline-primary" onclick="updateChart('hour')" id="btnHour">Heures</button>
                                        <button class="btn btn-sm btn-outline-primary" onclick="updateChart('day')" id="btnDay">Jours</button>
                                    </div>
                                    
                                    <!-- Graphique -->
                                    <div class="chart-container">
                                        <canvas id="storageChart"></canvas>
                                    </div>
                                    
                                    <!-- Statistiques sous le graphique - Données réelles de l'utilisateur -->
                                    <div class="row mt-4 text-center">
                                        <div class="col-4">
                                            <div class="text-muted small">Stockage actuel</div>
                                            <div class="fw-bold text-primary"><%= stockageFormate %></div>
                                        </div>
                                        <div class="col-4">
                                            <div class="text-muted small">Quota total</div>
                                            <div class="fw-bold text-warning"><%= quotaFormate %></div>
                                        </div>
                                        <div class="col-4">
                                            <div class="text-muted small">Espace libre</div>
                                            <div class="fw-bold text-success"><%= stockageRestantFormate %></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Carte avec résumé du stockage -->
                        <div class="col-12 col-lg-8">
                            <div class="card shadow-sm">
                                <div class="card-body">
                                    <h5 class="card-title mb-4">
                                        <i class="bi bi-pie-chart-fill text-primary me-2"></i>Résumé du stockage
                                    </h5>
                                    
                                    <div class="row align-items-center">
                                        <!-- Barre circulaire de progression -->
                                        <div class="col-12 col-md-5 mb-4 mb-md-0">
                                            <div class="donut-chart" id="donutChart">
                                                <svg viewBox="0 0 100 100">
                                                    <circle class="donut-bg" cx="50" cy="50" r="40"></circle>
                                                    <circle class="donut-progress" cx="50" cy="50" r="40" 
                                                            stroke="url(#gradient)" 
                                                            stroke-dasharray="0, 251.2"
                                                            id="progressCircle"></circle>
                                                    <defs>
                                                        <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="0%">
                                                            <stop offset="0%" style="stop-color:#0d6efd"/>
                                                            <stop offset="100%" style="stop-color:#6ea8fe"/>
                                                        </linearGradient>
                                                    </defs>
                                                </svg>
                                                <div class="donut-center">
                                                    <div class="percentage" id="percentageText">0%</div>
                                                    <div class="label">Utilisé</div>
                                                </div>
                                            </div>
                                        </div>
                                        
                                        <!-- Légende et détails -->
                                        <div class="col-12 col-md-7">
                                            <div class="legend-item">
                                                <div class="legend-color legend-used"></div>
                                                <div>
                                                    <div class="fw-bold">Espace utilisé</div>
                                                    <div class="text-primary fs-5 fw-bold"><%= stockageFormate %></div>
                                                </div>
                                            </div>
                                            <div class="legend-item mt-3">
                                                <div class="legend-color legend-free"></div>
                                                <div>
                                                    <div class="fw-bold">Espace disponible</div>
                                                    <div class="text-success fs-5 fw-bold"><%= stockageRestantFormate %></div>
                                                </div>
                                            </div>
                                            <hr class="my-3" style="border-color: var(--border-color);">
                                            <div class="d-flex justify-content-between">
                                                <span class="text-muted">Quota total:</span>
                                                <span class="fw-bold"><%= quotaFormate %></span>
                                            </div>
                                            <div class="d-flex justify-content-between mt-2">
                                                <span class="text-muted">Fichiers stockés:</span>
                                                <span class="fw-bold text-info"><%= nombreFichiers %></span>
                                            </div>
                                        </div>
                                    </div>
                                    
                                    <!-- Barre de progression linéaire -->
                                    <div class="mt-4">
                                        <div class="d-flex justify-content-between mb-2">
                                            <small class="text-muted">Progression</small>
                                            <small class="fw-bold"><%= stockageFormate %> / <%= quotaFormate %></small>
                                        </div>
                                        <div class="progress" style="height: 10px; border-radius: 10px; background-color: var(--border-color);">
                                            <div class="progress-bar <%= progressColor %>" 
                                                 role="progressbar" 
                                                 style="width: <%= pourcentage %>%; border-radius: 10px;" 
                                                 aria-valuenow="<%= pourcentage %>" 
                                                 aria-valuemin="0" 
                                                 aria-valuemax="100">
                                            </div>
                                        </div>
                                    </div>
                                    
                                    <% if (pourcentage > 80) { %>
                                        <div class="alert alert-danger d-flex align-items-center mt-3 mb-0" role="alert">
                                            <i class="bi bi-exclamation-triangle-fill me-2"></i>
                                            <div>Attention ! Votre espace de stockage est presque plein.</div>
                                        </div>
                                    <% } else if (pourcentage > 60) { %>
                                        <div class="alert alert-warning d-flex align-items-center mt-3 mb-0" role="alert">
                                            <i class="bi bi-exclamation-circle-fill me-2"></i>
                                            <div>Votre espace de stockage commence à se remplir.</div>
                                        </div>
                                    <% } %>
                                </div>
                            </div>
                        </div>

                        <!-- Statistiques rapides -->
                        <div class="col-12 col-lg-4">
                            <div class="row g-3">
                                <!-- Espace utilisé -->
                                <div class="col-12">
                                    <div class="card shadow-sm h-100">
                                        <div class="card-body text-center">
                                            <i class="bi bi-database-fill text-primary" style="font-size: 2.5rem;"></i>
                                            <h3 class="mt-2 mb-1 text-primary"><%= stockageFormate %></h3>
                                            <p class="text-muted mb-0">Espace utilisé</p>
                                        </div>
                                    </div>
                                </div>
                                
                                <!-- Espace restant -->
                                <div class="col-12">
                                    <div class="card shadow-sm h-100">
                                        <div class="card-body text-center">
                                            <i class="bi bi-hdd-stack-fill text-success" style="font-size: 2.5rem;"></i>
                                            <h3 class="mt-2 mb-1 text-success"><%= stockageRestantFormate %></h3>
                                            <p class="text-muted mb-0">Espace restant</p>
                                        </div>
                                    </div>
                                </div>
                                
                                <!-- Nombre de fichiers -->
                                <div class="col-12">
                                    <div class="card shadow-sm h-100">
                                        <div class="card-body text-center">
                                            <i class="bi bi-file-earmark-fill text-info" style="font-size: 2.5rem;"></i>
                                            <h3 class="mt-2 mb-1 text-info"><%= nombreFichiers %></h3>
                                            <p class="text-muted mb-0">Fichiers stockes</p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Détails du quota -->
                        <div class="col-12">
                            <div class="card shadow-sm">
                                <div class="card-body">
                                    <h5 class="card-title mb-4">
                                        <i class="bi bi-info-circle-fill text-primary me-2"></i>Details du quota
                                    </h5>
                                    <div class="table-responsive">
                                        <table class="table table-dark table-hover mb-0">
                                            <thead>
                                                <tr>
                                                    <th><i class="bi bi-tag-fill me-2"></i>Description</th>
                                                    <th class="text-end"><i class="bi bi-calculator me-2"></i>Valeur</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <tr>
                                                    <td>Quota total alloue</td>
                                                    <td class="text-end fw-bold text-primary"><%= quotaFormate %></td>
                                                </tr>
                                                <tr>
                                                    <td>Espace utilise</td>
                                                    <td class="text-end fw-bold text-warning"><%= stockageFormate %></td>
                                                </tr>
                                                <tr>
                                                    <td>Espace disponible</td>
                                                    <td class="text-end fw-bold text-success"><%= stockageRestantFormate %></td>
                                                </tr>
                                                <tr>
                                                    <td>Pourcentage utilisé</td>
                                                    <td class="text-end fw-bold"><%= pourcentage %>%</td>
                                                </tr>
                                                <tr>
                                                    <td>Nombre de fichiers</td>
                                                    <td class="text-end fw-bold text-info"><%= nombreFichiers %></td>
                                                </tr>
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>
                        </div>

                    </div>
                </section>
            </main>
        </div>
    </div>

    <script src="assets/bootstrap/js/bootstrap.bundle.min.js"></script>
    <script>
        // Données réelles de l'utilisateur connecté
        const currentStorage = <%= stockageUtilise %>;
        const totalQuota = <%= quota %>;
        const totalFiles = <%= nombreFichiers %>;
        const usagePercentage = <%= pourcentage %>;
        
        // Générer des données de simulation pour le graphique
        // L'historique est simulé mais le point final est TOUJOURS la vraie valeur de l'utilisateur
        function generateData(period) {
            const data = [];
            const now = new Date();
            let points, interval;
            
            switch(period) {
                case 'minute':
                    points = 60;
                    interval = 1000; // 1 seconde
                    break;
                case 'hour':
                    points = 60;
                    interval = 60 * 1000; // 1 minute
                    break;
                case 'day':
                    points = 24;
                    interval = 60 * 60 * 1000; // 1 heure
                    break;
                default:
                    points = 60;
                    interval = 1000;
            }
            
            // Utiliser les vraies données de l'utilisateur
            // Le point final est TOUJOURS la valeur réelle actuelle
            let finalValue = currentStorage > 0 ? currentStorage : 0;
            
            // Si l'utilisateur n'a pas de fichiers, afficher une ligne plate à 0
            if (finalValue === 0 || totalFiles === 0) {
                for (let i = points - 1; i >= 0; i--) {
                    const time = new Date(now.getTime() - (i * interval));
                    data.push({
                        x: time,
                        y: 0
                    });
                }
                return data;
            }
            
            // Simuler une évolution qui ARRIVE à la vraie valeur actuelle
            // On part d'une valeur plus basse et on monte progressivement
            const startValue = finalValue * 0.3; // Commencer à 30% de la valeur actuelle
            const growthPerPoint = (finalValue - startValue) / points;
            
            for (let i = points - 1; i >= 0; i--) {
                const time = new Date(now.getTime() - (i * interval));
                
                // Progression vers la valeur finale avec petites variations
                const progress = points - 1 - i;
                const baseAtPoint = startValue + (growthPerPoint * progress);
                
                // Légère variation pour rendre la courbe naturelle (±5%)
                const variation = (Math.sin(progress * 0.5) * 0.05 * baseAtPoint);
                
                let value;
                if (i === 0) {
                    // Le dernier point est EXACTEMENT la valeur réelle
                    value = finalValue;
                } else {
                    value = Math.max(0, baseAtPoint + variation);
                }
                
                data.push({
                    x: time,
                    y: Math.round(value)
                });
            }
            
            return data;
        }
        
        // Formater les octets
        function formatBytes(bytes) {
            if (bytes < 1024) return bytes + ' o';
            if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' Ko';
            if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' Mo';
            return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' Go';
        }
        
        // Configuration du graphique
        const ctx = document.getElementById('storageChart').getContext('2d');
        let storageChart;
        
        function createChart(period) {
            const data = generateData(period);
            
            // Format de l'axe X selon la période
            let timeUnit, displayFormat;
            switch(period) {
                case 'minute':
                    timeUnit = 'second';
                    displayFormat = 'HH:mm:ss';
                    break;
                case 'hour':
                    timeUnit = 'minute';
                    displayFormat = 'HH:mm';
                    break;
                case 'day':
                    timeUnit = 'hour';
                    displayFormat = 'HH:00';
                    break;
            }
            
            if (storageChart) {
                storageChart.destroy();
            }
            
            storageChart = new Chart(ctx, {
                type: 'line',
                data: {
                    datasets: [{
                        label: 'Taille du stockage',
                        data: data,
                        borderColor: '#0d6efd',
                        backgroundColor: function(context) {
                            const chart = context.chart;
                            const {ctx, chartArea} = chart;
                            if (!chartArea) return 'rgba(13, 110, 253, 0.1)';
                            const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
                            gradient.addColorStop(0, 'rgba(13, 110, 253, 0.4)');
                            gradient.addColorStop(0.5, 'rgba(13, 110, 253, 0.2)');
                            gradient.addColorStop(1, 'rgba(13, 110, 253, 0.0)');
                            return gradient;
                        },
                        borderWidth: 3,
                        fill: true,
                        tension: 0.5,
                        cubicInterpolationMode: 'monotone',
                        pointRadius: 0,
                        pointHoverRadius: 8,
                        pointBackgroundColor: '#0d6efd',
                        pointBorderColor: '#fff',
                        pointBorderWidth: 3,
                        pointHoverBackgroundColor: '#fff',
                        pointHoverBorderColor: '#0d6efd',
                        pointHoverBorderWidth: 3
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    interaction: {
                        intersect: false,
                        mode: 'index'
                    },
                    elements: {
                        line: {
                            tension: 0.5,
                            borderCapStyle: 'round',
                            borderJoinStyle: 'round'
                        }
                    },
                    plugins: {
                        legend: {
                            display: false
                        },
                        tooltip: {
                            backgroundColor: 'rgba(26, 26, 26, 0.95)',
                            titleColor: '#fff',
                            titleFont: {
                                size: 14,
                                weight: 'bold'
                            },
                            bodyColor: '#adb5bd',
                            bodyFont: {
                                size: 13
                            },
                            borderColor: '#0d6efd',
                            borderWidth: 2,
                            padding: 14,
                            cornerRadius: 10,
                            displayColors: false,
                            callbacks: {
                                title: function(context) {
                                    const date = new Date(context[0].parsed.x);
                                    return date.toLocaleTimeString('fr-FR');
                                },
                                label: function(context) {
                                    return 'Taille: ' + formatBytes(context.parsed.y);
                                }
                            }
                        }
                    },
                    scales: {
                        x: {
                            type: 'time',
                            time: {
                                unit: timeUnit,
                                displayFormats: {
                                    second: 'HH:mm:ss',
                                    minute: 'HH:mm',
                                    hour: 'HH:00'
                                }
                            },
                            grid: {
                                color: 'rgba(58, 58, 58, 0.5)',
                                drawBorder: false
                            },
                            ticks: {
                                color: '#adb5bd',
                                maxTicksLimit: 10
                            },
                            title: {
                                display: true,
                                text: 'Temps',
                                color: '#6ea8fe'
                            }
                        },
                        y: {
                            beginAtZero: false,
                            grid: {
                                color: 'rgba(58, 58, 58, 0.5)',
                                drawBorder: false
                            },
                            ticks: {
                                color: '#adb5bd',
                                callback: function(value) {
                                    return formatBytes(value);
                                }
                            },
                            title: {
                                display: true,
                                text: 'Taille (octets)',
                                color: '#6ea8fe'
                            }
                        }
                    }
                }
            });
        }
        
        // Fonction pour changer de période
        function updateChart(period) {
            // Mettre à jour les boutons actifs
            document.querySelectorAll('[onclick^="updateChart"]').forEach(btn => {
                btn.classList.remove('active');
            });
            document.getElementById('btn' + period.charAt(0).toUpperCase() + period.slice(1)).classList.add('active');
            
            createChart(period);
        }
        
        // Animation du diagramme circulaire
        document.addEventListener('DOMContentLoaded', function() {
            const percentage = <%= pourcentage %>;
            const circumference = 2 * Math.PI * 40;
            const progressCircle = document.getElementById('progressCircle');
            const percentageText = document.getElementById('percentageText');
            
            const dashArray = (percentage / 100) * circumference;
            
            setTimeout(() => {
                progressCircle.style.strokeDasharray = dashArray + ', ' + circumference;
            }, 100);
            
            let currentPercentage = 0;
            const duration = 1000;
            const steps = 60;
            const increment = percentage / steps;
            const stepDuration = duration / steps;
            
            const animatePercentage = setInterval(() => {
                currentPercentage += increment;
                if (currentPercentage >= percentage) {
                    currentPercentage = percentage;
                    clearInterval(animatePercentage);
                }
                percentageText.textContent = Math.round(currentPercentage) + '%';
            }, stepDuration);
            
            // Initialiser le graphique linéaire
            createChart('minute');
        });
    </script>
</body>
</html>
