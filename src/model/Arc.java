package model;

public class Arc {
    private Tache origine;
    private Tache destination;

    public Arc(Tache origine, Tache destination) {
        this.origine = origine;
        this.destination = destination;
    }

    public Tache getOrigine() { 
    	return origine; 
    }
    
    public Tache getDestination() { 
    	return destination; 
    }

    public int getDuree() {
        return origine.getDuree();
    }
}
