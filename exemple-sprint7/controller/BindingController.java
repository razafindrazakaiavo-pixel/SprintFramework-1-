package controller;

import model.Etudiant;
import src.annotation.Controller;
import src.annotation.Param;
import src.annotation.ParamObject;
import src.annotation.Post;
import src.annotation.Url;

@Controller
public class BindingController {

    // Test @Param : binding + conversion String -> int/double/boolean/Date
    // Ex : /bind-simple?nom=Loyard&age=25&moyenne=15.5&boursier=true&inscription=2026-09-30
    @Url(url = "/bind-simple")
    public String simple(
            @Param(name = "nom") String nom,
            @Param(name = "age") int age,
            @Param(name = "moyenne") double moyenne,
            @Param(name = "boursier") boolean boursier,
            @Param(name = "inscription") java.sql.Date inscription) {

        return "nom=" + nom
                + " | age=" + age
                + " | moyenne=" + moyenne
                + " | boursier=" + boursier
                + " | inscription=" + inscription;
    }

    // Test @ParamObject : binding via etudiant.nom, etudiant.age, ...
    // + validation (@Required, @Numeric, @Range, @DateFormat)
    @Post
    @Url(url = "/bind-objet")
    public String objet(@ParamObject(name = "etudiant") Etudiant etudiant) {
        return "nom=" + etudiant.getNom()
                + " | age=" + etudiant.getAge()
                + " | dateNaissance=" + etudiant.getDateNaissance()
                + " | email=" + etudiant.getEmail();
    }

    // Test @RequestParam : NON supporte par le framework (attendu : erreur ETU002391)
    // Si vous utilisez org.springframework.web.bind.annotation.RequestParam,
    // le framework leve : ETU002391 : @RequestParam non supporte, utilisez @Param.
    // Exemple (ne pas decommenter sans dependance Spring) :
    // @Url(url = "/bind-requestparam")
    // public String requestParam(@org.springframework.web.bind.annotation.RequestParam("nom") String nom) {
    //     return "nom=" + nom;
    // }
    @Url(url = "/bind-requestparam")
    public String requestParamDemo(@Param(name = "nom") String nom) {
        // Route de comparaison : meme URL en @Param (supporte).
        // Pour tester ETU002391, remplacez @Param par @RequestParam (Spring) -> erreur 400 ETU002391.
        return "nom=" + nom + " (comparaison : utilisez @RequestParam ici pour voir ETU002391)";
    }
}
