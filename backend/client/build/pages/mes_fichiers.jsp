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

                <% String message = (String) request.getAttribute("message");
                   if (message != null) { %>
                    <div class="alert alert-info"><%= message %></div>
                <% } %>

                <!-- Recherche -->
                <form class="row g-2 align-items-end mb-4" method="get" action="<%= request.getContextPath() %>/show/mes_fichiers">
                    <div class="col-12 col-md-4">
                        <label class="form-label text-muted">Nom</label>
                        <input class="form-control" type="text" name="q" value="<%= request.getParameter("q") != null ? request.getParameter("q") : "" %>" placeholder="Rechercher...">
                    </div>
                    <div class="col-12 col-md-3">
                        <label class="form-label text-muted">Type</label>
                        <select class="form-select" name="type">
                            <option value="all">Tous</option>
                            <option value="pdf">PDF</option>
                            <option value="image">Images</option>
                            <option value="video">Vidéos</option>
                            <option value="audio">Audio</option>
                            <option value="doc">Documents</option>
                            <option value="sheet">Tableurs</option>
                            <option value="txt">Texte</option>
                            <option value="zip">Archives</option>
                        </select>
                    </div>
                    <div class="col-6 col-md-2">
                        <label class="form-label text-muted">Taille min (octets)</label>
                        <input class="form-control" type="number" name="min" min="0" value="<%= request.getParameter("min") != null ? request.getParameter("min") : "" %>">
                    </div>
                    <div class="col-6 col-md-2">
                        <label class="form-label text-muted">Taille max (octets)</label>
                        <input class="form-control" type="number" name="max" min="0" value="<%= request.getParameter("max") != null ? request.getParameter("max") : "" %>">
                    </div>
                    <div class="col-12 col-md-1 d-grid">
                        <button class="btn btn-outline-primary" type="submit"><i class="bi bi-search"></i></button>
                    </div>
                </form>

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
                                    iconColor = "text-info";
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
                                                <a href="<%= request.getContextPath() %>/show/versions?file=<%= fileEncoded %>" 
                                                   class="btn btn-outline-secondary btn-sm" title="Versions">
                                                    <i class="bi bi-clock-history"></i>
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
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="deleteModalLabel">
                                    <i class="bi bi-exclamation-triangle text-warning me-2"></i>Confirmer la suppression
                                </h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fermer"></button>
                            </div>
                            <div class="modal-body">
                                <p>Êtes-vous sûr de vouloir supprimer le fichier <strong id="fileToDelete"></strong> ?</p>
                                <p class="text-muted small">Le fichier sera déplacé dans la corbeille (restauration possible).</p>
                            </div>
                            <div class="modal-footer">
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

    <script src="<%= request.getContextPath() %>/assets/bootstrap/js/bootstrap.bundle.min.js"></script>
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
