package org.example;

import guru.nidi.graphviz.engine.*;
import org.example.objects.PetriNet;
import org.example.services.PetriNetUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

import static org.example.services.PetriNetUtils.determineInheritanceType;


public class EnhancedPetriNetInheritanceApp {

    private File parentFile;
    private File childFile;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EnhancedPetriNetInheritanceApp::new);
    }

    public EnhancedPetriNetInheritanceApp() {
        JFrame frame = new JFrame("Petri Net Inheritance Analyzer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));

        JLabel titleLabel = new JLabel("Petri Net Inheritance Analyzer", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel fileSelectionPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        fileSelectionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Parent File Selection
        JPanel parentPanel = new JPanel(new BorderLayout(5, 5));
        JLabel parentLabel = new JLabel("Parent Net File:");
        JTextField parentField = new JTextField();
        parentField.setEditable(false);
        JButton parentButton = new JButton("Choose Parent File");

        parentButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                parentFile = fileChooser.getSelectedFile();
                parentField.setText(parentFile.getAbsolutePath());
            }
        });

        parentPanel.add(parentLabel, BorderLayout.WEST);
        parentPanel.add(parentField, BorderLayout.CENTER);
        parentPanel.add(parentButton, BorderLayout.EAST);

        // Child File Selection
        JPanel childPanel = new JPanel(new BorderLayout(5, 5));
        JLabel childLabel = new JLabel("Child Net File:");
        JTextField childField = new JTextField();
        childField.setEditable(false);
        JButton childButton = new JButton("Choose Child File");

        childButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                childFile = fileChooser.getSelectedFile();
                childField.setText(childFile.getAbsolutePath());
            }
        });

        childPanel.add(childLabel, BorderLayout.WEST);
        childPanel.add(childField, BorderLayout.CENTER);
        childPanel.add(childButton, BorderLayout.EAST);

        fileSelectionPanel.add(parentPanel);
        fileSelectionPanel.add(childPanel);

        // Analyze Button and Result Display
        JPanel analyzePanel = new JPanel(new BorderLayout(10, 10));
        JButton analyzeButton = new JButton("Analyze Inheritance");
        JLabel resultLabel = new JLabel("Result: ", SwingConstants.CENTER);
        resultLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        resultLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        analyzeButton.addActionListener(e -> {
            if (parentFile == null || childFile == null) {
                resultLabel.setText("Result: Please select both files.");
                return;
            }

            try {
                // Load Petri nets (old way using java.io.File)
                PetriNet parentNet = PetriNetUtils.loadPetriNet(parentFile);
                PetriNet childNet = PetriNetUtils.loadPetriNet(childFile);

                // Determine inheritance type
                String result = determineInheritanceType(parentNet, childNet);
                resultLabel.setText("Result: " + result);

            } catch (Exception ex) {
                resultLabel.setText("Error: " + ex.getMessage());
                ex.printStackTrace(); // Optionally log the error stack trace for debugging
            }
        });

        analyzePanel.add(analyzeButton, BorderLayout.NORTH);
        analyzePanel.add(resultLabel, BorderLayout.CENTER);

        mainPanel.add(fileSelectionPanel, BorderLayout.CENTER);
        mainPanel.add(analyzePanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);
    }


}
