package model;

public class User {
    String nom;
    String password;
    boolean admin;

    // getters
    public String getNom() {
        return nom;
    }
    public String getPassword() {
        return password;
    }
    public boolean isAdmin() {
        return admin;
    }

    // setters
    public void setNom(String nom) {
        this.nom = nom;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

}
