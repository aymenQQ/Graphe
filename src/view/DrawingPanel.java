package view;

import model.Arc;
import model.Tache;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DrawingPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /* ----------------- données du graphe ----------------- */
    private final List<Tache> taches = new ArrayList<>();
    private final List<Arc>  arcs   = new ArrayList<>();

    /* ----------------- état d’interaction ---------------- */
    private Tache  tacheSelectionne  = null;   // première tâche cliquée (pour créer un arc)
    private Tache  tacheEnDeplacement = null;  // qu’on est en train de glisser
    private Point  lastDragPoint      = null;  // pour le déplacement global (pan)

    private double zoom       = 1.0;           // zoom courant
    private int    translateX = 0, translateY = 0; // décalage global

    /* =====================================================
                      --- constructeur ---
       ===================================================== */
    public DrawingPanel() {
        setBackground(Color.WHITE);

        // deux tâches fixes
        taches.add(new Tache("Début", 0, 100, 50));
        taches.add(new Tache("Fin",   0, 600, 400));

        /* ========== écouteurs souris ========== */
        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                Point2D p = convertToGraphCoordinates(e.getPoint());
                int x = (int) p.getX(), y = (int) p.getY();
                tacheEnDeplacement = getTacheProche(x, y);
                if (tacheEnDeplacement == null) {           // pas sur un nœud → on prépare le pan
                    lastDragPoint = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                tacheEnDeplacement = null;
                lastDragPoint      = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                Point2D p = convertToGraphCoordinates(e.getPoint());
                int x = (int) p.getX(), y = (int) p.getY();

                /* ------------ clic gauche : création tâche / arc ------------ */
                if (SwingUtilities.isLeftMouseButton(e)) {
                    Tache clique = getTacheProche(x, y);

                    /* A/ vide → on crée une tâche */
                    if (clique == null) {
                        Tache nouvelle = demanderInfosTache(x, y);
                        if (nouvelle != null) {
                            taches.add(nouvelle);
                            repaint();
                        }
                        return;
                    }

                    /* B/ on a cliqué sur une tâche */
                    if (tacheSelectionne == null) {      // première extrémité
                        tacheSelectionne = clique;
                    } else {                             // deuxième extrémité
                        if (!tacheSelectionne.equals(clique)) {
                            boolean arcInverseExiste = arcs.stream()
                                    .anyMatch(a -> a.getOrigine() == clique
                                                && a.getDestination() == tacheSelectionne);
                            if (arcInverseExiste) {
                                JOptionPane.showMessageDialog(
                                        DrawingPanel.this,
                                        "Un arc dans l'autre sens existe déjà.",
                                        "Arc inverse interdit",
                                        JOptionPane.WARNING_MESSAGE);
                            } else {
                                arcs.add(new Arc(tacheSelectionne, clique));
                            }
                        }
                        tacheSelectionne = null;
                        repaint();
                    }
                }

                /* ------------ clic droit : menus contextuels ------------ */
                else if (SwingUtilities.isRightMouseButton(e)) {
                    Tache surTache = getTacheProche(x, y);
                    if (surTache != null) {
                        afficherMenuClicDroit(surTache, x, y);
                    } else {
                        Arc a = getArcProche(x, y);
                        if (a != null) afficherMenuClicDroitArc(a, x, y);
                    }
                }
            }
        });

        /* ========== drag (déplacement tâches ou pan) ========== */
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (tacheEnDeplacement != null) {
                    Point2D p = convertToGraphCoordinates(e.getPoint());
                    tacheEnDeplacement.setPosition((int)p.getX(), (int)p.getY());
                    repaint();
                } else if (lastDragPoint != null) {
                    translateX += e.getX() - lastDragPoint.x;
                    translateY += e.getY() - lastDragPoint.y;
                    lastDragPoint = e.getPoint();
                    repaint();
                }
            }
        });

        /* ========== molette (zoom) ========== */
        addMouseWheelListener(e -> {
            zoom *= (e.getPreciseWheelRotation() < 0) ? 1.1 : 1/1.1;
            repaint();
        });
    }

    /* =====================================================
                --- utilitaires géométriques ---
       ===================================================== */

    /** point sur le bord du rectangle depuis son centre vers (bx,by) */
    private Point2D intersectionRect(double cx,double cy,double hw,double hh,
                                     double bx,double by) {
        double dx = bx - cx, dy = by - cy;
        double scale = 1.0 / Math.max(Math.abs(dx)/hw, Math.abs(dy)/hh);
        return new Point2D.Double(cx + dx*scale, cy + dy*scale);
    }

    /* dist point → segment classique */
    private double distancePointSegment(int px,int py,int x1,int y1,int x2,int y2){
        double dx = x2-x1, dy = y2-y1;
        if (dx==0 && dy==0) return Math.hypot(px-x1, py-y1);
        double t = ((px-x1)*dx + (py-y1)*dy)/(dx*dx+dy*dy);
        t = Math.max(0,Math.min(1,t));
        double projX = x1 + t*dx, projY = y1 + t*dy;
        return Math.hypot(px-projX, py-projY);
    }

    /* =====================================================
                       --- détection ---
       ===================================================== */
    private Tache getTacheProche(int x,int y){
        Graphics2D g2=(Graphics2D)getGraphics();
        for (Tache t:taches){
            FontMetrics fm=g2.getFontMetrics();
            int hw = fm.stringWidth(t.getNom())/2 + 20;
            int hh = 20;
            if (x>=t.getX()-hw && x<=t.getX()+hw &&
                y>=t.getY()-hh && y<=t.getY()+hh) return t;
        }
        return null;
    }

    private Arc getArcProche(int x,int y){
        final int SEUIL=6;
        for (Arc a:arcs){
            if (distancePointSegment(x,y,
                    a.getOrigine().getX(),a.getOrigine().getY(),
                    a.getDestination().getX(),a.getDestination().getY())<SEUIL)
                return a;
        }
        return null;
    }

    /* =====================================================
                        --- I/O helpers ---
       ===================================================== */
    private Point2D convertToGraphCoordinates(Point p){
        try{
            AffineTransform at=new AffineTransform();
            at.translate(translateX,translateY);
            at.scale(zoom,zoom);
            return at.inverseTransform(p,null);
        }catch(NoninvertibleTransformException ex){
            ex.printStackTrace(); return p;
        }
    }

    /* ================= menus contextuels ================= */
    private void afficherMenuClicDroit(Tache t,int x,int y){
        JPopupMenu menu=new JPopupMenu();
        if (!t.getNom().equalsIgnoreCase("Début") &&
            !t.getNom().equalsIgnoreCase("Fin")){
            JMenuItem mod=new JMenuItem("Modifier");
            mod.addActionListener(_->{ modifierTache(t); repaint(); });
            JMenuItem sup=new JMenuItem("Supprimer");
            sup.addActionListener(_->{
                arcs.removeIf(a->a.getOrigine()==t||a.getDestination()==t);
                taches.remove(t); repaint();
            });
            menu.add(mod); menu.add(sup);
        }else{
            JMenuItem info=new JMenuItem("Tâche protégée"); info.setEnabled(false);
            menu.add(info);
        }
        menu.show(this,x,y);
    }
    private void afficherMenuClicDroitArc(Arc a,int x,int y){
        JPopupMenu menu=new JPopupMenu();
        JMenuItem sup=new JMenuItem("Supprimer l'arc");
        sup.addActionListener(_->{arcs.remove(a); repaint();});
        menu.add(sup); menu.show(this,x,y);
    }

    /* ========== boîte de dialogue création/modification ========== */
    private Tache demanderInfosTache(int x,int y){
        JTextField nomF=new JTextField();
        JTextField durF=new JTextField();
        JPanel p=new JPanel(new GridLayout(2,2));
        p.add(new JLabel("Nom :"));   p.add(nomF);
        p.add(new JLabel("Durée :")); p.add(durF);
        int res=JOptionPane.showConfirmDialog(this,p,"Nouvelle tâche",
                JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(res==JOptionPane.OK_OPTION){
            try{
                String nom=nomF.getText().trim();
                int d=Integer.parseInt(durF.getText().trim());
                if(!nom.isEmpty()) return new Tache(nom,d,x,y);
            }catch(NumberFormatException e){
                JOptionPane.showMessageDialog(this,"Durée invalide");
            }
        }
        return null;
    }
    private void modifierTache(Tache t){
        JTextField nomF=new JTextField(t.getNom());
        JTextField durF=new JTextField(String.valueOf(t.getDuree()));
        JPanel p=new JPanel(new GridLayout(2,2));
        p.add(new JLabel("Nom :"));   p.add(nomF);
        p.add(new JLabel("Durée :")); p.add(durF);
        int res=JOptionPane.showConfirmDialog(this,p,"Modifier tâche",
                JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(res==JOptionPane.OK_OPTION){
            try{
                String nom=nomF.getText().trim();
                int d=Integer.parseInt(durF.getText().trim());
                if(!nom.isEmpty()){ t.setNom(nom); t.setDuree(d); }
            }catch(NumberFormatException e){
                JOptionPane.showMessageDialog(this,"Durée invalide");
            }
        }
    }
    /** dessine un triangle plein orienté vers (x2,y2) */
    private void drawArrowHead(Graphics2D g2,
                               int x1,int y1,   // point de départ (pour l’angle)
                               int x2,int y2)   // pointe de la flèche
    {
        final int ARROW_SIZE = 10;
        double dx = x2 - x1, dy = y2 - y1;
        double angle = Math.atan2(dy, dx);

        Polygon arrow = new Polygon();
        arrow.addPoint(0,0);
        arrow.addPoint(-ARROW_SIZE, -ARROW_SIZE/2);
        arrow.addPoint(-ARROW_SIZE,  ARROW_SIZE/2);

        AffineTransform old = g2.getTransform();
        g2.translate(x2, y2);
        g2.rotate(angle);
        g2.fill(arrow);
        g2.setTransform(old);
    }


    /* =====================================================
                         --- dessin ---
       ===================================================== */
    @Override protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D)g;
        g2.translate(translateX,translateY);
        g2.scale(zoom,zoom);
        g2.setStroke(new BasicStroke(2));

        /* --- arcs --- */
        g2.setColor(Color.BLACK);
        for (Arc a:arcs){
            Tache o=a.getOrigine(), d=a.getDestination();
            int x1=o.getX(), y1=o.getY(), x2=d.getX(), y2=d.getY();

            FontMetrics fm=g2.getFontMetrics();
            int hw1=fm.stringWidth(o.getNom())/2+20, hh1=20;
            int hw2=fm.stringWidth(d.getNom())/2+20, hh2=20;

            Point2D p1=intersectionRect(x1,y1,hw1,hh1,x2,y2);
            Point2D p2=intersectionRect(x2,y2,hw2,hh2,x1,y1);

            int ax1=(int)p1.getX(), ay1=(int)p1.getY();
            int ax2=(int)p2.getX(), ay2=(int)p2.getY();

            g2.drawLine(ax1,ay1,ax2,ay2);
            drawArrowHead(g2,ax1,ay1,ax2,ay2);

            /* durée affichée au milieu */
            String txt=String.valueOf(o.getDuree());
            Font origF=g2.getFont();
            g2.setFont(origF.deriveFont(Font.BOLD,origF.getSize()+4));
            int lw=fm.stringWidth(txt);
            int xm=(ax1+ax2)/2, ym=(ay1+ay2)/2;

            g2.setColor(Color.WHITE);
            for(int dx=-2;dx<=2;dx++) for(int dy=-2;dy<=2;dy++)
                if(dx!=0||dy!=0) g2.drawString(txt,xm-lw/2+dx,ym+dy);
            g2.setColor(Color.BLACK);
            g2.drawString(txt,xm-lw/2,ym);
            g2.setFont(origF);
        }

        /* --- tâches (rectangles) --- */
        for (Tache t:taches){
            FontMetrics fm=g2.getFontMetrics();
            int hw=fm.stringWidth(t.getNom())/2+20, hh=20;

            g2.setColor(t.getNom().equalsIgnoreCase("Début")||
                        t.getNom().equalsIgnoreCase("Fin") ? Color.RED:Color.BLUE);
            g2.fillRect(t.getX()-hw, t.getY()-hh, hw*2, hh*2);

            g2.setColor(Color.WHITE);
            g2.drawString(t.getNom(),
                    t.getX()-fm.stringWidth(t.getNom())/2,
                    t.getY()+fm.getAscent()/2-2);

            String duree="Durée : "+t.getDuree();
            g2.setColor(Color.BLACK);
            g2.drawString(duree,
                    t.getX()-fm.stringWidth(duree)/2,
                    t.getY()+hh+15);
        }
    }

    /* =====================================================
                --- sauvegarde / chargement ---
       ===================================================== */
    public void sauvegarderTachesEtTransitions(File f){
        try(PrintWriter w=new PrintWriter(f)){
            w.println("TACHES");
            for(Tache t:taches)
                w.println(t.getNom()+";"+t.getX()+";"+t.getY()+";"+t.getDuree());

            w.println("TRANSITIONS");
            for(Arc a:arcs)
                w.println(a.getOrigine().getNom()+";"+a.getDestination().getNom());

            JOptionPane.showMessageDialog(this,"Sauvegarde OK");
        }catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,"Erreur sauvegarde");
        }
    }
    public void chargerTachesEtTransitions(File f){
        try(BufferedReader r=new BufferedReader(new FileReader(f))){
            taches.clear(); arcs.clear();
            Map<String,Tache> map=new HashMap<>();
            String line; boolean lt=false, lr=false;
            while((line=r.readLine())!=null){
                if(line.equalsIgnoreCase("TACHES")){lt=true;lr=false;continue;}
                if(line.equalsIgnoreCase("TRANSITIONS")){lt=false;lr=true;continue;}
                if(lt){
                    String[] p=line.split(";");
                    if(p.length>=4){
                        Tache t=new Tache(p[0],Integer.parseInt(p[3]),
                                          Integer.parseInt(p[1]),Integer.parseInt(p[2]));
                        taches.add(t); map.put(p[0],t);
                    }
                }else if(lr){
                    String[] p=line.split(";");
                    if(p.length>=2){
                        Tache o=map.get(p[0]), d=map.get(p[1]);
                        if(o!=null&&d!=null) arcs.add(new Arc(o,d));
                    }
                }
            }
            repaint();
            JOptionPane.showMessageDialog(this,"Chargement OK");
        }catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,"Erreur chargement");
        }
    }
}