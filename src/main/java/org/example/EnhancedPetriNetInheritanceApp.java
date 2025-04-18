package org.example;

import org.example.objects.PetriNet;
import org.example.services.PetriNetUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

import static org.example.services.PetriNetUtils.determineInheritanceType;

public class EnhancedPetriNetInheritanceApp {

    private JFrame frame;
    private JTextField parentField;
    private JTextField childField;
    private JLabel resultLabel;

    private File parentFile;
    private File childFile;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EnhancedPetriNetInheritanceApp::new);
    }

    public EnhancedPetriNetInheritanceApp() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        // 2) Hlavné okno
        frame = new JFrame("Petri Net Inheritance Analyzer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setMinimumSize(new Dimension(600, 400));
        frame.setLocationRelativeTo(null); // centrovanie na obrazovke

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(20, 20, 20, 20));
        content.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel title = new JLabel("Petri Net Inheritance Analyzer", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(new Color(0x1e88e5));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        content.add(title, gbc);

        // ---- Parent ---------------------------------------------------------
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        content.add(new JLabel("Parent Net:"), gbc);

        parentField = new JTextField();
        parentField.setEditable(false);
        gbc.gridx = 1;
        gbc.weightx = 1;
        content.add(parentField, gbc);

        JButton parentBtn = createBrowseButton("Parent");
        parentBtn.addActionListener(e -> chooseFile(true));
        gbc.gridx = 2;
        gbc.weightx = 0;
        content.add(parentBtn, gbc);

        // ---- Child ----------------------------------------------------------
        gbc.gridy = 2;
        gbc.gridx = 0;
        content.add(new JLabel("Child Net:"), gbc);

        childField = new JTextField();
        childField.setEditable(false);
        gbc.gridx = 1;
        gbc.weightx = 1;
        content.add(childField, gbc);

        JButton childBtn = createBrowseButton("Child");
        childBtn.addActionListener(e -> chooseFile(false));
        gbc.gridx = 2;
        gbc.weightx = 0;
        content.add(childBtn, gbc);

        // ---- Analyze button -------------------------------------------------
        JButton analyzeBtn = new JButton("Analyze Inheritance");
        analyzeBtn.setFont(analyzeBtn.getFont().deriveFont(Font.BOLD, 14f));
        analyzeBtn.addActionListener(e -> analyze());
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        content.add(analyzeBtn, gbc);

        // ---- Result label ---------------------------------------------------
        resultLabel = new JLabel("Result: ");
        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.PLAIN, 16f));
        resultLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        content.add(resultLabel, gbc);

        frame.setContentPane(content);
        frame.setVisible(true);
    }

    private JButton createBrowseButton(String tooltip) {
        JButton btn = new JButton("Browse…");
        btn.setToolTipText("Choose " + tooltip + " file");
        return btn;
    }

    private void chooseFile(boolean isParent) {
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(frame);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (isParent) {
                parentFile = f;
                parentField.setText(f.getAbsolutePath());
            } else {
                childFile = f;
                childField.setText(f.getAbsolutePath());
            }
        }
    }

    private void analyze() {
        if (parentFile == null || childFile == null) {
            resultLabel.setText("Result: Please choose both files.");
            return;
        }
        try {
            PetriNet parent = PetriNetUtils.loadPetriNet(parentFile);
            PetriNet child = PetriNetUtils.loadPetriNet(childFile);
            String result = determineInheritanceType(parent, child);
            resultLabel.setText("Result: " + result);
        } catch (Exception ex) {
            resultLabel.setText("No inheritance");
            ex.printStackTrace();
        }
    }
}
