<%@ page import="model.User" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/pages/home.jsp");
        return;
    }
    List<String> list_file = (ArrayList<String>) request.getAttribute("list_file");
    
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
            <!-- Import du sidebar réutilisable -->
            <jsp:include page="sidebar.jsp" />

            <!-- Main content -->
            <main class="col-12 col-md-9 col-lg-10 p-4">
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <h3 class="mb-0 text-primary">Mes fichiers</h3>
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
                        <% if (list_file != null && !list_file.isEmpty()) { 
                            for (String file : list_file) { 
                                String fileEncoded = java.net.URLEncoder.encode(file, "UTF-8");
                                // Déterminer l'icône selon l'extension
                                String icon = "bi-file-earmark";
                                String iconColor = "text-secondary";
                                if (file.endsWith(".pdf")) {
                                    icon = "bi-file-earmark-pdf-fill";
                                    iconColor = "text-danger";
                                } else if (file.endsWith(".jpg") || file.endsWith(".png") || file.endsWith(".gif") || file.endsWith(".jpeg")) {
                                    icon = "bi-file-earmark-image-fill";
                                    iconColor = "text-success";
                                } else if (file.endsWith(".doc") || file.endsWith(".docx")) {
                                    icon = "bi-file-earmark-word-fill";
                                    iconColor = "text-primary";
                                } else if (file.endsWith(".xls") || file.endsWith(".xlsx")) {
                                    icon = "bi-file-earmark-excel-fill";
                                    iconColor = "text-success";
                                } else if (file.endsWith(".txt")) {
                                    icon = "bi-file-earmark-text-fill";
                                    iconColor = "text-info";
                                } else if (file.endsWith(".zip") || file.endsWith(".rar")) {
                                    icon = "bi-file-earmark-zip-fill";
                                    iconColor = "text-warning";
                                } else if (file.endsWith(".mp3") || file.endsWith(".wav")) {
                                    icon = "bi-file-earmark-music-fill";
                                    iconColor = "text-purple";
                                } else if (file.endsWith(".mp4") || file.endsWith(".avi") || file.endsWith(".mkv")) {
                                    icon = "bi-file-earmark-play-fill";
                                    iconColor = "text-danger";
                                }
                            %>
                                <div class="col-12 col-sm-6 col-lg-4 col-xl-3">
                                    <div class="card shadow-sm h-100">
                                        <div class="card-body d-flex flex-column">
                                            <!-- Icône du fichier -->
                                            <div class="text-center mb-3">
                                                <i class="bi <%= icon %> <%= iconColor %>" style="font-size: 3rem;"></i>
                                            </div>
                                            <!-- Nom du fichier -->
                                            <h6 class="card-title text-truncate mb-3" title="<%= file %>">
                                                <%= file %>
                                            </h6>
                                            <!-- Boutons d'action -->
                                            <div class="mt-auto d-flex gap-2">
                                                <a href="<%= request.getContextPath() %>/show/view?file=<%= fileEncoded %>" 
                                                   class="btn btn-primary btn-sm flex-grow-1">
                                                    <i class="bi bi-eye me-1"></i>Voir
                                                </a>
                                                <a href="<%= request.getContextPath() %>/show/download?file=<%= fileEncoded %>" 
                                                   class="btn btn-outline-primary btn-sm">
                                                    <i class="bi bi-download"></i>
                                                </a>
                                                <button type="button" class="btn btn-outline-danger btn-sm" 
                                                        data-bs-toggle="modal" data-bs-target="#deleteModal" 
                                                        data-filename="<%= file %>">
                                                    <i class="bi bi-trash"></i>
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            <% } 
                        } else { %>
                            <!-- Message si aucun fichier -->
                            <div class="col-12">
                                <div class="text-center py-5">
                                    <i class="bi bi-folder-x text-muted" style="font-size: 4rem;"></i>
                                    <h5 class="mt-3 text-muted">Aucun fichier trouvé</h5>
                                    <p class="text-muted">Commencez par uploader un fichier</p>
                                </div>
                            </div>
                        <% } %>
                    </div>
                </section>

                <!-- Modal de confirmation de suppression -->
                <div class="modal fade" id="deleteModal" tabindex="-1" aria-labelledby="deleteModalLabel" aria-hidden="true">
                    <div class="modal-dialog modal-dialog-centered">
                        <div class="modal-content bg-dark text-light">
                            <div class="modal-header border-secondary">
                                <h5 class="modal-title" id="deleteModalLabel">
                                    <i class="bi bi-exclamation-triangle text-warning me-2"></i>Confirmer la suppression
                                </h5>
                                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fermer"></button>
                            </div>
                            <div class="modal-body">
                                <p>Êtes-vous sûr de vouloir supprimer le fichier <strong id="fileToDelete"></strong> ?</p>
                                <p class="text-muted small">Cette action est irréversible.</p>
                            </div>
                            <div class="modal-footer border-secondary">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annuler</button>
                                <a href="#" id="confirmDeleteBtn" class="btn btn-danger">
                                    <i class="bi bi-trash me-1"></i>Supprimer
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    </div>

    <script src="assets/bootstrap/js/bootstrap.bundle.min.js"></script>
    <script>
        // Gestion du modal de suppression
        const deleteModal = document.getElementById('deleteModal');
        if (deleteModal) {
            deleteModal.addEventListener('show.bs.modal', function (event) {
                const button = event.relatedTarget;
                const filename = button.getAttribute('data-filename');
                
                document.getElementById('fileToDelete').textContent = filename;
                document.getElementById('confirmDeleteBtn').href = 
                    '<%= request.getContextPath() %>/show/delete?file=' + encodeURIComponent(filename);
            });
        }
    </script>
</body>
</html>
