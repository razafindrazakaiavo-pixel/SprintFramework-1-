<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Formulaire Sprint 7 / 7b</title>
</head>
<body>
<h1>Tests Binding - Sprint 7 et 7b</h1>

<h2>Sprint 7 : /bind-simple (GET, @Param)</h2>
<form action="${pageContext.request.contextPath}/bind-simple" method="GET">
    Nom : <input type="text" name="nom" value="Loyard"><br>
    Age : <input type="number" name="age" value="25"><br>
    Moyenne : <input type="number" step="0.01" name="moyenne" value="15.5"><br>
    Boursier : <input type="checkbox" name="boursier" value="true" checked><br>
    Inscription : <input type="date" name="inscription" value="2026-09-30"><br>
    <button type="submit">Tester Sprint 7</button>
</form>

<hr>

<h2>Sprint 7b : /bind-objet (POST, @ParamObject)</h2>
<form action="${pageContext.request.contextPath}/bind-objet" method="POST">
    Nom : <input type="text" name="etudiant.nom" value="Loyard"><br>
    Age : <input type="number" name="etudiant.age" value="25"><br>
    Date naissance : <input type="date" name="etudiant.dateNaissance" value="2000-05-10"><br>
    Email : <input type="email" name="etudiant.email" value="loyard@test.mg"><br>
    <button type="submit">Tester Sprint 7b</button>
</form>

</body>
</html>
