<%@ page import="model.User" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/pages/home.jsp");
        return;
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
            <!-- Sidebar for md+ -->
            <aside class="col-12 col-md-3 col-lg-2 sidebar d-none d-md-block p-3">
                <div class="user-info mb-4">
                    <div class="fw-bold">Bonjour, <%= user.getNom() %></div>
                    <div class="user-email"><%= user.getNom() %>@example.com</div>
                </div>
                <nav class="nav flex-column">
                    <a class="nav-link active" href="#"><i class="bi bi-house-door-fill"></i>Accueil</a>
                    <a class="nav-link" href="#"><i class="bi bi-folder-fill"></i>Mes fichiers</a>
                    <a class="nav-link" href="#"><i class="bi bi-share-fill"></i>Partages</a>
                    <a class="nav-link" href="#"><i class="bi bi-gear-fill"></i>Parametres</a>
                </nav>
            </aside>

            <!-- Offcanvas sidebar for small screens -->
            <div class="offcanvas offcanvas-start" tabindex="-1" id="offcanvasSidebar">
                <div class="offcanvas-header">
                    <h5 class="offcanvas-title">SmartDrive</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="offcanvas"></button>
                </div>
                <div class="offcanvas-body">
                    <nav class="nav flex-column">
                        <a class="nav-link" href="#"><i class="bi bi-house-door-fill me-2"></i>Accueil</a>
                        <a class="nav-link" href="#"><i class="bi bi-folder-fill me-2"></i>Mes fichiers</a>
                        <a class="nav-link" href="#"><i class="bi bi-share-fill me-2"></i>Partages</a>
                        <a class="nav-link" href="#"><i class="bi bi-gear-fill me-2"></i>Parametres</a>
                    </nav>
                </div>
            </div>

            <!-- Main content -->
            <main class="col-12 col-md-9 col-lg-10 p-4">
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <h3 class="mb-0 text-primary">Mes fichiers</h3>
                    <div class="d-flex gap-2 align-items-center">
                        <form action="upload" method="post" enctype="multipart/form-data" class="m-0">
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
                        <div class="col-12 col-sm-6 col-lg-4">
                            <div class="card shadow-sm">
                                <div class="card-body">
                                    <h5 class="card-title text-primary">Documents personnels</h5>
                                    <p class="card-text text-muted">3 fichiers — modifie le 22 Janv 2026</p>
                                    <a href="#" class="btn btn-outline-primary btn-sm">Ouvrir</a>
                                </div>
                            </div>
                        </div>

                        <div class="col-12 col-sm-6 col-lg-4">
                            <div class="card shadow-sm">
                                <div class="card-body">
                                    <h5 class="card-title text-primary">Photos</h5>
                                    <p class="card-text text-muted">12 fichiers — modifie le 10 Janv 2026</p>
                                    <a href="#" class="btn btn-outline-primary btn-sm">Ouvrir</a>
                                </div>
                            </div>
                        </div>

                        <div class="col-12 col-sm-6 col-lg-4">
                            <div class="card shadow-sm">
                                <div class="card-body">
                                    <h5 class="card-title text-primary">Projets</h5>
                                    <p class="card-text text-muted">5 dossiers — modifie le 02 Janv 2026</p>
                                    <a href="#" class="btn btn-outline-primary btn-sm">Ouvrir</a>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>
            </main>
        </div>
    </div>

    <script src="assets/bootstrap/js/bootstrap.bundle.min.js"></script>
</body>
</html>
