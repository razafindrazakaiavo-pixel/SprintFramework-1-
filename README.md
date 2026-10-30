# SpringMVC

## Sessions HTTP

Un contrôleur peut recevoir directement `HttpSession` pour lire, enregistrer ou
supprimer des attributs. Une session est créée à la première utilisation de
`request.getSession()` :

```java
@GetMapping("/connexion")
public String connexion(HttpSession session) {
    session.setAttribute("utilisateur", "Amina");
    return "Connectée";
}
```

Pour injecter un attribut de session comme argument, utilisez
`@SessionAttribute`. Le nom peut être donné avec `name` ou `value` ; sans nom,
le framework utilise le nom du paramètre Java (compilez avec `-parameters`).
L'attribut est obligatoire par défaut. `required = false` permet son absence.
Les valeurs stockées sous forme de `String` sont converties vers le type du
paramètre :

```java
@GetMapping("/profil")
public String profil(@SessionAttribute(name = "utilisateur") String utilisateur) {
    return "Bonjour " + utilisateur;
}

@GetMapping("/deconnexion")
public String deconnexion(HttpSession session) {
    session.invalidate();
    return "Déconnectée";
}
```

## Sprint 10 : authentification et rôles

Le framework peut protéger une classe ou une méthode de contrôleur avec
`@Authenticated` (utilisateur connecté) et `@RolesAllowed` (au moins un des
rôles indiqués). Il s'appuie sur l'authentification du conteneur Servlet :
`request.getUserPrincipal()` et `request.isUserInRole()`. Les utilisateurs,
leurs mots de passe et l'association aux rôles doivent donc être configurés
dans le conteneur (par exemple Tomcat), et non dans le framework.

```java
@Controller
@RolesAllowed({"ADMIN"}) // toutes les routes de ce contrôleur
public class AdminController {
    @GetMapping("/admin")
    public String index() {
        return "admin";
    }

    @Authenticated // ajoute explicitement l'exigence d'authentification
    @GetMapping("/admin/profil")
    public String profil() {
        return "profil";
    }
}
```

Une annotation `@RolesAllowed` sur une méthode remplace les rôles déclarés
sur sa classe. `@Authenticated` sur une méthode ne retire pas les rôles de sa
classe. L'accès refusé renvoie HTTP 401 si aucun utilisateur n'est authentifié,
ou HTTP 403 si l'utilisateur ne possède aucun des rôles requis.
