# Exemple Sprint 7 / 7b - Binding + Formulaire

## 1. Framework (ce repo)

Sprint 7 (`@Param`) et 7b (`@ParamObject` + validation) implémentés :

- `src/annotation/Url.java` : `@Url(url="/bind-simple")`, compatible `@Get` / `@Post` marqueurs
  - `@Url` seul -> GET par défaut
  - `@Post @Url(url="/bind-objet")` -> POST
- `src/annotation/Get.java`, `Post.java` : marqueurs HTTP
- `src/annotation/Param.java` : `@Param(name="nom")`
- `src/annotation/ParamObject.java` : `@ParamObject(name="etudiant")`
- `src/annotation/Required.java`, `Numeric.java`, `Range.java`, `DateFormat.java` : validation 7b
- `src/ParamBinder.java` : binding + conversion + validation + erreur `ETU002391`
- `src/BindingException.java`
- `FrontControllerListener` : scan `@Url`

Conversions supportées (`@Param` et champs `@ParamObject`) :
`String, int/Integer, long, double, float, short, byte, boolean/Boolean (true/false/1/0/on/off/oui/non), char, Enum, java.sql.Date (yyyy-MM-dd), java.util.Date, Timestamp, LocalDate/LocalDateTime/LocalTime`

`@RequestParam` (Spring) = NON supporté -> `400 ETU002391 : @RequestParam non supporte... utilisez @Param`.

## 2. Code test à copier dans ton projet web

```
exemple-sprint7/
  model/Etudiant.java
  controller/BindingController.java
  webapp/formulaire-bind.html   <- LE FORMULAIRE (Sprint 7 + 7b)
  webapp/formulaire-bind.jsp    <- variante JSP
```

- `base-package` (web.xml) doit contenir `controller` (ou mets `BindingController` dans ton package scanné, ex `iavo.main`).
- Copie `formulaire-bind.html` à la racine web (ne pas mettre sous WEB-INF si tu veux y accéder direct).
- `Framework.jar` rebuildé avec `deploy.bat` / `deploy.sh`.

## 3. Tests

- Sprint 7 (GET) :
  `/bind-simple?nom=Loyard&age=25&moyenne=15.5&boursier=true&inscription=2026-09-30`
  ou via formulaire bloc 1.
  Attendu : `nom=Loyard | age=25 | moyenne=15.5 | boursier=true | inscription=2026-09-30`

- Sprint 7b (POST) :
  Formulaire bloc 2 avec champs `etudiant.nom`, `etudiant.age`, `etudiant.dateNaissance`, `etudiant.email`.
  Attendu : `nom=... | age=... | dateNaissance=... | email=...`
  Validation : vide `@Required`, age hors 0-150 `@Range`, date mauvais format `@DateFormat` -> `400 Echec validation...`

- Erreur volontaire :
  Remplace `@Param` par `@RequestParam` (Spring) sur `/bind-requestparam` -> `400 ETU002391`.
