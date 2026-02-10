<%
    String error = (String) request.getAttribute("error");
%>
<script>
    if ("<%= error %>" !== "null") {
        alert("<%= error %>");
    }
</script>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="../assets/bootstrap/css/bootstrap.min.css">
    <script src="../assets/bootstrap/js/bootstrap.bundle.min.js"></script>
    <title>SmartDrive</title>
</head>
<body class="bg-dark">

    <div class="container-fluid" style="padding: 10%; min-height: 100vh; display: flex; align-items: center; justify-content: center;">
        <div class="container shadow p-5 rounded-5 bg-dark border border-primary" style="width: 50%; height: auto;">

            <%-- // ================== Header ================= // --%>
            <div class="text-center">
                <h1 class="mb-4 text-white">Welcome to <span class="text-primary fw-bold">SmartDrive</span></h1>
            </div>

            <%-- // ================== Login Form ================= // --%>
            <form action="/SmartDrive/user" method="post">
                <div class="mb-4">
                    <label class="form-label text-white" for="nom"> Nom :</label>
                    <input class="form-control border-primary bg-dark text-white" type="text" name="nom" id="nom" placeholder="Votre nom ..." required>
                </div>
                <div class="mb-4">
                    <label class="form-label text-white" for="mdp"> Mot de passe :</label>
                    <input class="form-control border-primary bg-dark text-white" type="password" name="password" id="mdp" placeholder="Mot de Passe" required>
                </div>
                <div class="text-center">
                    <input class="btn btn-primary fw-bold" type="submit" value="Connexion">
                </div>
            </form> 

        </div>
    </div>

</body>
</html>