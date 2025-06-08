package view;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


@SuppressWarnings("unused")
public class MainWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    
    // Le panneau principal de dessin du graphe
    private DrawingPanel drawingPanel;

    public MainWindow() {
        setTitle("Graphe de Projet");
        setSize(800, 600);
        setLocationRelativeTo(null); // Centrer la fenêtre
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Confirmation à la fermeture
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                String[] options = {"Oui", "Non", "Annuler"};
                int choix = JOptionPane.showOptionDialog(
                    MainWindow.this,
                    "Voulez-vous enregistrer le graphe avant de quitter ?",
                    "Quitter l'application",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]
                );

                if (choix == 0) { // Oui
                    JFileChooser chooser = new JFileChooser();
                    if (chooser.showSaveDialog(MainWindow.this) == JFileChooser.APPROVE_OPTION) {
                        drawingPanel.sauvegarderTachesEtTransitions(chooser.getSelectedFile());
                        dispose();
                    }
                } else if (choix == 1) { // Non
                    dispose();
                }
                // Si choix == 2 (Annuler) ou fermeture de la boîte → ne rien faire
            }
        });


        // Initialisation du panneau de dessin
        drawingPanel = new DrawingPanel();
        add(drawingPanel);

        // Barre de menu (Sauvegarde/Chargement)
        JMenuBar menuBar = new JMenuBar();
        JMenu fichierMenu = new JMenu("Fichier");

        JMenuItem saveItem = new JMenuItem("Sauvegarder");
        saveItem.addActionListener(_ -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                drawingPanel.sauvegarderTachesEtTransitions(chooser.getSelectedFile());
            }
        });

        JMenuItem loadItem = new JMenuItem("Charger");
        loadItem.addActionListener(_ -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                drawingPanel.chargerTachesEtTransitions(chooser.getSelectedFile());
            }
        });

        fichierMenu.add(saveItem);
        fichierMenu.add(loadItem);
        menuBar.add(fichierMenu);
        setJMenuBar(menuBar);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainWindow fenetre = new MainWindow();
            fenetre.setVisible(true);
        });
    }
}
