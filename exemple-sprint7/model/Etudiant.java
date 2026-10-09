package model;

import src.annotation.DateFormat;
import src.annotation.Numeric;
import src.annotation.Range;
import src.annotation.Required;

// Sprint 7b : objet bindé via @ParamObject(name = "etudiant")
// Champs attendus dans le formulaire : etudiant.nom, etudiant.age, ...
public class Etudiant {

    @Required(message = "Nom obligatoire")
    private String nom;

    @Numeric(message = "Age doit etre numerique")
    @Range(min = 0, max = 150, message = "Age entre 0 et 150")
    private int age;

    @DateFormat(pattern = "yyyy-MM-dd", message = "Date naissance format yyyy-MM-dd attendu")
    private java.sql.Date dateNaissance;

    @Required(message = "Email obligatoire")
    private String email;

    public Etudiant() {
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public java.sql.Date getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(java.sql.Date dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
