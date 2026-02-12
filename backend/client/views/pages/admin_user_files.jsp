<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8" />
  <title>Fichiers utilisateur</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/bootstrap/css/bootstrap.min.css" />
</head>
<body>
<jsp:include page="sidebar.jsp" />

<div class="container mt-4">
  <h3 class="mb-3">Fichiers de <%= request.getAttribute("owner") == null ? "" : (String) request.getAttribute("owner") %></h3>

  <% String error = (String) request.getAttribute("error"); %>
  <% if (error != null && !error.trim().isEmpty()) { %>
    <div class="alert alert-danger"><%= error %></div>
  <% } %>

  <% String message = (String) request.getAttribute("message"); %>
  <% if (message != null && !message.trim().isEmpty()) { %>
    <div class="alert alert-info"><%= message %></div>
  <% } %>

  <%
    String owner = (String) request.getAttribute("owner");
    List<String> files = (List<String>) request.getAttribute("files");
    if (files == null) files = (List<String>) request.getAttribute("admin_user_files");
  %>

  <% if (owner == null || owner.trim().isEmpty()) { %>
    <div class="alert alert-warning">Utilisateur non spécifié.</div>
  <% } else if (files == null || files.isEmpty()) { %>
    <div class="alert alert-warning">Aucun fichier trouvé.</div>
  <% } else { %>
    <table class="table table-striped align-middle">
      <thead>
      <tr>
        <th>Nom</th>
        <th style="width: 220px;">Actions</th>
      </tr>
      </thead>
      <tbody>
      <% for (String f : files) {
           if (f == null) continue;
           String fileName = f;
           int semi = f.indexOf(';');
           if (semi >= 0) fileName = f.substring(0, semi);
      %>
        <tr>
          <td><%= fileName %></td>
          <td>
            <a class="btn btn-sm btn-primary" href="${pageContext.request.contextPath}/admin/user_view?owner=<%= java.net.URLEncoder.encode(owner, "UTF-8") %>&file=<%= java.net.URLEncoder.encode(fileName, "UTF-8") %>">Voir</a>
            <a class="btn btn-sm btn-secondary" href="${pageContext.request.contextPath}/admin/user_download?owner=<%= java.net.URLEncoder.encode(owner, "UTF-8") %>&file=<%= java.net.URLEncoder.encode(fileName, "UTF-8") %>">Télécharger</a>
          </td>
        </tr>
      <% } %>
      </tbody>
    </table>
  <% } %>

  <a class="btn btn-link" href="${pageContext.request.contextPath}/admin/accueil">Retour</a>
</div>

<script src="${pageContext.request.contextPath}/assets/bootstrap/js/bootstrap.bundle.min.js"></script>
</body>
</html>
