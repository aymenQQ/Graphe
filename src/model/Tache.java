package model;

public class Tache {
    private String nom;
    private int duree;
    private int x, y; // Position sur l'écran

    public Tache(String nom, int duree, int x, int y) {
        this.nom = nom;
        this.duree = duree;
        this.x = x;
        this.y = y;
    }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }

    public int getX() { return x; }
    public int getY() { return y; }
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
