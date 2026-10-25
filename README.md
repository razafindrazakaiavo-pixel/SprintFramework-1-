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
